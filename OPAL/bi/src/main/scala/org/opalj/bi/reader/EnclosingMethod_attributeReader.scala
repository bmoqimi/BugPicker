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
package bi
package reader

import java.io.DataInputStream

/**
 * Generic parser for the ''enclosing method'' attribute.
 *
 * @author Michael Eichberg
 */
trait EnclosingMethod_attributeReader extends AttributeReader {

    type EnclosingMethod_attribute <: Attribute

    def EnclosingMethod_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        class_index: Constant_Pool_Index,
        method_index: Constant_Pool_Index): EnclosingMethod_attribute

    //
    // IMPLEMENTATION
    //

    /* From The Specification
     * <pre>
     * EnclosingMethod_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 class_index
     *  u2 method_index;
     * }
     * </pre>
     */
    registerAttributeReader(
        EnclosingMethod_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                val attribute_length = in.readInt
                EnclosingMethod_attribute(
                    cp,
                    attribute_name_index,
                    in.readUnsignedShort,
                    in.readUnsignedShort
                )
            }
        )
    )
}

object EnclosingMethod_attributeReader {

    val ATTRIBUTE_NAME = "EnclosingMethod"
}
