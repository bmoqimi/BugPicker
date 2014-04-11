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
package reader

import reflect.ClassTag

import java.io.DataInputStream

/**
 * Generic parser for the target_type and target_info fields of type annotations. This
 * reader is intended to be used in conjunction with the
 * [[TargetAnnotationsReader]].
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationTargetReader extends Constant_PoolAbstractions {

    //    object TypeAnnotationLocation extends Enumeration {
    //        // the names are used as defined in the JVM 8 Spec.
    //        val ClassFile = Value("ClassFile")
    //        val MethodInfo = Value("method_info")
    //        val FieldInfo = Value("field_info")
    //        val Code = Value("Code")
    //    }
    //
    //    type TypeAnnotationLocation = TypeAnnotationLocation.Value
    //    import TypeAnnotationLocation._

    //
    // ABSTRACT DEFINITIONS
    //

    type TypeAnnotationTarget

    //______________________________
    // type_parameter_target
    def ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int): TypeAnnotationTarget
    def ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int): TypeAnnotationTarget

    //______________________________
    // supertype_target
    def SupertypeTarget(
        supertype_index: Int): TypeAnnotationTarget

    //______________________________
    // type_parameter_bound_target
    def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index: Int): TypeAnnotationTarget
    def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index: Int): TypeAnnotationTarget

    //______________________________
    // empty_target
    def FieldDeclaration: TypeAnnotationTarget
    def ReturnType: TypeAnnotationTarget
    def ReceiverType: TypeAnnotationTarget

    //______________________________
    // formal_parameter_target
    def FormalParameter(formal_parameter_index: Int): TypeAnnotationTarget

    //______________________________
    // throws_target
    def Throws(throws_type_index: Int): TypeAnnotationTarget

    //______________________________
    // catch_target
    def Catch(exception_table_index: Int): TypeAnnotationTarget

    //______________________________
    // localvar_target
    type LocalVarTableEntry

    type LocalVarTable = IndexedSeq[LocalVarTableEntry]

    def LocalVarTableEntry(
        start_pc: Int,
        length: Int,
        local_variable_table_index: Int): LocalVarTableEntry
    /**
     * Format
     * {{{
     * u2 table_length;
     * {    u2 start_pc;
     *      u2 length;
     *      u2 index; // index into the local variable table(!)
     * } table[table_length];
     * }}}
     */
    def LocalVarDecl(localVarTable: LocalVarTable): TypeAnnotationTarget
    def ResourceVarDecl(localVarTable: LocalVarTable): TypeAnnotationTarget

    //______________________________
    // offset_target
    def InstanceOf(offset: Int): TypeAnnotationTarget
    def New(offset: Int): TypeAnnotationTarget
    def MethodReferenceExpressionNew /*::New*/ (
        offset: Int): TypeAnnotationTarget
    def MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int): TypeAnnotationTarget

    //______________________________
    // type_arguement_target
    def CastExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def ConstructorInvocation(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def MethodInvocation(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def ConstructorInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def MethodInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget

    //
    // IMPLEMENTATION
    //

    def LocalVarTable(in: DataInputStream): LocalVarTable = {
        import util.ControlAbstractions.repeat

        repeat(in.readUnsignedShort) {
            LocalVarTableEntry(
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                in.readUnsignedShort())
        }
    }

    /* From the Specification
     * 
     * <pre>
     * u1 target_type;
     * union {
     *  type_parameter_target;
     *  supertype_target;
     *  type_parameter_bound_target;
     *  empty_target;
     *  method_formal_parameter_target;
     *  throws_target;
     *  localvar_target;
     *  catch_target;
     *  offset_target;
     *  type_argument_target;
     *  } target_info;
     * </pre>
     */
    def TypeAnnotationTarget(cp: Constant_Pool, in: DataInputStream): TypeAnnotationTarget = {
        val target_type = in.readUnsignedByte()
        (target_type: @scala.annotation.switch) match {
            case 0x00 ⇒ ParameterDeclarationOfClassOrInterface(in.readUnsignedByte())
            case 0x01 ⇒ ParameterDeclarationOfMethodOrConstructor(in.readUnsignedByte())
            case 0x10 ⇒ SupertypeTarget(in.readUnsignedShort())
            case 0x11 ⇒
                TypeBoundOfParameterDeclarationOfClassOrInterface(
                    in.readUnsignedByte(),
                    in.readUnsignedByte())
            case 0x12 ⇒
                TypeBoundOfParameterDeclarationOfMethodOrConstructor(
                    in.readUnsignedByte(),
                    in.readUnsignedByte())
            case 0x13 ⇒ FieldDeclaration
            case 0x14 ⇒ ReturnType
            case 0x15 ⇒ ReceiverType
            case 0x16 ⇒ FormalParameter(in.readUnsignedByte())
            case 0x17 ⇒ Throws(in.readUnsignedShort())
            case 0x40 ⇒ LocalVarDecl(LocalVarTable(in))
            case 0x41 ⇒ ResourceVarDecl(LocalVarTable(in))
            case 0x42 ⇒ Catch(in.readUnsignedShort())
            case 0x43 ⇒ InstanceOf(in.readUnsignedShort())
            case 0x44 ⇒ New(in.readUnsignedShort())
            case 0x45 ⇒ MethodReferenceExpressionNew(in.readUnsignedShort())
            case 0x46 ⇒ MethodReferenceExpressionIdentifier(in.readUnsignedShort())
            case 0x47 ⇒ CastExpression(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x48 ⇒
                ConstructorInvocation(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x49 ⇒
                MethodInvocation(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x4A ⇒
                ConstructorInMethodReferenceExpression(
                    in.readUnsignedShort(),
                    in.readUnsignedByte())
            case 0x4B ⇒
                MethodInMethodReferenceExpression(
                    in.readUnsignedShort(),
                    in.readUnsignedByte())
        }
    }
}


