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
package ai
package dataflow

import java.net.URL

import scala.collection.{ Map, Set }

import bi.AccessFlagsMatcher

import br._
import br.analyses._
import br.instructions._

import domain._
import domain.l0._

/**
 * Support methods to make it possible to solve a single [[DataFlowProblem]].
 *
 * ==Usage==
 * Mix in this trait in the object which specifies your data-flow problem. After
 * that you can run it.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait DataFlowProblemRunner extends AnalysisExecutor {
    dataFlowProblemFactory: DataFlowProblemFactory ⇒

    final override val analysis = new Analysis[URL, ReportableAnalysisResult] {

        override def title: String =
            dataFlowProblemFactory.title

        override def description: String =
            dataFlowProblemFactory.description

        override def analyze(
            project: Project[URL],
            parameters: Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement): ReportableAnalysisResult = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            val pm = initProgressManagement(2)
            pm.start(1, "setup")
            val initializedDataFlowProblem = time {
                val params = dataFlowProblemFactory.processAnalysisParameters(parameters)
                val dataFlowProblem = dataFlowProblemFactory.create(project, params)
                dataFlowProblem.initializeSourcesAndSinks
                println(f"[info] Number of source values: ${dataFlowProblem.sourceValues.size}.")
                println(f"[info] Number of sinks: ${dataFlowProblem.sinkInstructions.size}.")
                dataFlowProblem
            } { t ⇒
                println(f"[info] Setup of the data-flow problem took ${ns2sec(t)}%.4f seconds.")
            }
            pm.end(1)

            if (pm.isInterrupted())
                return null

            pm.start(2, "solving data-flow problem")
            val result = time {
                initializedDataFlowProblem.solve()
            } { t ⇒
                println(f"[info] Solving the data-flow problem took ${ns2sec(t)}%.4f seconds.")
            }
            pm.end(2)

            BasicReport(result)
        }
    }
}

