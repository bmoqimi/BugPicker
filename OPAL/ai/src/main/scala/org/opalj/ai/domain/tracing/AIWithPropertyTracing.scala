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
package tracing

import br._

/**
 * Abstract interpreter that (in combination with an appropriate domain)
 * facilitates the analysis of properties that are control-flow dependent.
 *
 * Basically this abstract interpreter can be used as a drop-in replacement
 * of the default abstract interpreter if the domain supports property
 * tracing.
 *
 * @author Michael Eichberg
 */
trait AIWithPropertyTracing[D <: Domain with PropertyTracing] extends AI[D] {

    /**
     * Performs an abstract interpretation of the given code snippet.
     *
     * Before actually starting the interpretation the domain is called to
     * let it initialize its properties.
     */
    override def perform(
        code: Code,
        theDomain: D)(
            initialOperands: theDomain.Operands,
            initialLocals: theDomain.Locals): AIResult { val domain: theDomain.type } = {

        theDomain.initProperties(code, initialOperands, initialLocals)
        super.perform(code, theDomain)(initialOperands, initialLocals)
    }
}

