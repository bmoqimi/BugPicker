/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
