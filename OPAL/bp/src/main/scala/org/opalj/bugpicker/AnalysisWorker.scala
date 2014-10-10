package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.concurrent.Service
import javafx.concurrent.{ Service ⇒ jService, Task ⇒ jTask }
import scalafx.beans.property.ObjectProperty
import scala.xml.{ Node ⇒ xmlNode }
import org.opalj.br.analyses.Project
import org.opalj.ai.debug.XHTML
import scala.io.Source
import org.opalj.br.analyses.ProgressManagement
import java.net.URL

class AnalysisWorker(
    doc: ObjectProperty[xmlNode],
    project: Project[URL],
    initProgressManagement: Int ⇒ ProgressManagement) extends Service[Unit](new jService[Unit]() {

    protected def createTask(): jTask[Unit] = new jTask[Unit] {
        protected def call(): Unit = {
            val results @ (analysisTime, methodsWithDeadCode) = AnalysisRunner.analyze(project, Seq.empty, initProgressManagement)
            doc() = createHTMLReport(results)
        }

        def createHTMLReport(results: (Long, Iterable[BugReport])): scala.xml.Node = {
            var report = XHTML.createXHTML(Some(AnalysisRunner.title), DeadCodeAnalysis.resultsAsXHTML(results))

            val additionalStyles = process(getClass.getResourceAsStream("report.styles.css")) {
                Source.fromInputStream(_).mkString
            }
            val stylesNode = <style type="text/css">{ scala.xml.Unparsed(additionalStyles) }</style>

            val newHead = <head>{ (report \ "head" \ "_") }{ stylesNode }</head>

            new scala.xml.Elem(report.prefix, report.label, report.attributes, report.scope, false,
                (newHead ++ (report \ "body"): _*))
        }
    }
})