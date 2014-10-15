package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.scene.control.ListView
import scalafx.application.Platform
import org.opalj.br.analyses.ProgressManagement
import scalafx.beans.property.ReadOnlyBooleanProperty
import scalafx.beans.property.DoubleProperty
import scalafx.stage.Stage
import org.opalj.br.analyses.EventType

class InitProgressManagement(
        interrupted: ReadOnlyBooleanProperty,
        theProgress: DoubleProperty,
        progressListView: ListView[String],
        progressListItems: scala.collection.mutable.HashMap[String, String],
        classCount: Double,
        progStage: Stage) extends Function1[Int, ProgressManagement] {

    override def apply(x: Int): ProgressManagement = new ProgressManagement {

        final def progress(step: Int, evt: EventType.Value, msg: Option[String]): Unit = evt match {
            case EventType.Start ⇒ {
                Platform.runLater(new Runnable() {
                    override def run() {
                        progressListView.items() += step.toString+": "+msg.get
                        progressListItems += ((step.toString, msg.get))
                        progressListView.scrollTo(progressListView.getItems.size() - 1)
                    }
                }
                )
            }
            case EventType.End ⇒ {
                Platform.runLater(new Runnable() {
                    override def run() {
                        progressListView.items() -= step.toString+": "+progressListItems.get(step.toString).get
                        progressListItems.remove(step.toString)
                        val prog = step.toDouble / classCount
                        theProgress.synchronized(if (prog > theProgress()) theProgress() = prog)
                        if (theProgress() == 1)
                            progStage.close
                    }
                })
            }
        }

        final def isInterrupted: Boolean = interrupted()
    }
}