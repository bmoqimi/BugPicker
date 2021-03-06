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
package instance

import org.opalj.bi.AccessFlagsMatcher._
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.br.analyses._
import org.opalj.ai.domain._
import org.opalj.ai.domain.l0._

import spec._

import solver.NaiveSolver

/**
 * Searches for strings that are passed to `Class.forName(_)` calls.
 *
 * @author Michael Eichberg and Ben Hermann
 */
abstract class StringPassedToClassForName[Source]
        extends DataFlowProblemSpecification[Source, (String) ⇒ Boolean] {

    type P = (String) ⇒ Boolean
    val definedInRestrictedPackage = p

    //
    // Specification of the sources and sinks
    //

    //    sources(Methods(
    //        properties = { case Method(PUBLIC___OR___PROTECTED_AND_NOT_FINAL(), _, md) ⇒ md.parametersCount >= 1 },
    //        parameters = { case (_ /*ID*/ , ObjectType.String) ⇒ true }
    //    ))

    sources(
        classFile ⇒ !definedInRestrictedPackage(classFile.thisType.packageName),
        {
            case method @ Method(PUBLIC___OR___PROTECTED_AND_NOT_FINAL(), _, md) ⇒
                md.selectParameter(_ == ObjectType.String).toSet.map(
                    parameterToValueIndex(method.isStatic, method.descriptor, _: Int)
                )
        }
    )

    sinks(
        Calls(
            {
                case (
                    ObjectType.Class, "forName",
                    SingleArgumentMethodDescriptor((ObjectType.String, ObjectType.Class))
                    ) ⇒ true
            }
        )
    )

    // Scenario: ... s.subString(...)
    call {
        case Invoke(
            ObjectType.String,
            _, // methodName
            MethodDescriptor(_, rt: ObjectType.String.type), // the called method returns a string
            _, // calling context (Method,...)
            _, // the caller
            receiver @ Tainted(_ /*String*/ ), // receiver type
            param @ _ // parameters,
            ) ⇒
            CallResult(
                receiver, // our string remains tainted
                param, // we don't care // example: r.addTo(Set s)
                ValueIsTainted // the RESULT
            )
    }

    // Scenario: assign a tainted string to a field of some class and mark the class as tainted
    write {
        case FieldWrite(
            _,
            _,
            ObjectType.String,
            _,
            _, // the caller
            Tainted(value: IsAReferenceValue), // receiver type
            receiver
            ) if value.isValueSubtypeOf(ObjectType.String).isYesOrUnknown ⇒
            ValueIsTainted
    }

}

object StringPassedToClassForName extends DataFlowProblemFactory with DataFlowProblemRunner {

    type P = (String) ⇒ Boolean

    //
    // Handling for the specified "java.security" file.
    //

    override def title = "StringPassedToClassForName"

    override def description = "Finds calls to Class.forName from non-privliged code"

    final val javaSecurityParameter = "-java.security="

    override def analysisSpecificParametersDescription: String =
        javaSecurityParameter+"<JRE/JDK Security Policy File>"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean = {
        parameters.size == 1 && {
            val securityFileParameter = parameters.head
            securityFileParameter.startsWith(javaSecurityParameter) &&
                new java.io.File(securityFileParameter.substring(javaSecurityParameter.length())).exists()
        }
    }

    override def processAnalysisParameters(parameters: Seq[String]): P = {
        val javaSecurityFile = parameters.head.substring(javaSecurityParameter.length())

        val restrictedPackages = process(new java.io.FileInputStream(javaSecurityFile)) { in ⇒
            val properties = new java.util.Properties()
            properties.load(in)
            properties.getProperty("package.access", "").
                split(",").
                map(_.trim.replace('.', '/'))
        }
        def definedInRestrictedPackage(packageName: String): Boolean =
            restrictedPackages.exists(packageName.startsWith(_))

        definedInRestrictedPackage
    }

    //
    // Factory method
    //

    override def create[Source](
        theProject: Project[Source],
        theP: P): DataFlowProblem[Source, P] = {
        object StringPassedToClassForNameWithSimpleSolver extends {
            // early definition block
            final val project = theProject
            final val p = theP
        } with StringPassedToClassForName[Source] with NaiveSolver[Source, P]
        StringPassedToClassForNameWithSimpleSolver
    }

}
