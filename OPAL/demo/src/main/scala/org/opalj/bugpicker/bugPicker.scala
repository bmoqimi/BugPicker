package org.opalj
package bugpicker

import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.geometry.Insets
import scalafx.scene.layout.{ BorderPane, VBox }
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.control.{ TitledPane, TextArea, ToolBar, Button }
import scalafx.scene.web.WebView
import scalafx.scene.control.{ MenuBar, Menu, MenuItem }
import scalafx.event.{ Event, ActionEvent }
import scalafx.scene.control.SplitPane
import scalafx.Includes._
import scalafx.scene.control.Label
import scalafx.stage.FileChooser
import scalafx.stage.DirectoryChooser
import scalafx.stage.Stage
import org.opalj.bugpicker.DeadCode._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.OutputStream
import scala.concurrent.FutureTaskRunner
import java.util.concurrent.FutureTask
import org.opalj.br.analyses._
import org.opalj.ai.debug.XHTML
import org.opalj.br.analyses.BasicReport
import org.opalj.util.PerformanceEvaluation._
import scalafx.concurrent.Task
import javafx.{ concurrent ⇒ jfxc }
import scalafx.concurrent.Service
import scala.xml.Node
import scalafx.event.EventType
import scalafx.event.EventHandler
import scalafx.concurrent.WorkerStateEvent
import java.util.EventListener
import scalafx.concurrent.WorkerStateEvent
import javafx.collections.ObservableList
import java.io.File
import java.net.URL
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.ListView
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses._
import org.opalj.br.analyses.{ EventType ⇒ ET }
import scala.collection.mutable.HashMap
import scalafx.application.Platform
import scalafx.scene.input.KeyCombination
import org.opalj.br.ObjectType
import javafx.concurrent.Worker
import javafx.concurrent.Worker.State
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.Priority
import scalafx.scene.layout.ColumnConstraints

object bugPicker extends JFXApp {
    final val MESSAGE_ANALYSIS_RUNNING =
        <html>
            <h1>Analysis is running, please wait...</h1>
        </html>.toString
    final val MESSAGE_GENERATING_REPORT =
        <html>
            <h1>Analysis is finished.<br/>Generating Report, please wait...</h1>
        </html>.toString

    object ae extends AnalysisExecutor {
        val deadCodeAnalysis = new DeadCodeAnalysis
        lazy val analysis = new Analysis[URL, BasicReport] {
            override def title: String = deadCodeAnalysis.title

            override def description: String = deadCodeAnalysis.description

            override def analyze(theProject: Project[URL], parameters: Seq[String], initProgressManagement: (Int) ⇒ ProgressManagement) = {
                val results @ (analysisTime, methodsWithDeadCode) =
                    deadCodeAnalysis.analyze(theProject, parameters, initProgressManagement)

                val doc = XHTML.createXHTML(Some(title), DeadCodeAnalysis.resultsAsXHTML(results))

                BasicReport(
                    "Dead code (number of dead branches: "+methodsWithDeadCode.size+") "+
                        f"identified in: ${ns2sec(analysisTime)}%2.2f seconds."
                )
            }
        }
    }
    val downSplitpane = new SplitPane {}
    stage = new PrimaryStage {
        title = "Bug Picker"
        scene = drawScene()
        def drawScene() = {
            val thisScene = new Scene() {}
            val gridpane = new GridPane {

            }
            val vbox = new VBox {
                content = createViews()
            }
            VBox.setVgrow(downSplitpane, Priority.ALWAYS)
            //vbox.children.addAll(createViews)
            GridPane.setVgrow(vbox, Priority.ALWAYS)
            GridPane.setHgrow(vbox, Priority.ALWAYS)
            gridpane.children.add(vbox)
            gridpane.prefHeight = Double.MaxValue

            thisScene.root = gridpane

            thisScene
        }
    }
    var loadedFiles: List[List[File]] = null
    var sourceDir: java.io.File = null
    var project: Project[URL] = null
    var analysisDisabled = true

