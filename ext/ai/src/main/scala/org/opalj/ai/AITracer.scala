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
package ai

import instructions._

/**
 * Defines the interface between the abstract interpreter and a module for
 * tracing the interpreter's behavior. In general, a tracer is first registered with an
 * abstract interpreter. After that, when a method is analyzed, OPAL-AI calls the
 * tracer's methods at the respective points in time.
 *
 * A tracer is registered with an abstract interpreter by creating a new subclass of
 * `AI` and overriding the method `tracer`.
 *
 * @note In general, all mutable data structures (e.g. the current locals and the
 *      operand stack) passed to the tracer should not be mutated as '''these data
 *      structures are the original data structures on which the abstract interpreter
 *      works and they are not copies'''.
 *      However, using the `AITracer` it is possible to develop a debugger for OPAL-AI and
 *      to enable the user to perform certain mutations.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * Called by OPAL-AI immediately before the (abstract) interpretation of the
     * specified code is performed.
     *
     * The tracer is not expected to make any changes to the data structures
     * (`operandsArray` and `localsArray`). If the tracer makes such changes, it is
     * the responsibility of the tracer to ensure that the updates are meaningful.
     * OPAL-AI will not perform any checks.
     */
    def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: Array[List[domain.DomainValue]],
            localsArray: Array[Array[domain.DomainValue]])

    /**
     * Called by OPAL-AI always before an instruction is evaluated.
     *
     * This enables the tracer to precisely log the behavior of the abstract
     * interpreter, but also enables the tracer to interrupt the evaluation
     * to, e.g., enable stepping through a program.
     *
     * @param operands The operand stack before the execution of the instruction.
     * @param locals The registers before the execution of the instruction.
     */
    def instructionEvalution(
        domain: Domain)(
            pc: PC,
            instruction: Instruction,
            operands: List[domain.DomainValue],
            locals: Array[domain.DomainValue]): Unit

    /**
     * Called by OPAL-AI after an instruction (`currentPC`) was evaluated and before the
     * `targetPC` may be evaluated.
     *
     * This method is only called if the instruction with the program counter
     * `targetPC` will be evaluated. I.e., when the abstract interpreter
     * determines that the evaluation of an instruction does not change the abstract
     * state (associated with the successor instruction) and, therefore, will not
     * schedule the successor instruction this method is not called.
     *
     * In case of `if` or `switch` instructions `flow` may be
     * called multiple times before the method `instructionEvaluation` is called again.
     *
     * Recall that OPAL-AI performs a depth-first exploration.
     */
    def flow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit

    /**
     * Called if the instruction with the `targetPC` was rescheduled. I.e., the
     * instruction was already scheduled for evaluation in the future, but was now
     * rescheduled for a more immediate evaluation. I.e., it was moved to the first
     * position in the list that contains the instructions that will be evaluated.
     * However, further instructions may be appended to the list before the
     * next `instructionEvaluation` takes place.
     *
     * Recall that OPAL-AI performs a depth-first exploration.
     */
    def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit

    /**
     * Called by OPAL-AI whenever two paths converge and the values on the operand stack
     * and the registers are joined.
     *
     * @param thisOperands The operand stack as it was used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param thisLocals The registers as they were used the last time when the
     * 		instruction with the given program counter was evaluated.
     * @param otherOperands The current operand stack when we re-reach the instruction
     * @param otherLocals The current registers.
     * @param result The result of joining the operand stacks and register
     * 		assignment.
     */
    def join(
        domain: Domain)(
            pc: PC,
            thisOperands: domain.Operands,
            thisLocals: domain.Locals,
            otherOperands: domain.Operands,
            otherLocals: domain.Locals,
            result: Update[(domain.Operands, domain.Locals)])

    /**
     * Called before a jump to a subroutine.
     */
    def jumpToSubroutine(domain: Domain)(pc: PC): Unit

    /**
     * Called when a `RET` instruction is encountered. (That does not necessary imply
     * that the evaluation of the subroutine as such has finished. It is possible
     * that other paths still need to be pursued.)
     */
    def ret(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            oldWorklist: List[PC],
            newWorklist: List[PC])

    /**
     * Called when the evaluation of a subroutine (JSR/RET) as a whole is completed.
     * I.e., all possible paths are analyzed and the fixpoint is reached.
     */
    def returnFromSubroutine(
        domain: Domain)(
            pc: PC,
            returnAddress: PC,
            subroutineInstructions: List[PC]): Unit

    /**
     * Called when the analyzed method throws an exception (i.e., the interpreter
     * evaluates an `athrow` instruction) that is not caught within
     * the method.
     */
    def abruptMethodExecution(
        domain: Domain)(
            pc: PC,
            exception: domain.DomainValue)

    /**
     * Called by OPAL-AI when the abstract interpretation of a method has completed/was
     * interrupted.
     */
    def result(result: AIResult): Unit
}