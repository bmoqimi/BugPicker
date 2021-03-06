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
 * Generic infrastructure used by specific parsers of class file attributes to register
 * with the overall framework ([[org.opalj.bi.reader.AttributesReader]]).
 *
 * @author Michael Eichberg
 */
trait AttributeReader
        extends Constant_PoolAbstractions
        with AttributesAbstractions {

    type Attribute >: Null

    /**
     * Called (typically by subclasses) to register a reader for a concrete attribute.
     * This function is intended to be provided/implemented by an `AttributesReader`
     * that manages the attributes of a class, method_info, field_info or
     * code_attribute structure.
     *
     * @param reader A map where the key is the name of an attribute and the value is
     *  a function that given a data input stream that is positioned directly
     *  at the beginning of the attribute, the constant pool, the index of the attribute's
     *  name and the parent of the attribute reads in the attribute and returns it.
     */
    def registerAttributeReader(
        reader: (String, (AttributeParent, Constant_Pool, /* attribute name */ Constant_Pool_Index, DataInputStream) ⇒ Attribute)): Unit

    /**
     * Registers a new processor for the list of all attributes of a given class file
     * structure (class, field_info, method_info, code_attribute). This can be used to
     * post-process attributes. E.g., to merge multiple line number tables if they exist
     * or to remove attributes if they are completely resolved.
     *
     * @see The implementation of
     *      [[org.opalj.br.reader.UnpackedLineNumberTable_attributeBinding]]
     *      for a concrete example.
     */
    def registerAttributesPostProcessor(p: (Attributes) ⇒ Attributes): Unit
}
