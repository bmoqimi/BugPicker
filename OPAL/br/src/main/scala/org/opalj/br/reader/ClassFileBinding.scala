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
package reader

import org.opalj.bi.reader.ClassFileReader

/**
 *
 * @author Michael Eichberg
 */
trait ClassFileBinding extends ClassFileReader {
    this: ConstantPoolBinding with MethodsBinding with FieldsBinding with AttributeBinding ⇒

    type ClassFile = br.ClassFile

    type Fields <: IndexedSeq[Field_Info]
    type Methods <: IndexedSeq[Method_Info]

    def ClassFile(
        cp: Constant_Pool,
        minor_version: Int, major_version: Int,
        access_flags: Int,
        this_class_index: Constant_Pool_Index,
        super_class_index: Constant_Pool_Index,
        interfaces: IndexedSeq[Constant_Pool_Index],
        fields: Fields,
        methods: Methods,
        attributes: Attributes): ClassFile = {
        br.ClassFile(
            minor_version, major_version, access_flags,
            cp(this_class_index).asObjectType(cp),
            // to handle the special case that this class file represents java.lang.Object
            {
                if (super_class_index == 0)
                    None
                else
                    Some(cp(super_class_index).asObjectType(cp))
            },
            interfaces.map(cp(_).asObjectType(cp)),
            fields,
            methods,
            attributes)
    }

    val removeBootstrapMethodAttribute: Seq[ClassFile] ⇒ Seq[ClassFile] = { classFiles ⇒
        val classFile = classFiles.head
        val attributes = classFile.attributes
        if (classFile.majorVersion <= 50 /*does not have BootstrapMethodTable*/ ||
            attributes.size == 0 ||
            attributes.forall(attribute ⇒ !attribute.isInstanceOf[BootstrapMethodTable]))
            classFiles
        else {
            val newAttributes = classFile.attributes filter { attribute ⇒
                !attribute.isInstanceOf[BootstrapMethodTable]
            }
            classFile.updateAttributes(newAttributes) +: classFiles.tail
        }
    }

    registerClassFilePostProcessor(removeBootstrapMethodAttribute)
}

