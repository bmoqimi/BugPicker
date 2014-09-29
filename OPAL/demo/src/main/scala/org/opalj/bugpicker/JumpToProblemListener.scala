package org.opalj
package bugpicker

import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker.State
import javafx.beans.value.ObservableValue
import scalafx.scene.web.WebView
import scala.io.Source

class JumpToProblemListener(
        sourceWebview: WebView,
        methodOption: Option[String],
        pcOption: Option[String],
        lineOption: Option[String]) extends ChangeListener[State] {

    val worker = sourceWebview.engine.delegate.getLoadWorker

    worker.stateProperty.addListener(this)

    def runScript(script: String): Unit = sourceWebview.engine.delegate.executeScript(script)

    override def changed(
        observable: ObservableValue[_ <: State],
        oldValue: State,
        newValue: State) {

        if (newValue != State.SUCCEEDED) return

        val method = methodOption.getOrElse("undefined")
        val pc = pcOption.getOrElse("undefined")
        val line = lineOption.getOrElse("undefined")
        val jumpCall = s"jumpToProblem($method,$pc,$line)"
        runScript(JumpToProblemListener.JUMP_JS + jumpCall)

        worker.stateProperty.removeListener(this)
    }
}

object JumpToProblemListener {
    final lazy val JUMP_JS: String = Source.fromURL(getClass.getResource("jump-to-problem.js")).mkString
}