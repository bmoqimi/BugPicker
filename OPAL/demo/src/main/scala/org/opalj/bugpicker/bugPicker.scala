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
import scalafx.stage.Screen
import scalafx.scene.control.TabPane
import scalafx.scene.control.Tab
import scalafx.event.subscriptions.Subscription
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.event.EventHandler

object bugPicker extends JFXApp {
    def getMessage(path: String): String = process(getClass.getResourceAsStream(path))(Source.fromInputStream(_).mkString)

    final val MESSAGE_ANALYSIS_RUNNING = getMessage("/org/opalj/bugpicker/messages/analysisrunning.html")
    final val MESSAGE_ANALYSIS_FINISHED = getMessage("/org/opalj/bugpicker/messages/analysisfinished.html")
    final val MESSAGE_LOADING_FINISHED = getMessage("/org/opalj/bugpicker/messages/projectloadingfinished.html")
    final val MESSAGE_APP_STARTED = getMessage("/org/opalj/bugpicker/messages/appstarted.html")
    final val MESSAGE_LOADING_STARTED = getMessage("/org/opalj/bugpicker/messages/projectloadingstarted.html")
    final val MESSAGE_ANALYSES_CANCELLING = getMessage("/org/opalj/bugpicker/messages/analysescancelling.html")
    final val MESSAGE_LOAD_CLASSES_FIRST = getMessage("/org/opalj/bugpicker/messages/loadclassesfirst.html")

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

    def maximizeOnCurrentScreen(stage: Stage) {
        val currentScreen = Screen.primary //screensForRectangle(stage.x(), stage.y(), stage.width(), stage.height())(0)
        val currentScreenDimensions = currentScreen.getVisualBounds()
        stage.x = currentScreenDimensions.minX
        stage.y = currentScreenDimensions.minY
        stage.width = currentScreenDimensions.width
        stage.height = currentScreenDimensions.height
    }

    val downSplitpane = new SplitPane {}
    stage = new PrimaryStage {
        title = "Bug Picker"
        scene = new Scene {
            root = new GridPane {
                prefHeight = Double.MaxValue
                content = new VBox {
                    self ⇒
                    GridPane.setVgrow(self, Priority.ALWAYS)
                    GridPane.setHgrow(self, Priority.ALWAYS)
                    content = createViews()
                }
                VBox.setVgrow(downSplitpane, Priority.ALWAYS)
            }
        }
    }

    maximizeOnCurrentScreen(stage)

    var sourceDir: java.io.File = null
    var project: Project[URL] = null
    var analysisDisabled = true

    def createViews() = {

        val infoText = new TextArea {
            text = "Please use the toolbar to load the project"
        }
        val resultWebview = new WebView()
        resultWebview.engine.loadContent(MESSAGE_APP_STARTED)
        val tabbedArea = new TabPane()
        val sourceWebview = new WebView()
        val bytecodeWebview = new WebView()
        tabbedArea += new Tab {
            text = "Bytecode"
            content = bytecodeWebview
            closable = false
        }
        tabbedArea += new Tab {
            text = "Source code"
            content = sourceWebview
            closable = false
        }
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
            def onCancel() {
                cancelled = true
                interuptAnalysis = true
                resultWebview.engine.loadContent(MESSAGE_ANALYSES_CANCELLING)
                outer.close
            }
            scene = new Scene {
                root = new BorderPane {
                    center = progressListView
                    bottom = new Button {
                        id = "Cancel"
                        text = "Cancel"
                        minWidth = 80
                        onAction = { e: ActionEvent ⇒
                            {
                                onCancel()
                            }
                        }
                        BorderPane.setAlignment(this, Pos.CENTER)
                        BorderPane.setMargin(this, new Insets(10))
                    }
                }
            }
            scene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
                override def handle(e: KeyEvent) {
                    if (e.getCode().equals(KeyCode.ESCAPE)) {
                        onCancel()
                    }
                }
            })
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
        }) {
            handleEvent(WorkerStateEvent.ANY) { event: WorkerStateEvent ⇒
                event.eventType match {
                    case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                        resultWebview.engine.loadContent(doc.toString)
                        new AddClickListenersOnLoadListener(project, sourceDir, resultWebview, bytecodeWebview, sourceWebview, { view ⇒
                            if (view == bytecodeWebview) tabbedArea.selectionModel().select(0)
                            else if (view == sourceWebview) tabbedArea.selectionModel().select(1)
                        })
                        bytecodeWebview.engine.loadContent(MESSAGE_ANALYSIS_FINISHED)
                        sourceWebview.engine.loadContent(MESSAGE_ANALYSIS_FINISHED)
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
                            resultWebview.engine.loadContent(MESSAGE_LOAD_CLASSES_FIRST)
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
                } else {
                    sourceDir = null
                }
            }
        }

        def showHelpMessage(message: String) {
            val helpStage: Stage = new Stage {
                val self = this
                title = "Help"
                width = 800
                height = 600
                scene = new Scene {
                    root = new WebView {
                        engine.loadContent(message)
                    }
                }
                scene.delegate().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE), new Runnable() {
                    override def run() {
                        self.close()
                    }
                })
            }
            helpStage.initModality(Modality.NONE)
            helpStage.initOwner(stage)
            helpStage.initStyle(StageStyle.UTILITY)
            helpStage.centerOnScreen
            helpStage.show
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
                        },
                        new MenuItem("_Quit") {
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+Q")
                            onAction = { e: ActionEvent ⇒ stage.close() }
                        })
                },
                analyseButton,
                new Menu("_Help") {
                    mnemonicParsing = true
                    items = List(
                        new MenuItem("_Loading a project") {
                            mnemonicParsing = true
                            onAction = { e: ActionEvent ⇒ showHelpMessage(MESSAGE_APP_STARTED) }
                        },
                        new MenuItem("_Browsing the report") {
                            mnemonicParsing = true
                            onAction = { e: ActionEvent ⇒ showHelpMessage(MESSAGE_ANALYSIS_FINISHED) }
                        }
                    )
                }
            )
        }
        downSplitpane.getItems().addAll(resultWebview, tabbedArea)
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
