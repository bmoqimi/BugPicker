/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package util
package graphs

/**
 * Represents a node of a graph. Two nodes are considered equal if they have the
 * same unique id.
 *
 * @see [[de.tud.cs.st.bat.resolved.analyses.ClassHierarchy]]'s `toGraph` method for
 *      an example usage.
 *
 * @author Michael Eichberg
 */
trait Node {

    /**
     * Returns a humane readable representation (HRR) of this node.
     */
    def toHRR: Option[String]

    /**
     * The name of the background color base on the X11 color scheme.
     *
     * see [[http://www.graphviz.org/content/color-names]] for further details.
     */
    def backgroundColor: Option[String]

    /**
     * An identifier that uniquely identifies this node in the graph to which this
     * node belongs. By default two nodes are considered equal if they have the same
     * unique id.
     */
    def uniqueId: Int

    /**
     * Applies the given function for each successor node.
     */
    def foreachSuccessor(f: Node ⇒ Unit): Unit

    /**
     * Returns `true` if this node has successor nodes.
     */
    def hasSuccessors(): Boolean

    override def hashCode: Int = uniqueId

    override def equals(other: Any): Boolean = {
        other match {
            case otherNode: Node ⇒ otherNode.uniqueId == this.uniqueId
            case _               ⇒ false
        }
    }
}


