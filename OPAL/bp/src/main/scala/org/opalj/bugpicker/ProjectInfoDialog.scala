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
    def show(owner: Stage, project: Project[URL], sources: Seq[File]) {
        val wv = new WebView

        wv.engine.loadContent("<html><body style=\"font: 14px sans-serif;\">"+
            "<h2>Project statistics</h2>"+
            statistics(project)+
            "<h2>Source directories</h2>"+
            sourceInfo(sources)+
            "</body></html>")

        val button = new Button {
            text = "Close"
            defaultButton = true
        }

        val stage = new DialogStage(owner) {
            scene = new Scene {
                root = new BorderPane {
                    center = wv
                    bottom = new HBox {
                        content = button
                        alignment = Pos.CENTER
                    }
                    HBox.setMargin(button, Insets(10))
                }
            }
        }

        button.onAction = { e: ActionEvent ⇒ stage.close() }

        stage.title = "Project info"
        stage.showAndWait
    }

    private def statistics(project: Project[URL]): String =
        if (project == null) ""
        else project.statistics.toList.map(e ⇒ s"<p>${e._1}: ${e._2}</p>").mkString("\n")

    private def sourceInfo(sources: Seq[File]): String =
        if (sources == null) ""
        else sources.map(f ⇒ s"<p>${f.getAbsolutePath}</p>").mkString("\n")
}