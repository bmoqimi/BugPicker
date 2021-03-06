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

/**
 * Enumeration of the different contexts in which the JVM Spec. uses
 * `access_flags` fields and also an enumeration which `access_flags` are found
 * in which context.
 *
 * @author Michael Eichberg
 */
object AccessFlagsContexts extends Enumeration {

    val INNER_CLASS, CLASS, METHOD, FIELD, METHOD_PARAMETERS = Value

    val INNER_CLASS_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_SUPER /*NOT SPECIFIED IN THE JVM SPEC. - MAYBE THIS BIT IS JUST SET BY THE SCALA COMPILER!*/ ,
            ACC_FINAL,
            ACC_INTERFACE,
            ACC_ABSTRACT,
            ACC_SYNTHETIC,
            ACC_ANNOTATION,
            ACC_ENUM)

    val CLASS_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC,
            ACC_FINAL,
            ACC_SUPER,
            ACC_INTERFACE,
            ACC_ABSTRACT,
            ACC_SYNTHETIC,
            ACC_ANNOTATION,
            ACC_ENUM)

    val FIELD_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_FINAL,
            ACC_VOLATILE,
            ACC_TRANSIENT,
            ACC_SYNTHETIC,
            ACC_ENUM)

    val METHOD_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC,
            ACC_PRIVATE,
            ACC_PROTECTED,
            ACC_STATIC,
            ACC_FINAL,
            ACC_SYNCHRONIZED,
            ACC_BRIDGE,
            ACC_VARARGS,
            ACC_NATIVE,
            ACC_ABSTRACT,
            ACC_STRICT,
            ACC_SYNTHETIC)

    val METHOD_PARAMETER_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_FINAL, ACC_SYNTHETIC, ACC_MANDATED)

    val CLASS_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC)

    val MEMBER_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] =
        IndexedSeq(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED)

    val INNER_CLASS_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    val FIELD_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    val METHOD_VISIBILITY_FLAGS: IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS

    def potentialAccessFlags(ctx: AccessFlagsContext): IndexedSeq[AccessFlag] = {
        ctx match {
            case INNER_CLASS       ⇒ INNER_CLASS_FLAGS
            case CLASS             ⇒ CLASS_FLAGS
            case METHOD            ⇒ METHOD_FLAGS
            case FIELD             ⇒ FIELD_FLAGS
            case METHOD_PARAMETERS ⇒ METHOD_PARAMETER_FLAGS
        }
    }
}