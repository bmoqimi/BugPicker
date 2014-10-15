package org.opalj
package bugpicker
package dialogs

import org.opalj.bugpicker.BugPicker
import org.opalj.bugpicker.Messages

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.application.Platform
import scalafx.beans.binding.NumberBinding.sfxNumberBinding2jfx
import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.ReadOnlyDoubleProperty
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
import scalafx.scene.web.WebView
import scalafx.scene.web.WebView.sfxWebView2jfx
import scalafx.stage.Modality
import scalafx.stage.Stage
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