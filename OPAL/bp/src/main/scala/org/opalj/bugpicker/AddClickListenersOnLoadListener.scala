package org.opalj
package bugpicker

import java.net.URL
import org.opalj.br.analyses.Project
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.scene.web.WebView
import java.io.File

/**
 * Adds onClick listeners on `td` elements in `resultWebview`'s document (once it has finished loading).
 * Clicking them decompiles and opens the bytecode of the problem spot in `sourceWebview`.
 * Once the onClick listeners have been added, this listener unregisters itself from `resultWebview`.
 *
 * @param focus This function takes either bytecodeWebview or sourceWebview and ensures that the respective tab is focused
 */
class AddClickListenersOnLoadListener(
        project: Project[URL],
        sources: Seq[File],
        resultWebview: WebView,
        bytecodeWebview: WebView,
        sourceWebview: WebView,
        focus: WebView ⇒ Unit) extends ChangeListener[State] {

    private val loadWorker = resultWebview.engine.delegate.getLoadWorker

    loadWorker.stateProperty.addListener(this)

    override def changed(observable: ObservableValue[_ <: State], oldValue: State, newValue: State) {
        if (newValue != State.SUCCEEDED) return

        val document = resultWebview.engine.document
        val nodes = document.getElementsByTagName("span")

        for {
            i ← (0 to nodes.getLength)
            node = nodes.item(i)
            if node != null && node.getAttributes() != null &&
                node.getAttributes().getNamedItem("data-class") != null
        } {
            val eventTarget = node.asInstanceOf[org.w3c.dom.events.EventTarget]
            val listener = new DOMNodeClickListener(project, sources, node, bytecodeWebview, sourceWebview, focus)
            eventTarget.addEventListener("click", listener, false)
        }

        loadWorker.stateProperty.removeListener(this)
    }
}