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

object bugPicker extends JFXApp {

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
    stage = new PrimaryStage {
        title = "Bug Picker"
        scene = drawScene()
        def drawScene() = {
            val thisScene = new Scene() {
                root = new BorderPane {
                    top = new VBox {
                        content = createViews()
                    }
                }
            }
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
                                        resultWebview.engine.load("Please wait until all analyses are cancelled")
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

        def setupListenersOnFinish() {
            val loadWorker = resultWebview.engine.delegate.getLoadWorker
            val listener: ChangeListener[State] = new ChangeListener[State] {
                override def changed(
                    observable: ObservableValue[_ <: State],
                    oldValue: State,
                    newValue: State) {

                    val document = resultWebview.engine.document
                    val nodes = document.getElementsByTagName("td")

                    def splitParameters(parameters: String): Map[String, String] = {
                        var map = Map[String, String]()
                        parameters.split("&").foreach { pair ⇒
                            val Array(key, value) = pair.split("=", 2)
                            map += key -> value
                        }
                        map
                    }

                    def listener(node: org.w3c.dom.Node) = new org.w3c.dom.events.EventListener {
                        override def handleEvent(event: org.w3c.dom.events.Event) {
                            val sourceValue = node.getAttributes.getNamedItem("data-source").getTextContent
                            val parameters = splitParameters(sourceValue)
                            val sourceType = ObjectType(parameters("class"))
                            val sourceClassFile = project.source(sourceType).map { url ⇒
                                val inStream = url.openStream
                                val cf = org.opalj.da.ClassFileReader.ClassFile(() ⇒ inStream)
                                inStream.close
                                cf.head
                            }
                            if (sourceClassFile.isDefined) {
                                val methodOption = parameters.get("method")
                                val pcOption = parameters.get("pc")
                                val sourceDoc = sourceClassFile.get.toXHTML
                                sourceWebview.engine.loadContent(sourceDoc.toString)
                                val worker = sourceWebview.engine.delegate.getLoadWorker
                                val listener: ChangeListener[State] = new ChangeListener[State] {
                                    override def changed(
                                        observable: ObservableValue[_ <: State],
                                        oldValue: State,
                                        newValue: State) {

                                        def run(s: String) { sourceWebview.engine.delegate.executeScript(s) }
                                        if (methodOption.isDefined) {
                                            val index = methodOption.get
                                            val openMethodsBlock = "document.getElementsByClassName('methods')[0].getElementsByTagName('details')[0].open = true;"
                                            val openMethod = s"document.getElementById('m$index').getElementsByTagName('details')[0].open = true;"
                                            val scrollToTarget =
                                                if (pcOption.isDefined) {
                                                    val pc = pcOption.get
                                                    s"document.getElementById('m${index}_pc$pc').scrollIntoView();"
                                                } else {
                                                    s"document.getElementById('m$index').scrollIntoView();"
                                                }

                                            run(openMethodsBlock)
                                            run(openMethod)
                                            run(scrollToTarget)
                                        } else {
                                            val scrollToTop = "window.scrollTo(0,0);"
                                            run(scrollToTop)
                                        }
                                        worker.stateProperty.removeListener(this)
                                    }
                                }
                                worker.stateProperty.addListener(listener)
                            }
                        }
                    }

                    for {
                        i ← (0 to nodes.getLength)
                        node = nodes.item(i)
                        if node != null && node.getAttributes() != null &&
                            node.getAttributes().getNamedItem("data-source") != null
                    } {
                        val eventTarget = node.asInstanceOf[org.w3c.dom.events.EventTarget]
                        eventTarget.addEventListener("click", listener(node), false)
                    }

                    loadWorker.stateProperty.removeListener(this)
                }
            }
            loadWorker.stateProperty.addListener(listener)
        }

        def runAnalysis(files: List[java.io.File]) {
            val et = WorkerStateEvent.ANY
            Worker.handleEvent(et) {
                event: WorkerStateEvent ⇒
                    {
                        event.eventType match {
                            case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                                resultWebview.engine.loadContent(doc.toString)
                                setupListenersOnFinish()
                            }
                            case WorkerStateEvent.WORKER_STATE_RUNNING ⇒ {
                                val loadingURL = getClass.getResource("/cat_loading.gif").toURI().toURL()
                                resultWebview.engine.load(loadingURL.toString())
                            }
                            case _default ⇒ {
                                println(event.eventType.toString)
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
                                    resultWebview.engine.load("Please use File menu to add some class files to analyse first")
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
                    sourceDir = files(1)
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

        val downSplitpane = new SplitPane {}
        downSplitpane.getItems().addAll(resultWebview, sourceWebview)

        List(menuBar, infoText, downSplitpane)
    }

    /**
     * The open File dialog box. Preferrebly this should change into a dialog box
     *  instead of a whole stage and scene
     */
    def loadProjectStage: List[List[File]] = {
        var jars: List[java.io.File] = List()
        var sources: java.io.File = null
        var cancelled = false
        val listview = new ListView[String]()
        val outStage = new Stage {
            outer ⇒
            title = "Load project files"
            width = 800
            height = 400
            scene = new Scene {
                root = new BorderPane {
                    //top = 
                    val vbox = new VBox()
                    vbox.content = {
                        //content = 
                        List(
                            new Label {
                                text = "Select files(jars/.class/directory) to be analysed"
                            },
                            new Button {
                                id = "Select a Jar/Class File"
                                text = "Add Jar/Class Files"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        val fcb = new FileChooser {
                                            title = "Open Dialog"
                                        }
                                        fcb.extensionFilters.addAll(
                                            new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                            new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                        val file = fcb.showOpenDialog(vbox.getScene().getWindow())
                                        if (file != null) {
                                            jars :::= List(file)
                                            listview.items() += jars(0).toString()
                                        }

                                    }
                                }
                            },
                            new Label {
                                text = "Choose the  class directory containing jar of class files"
                            },
                            new Button {
                                text = "Open"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        val dc = new DirectoryChooser {
                                            title = "Select Directory"
                                        }
                                        val file = dc.showDialog(vbox.getScene().window())
                                        if (file != null) {

                                            jars :::= List(file)
                                            listview.items() += jars(0).toString()
                                        }
                                    }
                                }
                            },
                            new Label {
                                text = "select the source directory of your project"
                            },

                            new Button {
                                text = "Open"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        val dc = new DirectoryChooser {
                                            title = "Open Dialog"
                                        }
                                        val file = dc.showDialog(vbox.getScene().window())
                                        if (file != null) {
                                            sources = file
                                            listview.items() += sources.toString()
                                        }
                                    }
                                }
                            },

                            listview,

                            new Button {
                                text = "Cancel"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        cancelled = true
                                        outer.close
                                    }
                                }

                            },
                            new Button {
                                text = "Finish"
                                onAction = { e: ActionEvent ⇒
                                    {

                                        outer.close()
                                    }
                                }

                            }

                        )

                    }
                    top = vbox

                }
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