    def createViews() = {

        val infoText = new TextArea {
            text = "Please use the toolbar to load the project"
        }
        val resultWebview = new WebView()
        val sourceWebview = new WebView()
        var files: List[java.io.File] = List()
        var doc: Node = null
        var br: BasicReport = null
        var interuptAnalysis = false
        var initProgressManagement: (Int) ⇒ ProgressManagement = null

        val progressListView = new ListView[String]()
        val progressListItems = new HashMap[String, String]()
        var cancelled = false

        lazy val progStage = new Stage {
            outer ⇒
            title = "Analysis Progress "
            width = 800
            height = 600
            scene = new Scene {
                root = new BorderPane {
                    val vbox = new VBox()
                    vbox.content = {
                        List(
                            new Label {
                                text = "Click here to Interupt all Analyses"
                            },
                            new Button {
                                id = "Cancel"
                                text = "Cancel"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        cancelled = true
                                        interuptAnalysis = true
                                        resultWebview.engine.loadContent("Please wait until all analyses are cancelled")
                                        outer.close
                                    }
                                }
                            },
                            progressListView
                        )
                    }
                    top = vbox
                }
            }
        }

        def showProgressManagement(): Boolean = {
            cancelled = false
            interuptAnalysis = false
            progStage.showAndWait
            cancelled
        }
        val deadCodeAnalysis = new DeadCodeAnalysis
        initProgressManagement = (x) ⇒ new ProgressManagement {

            final def progress(step: Int, evt: ET.Value, msg: Option[String]): Unit = evt match {
                case ET.Start ⇒ {
                    Platform.runLater(new Runnable() {
                        override def run() {
                            progressListView.items() += step.toString+": "+msg.get
                            progressListItems += ((step.toString, msg.get))
                            progressListView.scrollTo(progressListView.getItems.size() - 1)
                        }
                    }
                    )
                }
                case ET.End ⇒ {
                    Platform.runLater(new Runnable() {
                        override def run() {
                            progressListView.items() -= step.toString+": "+progressListItems.get(step.toString).get
                            progressListItems.remove(step.toString)
                            if (progressListItems.isEmpty)
                                progStage.close
                        }
                    })
                }
            }

            final def isInterrupted: Boolean = interuptAnalysis
        }

        //object Worker extends Task(new jfxc.Task[String] {
        object Worker extends Service(new jfxc.Service[String]() {

            protected def createTask(): jfxc.Task[String] = new jfxc.Task[String] {
                protected def call(): String = {
                    val results @ (analysisTime, methodsWithDeadCode) = deadCodeAnalysis.analyze(project, Seq.empty, initProgressManagement)
                    doc = XHTML.createXHTML(Some(deadCodeAnalysis.title), DeadCodeAnalysis.resultsAsXHTML(results))
                    br = BasicReport(
                        methodsWithDeadCode.toList.sortWith((l, r) ⇒
                            l.classFile.thisType < r.classFile.thisType ||
                                (l.classFile.thisType == r.classFile.thisType && (
                                    l.method < r.method || (
                                        l.method == r.method &&
                                        l.ctiPC < r.ctiPC
                                    )
                                ))
                        ).mkString(
                            "Dead code (number of dead branches: "+methodsWithDeadCode.size+"): \n",
                            "\n",
                            f"%nIdentified in: ${ns2sec(analysisTime)}%2.2f seconds."))
                    br.message

                }

            }
        })

        def runAnalysis(files: List[java.io.File]) {
            val et = WorkerStateEvent.ANY
            Worker.handleEvent(et) {
                event: WorkerStateEvent ⇒
                    {
                        event.eventType match {
                            case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                                resultWebview.engine.loadContent(MESSAGE_GENERATING_REPORT)
                                resultWebview.engine.loadContent(doc.toString)
                                val l = new AddClickListenersOnLoadListener(project, sourceDir, resultWebview, sourceWebview)
                            }
                            case WorkerStateEvent.WORKER_STATE_RUNNING ⇒ {
                                resultWebview.engine.loadContent(MESSAGE_ANALYSIS_RUNNING)
                            }
                            case _default ⇒ {
                                resultWebview.engine.loadContent(event.eventType.toString)

                            }
                        }

                    }

            }
            interuptAnalysis = false
            //val thread = new Thread(Worker)
            //thread.setDaemon(true)
            //thread.start()
            Worker.restart
            showProgressManagement
        }
        val analyseButton = new Menu("Analysis") {
            items = List(
                new MenuItem("run") {
                    accelerator = KeyCombination("Ctrl+r")
                    disable = analysisDisabled
                    onAction = {
                        e: ActionEvent ⇒
                            {
                                if (!analysisDisabled)
                                    runAnalysis(files)
                                else {
                                    resultWebview.engine.loadContent("Please use File menu to add some class files to analyse first")
                                    println("ERROR: Please use File menu to add some class files to analyse first")
                                }
                            }
                    }
                }
            )
        }
        /**
         * * We update the text area after the user finishes choosing source and class folders
         */
        def displayProjectInfo(files: List[List[java.io.File]]) {
            val files = loadedFiles(0)
            val sources = loadedFiles(1)
            infoText.text() = ""
            if (!files.isEmpty) {
                if (files(0) != null) {
                    //project = Project(files(0))
                    project = ae.setupProject(files, List[File]())
                    val iterable = project.statistics.iterator
                    for (item ← iterable) {
                        infoText.text() += item.getKey()+" : "+item.getValue().toString+"\n"
                    }

                    //infoText.text = project
                }
                if (sources(0) != null) {
                    sourceDir = sources(0)
                    infoText.text() += "\n Source directory is set to: "+sourceDir.toString()
                }

            }
        }

        val menuBar = new MenuBar {
            menus = List(
                new Menu("File") {
                    items = List(
                        new MenuItem("Open ") {
                            accelerator = KeyCombination("Ctrl+o")
                            onAction = { e: ActionEvent ⇒
                                {
                                    loadedFiles = loadProjectStage
                                    if (loadedFiles != null)
                                        if (!loadedFiles(0).isEmpty) {
                                            analysisDisabled = false
                                            displayProjectInfo(loadedFiles)
                                        }
                                }
                            }
                        }

                    )
                },
                analyseButton
            )
        }
        downSplitpane.getItems().addAll(resultWebview, sourceWebview)
        List(menuBar, infoText, downSplitpane)
    }

    /**
     * The open File dialog box.
     * The source and class files are added here.
     */
    def loadProjectStage: List[List[File]] = {
        var jars: List[java.io.File] = List()
        var sources: java.io.File = null
        var cancelled = false
        val listview = new ListView[String]()
        val outStage = new Stage {
            outer ⇒ {
                title = "Load project files"
                width = 600
                height = 300
                val loadScene = new Scene
                val gp = new GridPane
                val l1 = new Label
                l1.text = "Select files(jars/.class/directory) to be analysed"
                val jarButton = new Button
                jarButton.id = "Select a Jar/Class File"
                jarButton.text = "Add Jar/Class Files"
                jarButton.onAction = { e: ActionEvent ⇒
                    {
                        val fcb = new FileChooser {
                            title = "Open Dialog"
                        }
                        fcb.extensionFilters.addAll(
                            new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                            new FileChooser.ExtensionFilter("Class Files", "*.class"))
                        val file = fcb.showOpenDialog(gp.getScene().getWindow())
                        if (file != null) {
                            jars :::= List(file)
                            listview.items() += jars(0).toString()
                        }

                    }
                }
                val l2 = new Label
                l2.text = "Choose the  class directory containing jar of class files"
                val dirButton = new Button {}
                dirButton.text = "Open"
                dirButton.onAction = { e: ActionEvent ⇒
                    {
                        val dc = new DirectoryChooser {
                            title = "Select Directory"
                        }
                        val file = dc.showDialog(gp.getScene().window())
                        if (file != null) {

                            jars :::= List(file)
                            listview.items() += jars(0).toString()
                        }
                    }
                }
                val l3 = new Label
                l3.text = "select the source directory of your project"
                val sourceButton = new Button
                sourceButton.text = "Open"
                sourceButton.onAction = { e: ActionEvent ⇒
                    {
                        val dc = new DirectoryChooser {
                            title = "Open Dialog"
                        }
                        val file = dc.showDialog(gp.getScene().window())
                        if (file != null) {
                            sources = file
                            listview.items() += sources.toString()
                        }
                    }
                }
                val cancelButton = new Button
                cancelButton.text = "Cancel"
                cancelButton.onAction = { e: ActionEvent ⇒
                    {
                        cancelled = true
                        outer.close
                    }
                }
                val finishButton = new Button
                finishButton.text = "Finish"
                finishButton.onAction = { e: ActionEvent ⇒
                    {

                        outer.close()
                    }
                }
                //val columnCons1 = new ColumnConstraints(400)
                //val columnCons2 = new ColumnConstraints(200)
                val vbox = new VBox {
                    content = List(jarButton, dirButton, sourceButton)
                }
                GridPane.setHgrow(listview, Priority.ALWAYS)
                GridPane.setHgrow(vbox, Priority.NEVER)
                gp.add(listview, 1, 1, 2, 3)
                //gp.add(jarButton, 3, 1)
                //gp.add(dirButton, 3, 2)
                //gp.add(sourceButton, 3, 3)
                gp.add(vbox, 3, 1, 2, 3)
                gp.add(finishButton, 1, 4)
                gp.add(cancelButton, 2, 4)
                //gp.columnConstraints.addAll(columnCons1, columnCons2)
                loadScene.root = gp
                scene = loadScene

            }
        }
        outStage.showAndWait
        if (cancelled) {
            null
        } else {
            val results = List(jars, List(sources))
            results
        }
    }
}
