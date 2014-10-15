package org.opalj
package bugpicker
package dialogs

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import scalafx.scene.control.Hyperlink

class AboutDialog(owner: Stage, showUrl: String ⇒ Unit) extends DialogStage(owner) {
    self ⇒

    title = "About BugPicker"

    scene = new Scene {
        root = new BorderPane {
            center = new VBox {
                margin = Insets(20)
                alignment = Pos.TOP_LEFT
                content = Seq(
                    new Label("The BugPicker is powered by the OPAL (OPen AnaLysis) framework."),
                    new HBox {
                        alignment = Pos.BASELINE_LEFT
                        content = Seq(
                            new Label("Visit "),
                            new Hyperlink {
                                text = "the OPAL project"
                                onAction = { e: ActionEvent ⇒
                                    showUrl("http://opal-project.de")
                                }
                            },
                            new Label(" and "),
                            new Hyperlink {
                                text = "the BugPicker project"
                                onAction = { e: ActionEvent ⇒
                                    showUrl("http://opal-project.de/bugpicker")
                                }
                            }
                        )
                    })
            }

            bottom = new HBox {
                content = new Button {
                    text = "Close"
                    defaultButton = true
                    HBox.setMargin(this, Insets(10))
                    onAction = { e: ActionEvent ⇒ close() }
                }
                alignment = Pos.CENTER
            }
        }
        stylesheets += BugPicker.defaultStyles
    }
}