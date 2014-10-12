package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.stage.StageStyle
import scalafx.scene.control.SplitPane
import scalafx.scene.control.ListView
import scalafx.scene.web.WebView
import scalafx.scene.control.SelectionMode

object HelpBrowser extends Stage {
    title = "BugPicker Help"
    minWidth = 600
    minHeight = 800

    scene = new Scene {
        root = new SplitPane {
            val list = new ListView[HelpTopic] {
                items() ++= Messages.helpTopics
                selectionModel.delegate().selectionMode = SelectionMode.SINGLE
            }
            val browser = new WebView
            list.selectionModel.delegate().selectedItemProperty().onChange { (observable, oldValue, newValue) ⇒
                browser.engine.loadContent(newValue.content)
                title = s"BugPicker Help - ${newValue.title}"
            }
            items ++= Seq(list, browser)
            dividerPositions = 0.3
        }
    }

    initStyle(StageStyle.UTILITY)

    override def show() {
        centerOnScreen()
        super.show()
    }
}