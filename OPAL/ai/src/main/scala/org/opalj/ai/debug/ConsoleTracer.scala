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
package debug

import br.Code
import br.instructions.Instruction

/**
 * A tracer that prints out a trace's results on the console.
 *
 * @author Michael Eichberg
 */
trait ConsoleTracer extends AITracer {

    import Console._

    val printOIDs: Boolean = false

    private def correctIndent(value: Object, printOIDs: Boolean): String = {
        if (value eq null)
            "<EMPTY>"
        else {
            val s = value.toString().replaceAll("\n\t", "\n\t\t\t").replaceAll("\n\\)", "\n\t\t)")
            if (printOIDs) {
                s+" [#"+System.identityHashCode(value).toHexString+"]"
            } else
                s
        }
    }

    override def instructionEvalution(
        domain: Domain)(
            pc: PC,
            instruction: Instruction,
            operands: domain.Operands,
            locals: domain.Locals): Unit = {
        val placeholder = "-"

        println(
            pc+":"+instruction.toString(pc)+" [\n"+
                operands.map { o ⇒
                    correctIndent(o, printOIDs)
                }.mkString("\toperands:\n\t\t", "\n\t\t", "\n\t;\n") +
                locals.zipWithIndex.map { vi ⇒
                    val (v, i) = vi
                    i+":"+(
                        if (v == null)
                            correctIndent("-", false)
                        else
                            correctIndent(v, printOIDs)
                    )
                }.mkString("\tlocals:\n\t\t", "\n\t\t", "\n")+"\t]")
    }

