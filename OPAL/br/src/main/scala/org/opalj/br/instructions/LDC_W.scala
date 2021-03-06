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
package br
package instructions

import language.existentials

/**
 * Push item from runtime constant pool.
 *
 * @author Michael Eichberg
 */
sealed abstract class LDC_W[@specialized(Int, Float) T]
        extends LoadConstantInstruction[T] {

    final def opcode: Opcode = 19

    final def mnemonic: String = "ldc_w"

    final def length: Int = 3
}

final case class LoadInt_W(value: Int) extends LDC_W[Int]

final case class LoadFloat_W(value: Float) extends LDC_W[Float]

final case class LoadClass_W(value: ReferenceType) extends LDC_W[ReferenceType]

final case class LoadMethodHandle_W(value: MethodHandle) extends LDC_W[MethodHandle]

final case class LoadMethodType_W(value: MethodDescriptor) extends LDC_W[MethodDescriptor]

final case class LoadString_W(value: String) extends LDC_W[String]

/**
 * Defines factory and extractor methods for LDC_W instructions.
 *
 * @author Michael Eichberg
 */
object LDC_W {

    def apply(constantValue: ConstantValue[_]): LDC_W[_] = {
        constantValue.value match {
            case i: Int               ⇒ LoadInt_W(i)
            case f: Float             ⇒ LoadFloat_W(f)
            case r: ReferenceType     ⇒ LoadClass_W(r)
            case s: String            ⇒ LoadString_W(s)
            case mh: MethodHandle     ⇒ LoadMethodHandle_W(mh)
            case md: MethodDescriptor ⇒ LoadMethodType_W(md)
            case _ ⇒
                throw new BytecodeProcessingFailedException(
                    "unsupported constant value: "+constantValue)
        }
    }

    def unapply[T](ldc: LDC_W[T]): Option[T] = Some(ldc.value)
}

