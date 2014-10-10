package org.opalj
package bugpicker

import scala.io.Source
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.Scene
import scalafx.scene.web.WebView
import scalafx.scene.input.KeyCombination
import scalafx.scene.input.KeyCodeCombination
import scalafx.scene.input.KeyCode
import scalafx.stage.Modality
import scalafx.stage.StageStyle

object Messages {
    def getMessage(path: String): String = process(getClass.getResourceAsStream(path))(Source.fromInputStream(_).mkString)

    final val ANALYSIS_RUNNING = getMessage("/org/opalj/bugpicker/messages/analysisrunning.html")
    final val ANALYSIS_FINISHED = getMessage("/org/opalj/bugpicker/messages/analysisfinished.html")
    final val LOADING_FINISHED = getMessage("/org/opalj/bugpicker/messages/projectloadingfinished.html")
    final val APP_STARTED = getMessage("/org/opalj/bugpicker/messages/appstarted.html")
    final val LOADING_STARTED = getMessage("/org/opalj/bugpicker/messages/projectloadingstarted.html")
    final val ANALYSES_CANCELLING = getMessage("/org/opalj/bugpicker/messages/analysescancelling.html")
    final val LOAD_CLASSES_FIRST = getMessage("/org/opalj/bugpicker/messages/loadclassesfirst.html")
    final val NO_BYTECODE_FOUND = getMessage("/org/opalj/bugpicker/messages/nobytecodefound.html")

    val helpTopics: Seq[HelpTopic] = Seq(
        new HelpTopic("How to load a project", APP_STARTED),
        new HelpTopic("How to run an analysis", LOADING_FINISHED),
        new HelpTopic("How to browse the report", ANALYSIS_FINISHED)
    )
}

class HelpTopic(val title: String, val content: String) {

    def show(owner: Stage) {
        val wv = new WebView
        wv.engine.loadContent(content)
        val stage = new DialogStage(owner) {
            scene = new Scene {
                root = wv
            }
        }
        stage.title = title
        stage.showAndWait
    }
}