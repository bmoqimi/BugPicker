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
package l2

import org.junit.runner.RunWith
import org.junit.Ignore
import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import org.opalj.bi.TestSupport.locateTestResources

import org.opalj.util._
import org.opalj.br._
import org.opalj.br.analyses.{ SomeProject, Project }
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.ai.domain.l1._
import org.opalj.ai.domain.l0.RecordMethodCallResults

/**
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PerformInvocationsTest extends FlatSpec with Matchers with ParallelTestExecution {

    import PerformInvocationsTestFixture._

    behavior of "PerformInvocations"

    // This primarily tests that mixing in the PerformInvocations trait does
    // not cause any immediate harm.
    it should ("be able to analyze a simple static method that does nothing") in {
        val method = StaticCalls.findMethod("doNothing").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        import domain._
        val result = BaseAI(StaticCalls, method, domain)
        result.domain.returnedNormally should be(true)
    }

    // This primarily tests that mixing in the PerformInvocations trait does
    // not cause any immediate harm.
    it should ("be able to analyze a simple static method that always throws an exception") in {
        val method = StaticCalls.findMethod("throwException").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(false)

        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 1) fail("expected one exception: "+exs.mkString(", "))
        exs forall { ex ⇒
            ex match {
                case domain.SObjectValue(ObjectType("java/lang/UnsupportedOperationException")) ⇒
                    true
                case _ ⇒ false
            }
        } should be(true)
    }

    it should ("be able to analyze a static method that calls another static method that my fail") in {
        val method = StaticCalls.findMethod("mayFail").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)

        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 1) fail("expected one exception: "+exs.mkString(", "))
        exs forall { ex ⇒
            ex match {
                case domain.SObjectValue(ObjectType("java/lang/UnsupportedOperationException")) ⇒
                    true
                case _ ⇒
                    false
            }
        } should be(true)
    }

    it should ("be able to analyze a static method that calls another static method") in {
        val method = StaticCalls.findMethod("performCalculation").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.thrownExceptions(result.domain, -1).size should be(0)
    }

    it should ("be able to analyze a static method that calls multiple other static methods") in {
        val method = StaticCalls.findMethod("doStuff").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.thrownExceptions(result.domain, -1).size should be(0)
    }

    it should ("be able to analyze a static method that processes the results of other static methods") in {
        val method = StaticCalls.findMethod("callComplexMult").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions should be(empty)
        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(110))
    }

    it should ("be able to analyze a static method that throws different exceptions using the same throws statement") in {
        val method = StaticCalls.findMethod("throwMultipleExceptions").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(false)
        val exs = domain.thrownExceptions(result.domain, -1)
        if (exs.size != 4) fail("too many exceptions: "+exs)
        var foundUnknownError = false
        var foundUnsupportedOperationException = false
        var foundNullPointerException = false
        var foundIllegalArgumentException = false

        exs forall { ex ⇒
            ex match {
                case domain.SObjectValue(ObjectType("java/lang/UnsupportedOperationException")) ⇒
                    foundUnsupportedOperationException = true
                    true
                case domain.SObjectValue(ObjectType.NullPointerException) ⇒
                    foundNullPointerException = true
                    true
                case domain.SObjectValue(ObjectType("java/lang/UnknownError")) ⇒
                    foundUnknownError = true
                    true
                case domain.SObjectValue(ObjectType("java/lang/IllegalArgumentException")) ⇒
                    foundIllegalArgumentException = true
                    true
                case _ ⇒
                    fail("unexpected exception: "+ex)
            }
        } should be(true)
        if (!(foundUnknownError &&
            foundUnsupportedOperationException &&
            foundIllegalArgumentException &&
            foundNullPointerException)) fail("Not all expected exceptions were thrown")
    }

    it should ("be able to analyze a static method that calls another static method that calls ...") in {
        val method = StaticCalls.findMethod("aLongerCallChain").get
        val domain = new LiInvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)
        val exs = domain.thrownExceptions(result.domain, -1)
        exs.size should be(0)

        domain.returnedValue(domain, -1).flatMap(domain.intValueOption(_)) should equal(Some(175))
    }

    it should ("be able to analyze a method that analyzes the correlation between values") in {
        val method = StaticCalls.findMethod("callAreEqual").get
        val domain = new L1InvocationDomain(PerformInvocationsTestFixture.project, method)
        val result = BaseAI(StaticCalls, method, domain)
        domain.returnedNormally should be(true)
        domain.allThrownExceptions.size should be(2) // the ArithmeticExceptions due to "%"

        domain.allReturnedValues.size should be(2)
        if (!domain.allReturnedValues.forall {
            e ⇒ domain.intValueOption(e._2).map(_ == 1).getOrElse(false)
        }) fail("unexpected result: "+domain.allReturnedValues)
    }

}

object PerformInvocationsTestFixture {

    trait L1Domain extends Domain
        with DefaultDomainValueBinding
        with TheProject[java.net.URL]
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l1.DefaultReferenceValuesBinding
        with l1.DefaultIntegerRangeValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultPrimitiveValuesConversions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with ProjectBasedClassHierarchy
        with IgnoreSynchronization
        with TheMethod

    trait LiDomain extends Domain
            with DefaultDomainValueBinding
            with TheProject[java.net.URL]
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l1.DefaultReferenceValuesBinding
            with li.DefaultPreciseIntegerValues
            with li.DefaultPreciseLongValues
            with l0.DefaultPrimitiveValuesConversions
            with l0.TypeLevelFieldAccessInstructions
            with l0.TypeLevelInvokeInstructions
            with ProjectBasedClassHierarchy
            with IgnoreSynchronization
            with TheMethod {
        override def maxUpdatesForIntegerValues: Long = Int.MaxValue.toLong * 2
    }

    abstract class InvocationDomain(
        val project: Project[java.net.URL],
        val method: Method)
            extends Domain
            with PerformInvocations
            with DefaultHandlingOfMethodResults
            with RecordMethodCallResults {
        domain: ValuesFactory with ClassHierarchy with Configuration with TheProject[_] with TheMethod ⇒

        def isRecursive(
            definingClass: ClassFile,
            method: Method,
            operands: Operands): Boolean = false

        def shouldInvocationBePerformed(
            definingClass: ClassFile,
            method: Method): Boolean = true

        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method: Method): InvocationDomain

        def invokeExecutionHandler(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            operands: Operands): InvokeExecutionHandler =
            new InvokeExecutionHandler {

                val domain: Domain with MethodCallResults =
                    createInvocationDomain(project, method)

                def ai: AI[_ >: domain.type] = BaseAI
            }
    }

    class LiInvocationDomain(project: Project[java.net.URL], method: Method)
            extends InvocationDomain(project, method) with LiDomain {
        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method: Method): InvocationDomain = new LiInvocationDomain(project, method)
    }

    class L1InvocationDomain(project: Project[java.net.URL], method: Method)
            extends InvocationDomain(project, method) with L1Domain {
        protected[this] def createInvocationDomain(
            project: Project[java.net.URL],
            method: Method): InvocationDomain = new L1InvocationDomain(project, method)
    }

    val testClassFileName = "classfiles/performInvocations.jar"
    val testClassFile = locateTestResources(testClassFileName, "ai")
    val project = Project(testClassFile)
    val StaticCalls = project.classFile(ObjectType("performInvocations/StaticCalls")).get

}