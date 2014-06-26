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
package domain
package l1

import org.opalj.br.{ ClassFile, Method }
import org.opalj.br.analyses.Project

/**
 * This domain uses the l1 level ''stable'', partial domains.
 *
 * @author Michael Eichberg
 */
class DefaultConfigurableDomain[I, Source](
    val id: I,
    val project: Project[Source],
    val classFile: ClassFile,
    val method: Method)
        extends Domain
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with ProjectBasedClassHierarchy
        with TheProject[Source]
        with TheMethod
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l1.DefaultReferenceValuesBinding
        // [NOT YET NEEDED] with PerInstructionPostProcessing
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultStringValuesBinding
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultClassValuesBinding
        // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
        with l1.DefaultIntegerRangeValues {

    type Id = I

    override protected def maxSizeOfIntegerRanges: Long = 25l

}

class DefaultDomain[Source](
    project: Project[Source],
    classFile: ClassFile,
    method: Method)
        extends DefaultConfigurableDomain[String, Source](
            classFile.thisType.toJava+"{ "+method.toJava+"}",
            project,
            classFile,
            method)