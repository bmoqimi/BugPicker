package org.opalj
package bugpicker

import scalafx.application.JFXApp
import scalafx.scene.Scene
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
import scala.io.Source
import scalafx.stage.Modality
import scalafx.stage.StageStyle
import scalafx.scene.layout.StackPane
import scalafx.scene.layout.HBox
import javafx.geometry.Insets
import scalafx.geometry.Pos

object bugPicker extends JFXApp {
    final val MESSAGE_ANALYSIS_RUNNING =
        <html>
            <h1>Running analysis and generating report&hellip;</h1>
        </html>.toString
    final val MESSAGE_ANALYSIS_FINISHED =
        <html>
            <h1>Click on any line to browse the relevant source code or bytecode</h1>
        </html>.toString
    final val MESSAGE_LOADING_FINISHED =
        <html>
            <h1>Now use the menu bar (or Ctrl+R/Cmd+R) to run the analysis</h1>
        </html>.toString
    final val MESSAGE_APP_STARTED =
        <html>
            <h1>Use the menu bar (or Ctrl+O/Cmd+O) load your project</h1>
        </html>.toString
    final val MESSAGE_LOADING_STARTED =
        <html>
            <h1>Please wait while the project is loaded&hellip;</h1>
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
        var progStage: Stage = null

        def progressStage = new Stage {
            outer ⇒
            title = "Analysis Progress "
            width = 800
            height = 600
            scene = new Scene {
                val but = new Button {
                    id = "Cancel"
                    text = "Cancel"
                    minWidth = 80
                    onAction = { e: ActionEvent ⇒
                        {
                            cancelled = true
                            interuptAnalysis = true
                            resultWebview.engine.loadContent("Please wait until all analyses are cancelled")
                            outer.close
                        }
                    }
                }
                root = new BorderPane {
                    center = progressListView
                    bottom = but
                }
                BorderPane.setAlignment(but, Pos.CENTER)
                BorderPane.setMargin(but, new Insets(10))
            }
        }

        def showProgressManagement(): Boolean = {
            progStage = progressStage
            cancelled = false
            interuptAnalysis = false
            progStage.initModality(Modality.WINDOW_MODAL)
            progStage.initOwner(stage.scene.window.value)
            progStage.initStyle(StageStyle.UTILITY)
            progStage.centerOnScreen
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
                    doc = createHTMLReport(results)
                    br = BasicReport(
                        methodsWithDeadCode.toList.sortWith((l, r) ⇒
                            l.classFile.thisType < r.classFile.thisType ||
                                (l.classFile.thisType == r.classFile.thisType && (
                                    l.method < r.method || (
                                        l.method == r.method &&
                                        l.pc < r.pc
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

        def createHTMLReport(results: (Long, Iterable[BugReport])): Node = {
            var report = XHTML.createXHTML(Some(deadCodeAnalysis.title), DeadCodeAnalysis.resultsAsXHTML(results))

            val additionalStyles = process(getClass.getResourceAsStream("report.styles.css")) {
                Source.fromInputStream(_).mkString
            }
            val stylesNode = <style type="text/css">{ scala.xml.Unparsed(additionalStyles) }</style>

            val newHead = <head>{ (report \ "head" \ "_") }{ stylesNode }</head>

            new scala.xml.Elem(report.prefix, report.label, report.attributes, report.scope, false,
                (newHead ++ (report \ "body"): _*))
        }

        def runAnalysis(files: List[java.io.File]) {
            val et = WorkerStateEvent.ANY
            Worker.handleEvent(et) {
                event: WorkerStateEvent ⇒
                    {
                        event.eventType match {
                            case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                                resultWebview.engine.loadContent(doc.toString)
                                sourceWebview.engine.loadContent(MESSAGE_ANALYSIS_FINISHED)
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
            Worker.restart
            showProgressManagement
        }

        val analyseButton = new Menu("_Analysis") {
            mnemonicParsing = true
            items = List(
                new MenuItem("_Run") {
                    mnemonicParsing = true
                    accelerator = KeyCombination("Shortcut+R")
                    disable = analysisDisabled
                    onAction = { e: ActionEvent ⇒
                        if (!analysisDisabled)
                            runAnalysis(files)
                        else {
                            resultWebview.engine.loadContent("Please use File menu to add some class files to analyse first")
                            println("ERROR: Please use File menu to add some class files to analyse first")
                        }
                    }
                }
            )
        }

        /**
         * * We update the text area after the user finishes choosing source and class folders
         */
        def displayProjectInfo(loadedFiles: List[List[java.io.File]]) {
            val files = loadedFiles(0)
            val sources = loadedFiles(1)
            val libs = loadedFiles(2)
            Platform.runLater(infoText.text() = "")
            if (!files.isEmpty) {
                if (files(0) != null) {
                    if (libs(0) == null) {
                        project = ae.setupProject(files, List[File]())
                    } else {
                        project = ae.setupProject(files, libs)
                    }
                    val iterable = project.statistics.iterator
                    for (item ← iterable) {
                        Platform.runLater(infoText.text() += item.getKey()+" : "+item.getValue().toString+"\n")
                    }
                    Platform.runLater(resultWebview.engine.loadContent(MESSAGE_LOADING_FINISHED))
                }
                if (sources(0) != null) {
                    sourceDir = sources(0)
                    Platform.runLater(infoText.text() += "\nSource directory is set to: "+sourceDir.toString())
                }

            }
        }

        val menuBar = new MenuBar {
            menus = List(
                new Menu("_File") {
                    mnemonicParsing = true
                    items = List(
                        new MenuItem("_Open") {
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+O")
                            onAction = { e: ActionEvent ⇒
                                val tempFiles = loadProjectStage
                                if (tempFiles != null && !tempFiles(0).isEmpty) {
                                    analysisDisabled = false
                                    val displayInfo = new Runnable() {
                                        override def run() {
                                            displayProjectInfo(tempFiles)
                                        }
                                    }
                                    resultWebview.engine.loadContent(MESSAGE_LOADING_STARTED)
                                    val infoThread = new Thread(displayInfo)
                                    infoThread.setDaemon(true)
                                    infoThread.start
                                }
                            }
                        })
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
        val dialog = new LoadProjectDialog()
        dialog.show(stage)
    }
}
