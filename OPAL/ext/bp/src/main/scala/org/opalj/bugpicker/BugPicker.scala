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
import scalafx.scene.input.KeyCodeCombination
import scalafx.scene.Node
import org.opalj.br.analyses.Project
import java.net.URL
import java.io.File
import scalafx.concurrent.Task
import scalafx.concurrent.Service
import javafx.concurrent.{ Service ⇒ jService, Task ⇒ jTask }
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
import java.util.prefs.Preferences
import org.opalj.bugpicker.dialogs.AboutDialog
import org.opalj.bugpicker.dialogs.DialogStage
import org.opalj.bugpicker.dialogs.ProjectInfoDialog
import org.opalj.bugpicker.dialogs.LoadProjectDialog
import org.opalj.bugpicker.dialogs.HelpBrowser
import org.opalj.bugpicker.dialogs.LoadedFiles
import org.opalj.bugpicker.analysis.DeadCodeAnalysis
import org.opalj.bugpicker.analysis.AnalysisParameters
import org.opalj.bugpicker.dialogs.AnalysisParametersDialog

object BugPicker extends JFXApp {
    final val PREFERENCES_KEY = "/org/opalj/bugpicker"
    final val PREFERENCES = Preferences.userRoot().node(PREFERENCES_KEY)
    final val PREFERENCES_KEY_CLASSES = "classes"
    final val PREFERENCES_KEY_LIBS = "libs"
    final val PREFERENCES_KEY_SOURCES = "sources"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR = "maxEvalFactor"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME = "maxEvalTime"
    final val PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES = "maxCardinalityOfIntegerRanges"

    def defaultStyles = getClass.getResource("/org/opalj/bugpicker/style.css").toExternalForm

    var project: Project[URL] = null
    var sources: Seq[File] = Seq.empty

    val sourceView: WebView = new WebView {
        contextMenuEnabled = false
    }
    val byteView: WebView = new WebView {
        contextMenuEnabled = false
    }
    val reportView: WebView = new WebView {
        contextMenuEnabled = false
        engine.loadContent(Messages.APP_STARTED)
    }
    val tabPane: TabPane = new TabPane {
        this += new Tab {
            text = "Source code"
            content = sourceView
            closable = false
        }
        this += new Tab {
            text = "Bytecode"
            content = byteView
            closable = false
        }
    }

    stage = new PrimaryStage {
        val theStage = this
        title = "BugPicker"

        scene = new Scene {
            val theScene = this
            root = new VBox {
                vgrow = Priority.ALWAYS
                hgrow = Priority.ALWAYS
                content = Seq(
                    createMenuBar(),
                    new SplitPane {
                        orientation = Orientation.VERTICAL
                        vgrow = Priority.ALWAYS
                        hgrow = Priority.ALWAYS
                        dividerPositions = 0.4

                        items ++= Seq(reportView, tabPane)
                    }
                )
            }

            stylesheets += defaultStyles
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
                            text = "L_oad"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+O")
                            onAction = loadProjectAction()
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
                            onAction = { e: ActionEvent ⇒
                                val parameters = loadParametersFromPreferences()
                                AnalysisRunner.runAnalysis(stage, project, sources, parameters,
                                    sourceView, byteView, reportView, tabPane)
                            }
                        },
                        new MenuItem {
                            text = "_Preferences"
                            mnemonicParsing = true
                            accelerator = KeyCombination("Shortcut+P")
                            onAction = { e: ActionEvent ⇒
                                val parameters = loadParametersFromPreferences
                                val dialog = new AnalysisParametersDialog(stage)
                                val newParameters = dialog.show(parameters)
                                if (newParameters.isDefined) {
                                    storeParametersToPreferences(newParameters.get)
                                }
                            }
                        }
                    )
                },
                new Menu {
                    text = "_Help"
                    mnemonicParsing = true
                    items = Seq(
                        new MenuItem {
                            text = "_Browse Help"
                            mnemonicParsing = true
                            accelerator = new KeyCodeCombination(KeyCode.F1)
                            onAction = { e: ActionEvent ⇒ HelpBrowser.show() }
                        },
                        new MenuItem {
                            text = "_About"
                            mnemonicParsing = true
                            onAction = { e: ActionEvent ⇒ aboutDialog.showAndWait() }
                        }
                    )
                })
        }
    }

    val sep = File.pathSeparator

    def loadParametersFromPreferences(): AnalysisParameters = {
        val maxEvalFactor = PREFERENCES.getDouble(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR,
            DeadCodeAnalysis.defaultMaxEvalFactor)
        val maxEvalTime = PREFERENCES.getInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME,
            DeadCodeAnalysis.defaultMaxEvalTime)
        val maxCardinalityOfIntegerRanges = PREFERENCES.getInt(
            PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES,
            DeadCodeAnalysis.defaultMaxCardinalityOfIntegerRanges)
        new AnalysisParameters(maxEvalTime, maxEvalFactor, maxCardinalityOfIntegerRanges)
    }

    def storeParametersToPreferences(parameters: AnalysisParameters) {
        PREFERENCES.putInt(PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_TIME, parameters.maxEvalTime)
        PREFERENCES.putDouble(PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_EVAL_FACTOR, parameters.maxEvalFactor)
        PREFERENCES.putInt(PREFERENCES_KEY_ANALYSIS_PARAMETER_MAX_CARDINALITY_OF_INTEGER_RANGES, parameters.maxCardinalityOfIntegerRanges)
    }

    def storeFilesToPreferences(loadedFiles: LoadedFiles) {
        def filesToPref(key: String, files: Seq[File]) =
            PREFERENCES.put(key, files.mkString(sep))

        filesToPref(PREFERENCES_KEY_CLASSES, loadedFiles.projectFiles)
        filesToPref(PREFERENCES_KEY_SOURCES, loadedFiles.projectSources)
        filesToPref(PREFERENCES_KEY_LIBS, loadedFiles.libraries)
    }

    def loadFilesFromPreferences(): Option[LoadedFiles] = {
        def prefAsFiles(key: String): Seq[File] =
            PREFERENCES.get(key, "").split(sep).filterNot(_.isEmpty).map(new File(_))

        if (!PREFERENCES.nodeExists(""))
            return None
        val classes = prefAsFiles(PREFERENCES_KEY_CLASSES)
        val libs = prefAsFiles(PREFERENCES_KEY_LIBS)
        val sources = prefAsFiles(PREFERENCES_KEY_SOURCES)
        Some(LoadedFiles(projectFiles = classes, projectSources = sources, libraries = libs))
    }

    private def loadProjectAction(): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
        val preferences = loadFilesFromPreferences()
        val dia = new LoadProjectDialog(preferences)
        val results = dia.show(stage)
        if (results.isDefined && !results.get.projectFiles.isEmpty) {
            storeFilesToPreferences(results.get)
            sourceView.engine.loadContent("")
            byteView.engine.loadContent("")
            reportView.engine.loadContent(Messages.LOADING_STARTED)
            Service {
                Task[Unit] {
                    val projectAndSources = ProjectHelper.setupProject(results.get, stage)
                    project = projectAndSources._1
                    sources = projectAndSources._2
                    Platform.runLater {
                        tabPane.tabs(0).disable = sources.isEmpty
                        if (sources.isEmpty) tabPane.selectionModel().select(1)
                        reportView.engine.loadContent(Messages.LOADING_FINISHED)
                    }
                }
            }.start
        } else if (results.isDefined && results.get.projectFiles.isEmpty) {
            DialogStage.showMessage("Error", "You have not specified any classes to be analyzed!", stage)
        }
    }
}
