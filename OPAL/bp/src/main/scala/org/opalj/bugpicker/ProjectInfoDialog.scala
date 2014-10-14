package org.opalj
package bugpicker

import scalafx.Includes._
import org.opalj.br.analyses.Project
import java.io.File
import scalafx.stage.Stage
import java.net.URL
import scalafx.scene.web.WebView
import scalafx.scene.control.Button
import scalafx.geometry.Insets
import scalafx.scene.layout.HBox
import scalafx.scene.layout.BorderPane
import scalafx.event.ActionEvent
import scalafx.geometry.Pos
import scalafx.scene.Scene

object ProjectInfoDialog {
    def toUL(files: Seq[File]): String = files.map(_.getAbsolutePath).mkString("<ul><li>", "</li><li>", "</li></ul>")

    def show(owner: Stage, project: Project[URL], sources: Seq[File]) {
        val (jarDirs, libDirs, sourceDirs) = BugPicker.loadPreferences()

        val html = report(project, jarDirs, libDirs, sourceDirs)

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

    private def report(project: Project[URL], jarDirs: Seq[File], libDirs: Seq[File], sourceDirs: Seq[File]): String =
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
                <ul>{ jarDirs.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Loaded libraries</h2>
                <ul>{ libDirs.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
                <h2>Loaded source directories</h2>
                <ul>{ sourceDirs.map(d ⇒ <li>{ d.getAbsolutePath }</li>) }</ul>
            </body>
        </html>.toString
}