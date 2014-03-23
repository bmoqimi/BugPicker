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

/**
 * A method handle.
 *
 * @author Michael Eichberg
 */
sealed trait MethodHandle extends BootstrapArgument {
    def toJava(bootstrapArguments: BootstrapArguments): String
}

sealed trait FieldAccessMethodHandle extends MethodHandle {
    def declaringType: ObjectType
    def name: String
    def fieldType: FieldType

    override def toJava(bootstrapArguments: BootstrapArguments): String = {
        val handleType = getClass.getSimpleName.toString
        val fieldName = declaringType.toJava+"."+name
        val returnType = ": "+fieldType.toJava
        handleType+": "+fieldName + returnType
    }
}

case class GetFieldMethodHandle(
    declaringType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldAccessMethodHandle

case class GetStaticMethodHandle(
    declaringType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldAccessMethodHandle

case class PutFieldMethodHandle(
    declaringType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldAccessMethodHandle

case class PutStaticMethodHandle(
    declaringType: ObjectType,
    name: String,
    fieldType: FieldType)
        extends FieldAccessMethodHandle

trait MethodCallMethodHandle extends MethodHandle {
    def receiverType: ReferenceType
    def name: String
    def methodDescriptor: MethodDescriptor

    override def toJava(bootstrapArguments: BootstrapArguments): String = {
        val handleType = getClass.getSimpleName.toString
        val typeName = receiverType.toJava
        val methodCall = name + methodDescriptor.toUMLNotation
        handleType+": "+typeName+"."+methodCall
    }
}

case class InvokeVirtualMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle

case class InvokeStaticMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle

case class InvokeSpecialMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle

case class NewInvokeSpecialMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle

case class InvokeInterfaceMethodHandle(
    receiverType: ReferenceType,
    name: String,
    methodDescriptor: MethodDescriptor)
        extends MethodCallMethodHandle