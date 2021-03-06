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
 * This analysis reports code that calls `SomeCollectionClassObject.toArray(T[])` with
 * zero-length array argument, for example:
 * {{{
 * myList.toArray(new T[0])
 * }}}
 * This is bad because this `toArray()` call will never optimize for speed by re-using the
 * array passed in as argument for returning the result. Such code should do something
 * like this instead:
 * {{{
 * myList.toArray(new T[myList.size()])
 * }}}
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 */
class InefficientToArray[Source] extends FindRealBugsAnalysis[Source] {

    /**
     * Returns a description text for this analysis.
     * @return analysis description
     */
    override def description: String = "Reports inefficient toArray(T[]) calls"

    private val objectArrayType = ArrayType(ObjectType.Object)
    private val toArrayDescriptor = MethodDescriptor(IndexedSeq(objectArrayType),
        objectArrayType)
    private val collectionInterface = ObjectType("java/util/Collection")
    private val listInterface = ObjectType("java/util/List")

    /**
     * Checks whether a type inherits from java/util/Collection or is java/util/List.
     * @param classHierarchy class hierarchy to search in
     * @param checkedType type, that is checked if it's a collection or list
     * @return true, if checkedType is a collection or list, false otherwise
     */
    private def isCollectionType(
        classHierarchy: ClassHierarchy)(checkedType: ReferenceType): Boolean = {
        checkedType.isObjectType &&
            (classHierarchy.isSubtypeOf(checkedType.asObjectType,
                collectionInterface).isNoOrUnknown || checkedType == listInterface)
        // TODO needs more heuristic or more analysis
    }

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
        isInterrupted: () ⇒ Boolean): Iterable[LineAndColumnBasedReport[Source]] = {

        val classHierarchy: ClassHierarchy = project.classHierarchy
        val isCollectionType = this.isCollectionType(classHierarchy) _

        // In all method bodies, look for calls to "toArray()" with "new ...[0]" argument,
        // on objects derived from the Collection classes.
        for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            method @ MethodWithBody(body) ← classFile.methods
            pc ← body.matchTriple {
                case (ICONST_0,
                    _: ANEWARRAY,
                    VirtualMethodInvocationInstruction(targetType, "toArray", `toArrayDescriptor`)
                    ) ⇒
                    isCollectionType(targetType)
                case _ ⇒ false
            }
        } yield {
            LineAndColumnBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                method.descriptor,
                method.name,
                body.lineNumber(pc),
                None,
                "Calling x.toArray(new T[0]) is inefficient, should be "+
                    "x.toArray(new T[x.size()])")
        }
    }
}
