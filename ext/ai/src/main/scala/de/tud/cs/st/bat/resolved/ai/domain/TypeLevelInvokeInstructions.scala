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

import analyses.{ Project, ClassHierarchy }
import de.tud.cs.st.bat.resolved.ai.IsReferenceType

/**
 *
 * @author Michael Eichberg
 */
trait TypeLevelInvokeInstructions { this: Domain[_] ⇒

    import ObjectType._

    protected def asTypedValue(pc: Int, someType: Type): Option[DomainValue] = {
        if (someType.isVoidType)
            None
        else
            Some(newTypedValue(pc, someType))
    }

    protected def handleInstanceBasedInvoke(
        pc: Int,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue]): Computation[Option[DomainValue], Set[DomainValue]] =
        isNull(operands.last) match {
            case Yes ⇒
                ThrowsException(Set(newInitializedObject(pc, NullPointerException)))
            case No ⇒
                ComputedValue(asTypedValue(pc, methodDescriptor.returnType))
            case Unknown ⇒
                ComputedValueAndException(
                    asTypedValue(pc, methodDescriptor.returnType),
                    Set(newObject(pc, NullPointerException)))
        }

    def invokeinterface(pc: Int,
                        declaringClass: ReferenceType,
                        name: String,
                        methodDescriptor: MethodDescriptor,
                        operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    def invokevirtual(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    def invokespecial(pc: Int,
                      declaringClass: ReferenceType,
                      name: String,
                      methodDescriptor: MethodDescriptor,
                      operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        handleInstanceBasedInvoke(pc, methodDescriptor, operands)

    def invokestatic(pc: Int,
                     declaringClass: ReferenceType,
                     name: String,
                     methodDescriptor: MethodDescriptor,
                     operands: List[DomainValue]): OptionalReturnValueOrExceptions =
        ComputedValue(asTypedValue(pc, methodDescriptor.returnType))

}

trait PerformInvocations[Source] extends TypeLevelInvokeInstructions { this: Domain[_] ⇒

    def project: Project[Source]

    def classHierarchy: ClassHierarchy

    def doInvokestatic(
        domain: Domain[_],
        pc: Int,
        declaringClass: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor,
        operands: List[DomainValue],
        isRecursiveCall: (ClassFile, Method, List[DomainValue]) ⇒ Boolean)(
            transformResult: (AIResult[domain.type]) ⇒ OptionalReturnValueOrExceptions): OptionalReturnValueOrExceptions = {

        def fallback = invokestatic(pc, declaringClass, methodName, methodDescriptor, operands)

        if (declaringClass.isArrayType)
            return fallback

        classHierarchy.resolveMethodReference(
            declaringClass.asObjectType,
            methodName,
            methodDescriptor,
            project) match {
                case Some((classFile, method)) if !method.isNative && !isRecursiveCall(classFile, method, operands) ⇒
                    //                    val newOperands = operands.reverse.zipWithIndex.map { operand_index ⇒
                    //                        val (operand, index) = operand_index
                    //                        operand.adapt(domain, -(index + 1))
                    //                    }.toArray(domain.DomainValueTag)
                    //                    transformResult(AI.perform(classFile, method, domain)(Some(newOperands)))
                    fallback
                case _ ⇒ fallback
            }
    }
}


