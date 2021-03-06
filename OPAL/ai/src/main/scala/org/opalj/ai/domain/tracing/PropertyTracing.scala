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
package ai
package domain
package tracing

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * Enables the tracing of some user-defined property while a method is analyzed.
 * A possible property could be, e.g., whether a certain check is performed on
 * all intra-procedural control flows.
 *
 * After the abstract interpretation of a method, the property is associated with
 * all ''executed instructions'' and can be queried. For example to get the information
 * whether the check was performed on all paths to all exit points.
 *
 * @author Michael Eichberg
 */
trait PropertyTracing extends CoreDomainFunctionality { domain: Domain ⇒

    trait Property {
        def join(otherProperty: DomainProperty): Update[DomainProperty]
    }

    type DomainProperty <: Property

    def initialPropertyValue(): DomainProperty

    /**
     * The type of the property. E.g., `Boolean` or some other type.
     */
    implicit val DomainPropertyTag: reflect.ClassTag[DomainProperty]

    /**
     * The array which stores the value the property has when the respective.
     * instruction is executed.
     */
    private var propertiesArray: Array[DomainProperty] = _

    def initProperties(
        code: Code,
        operandsArray: Operands,
        localsArray: Locals) = {

        this.propertiesArray = new Array(code.instructions.size)
        this.propertiesArray(0) = initialPropertyValue()
    }

    def getProperty(pc: PC): DomainProperty = {
        propertiesArray(pc)
    }

    def setProperty(pc: PC, property: DomainProperty) {
        propertiesArray(pc) = property
    }

    /**
     * Returns a string representation of the property associated with the given
     * instruction. This string representation is used by OPAL's tools to enable
     * a meaningful representation of the property.
     *
     * (Run `de...ai.util.InterpretMethod` with a domain that traces properties.)
     */
    override def properties(pc: Int, valueToString: AnyRef ⇒ String): Option[String] =
        Option(propertiesArray(pc)).map(_.toString())

    override def flow(
        currentPC: PC,
        successorPC: PC,
        isExceptionalControlFlow: Boolean,
        wasJoinPerformed: Boolean,
        worklist: List[PC],
        operandsArray: OperandsArray,
        localsArray: LocalsArray,
        tracer: Option[AITracer]): List[PC] = {

        val forceScheduling: Boolean = {
            if (!wasJoinPerformed /* weaker: || propertiesArray(successorPC) eq null*/ ) {
                propertiesArray(successorPC) = propertiesArray(currentPC)
                true
            } else {
                propertiesArray(successorPC) join propertiesArray(currentPC) match {
                    case NoUpdate ⇒ false
                    case StructuralUpdate(property) ⇒
                        propertiesArray(successorPC) = property
                        true
                    case MetaInformationUpdate(property) ⇒
                        propertiesArray(successorPC) = property
                        false
                }
            }
        }

        if (forceScheduling && (worklist.isEmpty || worklist.head != successorPC)) {
            val filteredList = util.removeFirst(worklist, successorPC)
            if (tracer.isDefined) {
                if (filteredList eq worklist)
                    // the instruction was not yet scheduled for another
                    // evaluation
                    tracer.get.flow(domain)(currentPC, successorPC, isExceptionalControlFlow)
                else
                    // the instruction was just moved to the beginning
                    tracer.get.rescheduled(domain)(currentPC, successorPC, isExceptionalControlFlow)

            }
            successorPC :: filteredList
        } else {
            worklist
        }
    }
}
