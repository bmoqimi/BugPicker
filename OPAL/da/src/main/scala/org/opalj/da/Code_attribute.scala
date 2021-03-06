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

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Code_attribute(
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        max_stack: Int,
        max_locals: Int,
        code: Code,
        exceptionTable: IndexedSeq[ExceptionTableEntry],
        attributes: Attributes) extends Attribute {

    /**
     * @ see `toXHTML(Int)(implicit Constant_Pool)
     */
    @throws[UnsupportedOperationException]("always")
    override def toXHTML(implicit cp: Constant_Pool): Node = {
        throw new UnsupportedOperationException(
            "the code attribute needs the method's id; "+
                "use the \"toXHTML(methodIndex: Int)(implicit cp: Constant_Pool)\" method")
    }

    def toXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Node = {

        val methodBodyHeader =
            s"Method Body (Size: ${code.instructions.size} bytes, Max Stack: $max_stack, Max Locals: $max_locals)"
        <details class="method_body">
            <summary>{ methodBodyHeader }</summary>
            {
                code.toXHTML(
                    methodIndex,
                    exceptionTable,
                    attributes.collectFirst({ case LineNumberTable_attribute(_, lnt) ⇒ lnt }))
            }
            { exception_handlersAsXHTML }
            { attributesAsXHTML }
        </details>

    }

    def attributesAsXHTML(implicit cp: Constant_Pool) = {
        for (attribute ← attributes) yield attribute.toXHTML(cp)
    }

    def exception_handlersAsXHTML(implicit cp: Constant_Pool): Node = {
        if (exceptionTable.length > 0)
            <div>
                <details>
                    <summary>Exception Table:</summary>
                    <ol class="exception_table">
                        { for (exception ← exceptionTable) yield exception.toXHTML(cp, code) }
                    </ol>
                </details>
            </div>
        else
            <div></div>
    }
}

