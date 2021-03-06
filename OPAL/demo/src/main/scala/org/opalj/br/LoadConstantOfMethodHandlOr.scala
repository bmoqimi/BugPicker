/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package br

import java.net.URL

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

import ObjectType.Class
import ObjectType.String
import br.analyses.AnalysisExecutor
import br.analyses.BasicReport
import br.analyses.OneStepAnalysis
import br.analyses.Project
import br.instructions.LoadMethodHandle
import br.instructions.LoadMethodHandle_W
import br.instructions.LoadMethodType
import br.instructions.LoadMethodType_W

/**
 * @author Michael Eichberg
 */
object LoadConstantOfMethodHandlOrMethodType extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String =
            "Prints information about loads of method handles and types."

        def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            val descriptor = MethodDescriptor(String, Class)
            val loads =
                for {
                    classFile ← project.classFiles
                    method @ MethodWithBody(code) ← classFile.methods
                    (pc, instruction) ← code.collect({
                        case LoadMethodHandle(mh)   ⇒ mh
                        case LoadMethodHandle_W(mh) ⇒ mh
                        case LoadMethodType(md)     ⇒ md
                        case LoadMethodType_W(md)   ⇒ md
                    })
                } yield {
                    classFile.fqn+" { "+
                        method.toJava+
                        "{ pc="+pc+
                        ";load constant="+instruction.valueToString+" } }"+
                        "<"+project.source(classFile.thisType)+">"
                }

            BasicReport(
                loads.mkString("\n\t")
            )
        }
    }
}