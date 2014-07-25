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

/**
 *
 * @note Mixin in this trait is strictly necessary when aliases are traced.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait IdentityBasedAliasBreakUpDetection extends CoreDomain {

    import java.util.{ IdentityHashMap ⇒ IDMap }

    // preallocated 
    protected[this] val aliasInformation = new IDMap[DomainValue, Integer]()

    protected[this] override def joinPostProcessing(
        updateType: UpdateType,
        pc: PC,
        oldOperands: Operands,
        oldLocals: Locals,
        newOperands: Operands,
        newLocals: Locals): Update[(Operands, Locals)] = {

        def liftUpdateType(v1Index: Int, v2Index: Int) = {
            //println(pc+": Lifted the udpate type; the following variables are no longer aliases: "+v1Index+" - "+v2Index)
            super.joinPostProcessing(
                StructuralUpdateType,
                pc, oldOperands, oldLocals, newOperands, newLocals)
        }

        if (updateType.isMetaInformationUpdate) {
            val aliasInformation = this.aliasInformation
            try {
                var opi = -1;
                oldOperands.foreach { op ⇒
                    val previousLocation = aliasInformation.get(op);
                    if (previousLocation == null)
                        aliasInformation.put(op, opi)
                    else {
                        // let's check if we can find the same alias in the new operands
                        if (newOperands(-previousLocation - 1) ne newOperands(-opi - 1))
                            return liftUpdateType(previousLocation, opi)
                    }
                    opi -= 1;
                }

                var li = 0;
                oldLocals.foreach { l ⇒
                    if ((l ne null) && (l ne TheIllegalValue)) {
                        val previousLocation = aliasInformation.get(l);
                        if (previousLocation == null)
                            aliasInformation.put(l, li)
                        else {
                            // let's check if we can find the same alias relation
                            if (previousLocation < 0) {
                                val v2 = newLocals(li)
                                if ((newOperands(-previousLocation - 1) ne v2) &&
                                    (v2 ne TheIllegalValue))
                                    return liftUpdateType(previousLocation, li)
                            } else /*previousLocation >= 0*/ {
                                val v1 = newLocals(previousLocation)
                                val v2 = newLocals(li)
                                if ((v1 ne v2) /* <=> the alias no longer exists */ &&
                                    // but, does it matter?
                                    (v1 ne TheIllegalValue) && (v2 ne TheIllegalValue))
                                    return liftUpdateType(previousLocation, li)
                            }
                        }
                    }
                    li += 1;
                }

            } finally {
                aliasInformation.clear
            }
        }

        super.joinPostProcessing(
            updateType,
            pc, oldOperands, oldLocals, newOperands, newLocals)
    }
}