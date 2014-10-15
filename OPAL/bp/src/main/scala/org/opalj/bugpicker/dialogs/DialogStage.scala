package org.opalj
package bugpicker
package dialogs

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxKeyEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.input.KeyCode
import scalafx.scene.input.KeyEvent
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
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

    def showMessage(theTitle: String, message: String, owner: Window) {
        val stage = new DialogStage(owner) {
            theStage ⇒
            title = theTitle
            scene = new Scene {
                root = new BorderPane {
                    center = new Label {
                        text = message
                        margin = Insets(20)
                    }
                    bottom = new HBox {
                        content = new Button {
                            text = "Close"
                            defaultButton = true
                            HBox.setMargin(this, Insets(10))
                            onAction = { e: ActionEvent ⇒
                                theStage.close()
                            }
                        }
                        alignment = Pos.CENTER
                    }
                }
                stylesheets += BugPicker.defaultStyles
            }
        }
        stage.showAndWait()
    }
}