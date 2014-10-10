/**
 *
 */
package org.opalj
package bugpicker

import javafx.scene.control.SeparatorMenuItem
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuBar
import scalafx.scene.control.MenuItem
import scalafx.scene.control.SplitPane
import scalafx.scene.control.Tab
import scalafx.scene.control.TabPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView
import scalafx.stage.Screen
import scalafx.stage.Stage
import scalafx.stage.WindowEvent
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Orientation
import scalafx.scene.input.KeyCombination
import scalafx.scene.Node
import org.opalj.br.analyses.Project
import java.net.URL
import java.io.File
import scalafx.concurrent.Task
import scalafx.concurrent.Service
import javafx.concurrent.{ Service ⇒ jService, Task ⇒ jTask }
import javafx.scene.web.{ WebView ⇒ jWebView }
import javafx.scene.control.{ TabPane ⇒ jTabPane }
import scalafx.application.Platform
import org.opalj.ai.debug.XHTML
import scalafx.scene.layout.BorderPane
import scalafx.concurrent.WorkerStateEvent
import scalafx.stage.StageStyle
import scalafx.event.EventHandler
import scalafx.stage.Modality
import scalafx.scene.input.KeyEvent
import scala.io.Source
import scalafx.scene.input.KeyCode
import javafx.event.EventHandler
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.{ EventType ⇒ ET }
import scalafx.beans.property.DoubleProperty
import scalafx.scene.control.ProgressBar
import scalafx.scene.layout.Priority
import scalafx.scene.layout.HBox

object BugPicker extends JFXApp {
    var project: Project[URL] = null
    var sources: Seq[File] = Seq.empty

    stage = new PrimaryStage {
        val theStage = this
        title = "BugPicker"

        scene = new Scene {
            val theScene = this
            root = new VBox {
                content = Seq(
                    createMenuBar(),
                    new SplitPane {
                        orientation = Orientation.VERTICAL
                        dividerPositions = 0.3

                        val reportView = new WebView {
                            id = "reportView"
                        }
                        val sourceTabs = new TabPane {
                            id = "sourceTabs"
                            this += new Tab {
                                id = "sourceTabs-source"
                                text = "Source code"
                                content = new WebView {
                                    id = "sourceView"
                                    engine.loadContent(Messages.APP_STARTED)
                                }
                                closable = false
                            }
                            this += new Tab {
                                id = "sourceTabs-byte"
                                text = "Bytecode"
                                content = new WebView {
                                    id = "byteView"
                                    engine.loadContent(Messages.APP_STARTED)
                                }
                                closable = false
                            }
                        }
                        items ++= Seq(reportView, sourceTabs)
                    }
                )
            }

            stylesheets += getClass.getResource("/org/opalj/bugpicker/style.css").toExternalForm
        }

        handleEvent(WindowEvent.WindowShowing) { e: WindowEvent ⇒
            maximizeOnCurrentScreen(this)
        }
    }

    def maximizeOnCurrentScreen(stage: Stage) {
        val currentScreen = Screen.primary
        val currentScreenDimensions = currentScreen.getVisualBounds()
        stage.x = currentScreenDimensions.minX
        stage.y = currentScreenDimensions.minY
        stage.width = currentScreenDimensions.width
        stage.height = currentScreenDimensions.height
    }

    val aboutDialog = new AboutDialog(stage)

    private def createMenuBar(): MenuBar = {
        new MenuBar {
            menus = Seq(
                new Menu {
                    text = "_File"
                    mnemonicParsing = true
                    items = Seq(
                        new MenuItem {
                            text = "_Load"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+O")
                            onAction = { e: ActionEvent ⇒
                                val dia = new LoadProjectDialog
                                val results = dia.show(stage)
                                val reportView = stage.scene().lookup("#reportView").asInstanceOf[jWebView]
                                val sourceView = stage.scene().lookup("#sourceView").asInstanceOf[jWebView]
                                val byteView = stage.scene().lookup("#byteView").asInstanceOf[jWebView]
                                val tabPane = stage.scene().lookup("#sourceTabs").asInstanceOf[jTabPane]
                                if (results != null && !results(0).isEmpty) {
                                    sourceView.engine.loadContent(Messages.LOADING_STARTED)
                                    byteView.engine.loadContent(Messages.LOADING_STARTED)
                                    reportView.engine.loadContent("")
                                    Service {
                                        Task[Unit] {
                                            val projectAndSources = ProjectHelper.setupProject(results, stage)
                                            project = projectAndSources._1
                                            sources = projectAndSources._2
                                            Platform.runLater {
                                                stage.scene().lookup("#sourceTabs-source").disable = sources.isEmpty
                                                if (sources.isEmpty) tabPane.selectionModel().select(1)
                                                sourceView.engine.loadContent(Messages.LOADING_FINISHED)
                                                byteView.engine.loadContent(Messages.LOADING_FINISHED)
                                            }
                                        }
                                    }.start
                                } else if (results != null && results(0).isEmpty) {
                                    DialogStage.showMessage("You have not specified any classes to be analyzed!", stage)
                                }
                            }
                        },
                        new MenuItem {
                            text = "_Project info"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+I")
                            onAction = { e: ActionEvent ⇒ ProjectInfoDialog.show(stage, project, sources) }
                        },
                        new SeparatorMenuItem,
                        new MenuItem {
                            text = "_Quit"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+Q")
                            onAction = { e: ActionEvent ⇒ stage.close }
                        }
                    )
                },
                new Menu {
                    text = "_Analysis"
                    mnemonicParsing = true
                    items = Seq(
                        new MenuItem {
                            text = "_Run"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+R")
                            onAction = { e: ActionEvent ⇒ AnalysisRunner.runAnalysis(stage, project, sources) }
                        }
                    )
                },
                new Menu {
                    text = "_Help"
                    mnemonicParsing = true
                    items = Messages.helpTopics.map { ht ⇒
                        new MenuItem {
                            text = ht.title
                            onAction = { e: ActionEvent ⇒ ht.show(stage) }
                        }
                    } ++ Seq[MenuItem](
                        new SeparatorMenuItem,
                        new MenuItem {
                            text = "_About"
                            mnemonicParsing = true
                            onAction = { e: ActionEvent ⇒ aboutDialog.showAndWait() }
                        }
                    )
                })
        }
    }
}