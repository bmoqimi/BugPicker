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

import java.net.URL

import scala.collection.SortedMap
import scala.xml.Node
import scala.xml.Unparsed

import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.collectWithOperandsAndIndex
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.ComputationalTypeLong
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.BinaryArithmeticInstruction
import org.opalj.br.instructions.ConditionalBranchInstruction
import org.opalj.br.instructions.IINC
import org.opalj.br.instructions.INSTANCEOF
import org.opalj.br.instructions.ShiftInstruction
import org.opalj.util.PerformanceEvaluation.time

import org.opalj.br.instructions.INEG
import org.opalj.br.instructions.LNEG

/**
 * A static analysis that analyzes the data-flow to detect useless computations and 
 * dead code. 
 * 
 * ==Precision==
 * The analysis is complete; i.e., every reported case is a true case. However, given
 * that we analyze Java bytecode, some findings may be the result of the compilation
 * scheme used for compiling the source code and, hence, cannot be resolved at the
 * sourcecode level.  
 */
class DeadCodeAnalysis extends Analysis[URL, (Long, Iterable[BugReport])] {

    override def title: String = "Dead/Useless/Buggy Code Identification"

    override def description: String = "Identifies dead/useless/buggy code using abstract interpretation."

    /**
     * Executes the analysis of the projects concrete methods.
     *
     * @param Either an empty sequence or a sequence that contains one or more of the following parametimport org.opalj.bugpicker.analysis.Percentage
     * import org.opalj.bugpicker.analysis.DeadCodeAnalysisDomain
     * import org.opalj.bugpicker.analysis.BugReport
     * ers:
     *      - a string that matches the following pattern: `-maxEvalFactor=(\d+(?:.\d+)?)`; e.g.,
     *      `-maxEvalFactor=0.5` or `-maxEvalFactor=1.5`. A value below 0.05 is usually
     *      not useable.
     *      - a string that machtes the following pattern: `-maxCardinalityOfIntegerRanges=(\d+)`.
     */
    override def analyze(
        theProject: Project[URL],
        parameters: Seq[String],
        initProgressManagement: (Int) ⇒ ProgressManagement): (Long, Iterable[BugReport]) = {

        val maxEvalFactor: Double =
            parameters.collectFirst {
                case DeadCodeAnalysis.maxEvalFactorPattern(d) ⇒
                    java.lang.Double.parseDouble(d).toDouble
            }.getOrElse(
                DeadCodeAnalysis.defaultMaxEvalFactor
            )
        val maxEvalTime: Int =
            parameters.collectFirst {
                case DeadCodeAnalysis.maxEvalTimePattern(l) ⇒
                    java.lang.Integer.parseInt(l).toInt
            }.getOrElse(
                DeadCodeAnalysis.defaultMaxEvalTime
            )
        val maxCardinalityOfIntegerRanges: Int =
            parameters.collectFirst {
                case DeadCodeAnalysis.maxCardinalityOfIntegerRangesPattern(i) ⇒
                    java.lang.Integer.parseInt(i).toInt
            }.getOrElse(
                DeadCodeAnalysis.defaultMaxCardinalityOfIntegerRanges
            )

        println("Settings:")
        println(s"\tmaxEvalFactor=$maxEvalFactor")
        println(s"\tmaxEvalTime=${maxEvalTime}ms")
        println(s"\tmaxCardinalityOfIntegerRanges=$maxCardinalityOfIntegerRanges")

        // related to managing the analysis progress
        val classFilesCount = theProject.projectClassFilesCount
        val progressManagement = initProgressManagement(classFilesCount)
        val doInterrupt: () ⇒ Boolean = progressManagement.isInterrupted

        val results = new java.util.concurrent.ConcurrentLinkedQueue[BugReport]()
        def analyzeMethod(classFile: ClassFile, method: Method, body: Code) {
            val domain =
                new DeadCodeAnalysisDomain(theProject, method, maxCardinalityOfIntegerRanges)
            val ai =
                new BoundedInterruptableAI[domain.type](
                    body,
                    maxEvalFactor,
                    maxEvalTime,
                    doInterrupt)
            val result = ai(classFile, method, domain)

            if (!result.wasAborted) {
                val operandsArray = result.operandsArray

                val methodWithDeadCode = for {
                    (ctiPC, instruction, branchTargetPCs) ← body collectWithIndex {
                        case (ctiPC, i: ConditionalBranchInstruction) if operandsArray(ctiPC) != null ⇒
                            (ctiPC, i, i.nextInstructions(ctiPC, /*not required*/ null))
                    }
                    branchTarget ← branchTargetPCs.iterator
                    if operandsArray(branchTarget) == null
                } yield {
                    val operands = operandsArray(ctiPC).take(instruction.operandCount)
                    DeadCode(classFile, method, ctiPC, operands, branchTarget, None)
                }
                for ((ln, dc) ← methodWithDeadCode.groupBy(_.line)) {
                    ln match {
                        case None ⇒
                            if (dc.tail.isEmpty) {
                                // we have just one message, but since we have 
                                // no line number we are still "doubtful"
                                results.add(dc.head.copy(accuracy = Some(Percentage(75))))
                            } else {
                                dc.foreach(i ⇒ results.add(i.copy(accuracy = Some(Percentage(5)))))
                            }

                        case Some(ln) ⇒
                            if (dc.tail.isEmpty)
                                // we have just one message,...
                                results.add(dc.head.copy(accuracy = Some(Percentage(100))))
                            else
                                dc.foreach(i ⇒ results.add(i.copy(accuracy = Some(Percentage(10)))))
                    }
                }

                val methodsWithUselessComputations = {
                    import result.domain.ConcreteIntegerValue
                    import result.domain.ConcreteLongValue
                    collectWithOperandsAndIndex(result.domain)(body, result.operandsArray) {

                        // HANDLING INT VALUES 
                        //
                        case (
                            pc,
                            instr @ BinaryArithmeticInstruction(ComputationalTypeInt),
                            Seq(ConcreteIntegerValue(a), ConcreteIntegerValue(b), _*)
                            ) ⇒
                            (pc, s"Constant computation: $b ${instr.operator} $a.")

                        case (pc, instr: INEG.type, Seq(ConcreteIntegerValue(a), _*)) ⇒
                            (pc, s"Constant computation: -${a}")

                        case (pc, instr @ IINC(_, v), Seq(ConcreteIntegerValue(a), _*)) ⇒
                            (pc, s"Constant computation (inc): ${a} + $v")

                        // HANDLING LONG VALUES 
                        //
                        case (
                            pc,
                            instr @ BinaryArithmeticInstruction(ComputationalTypeLong),
                            Seq(ConcreteLongValue(a), ConcreteLongValue(b), _*)
                            ) ⇒
                            (pc, s"Constant computation: ${b}l ${instr.operator} ${a}l.")
                        case (
                            pc,
                            instr @ ShiftInstruction(ComputationalTypeLong),
                            Seq(ConcreteLongValue(a), ConcreteIntegerValue(b), _*)
                            ) ⇒
                            (pc, s"Constant computation: ${b}l ${instr.operator} ${a}l.")

                        case (pc, instr: LNEG.type, Seq(ConcreteLongValue(a), _*)) ⇒
                            (pc, s"Constant computation: -${a}l")

                        // HANDLING REFERENCE VALUES
                        //

                        case (
                            pc,
                            INSTANCEOF(referenceType),
                            Seq(rv: domain.ReferenceValue, _*)
                            ) if result.domain.intValueOption(
                            result.operandsArray(pc + INSTANCEOF.length).head).isDefined ⇒
                            (pc, s"Useless type test: ${rv.upperTypeBound.map(_.toJava).mkString("", " with ", "")} instanceof ${referenceType.toJava}")

                    }.map { result ⇒
                        val (pc, message) = result
                        UselessComputation(classFile, method, pc, message)
                    }
                }
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(methodsWithUselessComputations)
                )

            } else if (!doInterrupt()) {
                println(
                    s"[warn] analysis of ${method.fullyQualifiedSignature(classFile.thisType)} aborted "+
                        s"after ${ai.currentEvaluationCount} steps "+
                        s"(code size: ${method.body.get.instructions.length})")
            } /* else (doInterrupt === true) the analysis as such was interrupted*/
        }

