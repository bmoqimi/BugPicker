package org.opalj
package bugpicker

import java.net.URL

import scala.io.Source
import scala.xml.{ Node ⇒ xmlNode }

import org.opalj.ai.debug.XHTML
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.Project
import org.opalj.bugpicker.analysis.AnalysisParameters
import org.opalj.bugpicker.analysis.BugReport
import org.opalj.bugpicker.analysis.DeadCodeAnalysis

import javafx.concurrent.{ Service ⇒ jService }
import javafx.concurrent.{ Task ⇒ jTask }
import scalafx.Includes._
import scalafx.beans.property.ObjectProperty
import scalafx.concurrent.Service

class AnalysisWorker(
    doc: ObjectProperty[xmlNode],
    project: Project[URL],
    parameters: AnalysisParameters,
    initProgressManagement: Int ⇒ ProgressManagement) extends Service[Unit](new jService[Unit]() {

    protected def createTask(): jTask[Unit] = new jTask[Unit] {
        protected def call(): Unit = {
            val results @ (analysisTime, methodsWithDeadCode) = AnalysisRunner.analyze(project, parameters.toStringParameters, initProgressManagement)
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