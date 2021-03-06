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
case class StackMapTable_attribute(
        attribute_name_index: Int,
        attribute_length: Int,
        stack_map_frames: IndexedSeq[StackMapFrame]) extends Attribute {

    //
    // ABSTRACT DEFINITIONS
    //

    type StackMapFrames = IndexedSeq[StackMapFrame]

    //
    // IMPLEMENTATION
    //

    val attribute_name = StackMapTable_attribute.name

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div>
            <details>
                <summary>Stack map table:</summary>
                <span> number of frames:{ stack_map_frames.length }</span>
                { stack_map_framestoXHTML(cp) }
            </details>
        </div>
    }

    def stack_map_framestoXHTML(implicit cp: Constant_Pool): Node = {
        var offset: Int = -1
        val frames = for (stack_map_frame ← stack_map_frames) yield {
            val Nodehtml = stack_map_frame.toXHTML(cp, offset)
            offset = stack_map_frame.initial_offset
            Nodehtml
        }
        <div> { frames } </div>
    }

}

object StackMapTable_attribute {

    val name = "StackMapTable"

}
