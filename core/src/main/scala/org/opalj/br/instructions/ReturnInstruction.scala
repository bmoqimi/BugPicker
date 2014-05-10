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
package instructions

/**
 * An instruction that returns from a method.
 *
 * @author Michael Eichberg
 */
abstract class ReturnInstruction extends Instruction {

    final override def runtimeExceptions: List[ObjectType] = 
        ReturnInstruction.runtimeExceptions

    final override def indexOfNextInstruction(currentPC: Int, code: Code): Int =
        currentPC + 1

    final override def nextInstructions(currentPC: PC, code: Code): PCs = {
        code.exceptionHandlersFor(currentPC) find { handler ⇒
            handler.catchType.isEmpty ||
                Code.preDefinedClassHierarchy.isSubtypeOf(
                    ObjectType.IllegalMonitorStateException,
                    handler.catchType.get).isYes
        } match {
            case Some(handler) ⇒ collection.mutable.UShortSet(handler.startPC)
            case None          ⇒ collection.mutable.UShortSet.empty
        }
    }

}
object ReturnInstruction {

    val runtimeExceptions = List(ObjectType.IllegalMonitorStateException)

}