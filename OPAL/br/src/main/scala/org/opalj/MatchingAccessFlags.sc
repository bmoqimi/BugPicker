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

import bi._
import br._

/**
 * Demonstrates how to select fields that have certain access flags (public, static,...)
 */
object MatchingAccessFlags {

    val resource = () ⇒ this.getClass.getResourceAsStream("br/analyses/Project.class")
    val classFile = br.reader.Java8Framework.ClassFile(resource)
    val fields = classFile.fields

    val publicFinalStaticFields = fields.collectFirst(
        { case f @ Field(AccessFlagsMatcher.PUBLIC_STATIC_FINAL(), _, _) ⇒ f }
    )

    val nonStaticFields = fields.collectFirst(
        { case f @ Field(AccessFlagsMatcher.NOT_STATIC(), _, _) ⇒ f }
    )

    val methods = classFile.methods.collect(
        { case m @ Method(AccessFlagsMatcher.PUBLIC___OR___PROTECTED_AND_NOT_FINAL(), _, _) ⇒ m.name }
    )

    // create a new matcher
    val PRIVATE_FINAL = ACC_PRIVATE && ACC_FINAL
    val privateFinalFields =
        fields.collectFirst({ case f @ Field(PRIVATE_FINAL(), _, _) ⇒ f })

    val NOT___PRIVATE_FINAL = !(ACC_PRIVATE && ACC_FINAL)
    val not___PrivateFinalFields =
        fields.collectFirst({ case f @ Field(NOT___PRIVATE_FINAL(), _, _) ⇒ f })
}