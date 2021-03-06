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

import org.opalj.br.Code
import org.opalj.br.instructions.Instruction

/**
 * Defines the interface between the abstract interpreter and a module for tracing and
 * debugging the interpreter's progress. In general, a tracer is first registered with an
 * abstract interpreter. After that, when a method is analyzed, the [[AI]] calls the
 * tracer's methods at the respective points in time.
 *
 * A tracer is registered with an abstract interpreter by creating a new subclass of
 * [[AI]] and overriding the method [[AI.tracer]].
 *
 * @note '''All data structures passed to the tracer are the original data structures
 *      used by the abstract interpreter.''' Hence, if a value is mutated (e.g., for
 *      debugging purposes) it has to be guaranteed that the VM's conditions are never
 *      violated. E.g., exchanging a integer value against a reference value will most
 *      likely crash the interpreter.
 *      However, using the [[AITracer]] it is possible to develop a debugger for OPAL and
 *      to enable the user to perform certain mutations.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * Called by OPAL immediately before the abstract interpretation of the
     * specified code is performed.
     *
     * If the tracer changes the `operandsArray` and/or `localsArray`, it is
     * the responsibility of the tracer to ensure that the data structures are still
     * valid afterwards.
     * OPAL will not perform any checks.
     */
    def continuingInterpretation(
        code: Code,
        domain: Domain)(
            initialWorkList: List[PC],
            alreadyEvaluated: List[PC],
            operandsArray: domain.OperandsArray,
            localsArray: domain.LocalsArray,
            memoryLayoutBeforeSubroutineCall: List[(domain.OperandsArray, domain.LocalsArray)]): Unit

    /**
     * Always called by OPAL before an instruction is evaluated.
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
            operands: domain.Operands,
            locals: domain.Locals): Unit

    /**
     * Called by the interpreter after an instruction (`currentPC`) was evaluated and
     * before the instruction with the program counter `targetPC` may be evaluated.
     *
     * This method is only called if the instruction with the program counter
     * `targetPC` will be evaluated. I.e., when the abstract interpreter
     * determines that the evaluation of an instruction does not change the abstract
     * state (associated with the successor instruction) and, therefore, will not
     * schedule the successor instruction this method is not called.
     *
     * In case of `if` or `switch` instructions `flow` may be
     * called multiple times (even with the same targetPC) before the method
     * `instructionEvaluation` is called again.
     *
     * @note OPAL performs a depth-first exploration.
     */
    def flow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit

    /**
     * Called by the interpreter if a successor instruction is NOT scheduled, because
     * the abstract state didn't change.
     */
    def noFlow(
        domain: Domain)(
            currentPC: PC,
            targetPC: PC): Unit

    /**
     * Called if the instruction with the `targetPC` was rescheduled. I.e., the
     * instruction was already scheduled for evaluation in the future, but was now
     * rescheduled for a more immediate evaluation. I.e., it was moved to the first
     * position in the list that contains the instructions that will be evaluated.
     * However, further instructions may be appended to the list before the
     * next `instructionEvaluation` takes place.
     *
     * @note OPAL performs a depth-first exploration.
     */
    def rescheduled(
        domain: Domain)(
            sourcePC: PC,
            targetPC: PC,
            isExceptionalControlFlow: Boolean): Unit

    /**
     * Called by the abstract interpreter whenever two paths converge and the values
     * on the operand stack and the registers are joined.
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
            result: Update[(domain.Operands, domain.Locals)]): Unit

    /**
     * Called before a jump to a subroutine.
     */
    def jumpToSubroutine(domain: Domain)(pc: PC, target: PC, nestingLevel: Int): Unit

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
            newWorklist: List[PC]): Unit

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
     * Called when the analyzed method throws an exception that is not caught within
     * the method. I.e., the interpreter evaluates an `athrow` instruction or some
     * other instruction that throws an exception.
     */
    def abruptMethodExecution(
        domain: Domain)(
            pc: PC,
            exception: domain.DomainValue): Unit

    /**
     * Called when the abstract interpretation of a method has completed/was
     * interrupted.
     */
    def result(result: AIResult): Unit

    /**
     * Called by the framework if a constraint is established. Constraints are generally
     * established whenever a conditional jump is performed and the
     * evaluation of the condition wasn't definitive. In this case a constraint will
     * be established for each branch. In general the constraint will be applied
     * before the join of the stack and locals with the successor instruction is done.
     */
    def establishedConstraint(
        domain: Domain)(
            pc: PC,
            effectivePC: PC,
            operands: domain.Operands,
            locals: domain.Locals,
            newOperands: domain.Operands,
            newLocals: domain.Locals): Unit

    /**
     * Called by the domain if something noteworthy was determined.
     *
     * @param domain The domain.
     * @param source The class (typically the (partial) domain) that generated the message.
     * @param typeID A `String` that identifies the message. This value must not be `null`,
     *      but it can be the empty string.
     * @param message The message; a non-null `String` that is formatted for the console.
     */
    def domainMessage(
        domain: Domain,
        source: Class[_], typeID: String,
        pc: Option[PC], message: ⇒ String): Unit

}
