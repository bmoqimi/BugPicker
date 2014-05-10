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
package reader

import reflect.ClassTag

import de.tud.cs.st.bat.reader.AnnotationAbstractions
import de.tud.cs.st.bat.reader.AnnotationDefault_attributeReader
import de.tud.cs.st.bat.reader.ElementValuePairsReader

/**
 * Factory methods to create representations of Java annotations.
 *
 * @author Michael Eichberg
 */
trait AnnotationsBinding
        extends AnnotationAbstractions
        with ElementValuePairsReader
        with ConstantPoolBinding {

    type Annotation = resolved.Annotation

    val AnnotationManifest: ClassTag[Annotation] = implicitly

    type ElementValue = resolved.ElementValue
    val ElementValueManifest: ClassTag[ElementValue] = implicitly

    type EnumValue = resolved.EnumValue

    type AnnotationValue = resolved.AnnotationValue

    type ByteValue = resolved.ByteValue

    type CharValue = resolved.CharValue

    type ShortValue = resolved.ShortValue

    type IntValue = resolved.IntValue

    type LongValue = resolved.LongValue

    type FloatValue = resolved.FloatValue

    type DoubleValue = resolved.DoubleValue

    type ArrayValue = resolved.ArrayValue

    type ClassValue = resolved.ClassValue

    type BooleanValue = resolved.BooleanValue

    type ElementValuePair = resolved.ElementValuePair
    val ElementValuePairManifest: ClassTag[ElementValuePair] = implicitly

    def ElementValuePair(
        cp: Constant_Pool,
        element_name_index: Constant_Pool_Index,
        element_value: ElementValue): ElementValuePair = {
        new ElementValuePair(cp(element_name_index).asString, element_value)
    }

    def ByteValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new ByteValue(cv.toByte)
    }

    def CharValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new CharValue(cv.toChar)
    }

    def DoubleValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new DoubleValue(cv.toDouble)
    }

    def FloatValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new FloatValue(cv.toFloat)
    }

    def IntValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new IntValue(cv.toInt)
    }

    def LongValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new LongValue(cv.toLong)
    }

    def ShortValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new ShortValue(cv.toShort)
    }

    def BooleanValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new BooleanValue(cv.toBoolean)
    }

    def StringValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val cv: ConstantValue[_] = cp(const_value_index).asConstantValue(cp)
        new StringValue(cv.toUTF8)
    }

    def ClassValue(
        cp: Constant_Pool,
        const_value_index: Constant_Pool_Index): ElementValue = {
        val rt: String = cp(const_value_index).asString
        new ClassValue(ReturnType(rt))
    }

    def EnumValue(
        cp: Constant_Pool,
        type_name_index: Constant_Pool_Index,
        const_name_index: Constant_Pool_Index): ElementValue = {
        new EnumValue(
            cp(type_name_index).asFieldType /*<= triggers the lookup in the CP*/ .asObjectType,
            cp(const_name_index).asString)
    }

    def AnnotationValue(
        cp: Constant_Pool,
        annotation: Annotation): ElementValue =
        new AnnotationValue(annotation)

    def ArrayValue(
        cp: Constant_Pool,
        values: ElementValues): ElementValue =
        new ArrayValue(values)

    def Annotation(
        cp: Constant_Pool,
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs) =
        new Annotation(cp(type_index).asFieldType, element_value_pairs)

}

