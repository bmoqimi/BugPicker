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

object bugPicker extends JFXApp {
    stage = new PrimaryStage {
        title = "Bug Picker"
        width = 1900
        height = 1200
        scene = drawScene()
        def drawScene() = {
            val thisScene = new Scene(1900, 1200) {
                root = new BorderPane {
                    top = new VBox {
                        content = createViews()
                    }
                }
            }
            thisScene
        }
    }
    var cp: String = null
    var sourceDir: java.io.File = null

    def createViews() = {

        val infoText = new TextArea {
            text = "Please use the toolbar to load the project"
        }
        val resultWebview = new WebView()
        val sourceWebview = new WebView()
        var files: List[java.io.File] = List()

        def runAnalysis(files: List[java.io.File]) {
            /**
             * TODO: Prepare some pre-execute hook thing including a LOADING gif
             */
            val outps = new ByteArrayOutputStream
            val pst = new PrintStream(outps)
            System.setOut(pst)
            Console.setOut(pst)
            var doc: scala.xml.Node = null
            val ft = new FutureTask(new Runnable() {

                @Override
                def run() {
                    val project = Project(files(0))
                    val deadCodeAnalysis = new DeadCodeAnalysis()
                    val results @ (analysisTime, methodsWithDeadCode) =
                        deadCodeAnalysis.analyze(project, Seq.empty)
                    doc = XHTML.createXHTML(Some(deadCodeAnalysis.title), DeadCodeAnalysis.resultsAsXHTML(results))
                    val br = BasicReport(
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
                    println(br.message)
                }
            }, null)

            val thread = new Thread(ft)
            thread.start()
            /**
             * TODO:
             * We have to make sure the analysis is finished before we update our stuff
             * 1) use isDone of FutureTask - busy waiting
             * 2) Use threadpool after execute hooks - update the UI thread from another thread?
             * 3) Use locks to wait and notify
             */
            while (thread.isAlive()) {
                Thread.sleep(1000)
            }
            resultWebview.engine.loadContent(doc.toString)
            outps.flush()
            pst.flush()
            infoText.text = outps.toString()

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
        def displayProjectInfo(files: List[java.io.File]) {
            if (!files.isEmpty) {
                if (files(0) != null) {
                    cp = "-cp="+files(0).toString()
                    infoText.text = cp+"\n"
                    // analyseButton.disable = false
                }
                if (files(1) != null) {
                    sourceDir = files(1)
                    infoText.text = infoText.text.value.toString()+"\n Source directory is set to: "+sourceDir.toString()
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
                                    files = loadProjectStage
                                    if (!files.isEmpty) {
                                        displayProjectInfo(files)
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
    def loadProjectStage(): List[java.io.File] = {
        var jars: java.io.File = null
        var sources: java.io.File = null
        val outStage = new Stage {
            outer ⇒
            title = "Load project files"
            width = 400
            height = 200
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
                                text = "Open"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        val fcb = new FileChooser {
                                            title = "Open Dialog"
                                        }
                                        fcb.extensionFilters.addAll(
                                            new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                            new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                        jars = fcb.showOpenDialog(vbox.getScene().getWindow())

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
                                        jars = dc.showDialog(vbox.getScene().window())

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

                                    }
                                }
                            },
                            new Button {
                                text = "Cancel"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        outer.close
                                    }
                                }

                            },
                            new Button {
                                text = "Finish"
                                onAction = { e: ActionEvent ⇒
                                    {
                                        println("finished")
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
        //if ( (sources != null) && (jars != null ))
        val results = List[java.io.File](jars, sources)
        results

    }

}