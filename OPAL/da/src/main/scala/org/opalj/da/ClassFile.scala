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
package da

import de.tud.cs.st.bat.{ AccessFlags, AccessFlagsContexts }

import scala.xml.Node

/**
 * @author Michael Eichberg
 */
case class ClassFile(
        constant_pool: Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags: Int,
        this_class: Constant_Pool_Index,
        super_class: Constant_Pool_Index,
        interfaces: IndexedSeq[Constant_Pool_Index],
        fields: Fields,
        methods: Methods,
        attributes: Attributes) {

    private[this] implicit val cp = constant_pool

    /**
     * The fully qualified name of the class in Java notation (i.e., using dots
     * to seperate packages.)
     */
    def fqn = cp(this_class).toString.replace('/', '.')

    /**
     * Converts the constant pool to (x)HTML5.
     */
    def cpToXHTML = {
        for { cpIndex ← (0 until constant_pool.length) if cp(cpIndex) != null } yield {
            <li value={ cpIndex.toString }>{ cp(cpIndex).toString() }</li>
        }
    }

    def fieldsToXHTML = {
        for (field ← fields) yield field.toXHTML(cp)
    }

    def toXHTML: Node =
        <html>
            <head></head>
            <body>
                <h1>{ fqn }</h1>
                <details>
                    <summary>Constant Pool</summary>
                    <ol>
                    { cpToXHTML }
                    </ol>
                </details>
                <dl>
                    <dt>Version</dt><dd>{ minor_version + "." + major_version }</dd>
                    <dt>Access Flags</dt><dd>{ AccessFlags.toString(access_flags, AccessFlagsContexts.CLASS) }</dd>
                </dl>
                <details>
                    <summary>Fields</summary>
                    { fieldsToXHTML }
                </details>
           </body>
        </html>
}