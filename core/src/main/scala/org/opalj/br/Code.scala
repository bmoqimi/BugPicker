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
package de.tud.cs.st
package bat
package resolved

import instructions._

/**
 * Representation of a method's code attribute, that is, representation of a method's
 * implementation.
 *
 * @param maxStack The maximum size of the stack during the execution of the method.
 *      This value is determined by the compiler and is not necessarily the minimum.
 *      However, in the vast majority of cases it is the minimum.
 * @param maxLocals The number of registers/local variables needed to execute the method.
 * @param instructions The instructions of this `Code` array/`Code` block. Since the code
 *      array is not completely filled (it contains `null` values) the preferred way
 *      to iterate over all instructions is to use for-comprehensions and pattern
 *      matching or to use one of the predefined methods [[foreach]], [[collect]],
 *      [[collectPair]], [[collectWithIndex]].
 *      The `Code` array must not be mutated!
 *
 * @author Michael Eichberg
 */
case class Code(
    maxStack: Int,
    maxLocals: Int,
    instructions: Array[Instruction],
    exceptionHandlers: ExceptionHandlers,
    attributes: Attributes)
        extends Attribute {

    /**
     * Returns a new iterator to iterate over the program counters of the instructions
     * of this `Code` block.
     */
    def programCounters: Iterator[PC] =
        new Iterator[PC] {
            var pc = 0 // there is always at least one instruction

            def next = {
                val next = pc
                pc = pcOfNextInstruction(pc)
                next
            }

            def hasNext = pc < instructions.size
        }

    /**
     * Iterates over all instructions and calls the given function `f`
     * for every instruction.
     */
    def foreach(f: (PC, Instruction) ⇒ Unit): Unit = {
        import de.tud.cs.st.util.ControlAbstractions.foreachNonNullValueOf
        foreachNonNullValueOf(instructions)(f)
    }

    /**
     * Returns a view of all potential exception handlers (if any) for the
     * instruction with the given program counter (`pc`).
     *
     * @param pc The program counter of an instruction of this `Code` array.
     */
    def exceptionHandlersFor(pc: PC): Iterable[ExceptionHandler] =
        exceptionHandlers.view.filter { handler ⇒
            handler.startPC <= pc && handler.endPC > pc
        }

    def handlerInstructionsFor(pc: PC): PCs = {
        var pcs = de.tud.cs.st.collection.mutable.UShortSet.empty
        exceptionHandlers foreach { handler ⇒
            if (handler.startPC <= pc && handler.endPC > pc)
                pcs = handler.handlerPC +≈: pcs
        }
        pcs
    }

    /**
     * Returns the program counter of the next instruction after the instruction with
     * the given counter (`currentPC`).
     *
     * @param currentPC The program counter of an instruction. If `currentPC` is the
     *      program counter of the last instruction of the code block then the returned
     *      program counter will be equivalent to the length of the Code/Instructions
     *      array.
     */
    @inline final def pcOfNextInstruction(currentPC: PC): PC = {
        val max_pc = instructions.size
        var nextPC = currentPC + 1
        while (nextPC < max_pc && (instructions(nextPC) eq null))
            nextPC += 1

        nextPC
    }

    /**
     * Returns the line number table - if any.
     *
     * ==Note==
     * A code attribute is allowed to have multiple line number tables. However, all
     * tables are merged into one by OPAL at class loading time.
     *
     * Depending on the configuration of the reader for `ClassFile`s this
     * attribute may not be reified.
     */
    def lineNumberTable: Option[LineNumberTable] =
        attributes collectFirst { case lnt: LineNumberTable ⇒ lnt }

    /**
     * Returns the line number associated with the instruction with the given pc if
     * it is available.
     *
     * @param pc Index of the instruction for which we want to get the line number.
     * @return `Some` line number or `None` if it's unavailable.
     */
    def lineNumber(pc: PC): Option[Int] =
        lineNumberTable.flatMap(_.lookupLineNumber(pc))

    /**
     * Collects all local variable tables.
     *
     * @note Depending on the configuration of the reader for `ClassFile`s this
     * 	    attribute may not be reified.
     */
    def localVariableTable: Seq[LocalVariables] =
        attributes collect { case LocalVariableTable(lvt) ⇒ lvt }

    /**
     * Collects all local variable type tables.
     *
     * @note Depending on the configuration of the reader for `ClassFile`s this
     * 	    attribute may not be reified.
     */
    def localVariableTypeTable: Seq[LocalVariableTypes] =
        attributes collect { case LocalVariableTypeTable(lvtt) ⇒ lvtt }

    /**
     * The JVM specification mandates that a Code attribute has at most one
     * StackMapTable attribute.
     *
     * @note Depending on the configuration of the reader for `ClassFile`s this
     * 	    attribute may not be reified.
     */
    def stackMapTable: Option[StackMapFrames] =
        attributes collectFirst { case StackMapTable(smf) ⇒ smf }

    /**
     * True if the instruction with the given program counter is modified by wide.
     *
     * @param pc A valid index in the code array.
     */
    def isModifiedByWide(pc: PC): Boolean = pc > 0 && instructions(pc - 1) == WIDE

    /**
     * Collects all instructions for which the given function is defined.
     *
     * ==Usage scenario==
     * Use this function if you want to search for and collect specific instructions and
     * when you do not immediately require the program counter/index of the instruction
     * in the instruction array to make the decision whether you want to collect the
     * instruction.
     *
     * ==Examples==
     * Example usage to collect the declaring class of all get field accesses where the
     * field name is "last".
     * {{{
     * collect({
     *  case GETFIELD(declaringClass, "last", _) ⇒ declaringClass
     * })
     * }}}
     *
     * Example usage to collect all instances of a "DUP" instruction.
     * {{{
     * code.collect({ case dup @ DUP ⇒ dup })
     * }}}
     *
     * @return The result of applying the function f to all instructions for which f is
     *      defined combined with the index (program counter) of the instruction in the
     *      code array.
     */
    def collect[B](f: PartialFunction[Instruction, B]): Seq[(PC, B)] = {
        val max_pc = instructions.size
        var pc = 0
        var result: List[(PC, B)] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if (instruction ne null) {
                if (f.isDefinedAt(instruction)) {
                    result = (pc, f(instruction)) :: result
                }
            }
            pc += 1
        }
        result.reverse
    }

    /**
     * Applies the given function `f` to all instruction objects for which the function is
     * defined. The function is passed a tuple consisting of the current program
     * counter/index in the code array and the corresponding instruction.
     *
     * ==Example==
     * Example usage to collect the program counters (indexes) of all instructions that
     * are the target of a conditional branch instruction:
     * {{{
     * code.collectWithIndex({
     *  case (pc, cbi: ConditionalBranchInstruction) ⇒
     *      Seq(cbi.indexOfNextInstruction(pc, code), pc + cbi.branchoffset)
     *  }) // .flatten should equal (Seq(...))
     * }}}
     */
    def collectWithIndex[B](f: PartialFunction[(PC, Instruction), B]): Seq[B] = {
        val max_pc = instructions.size
        var pc = 0
        var result: List[B] = List.empty
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if (instruction ne null) {
                val params = (pc, instruction)
                if (f.isDefinedAt(params)) {
                    result = f(params) :: result
                }
            }
            pc += 1
        }
        result.reverse
    }

    /**
     * Applies the given function to the first instruction for which the given function
     * is defined.
     */
    def collectFirstWithIndex[B](f: PartialFunction[(PC, Instruction), B]): Option[B] = {
        val max_pc = instructions.size
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if ((instruction ne null)) {
                val params = (pc, instruction)
                if (f.isDefinedAt(params))
                    return Some(f(params))
            }
            pc += 1
        }
        return None
    }

    /**
     * Tests if an instruction matches the given filter. If so, the index of the first
     * matching instruction is returned.
     */
    def find(f: Instruction ⇒ Boolean): Option[PC] = {
        val max_pc = instructions.size
        var pc = 0
        while (pc < max_pc) {
            val instruction = instructions(pc)
            if ((instruction ne null) && f(instruction)) {
                return Some(pc)
            }
            pc += 1
        }
        return None
    }

    /**
     * Returns a new sequence that pairs the program counter of an instruction with the
     * instruction.
     */
    def associateWithIndex(): Seq[(PC, Instruction)] = collect { case i ⇒ i }

    /**
     * Slides over the code array and tries to apply the given function to each sequence
     * of instructions consisting of `windowSize` elements.
     *
     * ==Scenario==
     * If you want to search for specific patterns of bytecode instructions. Some "bug
     * patterns" are directly related to specific bytecode sequences and these patterns
     * can easily be identified using this method.
     *
     * ==Example==
     * Search for sequences of the bytecode instructions `PUTFIELD` and `ALOAD_O` in the
     * method's body and return the list of program counters of the start of the
     * identified sequences.
     * {{{
     * code.slidingCollect(2)({
     *  case (pc, Seq(PUTFIELD(_, _, _), ALOAD_0)) ⇒ (pc)
     * }) should be(Seq(...))
     * }}}
     *
     * @note If possible, use one of the more specialized methods, such as, [[collectPair]].
     *      The pure iteration overhead caused by this method is roughly 10-20 times higher
     *      than this one.
     *
     * @param windowSize The size of the sequence of instructions that is passed to the
     *      partial function.
     *      It must be larger than 0. **Do not use this method with windowSize "1"**;
     *      it is more efficient to use the `collect` or `collectWithIndex` methods
     *      instead.
     *
     * @return The list of results of applying the function f for each matching sequence.
     */
    def slidingCollect[B](
        windowSize: Int)(
            f: PartialFunction[(PC, Seq[Instruction]), B]): Seq[B] = {
        require(windowSize > 0)

        import scala.collection.immutable.Queue

        val max_pc = instructions.size
        var instrs: Queue[Instruction] = Queue.empty
        var firstPC, lastPC = 0
        var elementsInQueue = 0

        //
        // INITIALIZATION
        //
        while (elementsInQueue < windowSize - 1 && lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))
            lastPC = pcOfNextInstruction(lastPC)
            elementsInQueue += 1
        }

        // 
        // SLIDING OVER THE CODE
        //
        var result: List[B] = List.empty
        while (lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))

            if (f.isDefinedAt((firstPC, instrs))) {
                result = f((firstPC, instrs)) :: result
            }

            firstPC = pcOfNextInstruction(firstPC)
            lastPC = pcOfNextInstruction(lastPC)
            instrs = instrs.tail
        }

        result.reverse
    }

    /**
     * Finds a sequence of instructions that are matched by the given partial function.
     *
     * @note If possible, use one of the more specialized methods, such as, [[collectPair]].
     *      The pure iteration overhead caused by this method is roughly 10-20 times higher
     *      than this one.
     *
     * @return List of pairs where the first element is the pc of the first instruction
     *      of a matched sequence and the second value is the result of the evaluation
     *      of the partial function.
     */
    def findSequence[B](
        windowSize: Int)(
            f: PartialFunction[Seq[Instruction], B]): List[(PC, B)] = {
        require(windowSize > 0)

        import scala.collection.immutable.Queue

        val max_pc = instructions.size
        var instrs: Queue[Instruction] = Queue.empty
        var firstPC, lastPC = 0
        var elementsInQueue = 0

        //
        // INITIALIZATION
        //
        while (elementsInQueue < windowSize - 1 && lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))
            lastPC = pcOfNextInstruction(lastPC)
            elementsInQueue += 1
        }

        // 
        // SLIDING OVER THE CODE
        //
        var result: List[(PC, B)] = List.empty
        while (lastPC < max_pc) {
            instrs = instrs.enqueue(instructions(lastPC))

            if (f.isDefinedAt(instrs)) {
                result = (firstPC, f(instrs)) :: result
            }

            firstPC = pcOfNextInstruction(firstPC)
            lastPC = pcOfNextInstruction(lastPC)
            instrs = instrs.tail
        }

        result.reverse
    }

    /**
     * Finds a pair of consecutive instructions that are matched by the given partial
     * function.
     *
     * ==Example Usage==
     * {{{
     * (pc, _) ← body.findPair {
     *      case (
     *          INVOKESPECIAL(receiver1, _, SingleArgumentMethodDescriptor((paramType: BaseType, _))),
     *          INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
     *      ) if (...) ⇒ (...)
     *      } yield ...
     * }}}
     */
    def collectPair[B](
        f: PartialFunction[(Instruction, Instruction), B]): List[(PC, B)] = {
        val max_pc = instructions.size

        var first_pc = 0
        var firstInstruction = instructions(first_pc)
        var second_pc = pcOfNextInstruction(0)
        var secondInstruction: Instruction = null

        var result: List[(PC, B)] = List.empty
        while (second_pc < max_pc) {
            secondInstruction = instructions(second_pc)
            val instrs = (firstInstruction, secondInstruction)
            if (f.isDefinedAt(instrs)) {
                result = (first_pc, f(instrs)) :: result
            }

            firstInstruction = secondInstruction
            first_pc = second_pc
            second_pc = pcOfNextInstruction(second_pc)
        }
        result
    }

    def collectPairWithPreFilter[B](
        preFilter: (Opcode, Opcode))(
            f: PartialFunction[(Instruction, Instruction), B]): List[(PC, B)] = {
        val max_pc = instructions.size

        val firstInstructionFilter = preFilter._1
        val secondInstructionFilter = preFilter._2

        var first_pc = 0
        var firstInstruction = instructions(first_pc)
        var second_pc = pcOfNextInstruction(0)
        var secondInstruction: Instruction = null

        var result: List[(PC, B)] = List.empty
        while (second_pc < max_pc) {
            secondInstruction = instructions(second_pc)
            if (firstInstruction.opcode == firstInstructionFilter) {
                if (secondInstruction.opcode == secondInstructionFilter) {

                    val instrs = (firstInstruction, secondInstruction)
                    if (f.isDefinedAt(instrs)) {
                        result = (first_pc, f(instrs)) :: result
                    }
                }
            }

            firstInstruction = secondInstruction
            first_pc = second_pc
            second_pc = pcOfNextInstruction(second_pc)
        }
        result
    }

    /**
     * Matches pairs of two consecutive instructions. For each matched pair,
     * the program counter of the first instruction is returned.
     * 
     * ==Example Usage==
     * {{{
     * for {
     *  classFile ← project.view.map(_._1).par
     *  method @ MethodWithBody(body) ← classFile.methods
     *  pc ← body.matchPair({
     *      case (
     *          INVOKESPECIAL(receiver1, _, TheArgument(parameterType: BaseType)),
     *          INVOKEVIRTUAL(receiver2, name, NoArgumentMethodDescriptor(returnType: BaseType))
     *      ) ⇒ { (receiver1 eq receiver2) && (returnType ne parameterType) }
     *      case _ ⇒ false
     *      })
     *  } yield (classFile, method, pc)
     * }}}
     */
    def matchPair(f: (Instruction, Instruction) ⇒ Boolean): List[PC] = {
        val max_pc = instructions.size
        var pc1 = 0
        var pc2 = pcOfNextInstruction(pc1)

        var result: List[PC] = List.empty
        while (pc2 < max_pc) {
            if (f(instructions(pc1), instructions(pc2))) {
                result = pc1 :: result
            }

            pc1 = pc2
            pc2 = pcOfNextInstruction(pc2)
        }
        result
    }

    /**
     * A complete representation of this code attribute (including instructions,
     * attributes, etc.).
     */
    override def toString = {
        "Code_attribute("+
            "maxStack="+maxStack+
            ", maxLocals="+maxLocals+","+
            (instructions.filter(_ ne null).deep.toString) +
            (exceptionHandlers.toString)+","+
            (attributes.toString)+
            ")"
    }

    /**
     * This attribute's kind id.
     */
    override def kindId: Int = Code.KindId

}

/**
 * Defines constants useful when analyzing a method's code.
 *
 * @author Michael Eichberg
 */
object Code {

    /**
     * The unique id associated with attributes of kind: [[Code]].
     *
     * `KindId`s can be used for efficient branching on attributes.
     */
    final val KindId = 6

    /**
     * Used to determine the potential handlers in case that an exception is
     * thrown by an instruction.
     */
    protected[resolved] val preDefinedClassHierarchy =
        analyses.ClassHierarchy.preInitializedClassHierarchy
}