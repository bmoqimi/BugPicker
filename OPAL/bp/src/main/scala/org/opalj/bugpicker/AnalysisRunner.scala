package org.opalj
package bugpicker

import java.io.File
import java.net.URL
import scala.io.Source
import scala.xml.{ Node ⇒ xmlNode }
import org.opalj.ai.debug.XHTML
import org.opalj.br.analyses.Project
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.{ Service ⇒ jService }
import javafx.concurrent.{ Task ⇒ jTask }
import javafx.concurrent.Worker.State
import javafx.scene.control.{ TabPane ⇒ jTabPane }
import javafx.scene.web.{ WebView ⇒ jWebView }
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.DoubleProperty
import scalafx.concurrent.WorkerStateEvent
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.geometry.Pos.sfxEnum2jfx
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.scene.control.ProgressBar
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.Priority
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scalafx.concurrent.Service
import scalafx.beans.property.ObjectProperty

object AnalysisRunner extends DeadCodeAnalysis {
    def runAnalysis(stage: Stage, project: Project[URL], sources: Seq[File]) {
        val interrupted = BooleanProperty(false)

        val scene: Scene = stage.scene()

        val reportView = scene.lookup("#reportView").get.delegate.asInstanceOf[jWebView]
        val sourceView = scene.lookup("#sourceView").get.delegate.asInstanceOf[jWebView]
        val byteView = scene.lookup("#byteView").get.delegate.asInstanceOf[jWebView]
        val tabPane = scene.lookup("#sourceTabs").get.delegate.asInstanceOf[jTabPane]

        val progressListView = new ListView[String] {
            prefHeight = (Runtime.getRuntime().availableProcessors() + 2) * 24 + 2
        }
        val progressListItems = scala.collection.mutable.HashMap[String, String]()
        val theProgress = DoubleProperty(0)
        val classCount = project.classFilesCount.toDouble

        val progStage = new ProgressManagementDialog(stage, reportView, progressListView, theProgress, interrupted)

        val initProgressManagement = new InitProgressManagement(interrupted, theProgress, progressListView, progressListItems, classCount, progStage)

        val doc = new ObjectProperty[xmlNode]

        val worker = new AnalysisWorker(doc, project, initProgressManagement)
        worker.handleEvent(WorkerStateEvent.ANY)(new WorkerFinishedListener(project, sources, doc, reportView, sourceView, byteView, tabPane))

        worker.start
        progStage.centerOnScreen
        progStage.showAndWait
    }

    private class WorkerFinishedListener(
            project: Project[URL],
            sources: Seq[File],
            doc: ObjectProperty[xmlNode],
            reportView: jWebView,
            sourceView: jWebView,
            byteView: jWebView,
            tabPane: jTabPane) extends Function1[WorkerStateEvent, Unit] {
        override def apply(event: WorkerStateEvent): Unit = {
            event.eventType match {
                case WorkerStateEvent.WORKER_STATE_SUCCEEDED ⇒ {
                    reportView.engine.loadContent(doc().toString)
                    new AddClickListenersOnLoadListener(project, sources, reportView, byteView, sourceView, { view ⇒
                        if (view == sourceView) tabPane.selectionModel().select(0)
                        else if (view == byteView) tabPane.selectionModel().select(1)
                    })
                    byteView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                    sourceView.engine.loadContent(Messages.ANALYSIS_FINISHED)
                }
                case WorkerStateEvent.WORKER_STATE_RUNNING ⇒ {
                    reportView.engine.loadContent(Messages.ANALYSIS_RUNNING)
                }
                case _default ⇒ {
                    reportView.engine.loadContent(event.eventType.toString)
                }
            }
        }
    }
}