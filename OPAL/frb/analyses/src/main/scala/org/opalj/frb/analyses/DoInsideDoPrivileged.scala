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
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * This analysis reports invocations of `java.lang.reflect.Field|Method.setAccessible()`
 * outside of doPrivileged blocks.
 *
 * @author Ralf Mitschke
 * @author Roberts Kolosovs
 */
class DoInsideDoPrivileged[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String =
        "Detects calls to setAccessible() outside of doPrivileged blocks."

    private val ReflectFieldType =
        ObjectType("java/lang/reflect/Field")
    private val ReflectMethodType =
        ObjectType("java/lang/reflect/Method")
    private val PriviledgedActionType =
        ObjectType("java/security/PrivilegedAction")
    private val PriviledgedExceptionActionType =
        ObjectType("java/security/PrivilegedExceptionAction")

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def doAnalyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty,
        isInterrupted: () ⇒ Boolean): Iterable[MethodBasedReport[Source]] = {

        // For all classes referencing neither privilegedAction nor 
        // privilegedExceptionAction, look for methods that call setAccessible() on 
        // java/lang/reflect/{Field|Method}.
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if !classFile.interfaceTypes.contains(PriviledgedActionType) &&
                !classFile.interfaceTypes.contains(PriviledgedExceptionActionType)
            method @ MethodWithBody(body) ← classFile.methods
            (_, INVOKEVIRTUAL(ReflectFieldType | ReflectMethodType,
                "setAccessible", _)) ← body.associateWithIndex
        } yield {
            MethodBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                method,
                "Calls java.lang.reflect.Field|Method.setAccessible() outside of "+
                    "doPrivileged block")
        }
    }
}
