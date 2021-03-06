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
package reader

import scala.language.implicitConversions
import scala.reflect.ClassTag

import org.opalj.bi.{ AttributeParent, AttributesParent }
import org.opalj.bi.reader.Constant_PoolReader

/**
 * A representation of the constant pool.
 *
 * @note The constant pool is considered to be static; i.e., references between
 *    constant pool entries are always resolved at most once and the results are cached.
 *    Hence, after reading the constant pool the constant pool is treated as
 *    immutable; the referenced constant pool entry must not change.
 *
 * @author Michael Eichberg
 */
trait ConstantPoolBinding extends Constant_PoolReader {

    type ClassFile <: br.ClassFile

    implicit def cpIndexTocpEntry(
        index: Constant_Pool_Index)(
            implicit cp: Constant_Pool): Constant_Pool_Entry =
        cp(index)

    trait Constant_Pool_Entry extends bi.reader.ConstantPoolEntry {
        def asString: String =
            throw new BytecodeProcessingFailedException(
                "conversion to string is not supported")

        def asFieldType: FieldType =
            throw new BytecodeProcessingFailedException(
                "conversion to field type is not supported")

        def asMethodDescriptor: MethodDescriptor =
            throw new BytecodeProcessingFailedException(
                "conversion to method descriptor is not supported")

        def asFieldTypeSignature: FieldTypeSignature =
            throw new BytecodeProcessingFailedException(
                "conversion to field type signature is not supported")

        def asSignature(ap: AttributeParent): Signature =
            throw new BytecodeProcessingFailedException(
                "conversion to signature attribute is not supported")

        def asConstantValue(cp: Constant_Pool): ConstantValue[_] =
            throw new BytecodeProcessingFailedException(
                "conversion of "+this.getClass.getSimpleName+" to constant value is not supported")

        def asConstantFieldValue(cp: Constant_Pool): ConstantFieldValue[_] =
            throw new BytecodeProcessingFailedException(
                "conversion of "+this.getClass.getSimpleName+" to constant field value is not supported")

        def asFieldref(cp: Constant_Pool): (ObjectType, String, FieldType) =
            throw new BytecodeProcessingFailedException(
                "conversion to field ref is not supported")

        def asMethodref(cp: Constant_Pool): (ReferenceType, String, MethodDescriptor) =
            throw new BytecodeProcessingFailedException(
                "conversion to method ref is not supported")

        def asObjectType(cp: Constant_Pool): ObjectType =
            throw new BytecodeProcessingFailedException(
                "conversion to object type is not supported")

        def asReferenceType(cp: Constant_Pool): ReferenceType =
            throw new BytecodeProcessingFailedException(
                "conversion to object type is not supported")

        def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument =
            throw new BytecodeProcessingFailedException(
                "conversion to bootstrap argument is not supported")

        def asMethodHandle(cp: Constant_Pool): MethodHandle =
            throw new BytecodeProcessingFailedException(
                "conversion to method handle is not supported")

        def asNameAndType: CONSTANT_NameAndType_info =
            throw new BytecodeProcessingFailedException(
                "conversion to name and type info is not supported")

        def asInvokeDynamic: CONSTANT_InvokeDynamic_info =
            throw new BytecodeProcessingFailedException(
                "conversion to invoke dynamic info is not supported")
    }

    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    case class CONSTANT_Class_info(
        name_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        override def asConstantValue(cp: Constant_Pool) =
            ConstantClass(asReferenceType(cp))

        override def asObjectType(cp: Constant_Pool) =
            ObjectType(cp(name_index).asString)

        override def asReferenceType(cp: Constant_Pool) =
            ReferenceType(cp(name_index).asString)

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument =
            asConstantValue(cp)
    }

    trait CONSTANT_FieldValue_info extends Constant_Pool_Entry {

        final override def asConstantValue(cp: Constant_Pool): ConstantFieldValue[_] =
            asConstantFieldValue(cp)

    }

