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
package instructions

import language.existentials

/**
 * Push item from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC[@specialized(Int, Float) T] extends LoadConstantInstruction {

    def value: T

    def opcode: Int = 18

    def mnemonic: String = "ldc"

    def indexOfNextInstruction(currentPC: Int, code: Code): Int = currentPC + 2
}

case class LoadInt(value: Int) extends LDC[Int]

case class LoadFloat(value: Float) extends LDC[Float]

case class LoadClass(value: ReferenceType) extends LDC[ReferenceType]

case class LoadString(value: String) extends LDC[String]

object LDC {

    def apply(constantValue: ConstantValue[_]): LDC[_] = {
        constantValue.value match {
            case i: Int           ⇒ LoadInt(i)
            case f: Float         ⇒ LoadFloat(f)
            case r: ReferenceType ⇒ LoadClass(r)
            case s: String        ⇒ LoadString(s)
            case _                ⇒ BATException("unsupported constant value: "+constantValue)
        }
    }

    def unapply[T](ldc: LDC[T]): Option[T] = Some(ldc.value)
}
