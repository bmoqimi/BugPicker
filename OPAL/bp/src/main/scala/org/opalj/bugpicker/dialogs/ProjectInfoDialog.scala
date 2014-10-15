package org.opalj
package bugpicker
package dialogs

import java.io.File
import java.net.URL

import org.opalj.br.analyses.Project

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.web.WebView
import scalafx.stage.Stage

object ProjectInfoDialog {
    def toUL(files: Seq[File]): String = files.map(_.getAbsolutePath).mkString("<ul><li>", "</li><li>", "</li></ul>")

    def show(owner: Stage, project: Project[URL], sources: Seq[File]) {
        if (project == null) {
            DialogStage.showMessage("Error", "You need to load a project before you can inspect it.", owner)
            return
        }

        val preferences = BugPicker.loadPreferences().getOrElse(LoadedFiles())

        val html = report(project, preferences)

        val stage = new DialogStage(owner) {
            theStage ⇒
            scene = new Scene {
                root = new BorderPane {
                    center = new WebView {
                        contextMenuEnabled = false
                        engine.loadContent(html)
                    }
                    bottom = new HBox {
                        content = new Button {
                            text = "Close"
                            defaultButton = true
                            onAction = { e: ActionEvent ⇒ theStage.close() }
                            HBox.setMargin(this, Insets(10))
                        }
                        alignment = Pos.CENTER
                    }
                }
                stylesheets += BugPicker.defaultStyles
            }
        }

        stage.title = "Project info"
        stage.showAndWait
    }

    private def statistics(project: Project[URL]): String =
        if (project == null) ""
        else project.statistics.toList.map(e ⇒ s"<li>${e._1}: ${e._2}</li>").mkString("<ul>", "", "</ul>")

    private def report(project: Project[URL], preferences: LoadedFiles): String =
        <html>
            <head>
                <style type="text/css"><![CDATA[
body {
	font: 14px sans-serif;
}
ul, li {
    list-style-type: none;
    padding-left: 0;
}
    				]]></style>
            </head>
            <body>
                <h2>Project statistics</h2>
                <ul>{ project.statistics.toList.map(e ⇒ <li>{ e._1 }: { e._2 }</li>) }</ul>
                <h2>Loaded jar files and directories</h2>
                <ul>{ preferences.projectFiles.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Loaded source directories</h2>
                <ul>{ preferences.projectSources.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Loaded libraries</h2>
                <ul>{ preferences.libraries.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
            </body>
        </html>.toString
}