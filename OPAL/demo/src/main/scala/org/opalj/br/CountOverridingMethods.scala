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
package br

import java.net.URL

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

/**
 * Counts the number of methods that override a concrete method.
 *
 * This analysis is an overapproximation due to the fact that we do not consider
 * the relation between the visibility modifiers and packages.
 *
 * For example, a method m in a Class C in package c that is public does not override
 * the method m in C's superclass B that is in package b and where the method has
 * default (package) visibility
 *
 * @author Michael Eichberg
 */
object CountOverridingMethods extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String =
            "Counts the number of methods that override a method."

        def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            import project.classHierarchy

            var methodsCount = 0
            var methodsThatOverrideAnotherMethodCount = 0

            def classFileHasImplementedMethod(
                methodName: String,
                methodDescriptor: MethodDescriptor)(
                    classFile: ClassFile): Boolean = {

                classFile.findMethod(methodName, methodDescriptor).map(method ⇒
                    !method.isAbstract && !method.isPrivate
                ).getOrElse(false)
            }

            var results = List[String]()
            for {
                classFile ← project.classFiles
                if !classFile.isInterfaceDeclaration
                method ← classFile.methods
                if !method.isPrivate
                if !method.isAbstract
                if !method.isStatic
                if method.name != "<init>" && method.name != "<clinit>"
            } {
                val implementsMethod = classFileHasImplementedMethod(method.name, method.descriptor) _

                methodsCount += 1
                classHierarchy.superclasses(classFile.thisType, project) {
                    !(_: ClassFile).isInterfaceDeclaration
                }.find(superclass ⇒ implementsMethod(superclass)) match {
                    case Some(cf) ⇒
                        results = (classFile.fqn+
                            " inherits from "+cf.fqn+
                            " overrides "+method.name + method.descriptor.toUMLNotation) :: results
                        methodsThatOverrideAnotherMethodCount += 1
                    case None ⇒ /*OK*/
                }
            }

            BasicReport(
                "Overridden methods:"+results.mkString("\n\t", "\n\t", "\n")+
                    "Overall number of relevant methods: "+methodsCount+
                    "\nNumber of methods that override a parent's (non-abstract) method: "+methodsThatOverrideAnotherMethodCount
            )
        }
    }
}