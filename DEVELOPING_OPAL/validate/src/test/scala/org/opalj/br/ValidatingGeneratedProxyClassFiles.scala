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
package br

import analyses.Project

import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.ai.BaseAI

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Checks that the ClassFileFactory produces valid proxy class files.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ValidatingGeneratedProxyClassFiles extends FunSpec with Matchers {
    describe("Validating ClassFileFactory's proxy class files") {

        val testProject = Project(TestSupport.locateTestResources("classfiles/proxy.jar", "br"))

        val StaticMethods = ObjectType("proxy/StaticMethods")
        val InstanceMethods = ObjectType("proxy/InstanceMethods")
        val Constructors = ObjectType("proxy/Constructors")
        val PrivateInstanceMethods = ObjectType("proxy/PrivateInstanceMethods")
        val InterfaceMethods = ObjectType("proxy/InterfaceMethods")

        def getMethods(
            theClass: ObjectType,
            repository: ClassFileRepository): Iterable[(ObjectType, Method)] =
            repository.classFile(theClass).map { cf ⇒
                cf.methods.map((theClass, _))
            }.getOrElse(Iterable.empty)

        val types = Seq(StaticMethods, InstanceMethods, Constructors,
            PrivateInstanceMethods, InterfaceMethods)

        it("should generate interpretable proxy methods") {
            types.par.foreach { theType ⇒
                val methods = getMethods(theType, testProject)
                methods should not be ('empty)
                methods.foreach { p ⇒
                    val (t: ObjectType, m: Method) = p
                    val definingType = TypeDeclaration(
                        ObjectType("ProxyValidation$"+t.toJava+":"+m.toJava.replace(' ', '_')+"$"),
                        false,
                        Some(ObjectType.Object),
                        Set.empty
                    )
                    val proxyMethodName = m.name+"$proxy"
                    val proxy = ClassFileFactory.Proxy(definingType,
                        proxyMethodName,
                        m.descriptor,
                        t,
                        m.name,
                        m.descriptor,
                        m.isStatic,
                        m.isPrivate,
                        testProject.classFile(t).map(_.isInterfaceDeclaration).getOrElse(false))
                    val proxyMethod = proxy.findMethod(proxyMethodName).get
                    val domain = new BaseDomain(testProject, proxy, proxyMethod)
                    val result = BaseAI(proxy, proxyMethod, domain)
                    result should not be ('wasAborted)
                    val instructions = proxyMethod.body.get.instructions
                    instructions.count(_ != null) should be >= 2
                    //                println(org.opalj.ai.memoryLayoutToText(result.domain)(result.operandsArray, result.localsArray))
                    result.operandsArray.zip(instructions).foreach { p ⇒
                        val (oa, i) = p
                        if (i != null) oa should not be (null)
                    }
                    for {
                        pc ← 0 until instructions.size
                    } {
                        if (instructions(pc) != null) {
                            val nextPc = instructions(pc).indexOfNextInstruction(pc, false)
                            instructions.slice(pc + 1, nextPc).foreach(_ should be(null))
                        }
                    }
                }
            }
        }
    }
}