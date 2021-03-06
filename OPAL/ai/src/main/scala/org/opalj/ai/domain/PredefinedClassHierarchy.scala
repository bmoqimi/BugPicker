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

import org.opalj.br.Code
import org.opalj.br.analyses.{ ClassHierarchy ⇒ TheClassHierarchy }

/**
 * This class uses OPAL's `preInitializedClassHierarchy` (see `ClassHierarchy` for details)
 * for class hierarchy related queries.
 *
 * '''Use this trait ONLY if you just want to do some testing.'''
 *
 * @author Michael Eichberg
 */
trait PredefinedClassHierarchy extends ClassHierarchy {

    /**
     * Returns the predefined class hierarchy unless explicitly overridden. OPAL's
     * built-in default class hierarchy only reflects the type-hierarchy between the
     * most basic types – in particular between the exceptions potentially thrown
     * by JVM instructions.
     *
     * @note '''This method is not intended to be overridden.''' Use a different domain
     *      as a foundation.
     */
    final def classHierarchy: TheClassHierarchy = Code.preDefinedClassHierarchy

}

