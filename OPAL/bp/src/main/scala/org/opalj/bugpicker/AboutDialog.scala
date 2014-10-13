package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.stage.WindowEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.layout.VBox
import scalafx.stage.Stage
import scalafx.scene.control.TextArea

class AboutDialog(owner: Stage) extends DialogStage(owner) {
    self ⇒

    title = "About BugPicker"

    scene = new Scene {
        root = new BorderPane {
            center = new VBox {
                content = new Label {
                    text = "The BugPicker is powered by the OPAL (OPen AnaLysis) framework.\n"+
                        "Visit http://opal-project.de and http://opal-project.de/bugpicker."
                    margin = Insets(20)
                }
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