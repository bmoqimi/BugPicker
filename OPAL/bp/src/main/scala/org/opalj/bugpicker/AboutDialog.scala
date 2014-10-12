package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.geometry.Pos
import scalafx.geometry.Insets
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView
import scalafx.scene.control.Hyperlink
import java.awt.Desktop
import java.net.URL
import javafx.application.HostServices
import scalafx.scene.control.Label

class AboutDialog(owner: Stage) extends DialogStage(owner) {
    final val OPAL_PROJECT = new URL("http://opal-project.de").toExternalForm

    title = "About BugPicker"

    scene = new Scene {
        root = new BorderPane {
            center = new VBox {
                content = Seq(
                    new Label {
                        margin = Insets(20)
                        text = "The BugPicker is powered by the OPAL (OPen AnaLysis) framework.\n"+
                            "Visit http://opal-project.de and http://opal-project.de/bugpicker."
                    }
                )
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
    }
}