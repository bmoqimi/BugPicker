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

import language.implicitConversions

/**
 * Records the values returned by the method. (Note that the computational type
 * of the value(s) is not recorded. It is directly determined by
 * the signature of method that is analyzed or can be extracted using the respective
 * method.)
 *
 * This trait can be used to record the return values independently of
 * the precision of the domain.
 *
 * ==Usage==
 * A domain that mixes in this trait should only be used to analyze a single method.
 *
 * ==Thread Safety==
 * This class is not thread safe. I.e., this domain can only be used if
 * an instance of this domain is not used by multiple threads.
 *
 * @author Michael Eichberg
 */
trait RecordReturnedValues extends Domain {

    /**
     * This type determines in which way the returned values are recorded.
     *
     * For example, if it is sufficient to just record the last value that was
     * returned by a specific return instruction, then the type could be `DomainValue`
     * and the implementation of `joinReturnedValues(...)` would just return the last
     * given value. Furthermore, `returnedValue` would be the identity function.
     *
     * However, if you have a (more) precise domain you may want to collect all
     * returned values. In this case the type of `ReturnedValue` could be Set[DomainValue].
     */
    type ReturnedValue <: AnyRef

    /**
     * Wraps the given value into a `ReturnedValue`.
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    def returnedValue(value: DomainValue): ReturnedValue

    /**
     * Joins the previously returned value and the newly given `value`. Both values
     * are returned by the same return instruction (same `pc`).
     *
     * @see For details study the documentation of the abstract type `ReturnedValue`
     *      and study the subclass(es) of `RecordReturnedValues`.
     */
    def joinReturnedValues(
        previouslyReturnedValue: ReturnedValue,
        value: DomainValue): ReturnedValue

    @volatile private[this] var returnedValues: Map[PC, ReturnedValue] = Map.empty

    def allReturnedValues: Map[PC, ReturnedValue] = returnedValues

    protected[this] def recordReturnedValue(pc: PC, value: DomainValue) {
        returnedValues =
            returnedValues.updated(
                pc,
                returnedValues.get(pc) match {
                    case Some(returnedValue) ⇒
                        joinReturnedValues(returnedValue, value)
                    case None ⇒
                        returnedValue(value)
                }
            )
    }

    abstract override def areturn(pc: PC, value: DomainValue) {
        recordReturnedValue(pc, value)
        super.areturn(pc,value)
    }

    abstract override def dreturn(pc: PC, value: DomainValue) {
        recordReturnedValue(pc, value)
        super.dreturn(pc,value)
    }

    abstract override def freturn(pc: PC, value: DomainValue) {
        recordReturnedValue(pc, value)
        super.freturn(pc,value)
    }

    abstract override def ireturn(pc: PC, value: DomainValue) {
        recordReturnedValue(pc, value)
        super.ireturn(pc,value)
    }

    abstract override def lreturn(pc: PC, value: DomainValue) {
        recordReturnedValue(pc, value)
        super.lreturn(pc,value)
    }

}