        var analysisTime: Long = 0l
        val identifiedIssues = time {
            val stepIds = new java.util.concurrent.atomic.AtomicInteger(0)

            for {
                classFile ← theProject.projectClassFiles.par
                if !progressManagement.isInterrupted()
            } {
                val stepId = stepIds.incrementAndGet()
                try {
                    progressManagement.start(stepId, classFile.thisType.toJava)
                    for (method @ MethodWithBody(body) ← classFile.methods) {
                        try {
                            analyzeMethod(classFile, method, body)
                        } catch {
                            case afe: InterpretationFailedException ⇒
                                val ms = method.fullyQualifiedSignature(classFile.thisType)
                                val steps = afe.ai.asInstanceOf[BoundedInterruptableAI[_]].currentEvaluationCount
                                println(
                                    s"[error] the analysis of ${ms} failed after $steps steps: "+afe.cause)
                        }
                    }
                } finally {
                    progressManagement.end(stepId)
                }
            }
            scala.collection.JavaConversions.collectionAsScalaIterable(results)
        } { t ⇒ analysisTime = t }

        (analysisTime, identifiedIssues)
    }
}

object DeadCodeAnalysis {

    // we want to match expressions such as:
    // -maxEvalFactor=1
    // -maxEvalFactor=20
    // -maxEvalFactor=1.25
    // -maxEvalFactor=10.5
    final val maxEvalFactorPattern = """-maxEvalFactor=(\d+(?:.\d+)?)""".r
    final val defaultMaxEvalFactor = 1.75d

