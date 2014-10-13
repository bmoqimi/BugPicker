package org.opalj.bugpicker

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.application.Platform
import scalafx.scene.input.KeyEvent
import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker.State
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.control.ProgressBar
import scalafx.scene.web.WebView
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.Insets
import scalafx.beans.property.ReadOnlyDoubleProperty
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos
import javafx.beans.value.ObservableValue
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.event.ActionEvent
import scalafx.stage.Modality
import scalafx.stage.StageStyle

class ProgressManagementDialog(
        owner: Stage,
        reportView: WebView,
        progressListView: ListView[String],
        theProgress: ReadOnlyDoubleProperty,
        interrupted: BooleanProperty) extends Stage {

    theStage ⇒
    title = "Analysis Progress "
    width = 800

    val cancelAnalysisAndCloseWindow: () ⇒ Unit = () ⇒ {
        reportView.engine.loadContent(Messages.ANALYSES_CANCELLING)
        val listener = new ChangeListener[State] {
            override def changed(obs: ObservableValue[_ <: State], oldValue: State, newValue: State) {
                Platform.runLater {
                    interrupted() = true
                    theStage.close
                }
                reportView.getEngine.getLoadWorker.stateProperty.removeListener(this)
            }
        }
        reportView.getEngine.getLoadWorker.stateProperty.addListener(listener)
    }

    scene = new Scene {
        root = new BorderPane {
            top = new HBox {
                content = new ProgressBar {
                    progress <== theProgress
                    margin = Insets(5)
                    HBox.setHgrow(this, Priority.ALWAYS)
                    prefWidth <== theStage.width - 20
                    prefHeight = 30
                }
                alignment = Pos.CENTER
                hgrow = Priority.ALWAYS
            }
            center = progressListView
            bottom = new Button {
                id = "Cancel"
                text = "Cancel"
                minWidth = 80
                defaultButton = true
                onAction = { e: ActionEvent ⇒ cancelAnalysisAndCloseWindow() }
                BorderPane.setAlignment(this, Pos.CENTER)
                BorderPane.setMargin(this, Insets(10))
            }
        }
        stylesheets += BugPicker.defaultStyles
        filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
            if (e.code.equals(KeyCode.ESCAPE)) cancelAnalysisAndCloseWindow()
        }
    }

    initModality(Modality.WINDOW_MODAL)
    initOwner(owner)
    initStyle(StageStyle.DECORATED)
}