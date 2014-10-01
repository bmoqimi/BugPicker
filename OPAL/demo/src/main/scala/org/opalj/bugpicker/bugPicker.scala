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
            <h1>Now use the menu bar (or Ctrl + r ) to run the analysis</h1>
        </html>.toString
    final val MESSAGE_APP_STARTED =
        <html>
            <h1>Use the menu bar (or Ctrl + o ) load your project</h1>
        </html>.toString
    final val MESSAGE_LOADING_STARTED =
        <html>
            <h1>Please wait while the project is loaded . . . </h1>
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
                val bp = new BorderPane {}
                /* val vbox = new VBox()
                    vbox.alignment = Pos.CENTER
                    vbox.content = {
                        List(
                            progressListView,
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
                                padding = new Insets(15)
                                alignment = Pos.CENTER
                            }
                        )
                    }
                    * */

                val but = new Button {
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
                }
                BorderPane.setAlignment(but, Pos.CENTER)
                BorderPane.setMargin(but, new Insets(4))
                //but.alignment = Pos.CENTER
                //bp.padding = new Insets(10)
                bp.top = progressListView
                bp.center = but
                root = bp
            }
        }

        def showProgressManagement(): Boolean = {
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

        def createHTMLReport(results: (Long, Iterable[DeadCode])): Node = {
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
            val libs = loadedFiles(2)
            infoText.text() = ""
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
                    Platform.runLater(infoText.text() += "\n Source directory is set to: "+sourceDir.toString())
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
                                    val tempFiles = loadProjectStage
                                    if (tempFiles != null) {
                                        loadedFiles = tempFiles
                                        if (loadedFiles != null)
                                            if (!loadedFiles(0).isEmpty) {
                                                analysisDisabled = false
                                                val displayInfo = new Runnable() {
                                                    override def run() {
                                                        displayProjectInfo(loadedFiles)
                                                    }
                                                }
                                                resultWebview.engine.loadContent(MESSAGE_LOADING_STARTED)
                                                val infoThread = new Thread(displayInfo)
                                                infoThread.setDaemon(true)
                                                infoThread.start

                                            }
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
        var libs: java.io.File = null
        var cancelled = false
        val jarListview = new ListView[String]()
        val libsListview = new ListView[String]()
        val sourceListview = new ListView[String]()
        val outStage = new Stage {
            outer ⇒ {
                title = "Load project files"
                width = 800
                height = 600
                maxWidth = 800
                maxHeight = 600
                val loadScene = new Scene
                val alloverVbox = new VBox
                val l1 = new Label
                l1.text = "Select files(jars/.class/directory) to be analysed"
                val jarButton = new Button
                jarButton.id = "Select a Jar/Class File"
                jarButton.text = "Add Jar/Class Files to be Analysed"
                jarButton.onAction = { e: ActionEvent ⇒
                    {
                        val fcb = new FileChooser {
                            title = "Open Dialog"
                        }
                        fcb.extensionFilters.addAll(
                            new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                            new FileChooser.ExtensionFilter("Class Files", "*.class"))
                        val file = fcb.showOpenDialog(alloverVbox.getScene().getWindow())
                        if (file != null) {
                            jars :::= List(file)
                            jarListview.items() += jars(0).toString()
                        }

                    }
                }
                val jarRemove = new Button
                jarRemove.id = "remove jar/class File/Directory"
                jarRemove.text = "Remove"
                jarRemove.onAction = { e: ActionEvent ⇒
                    {
                        val removed = jarListview.selectionModel().getSelectedItem()
                        val temp = jars
                        jars = List[java.io.File]()
                        temp.foreach {
                            file ⇒
                                {
                                    if (file.toString != removed) {
                                        jars :::= List(file)
                                    }
                                }
                        }
                        jarListview.items() -= removed
                    }
                }
                val l2 = new Label
                l2.text = "Choose the  class directory containing jar of class files"
                val dirButton = new Button {}
                dirButton.text = "Add Directory to be Analysed"
                dirButton.onAction = { e: ActionEvent ⇒
                    {
                        val dc = new DirectoryChooser {
                            title = "Select Directory"
                        }
                        val file = dc.showDialog(alloverVbox.getScene().window())
                        if (file != null) {

                            jars :::= List(file)
                            jarListview.items() += jars(0).toString()
                        }
                    }
                }
                val libsButton = new Button
                libsButton.id = "Select a Jar/Class Library"
                libsButton.text = "Add Jar File as a Library (Optional)"
                libsButton.onAction = { e: ActionEvent ⇒
                    {
                        val fcb = new FileChooser {
                            title = "Open Dialog"
                        }
                        fcb.extensionFilters.addAll(
                            new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                            new FileChooser.ExtensionFilter("Class Files", "*.class"))
                        val file = fcb.showOpenDialog(alloverVbox.getScene().getWindow())
                        if (file != null) {
                            libs = file
                            libsListview.items() += libs.toString()
                        }

                    }
                }

                val libsRemove = new Button
                libsRemove.id = "Remove Library"
                libsRemove.text = "Remove"
                libsRemove.onAction = { e: ActionEvent ⇒
                    {
                        libsListview.items() -= libs.toString()
                        libs = null
                    }
                }
                val l3 = new Label
                l3.text = "select the source directory of your project"
                val sourceButton = new Button
                sourceButton.text = "Add source Directory (Optional)"
                sourceButton.onAction = { e: ActionEvent ⇒
                    {
                        val dc = new DirectoryChooser {
                            title = "Open Dialog"
                        }
                        val file = dc.showDialog(alloverVbox.getScene().window())
                        if (file != null) {
                            sources = file
                            sourceListview.items() += sources.toString()
                        }
                    }
                }
                val sourceRemove = new Button
                sourceRemove.id = "Remove Source"
                sourceRemove.text = "Remove"
                sourceRemove.onAction = { e: ActionEvent ⇒
                    {
                        sourceListview.items() -= sources.toString()
                        sources = null
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

                loadScene.stylesheets.add(getClass.getResource("borderPane.css").toURI().toURL().toString())
                // Jar/Class set of button
                val classButtonVbox = new VBox
                val classHbox = new HBox
                classButtonVbox.children.add(jarButton)
                classButtonVbox.children.add(jarRemove)
                classButtonVbox.children.add(dirButton)
                classButtonVbox.spacing = 20
                classHbox.styleClass.add("bordered")
                HBox.setHgrow(jarListview, Priority.ALWAYS)
                classHbox.children.add(jarListview)
                classHbox.children.add(classButtonVbox)

                // Library set of Buttons
                val libsButtonvbox = new VBox
                val libsHbox = new HBox
                libsButtonvbox.children.add(libsButton)
                libsButtonvbox.children.add(libsRemove)
                libsButtonvbox.spacing = 20
                libsHbox.styleClass.add("bordered")
                HBox.setHgrow(libsListview, Priority.ALWAYS)
                libsHbox.children.add(libsListview)
                libsHbox.children.add(libsButtonvbox)

                // Source set of buttons
                val sourceButtonvbox = new VBox
                val sourceHbox = new HBox
                sourceButtonvbox.children.add(sourceButton)
                sourceButtonvbox.children.add(sourceRemove)
                sourceButtonvbox.spacing = 20
                sourceHbox.styleClass.add("bordered")
                HBox.setHgrow(sourceListview, Priority.ALWAYS)
                HBox.setHgrow(sourceButtonvbox, Priority.NEVER)
                sourceHbox.children.add(sourceListview)
                sourceHbox.children.add(sourceButtonvbox)

                val finishButtonshbox = new HBox
                finishButtonshbox.alignment = Pos.CENTER
                VBox.setMargin(finishButton, new Insets(20))
                VBox.setMargin(cancelButton, new Insets(20))
                finishButtonshbox.padding = new Insets(6)
                finishButtonshbox.styleClass.add("bordered")
                finishButtonshbox.children.add(finishButton)
                finishButtonshbox.children.add(cancelButton)

                classHbox.padding = new Insets(6)
                libsHbox.padding = new Insets(6)
                sourceHbox.padding = new Insets(6)

                alloverVbox.children.addAll(classHbox, libsHbox, sourceHbox, finishButtonshbox)

                loadScene.root = alloverVbox
                scene = loadScene

            }
        }
        outStage.initModality(Modality.WINDOW_MODAL)
        outStage.initOwner(stage.scene.window.value)
        outStage.initStyle(StageStyle.UTILITY)
        outStage.centerOnScreen
        outStage.showAndWait
        if (cancelled) {
            null
        } else {
            val results = List(jars, List(sources), List(libs))
            results
        }
    }
}
