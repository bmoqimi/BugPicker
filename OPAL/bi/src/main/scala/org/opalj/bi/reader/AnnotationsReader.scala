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

import scala.reflect.ClassTag

import java.io.DataInputStream

/**
 * Generic parser to parse a list of annotations. This
 * reader is intended to be used in conjunction with the
 * Runtime(In)Visible(Parameter)Annotations_attributeReaders.
 *
 * @author Michael Eichberg
 */
trait AnnotationsReader extends AnnotationAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    implicit val AnnotationManifest: ClassTag[Annotation]

    //
    // IMPLEMENTATION
    //

    type Annotations = IndexedSeq[Annotation]

    /**
     * Reads the annotations of a annotations attributes.
     *
     * ''' From the Specification'''
     * <pre>
     * annotation {
     *      u2 type_index;
     *      u2 num_element_value_pairs;
     *      {   u2 element_name_index;
     *          element_value value;
     *      }   element_value_pairs[num_element_value_pairs]
     * }
     * </pre>
     */
    def Annotations(cp: Constant_Pool, in: DataInputStream): Annotations = {
        repeat(in.readUnsignedShort) {
            Annotation(cp, in)
        }
    }
}
