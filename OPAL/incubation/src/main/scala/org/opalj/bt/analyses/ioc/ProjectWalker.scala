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
package analyses
package ioc

import instructions._

/**
 * Controls the execution of multiple analyses; i.e., the director takes care of
 * traversing all resources of a project and calling the individual analyses to perform
 * certain steps of the analysis. This inversion of control makes the implementation of
 * many analyses easier and also enables several types of optimizations to speed up the
 * overall processing.
 *
 * @todo This implementation is not yet usable.
 * @author Michael Eichberg
 */
trait AnalysesDirector[Source]
        extends Analysis[Source, Iterable[ReportableAnalysisResult]] {

    def description: String = " TODO "

    protected[this] val results = scala.collection.mutable.Buffer[ReportableAnalysisResult]()

    def result(result: ReportableAnalysisResult): Unit = results.synchronized {
        results += result
    }

    /**
     * Analyzes the given project and reports the result(s).
     */
    def analyze(project: Project[Source]): Iterable[ReportableAnalysisResult] = {

        results
    }

    def project: Project[Source]

    def classFile: ClassFile

    @throws[IllegalStateException]("Defined only during the analysis of a method")
    def method: Method

    def register(analysis: DirectedAnalysis[Source])
}

trait DirectedAnalysis[Source] {

    def director: AnalysesDirector[Source]

    def project = director.project

    def classFile: ClassFile = director.classFile

    /**
     * Returns the method that is currently analyzed.
     */
    def method: Method = director.method

    def result(result: ReportableAnalysisResult)

    def classFile(f: PartialFunction[ClassFile, Boolean]): Boolean = { true }

    def field(f: PartialFunction[Field, Boolean]): Boolean = { true }

    def method(f: PartialFunction[Method, Boolean]): Boolean = { true }

    def Code: Code = method.body.get

    def code(f: PartialFunction[Code, _]): Unit = {}
}

//class FinalClassDeclaresProtectedField[Source](
//    val director: AnalysesDirector[Source])
//        extends DirectedAnalysis[Source] {
//
//    classFile { case c if c.isFinal ⇒ true }
//
//    field {
//        case f if f.isProtected ⇒ result(
//            FieldBasedReport[Source](
//                project.sources.get(classFile.thisType),
//                f.toJavaSignature,
//                Some("note"),
//                "The class "+
//                    classFile.thisType.toJava+
//                    " is final, but declares a protected field: "+
//                    f.name+"."
//            ))
//    }
//}