    final val maxEvalTimePattern = """-maxEvalTime=(\d+)""".r
    final val defaultMaxEvalTime = 10000

    final val maxCardinalityOfIntegerRangesPattern =
        """-maxCardinalityOfIntegerRanges=(\d+)""".r
    final val defaultMaxCardinalityOfIntegerRanges = 16

    def resultsAsXHTML(results: (Long, Iterable[BugReport])): Node = {
        val (analysisTime, methodsWithDeadCode) = results
        val methodWithDeadCodeCount = methodsWithDeadCode.size

        <div id="dead_code_results">
            <script type="text/javascript">
                {
                    new Unparsed(
                        """function updateAccuracy(value) {
                            document.querySelectorAll("tr[data-accuracy]").forEach(
                                function(tr){
                                    tr.dataset.accuracy < value ? tr.style.display="none" : tr.style.display="table-row"
                                }
                            )
                        }""")
                }
            </script>
            <div>Number of identified issues: { methodWithDeadCodeCount }</div>
            <div>
                Suppress identified issues with an estimated
                <span class="tooltip">importance<span>The importance is calculated using the available context information.<br/>E.g., a dead <i>default case</i> in a switch statement is often the result of defensive programming and, hence, not important.</span></span>
                less than:
                <span class="tooltip">1<span>The identified issue is probably not important or is just a technical artifact.</span></span>
                <input type="range" name="accuracy" id="accuracy" min="1" max="100" onchange="updateAccuracy(this.valueAsNumber)"/>
                <span class="tooltip">100<span>The identified issue is probably very important.</span></span>
            </div>
            <table>
                <tr>
                    <th>Class</th>
                    <th>Method</th>
                    <th class="pc">Program Counter /<br/>Line Number</th>
                    <th>Message</th>
                </tr>
                {
                    import scala.collection.SortedMap
                    val groupedMessages =
                        SortedMap.empty[String, Seq[DeadCode]] ++
                            methodsWithDeadCode.groupBy(dc ⇒ dc.classFile.thisType.packageName)
                    for { (pkg, mdc) ← groupedMessages } yield {
                        Seq(
                            <tr><td class="caption" colspan="4">{ pkg.replace('/', '.') }</td></tr>,
                            mdc.toSeq.sorted(BugReportOrdering).map(_.toXHTML)
                        )
                    }.flatten
                }
            </table>
            <script type="text/javascript">
                document.getElementById('accuracy').value=75;
                updateAccuracy(75);
            </script>
        </div>
    }
}

