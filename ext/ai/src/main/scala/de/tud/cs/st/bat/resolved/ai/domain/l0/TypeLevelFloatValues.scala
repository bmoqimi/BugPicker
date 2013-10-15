/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

import de.tud.cs.st.util.{ Answer, Yes, No, Unknown }

/**
 * @author Michael Eichberg
 */
trait TypeLevelFloatValues[+I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF FLOAT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Abstracts over all values with computational type `float`.
     */
    trait FloatValue extends Value { this: DomainValue ⇒
        final def computationalType: ComputationalType = ComputationalTypeFloat
    }

    private val typesAnswer: IsPrimitiveType = IsPrimitiveType(FloatType)

    abstract override def types(value: DomainValue): TypesAnswer = {
        value match {
            case r: FloatValue ⇒ typesAnswer
            case _             ⇒ super.types(value)
        }
    }

    protected def newFloatValue(): DomainValue

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    def fcmpg(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue(pc)

    def fcmpl(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newIntegerValue(pc)

    //
    // UNARY EXPRESSIONS
    //
    def fneg(pc: PC, value: DomainValue) = newFloatValue()

    //
    // BINARY EXPRESSIONS
    //

    def fadd(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newFloatValue()

    def fdiv(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newFloatValue()

    def fmul(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newFloatValue()

    def frem(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newFloatValue()

    def fsub(pc: PC, value1: DomainValue, value2: DomainValue): DomainValue =
        newFloatValue()

    //
    // TYPE CONVERSIONS
    //

    def f2d(pc: PC, value: DomainValue): DomainValue = newDoubleValue(pc)
    def f2i(pc: PC, value: DomainValue): DomainValue = newIntegerValue(pc)
    def f2l(pc: PC, value: DomainValue): DomainValue = newLongValue(pc)

}


