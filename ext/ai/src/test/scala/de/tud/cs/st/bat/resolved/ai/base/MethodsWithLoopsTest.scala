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
package de.tud.cs.st.bat
package resolved
package ai
package base

import reader.Java7Framework

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.ShouldMatchers

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithLoopsTest
        extends FlatSpec
        with ShouldMatchers
        with ParallelTestExecution {

    import util.Util.dumpOnFailureDuringValidation

    class RecordingDomain extends DefaultDomain with ConstraintManifestation {
        var returnedValues: List[(String, Value)] = List()
        override def areturn(value: Value) { returnedValues = ("areturn", value) :: returnedValues }
        override def dreturn(value: Value) { returnedValues = ("dreturn", value) :: returnedValues }
        override def freturn(value: Value) { returnedValues = ("freturn", value) :: returnedValues }
        override def ireturn(value: Value) { returnedValues = ("ireturn", value) :: returnedValues }
        override def lreturn(value: Value) { returnedValues = ("lreturn", value) :: returnedValues }
        override def returnVoid() { returnedValues = ("return", null) :: returnedValues }

        var constraints: List[ReifiedConstraint] = List()

        def addConstraint(constraint: ReifiedConstraint) {
            constraints = constraint :: constraints
        }
    }

    val classFiles = Java7Framework.ClassFiles(TestSupport.locateTestResources("classfiles/ai.jar", "ext/ai"))
    val classFile = classFiles.map(_._1).find(_.thisClass.className == "ai/MethodsWithLoops").get

    def findMethod(name: String): Method = {
        classFile.methods.find(_.name == name).get
    }

    private def evaluateMethod(name: String, f: RecordingDomain ⇒ Unit) {
        val domain = new RecordingDomain; import domain._
        val method = classFile.methods.find(_.name == name).get
        val result = AI(classFile, method, domain)

        dumpOnFailureDuringValidation(Some(classFile), Some(method), method.body.get, result) {
            f(domain)
        }
    }

    behavior of "the abstract interpreter when analyzing methods with loops"

    //
    // RETURNS
    it should "be able to analyze a method that never terminates" in {
        val domain = new RecordingDomain;
        val method = findMethod("endless")
        val result = AI(classFile, method, domain) // TODO [AI - Test] Update the test to not wait forever in case of a bug.
    }

}
