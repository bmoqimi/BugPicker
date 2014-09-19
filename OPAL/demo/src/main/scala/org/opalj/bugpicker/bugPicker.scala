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
import org.opalj.br.analyses.Project
import org.opalj.ai.debug.XHTML
import org.opalj.br.analyses.BasicReport
import org.opalj.util.PerformanceEvaluation._
import scalafx.concurrent.Task
import javafx.{ concurrent ⇒ jfxc }
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
import org.opalj.br.analyses.Analysis

object bugPicker extends JFXApp {

    object ae extends AnalysisExecutor {
        val deadCodeAnalysis = new DeadCodeAnalysis
        lazy val analysis = new Analysis[URL, BasicReport] {
            override def title: String = deadCodeAnalysis.title

            override def description: String = deadCodeAnalysis.description

            override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
                val results @ (analysisTime, methodsWithDeadCode) =
                    deadCodeAnalysis.analyze(theProject, parameters)

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

    def createViews() = {

        val infoText = new TextArea {
            text = "Please use the toolbar to load the project"
        }
        val resultWebview = new WebView()
        val sourceWebview = new WebView()
        var files: List[java.io.File] = List()
        var doc: Node = null
        var br: BasicReport = null
        object Worker extends Task(new jfxc.Task[String] {

            protected def call(): String = {
                //val project = new Project(files(0))
                val deadCodeAnalysis = new DeadCodeAnalysis
                val results @ (analysisTime, methodsWithDeadCode) = deadCodeAnalysis.analyze(project, Seq.empty)
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

        })

        def runAnalysis(files: List[java.io.File]) {
            /**
             * TODO: Prepare some pre-execute hook thing including a LOADING gif
             */
            /*
            val outps = new ByteArrayOutputStream
            val pst = new PrintStream(outps)
            System.setOut(pst)
            Console.setOut(pst)
            */
            val et = WorkerStateEvent.ANY
            Worker.handleEvent(et) {
                event: WorkerStateEvent ⇒
                    {
                        event.eventType match {
                            case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                                resultWebview.engine.loadContent(doc.toString)
                                infoText.text = br.message
                            }
                            case WorkerStateEvent.WORKER_STATE_SCHEDULED ⇒ {
                                val loadingURL = getClass.getResource("/cat_loading.gif").toURI().toURL()
                                resultWebview.engine.load(loadingURL.toString())
                            }
                        }

                    }

            }
            val thread = new Thread(Worker)
            thread.setDaemon(true)
            thread.start()

        }
        val analyseButton = new Menu("Analysis") {
            items = List(
                new MenuItem("run") {
                    //disable = true
                    onAction = {
                        e: ActionEvent ⇒ runAnalysis(files)
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
                        new MenuItem("Open") {
                            onAction = { e: ActionEvent ⇒
                                {
                                    loadedFiles = loadProjectStage
                                    if (!loadedFiles(0).isEmpty) {
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
                                        jars :::= List(fcb.showOpenDialog(vbox.getScene().getWindow()))
                                        listview.items() += jars(0).toString()

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
                                        jars :::= List(dc.showDialog(vbox.getScene().window()))
                                        listview.items() += jars(0).toString()

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

                                        sources = dc.showDialog(vbox.getScene().getWindow())
                                        listview.items() += sources.toString()

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
            jars = null
        }
        val results = List(jars, List(sources))
        results

    }

}