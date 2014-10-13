package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.Node
import scalafx.scene.Parent
import scalafx.scene.input.KeyEvent
import scalafx.scene.input.KeyCode
import scalafx.stage.Modality
import scalafx.stage.StageStyle
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import scalafx.geometry.Insets
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.stage.Window

class DialogStage(owner: Window) extends Stage {
    filterEvent(KeyEvent.KeyPressed) { e: KeyEvent ⇒
        if (e.code == KeyCode.ESCAPE) {
            close()
        }
    }

    initModality(Modality.APPLICATION_MODAL)
    initStyle(StageStyle.DECORATED)
    initOwner(owner)
}

object DialogStage {

    def showMessage(message: String, owner: Window) {
        val button = new Button {
            text = "Close"
        }
        val stage = new DialogStage(owner) {
            scene = new Scene {
                root = new BorderPane {
                    center = new Label {
                        text = message
                        margin = Insets(20)
                    }
                    bottom = new HBox {
                        content = button
                        alignment = Pos.CENTER
                        HBox.setMargin(button, Insets(10))
                    }
                }
                stylesheets += BugPicker.defaultStyles
            }
        }
        button.onAction = { e: ActionEvent ⇒
            stage.close()
        }
        stage.showAndWait()
    }
}