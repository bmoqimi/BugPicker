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
package bugpicker
package analysis

import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.xml.Node
import scala.xml.Text
import scala.xml.UnprefixedAttribute

import org.opalj.ai.debug.XHTML
import org.opalj.br.ClassFile
import org.opalj.br.Method

case class UselessComputation(
        classFile: ClassFile,
        method: Method,
        pc: PC,
        computation: String) extends BugReport {

    def opcode: Int = method.body.get.instructions(pc).opcode

    def line: Option[Int] = method.body.get.lineNumber(pc)

    private val methodIndex = classFile.methods.sortWith(_.toJava < _.toJava).indexOf(method).toString

    def message: String = {

        val line = this.line.map("(line:"+_+")").getOrElse("")

        "useless computation "+
            classFile.thisType.toJava+"{ "+method.toJava+"{ "+pc + line+": "+computation+" }}"
    }

    override def toString = {
        import Console._

        val line = this.line.map("(line:"+_+")").getOrElse("")

        "useless computation "+
            BOLD + BLUE + classFile.thisType.toJava + RESET+
            "{ "+method.toJava+"{ "+
            GREEN + pc + line+": "+message +
            RESET+" }}"
    }

    def toXHTML: Node = {

        val pcNode = <span data-class={ classFile.fqn } data-method={ methodIndex } data-line={ line.map(_.toString).getOrElse("") } data-pc={ pc.toString } data-show="bytecode">{ pc }</span>

        val methodLine: String = method.body.flatMap(_.firstLineNumber.map(_.toString)).getOrElse("")

        val node =
            <tr style="color:rgb(126, 64, 64);">
                <td><span data-class={ classFile.fqn }>
                        { XHTML.typeToXHTML(classFile.thisType) }
                    </span></td>
                <td><span data-class={ classFile.fqn } data-method={ methodIndex } data-line={ methodLine }>
                        { XHTML.methodToXHTML(method.name, method.descriptor) }
                    </span></td>
                <td>{ pcNode }{
                    Text("/ ") ++ line.map(ln ⇒
                        <span data-class={ classFile.fqn } data-method={ methodIndex } data-line={ ln.toString } data-pc={ pc.toString } data-show="sourcecode">{ ln }</span>).getOrElse(Text("N/A"))
                }</td>
                <td>{ computation }</td>
            </tr>

        node % (
            new UnprefixedAttribute("data-accuracy", "100", scala.xml.Null)
        )
    }
}

