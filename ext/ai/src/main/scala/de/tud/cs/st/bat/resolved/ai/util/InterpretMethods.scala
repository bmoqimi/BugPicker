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
package de.tud.cs.st
package bat
package resolved
package ai
package util

import reader.Java7Framework.ClassFile
import domain.l0.BaseConfigurableDomain

import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.ByteArrayInputStream

import scala.Console._
import scala.util.control.ControlThrowable

/**
 * Performs an abstract interpretation of all methods of the given class file(s).
 *
 * This class is very helpful during the development and testing of new domains.
 *
 * @author Michael Eichberg
 */
object InterpretMethods {

    import de.tud.cs.st.util.debug._
    import de.tud.cs.st.util.ControlAbstractions._

    import scala.collection.JavaConversions._

    val performanceEvaluationContext = new de.tud.cs.st.util.debug.PerformanceEvaluation {}
    import performanceEvaluationContext._

    def println(s: String) { System.out.println(s); System.out.flush() }

    def main(args: Array[String]) {
        if (args.size == 0) {
            println("Performs an abstract interpretation of all methods of all classes.")
            println("1. [Optional] -domain=<DOMAIN CLASS> the configurable domain to use during the abstract interpretation.")
            println("... jar files and directories containing jar files.")
            return ;
        }
        if (args.size > 0 && args(0).startsWith("domain=")) {
            interpret(
                Class.forName(args.head.substring(8)).asInstanceOf[Class[_ <: Domain[_]]],
                args.tail.map(new java.io.File(_)),
                true).
                map(System.err.println(_))
        } else {
            interpret(
                classOf[BaseConfigurableDomain[_]],
                args.map(new java.io.File(_)),
                true).
                map(System.err.println(_))
        }
    }

    val timeLimit: Long = 250l //milliseconds

    def interpret(
        domainClass: Class[_ <: Domain[_]],
        files: Seq[java.io.File],
        beVerbose: Boolean = false): Option[String] = {

        reset('OVERALL)
        reset('READING)
        reset('PARSING)
        reset('AI)

        var collectedExceptions: List[(String, ClassFile, Method, Throwable)] = List()
        var classesCount = 0
        var methodsCount = 0

        val domainConstructor = domainClass.getConstructor(classOf[Object])

        time('OVERALL) {
            val theFiles = files.flatMap { file ⇒
                if (file.isDirectory())
                    file.listFiles()
                else
                    List(file)
            }
            for {
                file ← theFiles
                if (file.toString().endsWith(".jar"))
                jarFile = {
                    if (beVerbose) println(Console.BOLD + file.toString + Console.RESET)
                    new ZipFile(file)
                }
                jarEntry ← (jarFile).entries
                if !jarEntry.isDirectory && jarEntry.getName.endsWith(".class")
            } {
                val data = new Array[Byte](jarEntry.getSize().toInt)
                time('READING) {
                    process {
                        new DataInputStream(jarFile.getInputStream(jarEntry))
                    } { in ⇒
                        in.readFully(data)
                    }
                }
                analyzeClassFile(file.getName(), data)
            }

            def analyzeClassFile(resource: String, data: Array[Byte]) {
                val classFile = time('PARSING) {
                    ClassFile(new DataInputStream(new ByteArrayInputStream(data)))
                }
                classesCount += 1
                if (beVerbose) println(classFile.thisType.fqn)
                for (method ← classFile.methods; if method.body.isDefined) {
                    methodsCount += 1
                    if (beVerbose) println("  =>  "+method.toJava)
                    //                    val runnable = new Runnable {
                    //                        def run() {
                    try {
                        time('AI) {
                            if (BaseAI(
                                classFile,
                                method,
                                domainConstructor.newInstance((classFile, method))).wasAborted)
                                throw new InterruptedException();
                        }
                    } catch {
                        case ct: ControlThrowable ⇒ throw ct
                        case t: Throwable ⇒ {
                            // basically, we want to catch everything!
                            collectedExceptions = (resource, classFile, method, t) :: collectedExceptions
                        }
                    }
                    //                        }
                    //                    }
                    //                    val thread = new Thread(runnable)
                    //                    thread.start
                    //                    thread.join(timeLimit)
                    //                    thread.interrupt
                    //                    thread.join()
                }
            }
        }

        if (collectedExceptions.nonEmpty) {
            var report = "Exceptions: "

            collectedExceptions.groupBy(e ⇒ e._1) foreach { ge ⇒
                val (exResource, exInstances) = ge
                report +=
                    ("\n"+exResource+"("+exInstances.size+")").padTo(80, '_')

                report +=
                    exInstances.map { ex ⇒
                        val (resource, classFile, method, throwable) = ex
                        UNDERLINED + classFile.thisType.fqn+"\033[24m"+"{ "+
                            method.toJava+" => "+
                            RED +
                            (if (throwable != null)
                                throwableToString(throwable)
                            else
                                ""
                            ) +
                            RESET+
                            " }"
                    }.mkString("\n\t", ",\n\t", "\n")
            }
            report +=
                "\nDuring the interpretation of "+
                methodsCount+" methods in "+
                classesCount+" classes (overall: "+nsToSecs(getTime('OVERALL))+
                "secs. (reading: "+nsToSecs(getTime('READING))+
                "secs., parsing: "+nsToSecs(getTime('PARSING))+
                "secs., ai: "+nsToSecs(getTime('AI))+
                "secs.)) "+collectedExceptions.size+" exceptions occured."
            Some(report)
        } else {
            None
        }
    }

    def throwableToString(throwable: Throwable): String = {
        val baseThrowable = throwable match {
            case ie: InterpreterException[_] ⇒ ie.throwable
            case _                           ⇒ throwable
        }
        val stackTrace =
            Option(
                if (baseThrowable.getStackTrace() != null && baseThrowable.getStackTrace().size > 0)
                    baseThrowable.getStackTrace()
                else
                    null
            )

        (
            baseThrowable.getClass().getSimpleName() +
            stackTrace.map(s ⇒ Console.GREEN+" ("+s(0).getClassName()+":"+s(0).getLineNumber()+") "+Console.RED).getOrElse("")+
            " => "+
            baseThrowable.getMessage()
        ).trim
    }

}
