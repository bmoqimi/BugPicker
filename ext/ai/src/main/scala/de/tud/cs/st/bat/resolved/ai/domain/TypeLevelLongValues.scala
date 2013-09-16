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

trait TypeLevelLongValues[I] extends Domain[I] {

    // -----------------------------------------------------------------------------------
    //
    // REPRESENTATION OF LONG VALUES
    //
    // -----------------------------------------------------------------------------------

    trait LongValue extends Value {
        final def computationalType: ComputationalType = ComputationalTypeLong
    }

    private val typesAnswer: IsPrimitiveType = IsPrimitiveType(LongType)

    abstract override def types(value: DomainValue): TypesAnswer[_] = {
        value match {
            case r: LongValue ⇒ typesAnswer
            case _            ⇒ super.types(value)
        }
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING OF COMPUTATIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // RELATIONAL OPERATORS
    //
    def lcmp(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newIntegerValue

    //
    // UNARY EXPRESSIONS
    //
    def lneg(pc: Int, value: DomainValue) = newLongValue

    //
    // BINARY EXPRESSIONS
    //

    def ladd(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def land(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def ldiv(pc: Int, value1: DomainValue, value2: DomainValue) = ComputedValue(newLongValue)
    def lmul(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lrem(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lsub(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue
    def lxor(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = newLongValue

    //
    // CONVERSION INSTRUCTIONS
    //

    def l2d(pc: Int, value: DomainValue): DomainValue = newDoubleValue
    def l2f(pc: Int, value: DomainValue): DomainValue = newFloatValue
    def l2i(pc: Int, value: DomainValue): DomainValue = newIntegerValue
}

trait DefaultTypeLevelLongValues[I]
        extends DefaultValueBinding[I]
        with TypeLevelLongValues[I] {

    case object LongValue extends super.LongValue {
        override def merge(pc: Int, value: DomainValue): Update[DomainValue] = value match {
            case LongValue ⇒ NoUpdate
            case _         ⇒ MetaInformationUpdateIllegalValue
        }
    }

    def newLongValue(): LongValue = LongValue

    def newLongValue(pc: Int, value: Long): LongValue = LongValue
}

