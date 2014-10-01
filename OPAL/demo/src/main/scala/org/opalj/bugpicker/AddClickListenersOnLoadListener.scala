package org.opalj
package bugpicker

import java.net.URL

import org.opalj.br.analyses.Project

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker.State
import scalafx.scene.web.WebView

/**
 * Adds onClick listeners on `td` elements in `resultWebview`'s document (once it has finished loading).
 * Clicking them decompiles and opens the bytecode of the problem spot in `sourceWebview`.
 * Once the onClick listeners have been added, this listener unregisters itself from `resultWebview`.
 */
class AddClickListenersOnLoadListener(
        project: Project[URL],
        sourceDir: java.io.File,
        resultWebview: WebView,
        sourceWebview: WebView) extends ChangeListener[State] {

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
            val listener = new DOMNodeClickListener(project, sourceDir, node, sourceWebview)
            eventTarget.addEventListener("click", listener, false)
        }

        loadWorker.stateProperty.removeListener(this)
    }
}