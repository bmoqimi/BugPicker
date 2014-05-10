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
package domain

import de.tud.cs.st.util.{ Yes, No, Unknown }

/**
 * Provides a default implementation for the instructions related to synchronization.
 *
 * @author Michael Eichberg
 */
trait IgnoreSynchronization { this: Domain ⇒

    protected[this] def sideEffectOnlyOrNullPointerException(
        pc: PC,
        value: DomainValue): Computation[Nothing, ExceptionValue] = {
        refIsNull(value) match {
            case Yes ⇒
                ThrowsException(NullPointerException(pc))
            case No ⇒
                ComputationWithSideEffectOnly
            case Unknown ⇒
                ComputationWithSideEffectOrException(NullPointerException(pc))
        }
    }

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    override def monitorenter(
        pc: PC,
        value: DomainValue): Computation[Nothing, ExceptionValue] = {
        sideEffectOnlyOrNullPointerException(pc, value)
    }

    /**
     * Handles a `monitorenter` instruction.
     *
     * @note The default implementation checks if the given value is `null` and raises
     * an exception if it is `null` or maybe `null`. In the later case or in case that
     * the value is known not to be `null` the given value is (also) returned as this
     * computation's results.
     */
    override def monitorexit(
        pc: PC,
        value: DomainValue): Computation[Nothing, ExceptionValue] = {
        sideEffectOnlyOrNullPointerException(pc, value)
    }
}




