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
package de.tud.cs.st
package bat
package resolved
package ai
package project

import bat.resolved.analyses.{ SomeProject, Project }
import bat.resolved.ai.domain._

/**
 * Configuration of a specific call graph algorithm. Basically, the configuration
 * consist of a method to create a `Cache` object that will be used during the
 * computation of the call graph and a factory method to create new domain instance
 * for each method that is analyzed during the construction of the graph.
 *
 * @author Michael Eichberg
 */
trait CallGraphAlgorithmConfiguration {

    /**
     * The contour identifies the key of the CallGraphCache.
     */
    type Contour

    /**
     * The type of the cached values. 
     */
    type Value

    type Cache <: CallGraphCache[Contour, Value]

    /**
     * Creates a new cache that is used to cache intermediate results while
     * computing the call graph.
     *
     * Usually created only once per run.
     */
    def Cache(): this.type#Cache
    

    /**
     * Returns the new domain object that will be used to analyze the given
     * method.
     */
    def Domain(
        theProject: SomeProject,
        cache: this.type#Cache,
        classFile: ClassFile,
        method: Method): CallGraphDomain

}