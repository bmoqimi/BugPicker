package org.opalj
package bugpicker

import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker.State
import javafx.beans.value.ObservableValue
import scalafx.scene.web.WebView
import scala.io.Source

class JumpToProblemListener(
        webview: WebView,
        methodOption: Option[String],
        pcOption: Option[String],
        lineOption: Option[String]) extends ChangeListener[State] {

    val worker = webview.engine.delegate.getLoadWorker

    worker.stateProperty.addListener(this)

    def runScript(script: String): Unit = webview.engine.delegate.executeScript(script)

    override def changed(
        observable: ObservableValue[_ <: State],
        oldValue: State,
        newValue: State) {

        if (newValue != State.SUCCEEDED) return

        val jumpCall =
            if (lineOption.isDefined) {
                s"jumpToLineInSourceCode(${lineOption.get})"
            } else if (methodOption.isDefined && !pcOption.isDefined) {
                s"jumpToMethodInBytecode(${methodOption.get});"
            } else if (methodOption.isDefined && pcOption.isDefined) {
                s"jumpToProblemInBytecode(${methodOption.get}, ${pcOption.get});"
            } else {
                "window.scrollTo(0,0);"
            }
        runScript(JumpToProblemListener.JUMP_JS + jumpCall)

        worker.stateProperty.removeListener(this)
    }
}

object JumpToProblemListener {
    final lazy val JUMP_JS: String = Source.fromURL(getClass.getResource("jump-to-problem.js")).mkString
}