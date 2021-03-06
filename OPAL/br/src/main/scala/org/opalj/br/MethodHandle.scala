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

/**
 * A method handle.
 *
 * @author Michael Eichberg
 */
sealed trait MethodHandle extends ConstantValue[MethodHandle] {

    final override def value: this.type = this

    /**
     * Returns `ObjectType.MethodHandle`;
     * the type of the value pushed onto the stack by an ldc(_w) instruction.
     */
    override def valueType: ObjectType = ObjectType.MethodHandle

    override def valueToString: String = this.toString

    def toJava: String
}

sealed trait FieldAccessMethodHandle extends MethodHandle {
    def declaringClassType: ObjectType
    def name: String
    def fieldType: FieldType

    def asVirtualField = VirtualField(declaringClassType, name, fieldType)

    override def toJava: String = {
        val handleType = getClass.getSimpleName.toString
        val fieldName = declaringClassType.toJava+"."+name
        val returnType = ": "+fieldType.toJava
        handleType+": "+fieldName + returnType
    }
}

sealed trait FieldReadAccessMethodHandle extends FieldAccessMethodHandle

sealed trait FieldWriteAccessMethodHandle extends FieldAccessMethodHandle

case class GetFieldMethodHandle(
    declaringClassType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldReadAccessMethodHandle

case class GetStaticMethodHandle(
    declaringClassType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldReadAccessMethodHandle

case class PutFieldMethodHandle(
    declaringClassType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldWriteAccessMethodHandle

case class PutStaticMethodHandle(
    declaringClassType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldWriteAccessMethodHandle

trait MethodCallMethodHandle extends MethodHandle {
    def receiverType: ReferenceType
    def name: String
    def methodDescriptor: MethodDescriptor

    override def toJava: String = {
        val handleType = getClass.getSimpleName
        val typeName = receiverType.toJava
        val methodCall = name + methodDescriptor.toUMLNotation
        handleType+": "+typeName+"."+methodCall
    }

    def opcodeOfUnderlyingInstruction: Opcode
}

object MethodCallMethodHandle {

    def unapply(handle: MethodCallMethodHandle): Option[(ReferenceType, String, MethodDescriptor)] =
        Some((handle.receiverType, handle.name, handle.methodDescriptor))

}

case class InvokeVirtualMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle {

    override val opcodeOfUnderlyingInstruction = instructions.INVOKEVIRTUAL.opcode
}

case class InvokeStaticMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle {

    override val opcodeOfUnderlyingInstruction = instructions.INVOKESTATIC.opcode
}

case class InvokeSpecialMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle {

    override val opcodeOfUnderlyingInstruction = instructions.INVOKESPECIAL.opcode
}

case class NewInvokeSpecialMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle {

    override val opcodeOfUnderlyingInstruction = instructions.INVOKESPECIAL.opcode
}

case class InvokeInterfaceMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle {

    override val opcodeOfUnderlyingInstruction = instructions.INVOKEINTERFACE.opcode
}