    override def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: domain.OperandsArray,
            localsArray: domain.LocalsArray,
            memoryLayoutBeforeSubroutineCall: List[(domain.OperandsArray, domain.LocalsArray)]) {

        println(BLACK_B + WHITE+"Starting Code Analysis"+RESET)
        println("Number of registers:      "+code.maxLocals)
        println("Size of operand stack:    "+code.maxStack)
        println("Join instructions:        "+code.joinInstructions.mkString(", "))
        //println("Program counters:         "+code.programCounters.mkString(", "))     
    }

    override def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit = {
        println(CYAN_B + RED+
            "rescheduled the evaluation of the instruction with the program counter: "+
            targetPC + RESET)
    }

    override def flow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean) { /* ignored */ }

    override def noFlow(domain: Domain)(currentPC: PC, targetPC: PC) {
        println(Console.RED_B + Console.YELLOW+
            "did not schedule the interpretation of instruction "+
            targetPC+
            "; the abstract state didn't change"+Console.RESET)
    }

    override def join(
        domain: Domain)(
            pc: PC,
            thisOperands: domain.Operands,
            thisLocals: domain.Locals,
            otherOperands: domain.Operands,
            otherLocals: domain.Locals,
            result: Update[(domain.Operands, domain.Locals)]): Unit = {

        print(Console.BLUE + pc+": JOIN: ")
        result match {
            case NoUpdate ⇒ println("no changes")
            case u @ SomeUpdate((updatedOperands, updatedLocals)) ⇒
                println(u.updateType)
                println(
                    thisOperands.
                        zip(otherOperands).
                        zip(updatedOperands).
                        map { v ⇒
                            val ((thisOp, thatOp), updatedOp) = v
                            val s = if (thisOp eq updatedOp)
                                "✓ "+Console.GREEN + updatedOp.toString
                            else {
                                "given "+correctIndent(thisOp, printOIDs)+"\n\t\t join "+
                                    correctIndent(thatOp, printOIDs)+"\n\t\t   => "+
                                    correctIndent(updatedOp, printOIDs)
                            }
                            s + Console.RESET
                        }.
                        mkString("\tOperands:\n\t\t", "\n\t\t", "")
                )
                println(
                    thisLocals.
                        zip(otherLocals).
                        zip(updatedLocals.iterator).map(v ⇒ (v._1._1, v._1._2, v._2)).
                        zipWithIndex.map(v ⇒ (v._2, v._1._1, v._1._2, v._1._3)).
                        filterNot(v ⇒ (v._2 eq null) && (v._3 eq null)).
                        map(v ⇒
                            v._1 + {
                                if (v._2 == v._3)
                                    ": ✓ "+Console.GREEN
                                else {
                                    ":\n\t\t   given "+correctIndent(v._2, printOIDs)+
                                        "\n\t\t    join "+correctIndent(v._3, printOIDs)+
                                        "\n\t\t      => "
                                }
                            } +
                                correctIndent(v._4, printOIDs)
                        ).
                        mkString("\tLocals:\n\t\t", "\n\t\t"+Console.BLUE, Console.BLUE)
                )
        }
        println(Console.RESET)
    }

    override def establishedConstraint(
        domain: Domain)(
            pc: PC,
            effectivePC: PC,
            operands: domain.Operands,
            locals: domain.Locals,
            newOperands: domain.Operands,
            newLocals: domain.Locals): Unit = {
        println(pc+":"+YELLOW_B + BLUE+"Establishing Constraint w.r.t. "+effectivePC+":")
        val changedOperands = operands.view.zip(newOperands).filter(ops ⇒ ops._1 ne ops._2).force
        if (changedOperands.nonEmpty) {
            println(YELLOW_B + BLUE+"\tUpdated Operands:")
            changedOperands.foreach(ops ⇒ print(YELLOW_B + BLUE+"\t\t"+ops._1+" => "+ops._2+"\n"))
        }
        val changedLocals =
            locals.zip(newLocals).zipWithIndex.map(localsWithIdx ⇒
                (localsWithIdx._1._1, localsWithIdx._1._2, localsWithIdx._2)
            ).filter(ops ⇒ ops._1 ne ops._2)
        if (changedLocals.hasNext) {
            println(YELLOW_B + BLUE+"\tUpdated Locals:")
            changedLocals.foreach(locals ⇒ print(YELLOW_B + BLUE+"\t\t"+locals._3+":"+locals._1+" => "+locals._2+"\n"))
        }
        println(YELLOW_B + BLUE+"\tDone"+RESET)

    }

    override def abruptMethodExecution(
        domain: Domain)(
            pc: Int,
            exception: domain.DomainValue): Unit = {
        println(Console.BOLD +
            Console.RED +
            pc+":RETURN FROM METHOD DUE TO UNHANDLED EXCEPTION: "+exception +
            Console.RESET)
    }

    override def jumpToSubroutine(
        domain: Domain)(
            pc: PC, target: PC, nestingLevel: Int): Unit = {
        import Console._
        println(pc+":"+YELLOW_B + BOLD+"JUMP TO SUBROUTINE(Nesting level: "+nestingLevel+"): "+target + RESET)
    }

    override def returnFromSubroutine(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            subroutineInstructions: List[PC]): Unit = {
        println(Console.YELLOW_B +
            Console.BOLD +
            pc+":RETURN FROM SUBROUTINE: target="+returnAddress+
            " : RESETTING : "+subroutineInstructions.mkString(", ") +
            Console.RESET)
    }

    /**
     * Called when a ret instruction is encountered.
     */
    override def ret(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC]): Unit = {
        println(Console.GREEN_B +
            Console.BOLD +
            pc+":RET : target="+returnAddress+
            " : OLD_WORKLIST : "+oldWorklist.mkString(", ")+
            " : NEW_WORKLIST : "+newWorklist.mkString(", ") +
            Console.RESET)
    }

    override def result(result: AIResult): Unit = { /*ignored*/ }

    override def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[PC], message: ⇒ String): Unit = {
        val loc = pc.map(pc ⇒ s"$pc:").getOrElse("<NO PC>")
        println(
            s"$loc[Domain:${source.getSimpleName().split('$')(0)} - $typeID] $message"
        )
    }
}