    case class CONSTANT_Double_info(
        value: ConstantDouble)
            extends CONSTANT_FieldValue_info {

        def this(value: Double) { this(ConstantDouble(value)) }

        override def asConstantFieldValue(cp: Constant_Pool): ConstantDouble = value

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
            asConstantValue(cp)
        }
    }

    case class CONSTANT_Float_info(
        value: ConstantFloat)
            extends CONSTANT_FieldValue_info {

        def this(value: Float) { this(ConstantFloat(value)) }

        override def asConstantFieldValue(cp: Constant_Pool): ConstantFloat = value

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
            asConstantValue(cp)
        }
    }

    case class CONSTANT_Integer_info(
        value: ConstantInteger)
            extends CONSTANT_FieldValue_info {

        def this(value: Int) { this(ConstantInteger(value)) }

        override def asConstantFieldValue(cp: Constant_Pool): ConstantInteger = value

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
            asConstantValue(cp)
        }

    }

    case class CONSTANT_Long_info(
        value: ConstantLong)
            extends CONSTANT_FieldValue_info {

        def this(value: Long) { this(ConstantLong(value)) }

        override def asConstantFieldValue(cp: Constant_Pool): ConstantLong = value

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
            asConstantValue(cp)
        }
    }

    case class CONSTANT_Utf8_info(
        value: String)
            extends Constant_Pool_Entry {

        override def asString = value

        private[this] var methodDescriptor: MethodDescriptor = null // to cache the result
        override def asMethodDescriptor = {
            if (methodDescriptor eq null) { methodDescriptor = MethodDescriptor(value) };
            methodDescriptor
        }

        private[this] var fieldType: FieldType = null // to cache the result
        override def asFieldType = {
            if (fieldType eq null) { fieldType = FieldType(value) };
            fieldType
        }

        override def asFieldTypeSignature =
            // should be called at most once => caching doesn't make sense
            SignatureParser.parseFieldTypeSignature(value)

        override def asSignature(ap: AttributeParent) =
            // should be called at most once => caching doesn't make sense
            ap match {
                case AttributesParent.Field ⇒
                    SignatureParser.parseFieldTypeSignature(value)
                case AttributesParent.ClassFile ⇒
                    SignatureParser.parseClassSignature(value)
                case AttributesParent.Method ⇒
                    SignatureParser.parseMethodTypeSignature(value)
                case AttributesParent.Code ⇒
                    throw new BytecodeProcessingFailedException(
                        "unexpected signature attribute found in a code_attribute's attributes table")
            }

        override def asConstantValue(cp: Constant_Pool) =
            // required to support annotations; should be called at most once => caching doesn't make sense
            ConstantString(value)
    }

    case class CONSTANT_String_info(
        string_index: Constant_Pool_Index)
            extends CONSTANT_FieldValue_info {

        override def asConstantFieldValue(cp: Constant_Pool): ConstantString =
            ConstantString(cp(string_index).asString)

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument = {
            asConstantValue(cp)
        }
    }

    case class CONSTANT_Fieldref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        // We don't mind if the field is initialized more than once (if reading the classfile 
        // should be parallelized) as it is just an optimization and the object reference
        // is of now importance; an equals check would even return true. Hence, w.r.t. the
        // previous definition this code is thread-safe.
        private[this] var fieldref: (ObjectType, String, FieldType) = null // to cache the result
        override def asFieldref(cp: Constant_Pool): (ObjectType, String, FieldType) = {
            if (fieldref eq null) {
                val nameAndType = cp(name_and_type_index).asNameAndType
                fieldref = (cp(class_index).asObjectType(cp),
                    nameAndType.name(cp),
                    nameAndType.fieldType(cp)
                )
            }
            fieldref
        }
    }

    private[ConstantPoolBinding] trait AsMethodref extends Constant_Pool_Entry {

        def class_index: Constant_Pool_Index

        def name_and_type_index: Constant_Pool_Index

        // to cache the result
        private[this] var methodref: (ReferenceType, String, MethodDescriptor) = null
        override def asMethodref(
            cp: Constant_Pool): (ReferenceType, String, MethodDescriptor) = {
            if (methodref eq null) {
                val nameAndType = cp(name_and_type_index).asNameAndType
                methodref =
                    (
                        cp(class_index).asReferenceType(cp),
                        nameAndType.name(cp),
                        nameAndType.methodDescriptor(cp)
                    )
            }
            methodref
        }
    }

    case class CONSTANT_Methodref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index)
            extends AsMethodref

    case class CONSTANT_InterfaceMethodref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index)
            extends AsMethodref

    case class CONSTANT_NameAndType_info(
        name_index: Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        override def asNameAndType: CONSTANT_NameAndType_info = this

        def name(cp: Constant_Pool): String =
            // this operation is very cheap and hence, it doesn't make sense to cache the result
            cp(name_index).asString

        def fieldType(cp: Constant_Pool): FieldType =
            cp(descriptor_index).asFieldType

        def methodDescriptor(cp: Constant_Pool): MethodDescriptor =
            cp(descriptor_index).asMethodDescriptor
    }

    case class CONSTANT_MethodHandle_info(
        referenceKind: Int,
        referenceIndex: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument =
            asMethodHandle(cp)

        override def asConstantValue(cp: Constant_Pool): MethodHandle =
            asMethodHandle(cp)

        override def asMethodHandle(cp: Constant_Pool): MethodHandle = {
            (this.referenceKind: @scala.annotation.switch) match {
                case bi.REF_getField.referenceKind ⇒ {
                    val (declaringType, name, fieldType) =
                        cp(referenceIndex).asFieldref(cp)
                    GetFieldMethodHandle(declaringType, name, fieldType)
                }
                case bi.REF_getStatic.referenceKind ⇒ {
                    val (declaringType, name, fieldType) =
                        cp(referenceIndex).asFieldref(cp)
                    GetStaticMethodHandle(declaringType, name, fieldType)
                }
                case bi.REF_putField.referenceKind ⇒ {
                    val (declaringType, name, fieldType) =
                        cp(referenceIndex).asFieldref(cp)
                    PutFieldMethodHandle(declaringType, name, fieldType)
                }
                case bi.REF_putStatic.referenceKind ⇒ {
                    val (declaringType, name, fieldType) =
                        cp(referenceIndex).asFieldref(cp)
                    PutStaticMethodHandle(declaringType, name, fieldType)
                }
                case bi.REF_invokeVirtual.referenceKind ⇒ {
                    val (receiverType, name, methodDescriptor) =
                        cp(referenceIndex).asMethodref(cp)
                    InvokeVirtualMethodHandle(receiverType, name, methodDescriptor)
                }

                case bi.REF_invokeStatic.referenceKind ⇒ {
                    val (receiverType, name, methodDescriptor) =
                        cp(referenceIndex).asMethodref(cp)
                    InvokeStaticMethodHandle(receiverType, name, methodDescriptor)
                }

                case bi.REF_invokeSpecial.referenceKind ⇒ {
                    val (receiverType, name, methodDescriptor) =
                        cp(referenceIndex).asMethodref(cp)
                    InvokeSpecialMethodHandle(receiverType, name, methodDescriptor)
                }

                case bi.REF_newInvokeSpecial.referenceKind ⇒ {
                    val (receiverType, name, methodDescriptor) =
                        cp(referenceIndex).asMethodref(cp)
                    NewInvokeSpecialMethodHandle(receiverType, name, methodDescriptor)
                }

                case bi.REF_invokeInterface.referenceKind ⇒ {
                    val (receiverType, name, methodDescriptor) =
                        cp(referenceIndex).asMethodref(cp)
                    InvokeInterfaceMethodHandle(receiverType, name, methodDescriptor)
                }
            }
        }
    }

    case class CONSTANT_MethodType_info(
        descriptorIndex: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        def methodDescriptor(cp: Constant_Pool): MethodDescriptor =
            cp(descriptorIndex).asMethodDescriptor

        override def asConstantValue(cp: Constant_Pool): MethodDescriptor =
            methodDescriptor(cp)

        override def asBootstrapArgument(cp: Constant_Pool): BootstrapArgument =
            methodDescriptor(cp)
    }

    /**
     * @param bootstrapMethodAttributeIndex This is an index into the bootstrap table.
     *    Since the bootstrap table is a class level attribute it is only possible
     *    to resolve this reference after loading the entire class file (class level
     *    attributes are loaded last).
     */
    case class CONSTANT_InvokeDynamic_info(
        bootstrapMethodAttributeIndex: Int,
        nameAndTypeIndex: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        override def asInvokeDynamic: CONSTANT_InvokeDynamic_info = this

        def methodName(cp: Constant_Pool): String =
            cp(nameAndTypeIndex).asNameAndType.name(cp)

        def methodDescriptor(cp: Constant_Pool): MethodDescriptor =
            cp(nameAndTypeIndex).asNameAndType.methodDescriptor(cp)

    }

    //
    // IMPLEMENTATION OF THE CONSTANT POOL READER'S FACTORY METHODS
    //

    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = new CONSTANT_Class_info(i)

    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    def CONSTANT_Utf8_info(s: String): CONSTANT_Utf8_info = new CONSTANT_Utf8_info(s)

    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    def CONSTANT_Fieldref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index): CONSTANT_Fieldref_info =
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)

    def CONSTANT_Methodref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index): CONSTANT_Methodref_info =
        new CONSTANT_Methodref_info(class_index, name_and_type_index)

    def CONSTANT_InterfaceMethodref_info(
        class_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index): CONSTANT_InterfaceMethodref_info =
        new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)

    def CONSTANT_NameAndType_info(
        name_index: Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index): CONSTANT_NameAndType_info =
        new CONSTANT_NameAndType_info(name_index, descriptor_index)

    def CONSTANT_MethodHandle_info(
        reference_kind: Int,
        reference_index: Int): CONSTANT_MethodHandle_info =
        new CONSTANT_MethodHandle_info(reference_kind, reference_index)

    def CONSTANT_MethodType_info(
        descriptor_index: Constant_Pool_Index): CONSTANT_MethodType_info =
        new CONSTANT_MethodType_info(descriptor_index)

    def CONSTANT_InvokeDynamic_info(
        bootstrap_method_attr_index: Constant_Pool_Index,
        name_and_type_index: Constant_Pool_Index): CONSTANT_InvokeDynamic_info =
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)

    protected[this] def createDeferredActionsStore(): DeferredActionsStore = {
        new scala.collection.mutable.ArrayBuffer[ClassFile ⇒ ClassFile] with Constant_Pool_Entry {}
    }

}

