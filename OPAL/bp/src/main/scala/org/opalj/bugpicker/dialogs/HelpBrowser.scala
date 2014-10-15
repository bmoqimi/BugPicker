package org.opalj
package bugpicker
package dialogs

import scalafx.Includes.jfxMultipleSelectionModel2sfx
import scalafx.Includes.jfxObjectProperty2sfx
import scalafx.Includes.jfxReadOnlyObjectProperty2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.ListView
import scalafx.scene.control.ListView.sfxListView2jfx
import scalafx.scene.control.SelectionMode
import scalafx.scene.control.SplitPane
import scalafx.scene.web.WebView
import scalafx.scene.web.WebView.sfxWebView2jfx
import scalafx.stage.Stage
import scalafx.stage.StageStyle

object HelpBrowser extends Stage {
    title = "BugPicker Help"
    minWidth = 800
    minHeight = 600

    scene = new Scene {
        root = new SplitPane {
            val list = new ListView[HelpTopic] {
                items() ++= Messages.helpTopics
                selectionModel.delegate().selectionMode = SelectionMode.SINGLE
            }
            val browser = new WebView
            browser.contextMenuEnabled = false
            list.selectionModel.delegate().selectedItemProperty().onChange { (observable, oldValue, newValue) ⇒
                browser.engine.loadContent(newValue.content)
                title = s"BugPicker Help - ${newValue.title}"
            }
            items ++= Seq(list, browser)
            dividerPositions = 0.3
        }
        stylesheets += BugPicker.defaultStyles
    }

    initStyle(StageStyle.DECORATED)

    override def show() {
        centerOnScreen()
        super.show()
    }
}