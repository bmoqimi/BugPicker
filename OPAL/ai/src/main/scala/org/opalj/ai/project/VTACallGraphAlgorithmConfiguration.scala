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
package project

import scala.collection.Set
import scala.collection.Map

import domain._

import br._
import br.analyses._

/**
 * Configuration of a call graph algorithm that uses CHA.
 *
 * ==Thread Safety==
 * This class is thread-safe (it contains no mutable state.)
 *
 * ==Usage==
 * Instances of this class are passed to a `CallGraphFactory`'s `create` method.
 *
 * @author Michael Eichberg
 */
class VTACallGraphAlgorithmConfiguration extends CallGraphAlgorithmConfiguration {

    type Contour = MethodSignature
    type Value = Set[Method]
    type Cache = CallGraphCache[Contour, Value]
    def Cache(): this.type#Cache = new CallGraphCache[MethodSignature, Value]

    def Domain[Source](
        theProject: Project[Source],
        cache: Cache,
        classFile: ClassFile,
        method: Method): VTACallGraphDomain =
        new DefaultVTACallGraphDomain(theProject, cache, classFile, method, 2)
}

