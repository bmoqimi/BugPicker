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
package dependency

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import de.tud.cs.st.bat.resolved.reader.Java7Framework
import java.util.zip.ZipFile

/**
 * Tests whether all class files contained in the "test/classfiles" directory
 * can be processed by the <code>DependencyExtractor</code> without failure.
 * The results themselves will not be verified in these test cases.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 * @author Marco Jacobasch
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ExtractDependenciesFromClassFilesTest extends FlatSpec with Matchers {

  for {
    file ← TestSupport.locateTestResources("classfiles", "ext/dependencies").listFiles()
    if (file.isFile && file.canRead && file.getName.endsWith(".jar"))
  } {
    val zipfile = new ZipFile(file)
    val zipentries = (zipfile).entries
    while (zipentries.hasMoreElements) {
      val zipentry = zipentries.nextElement
      if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {

        val dependencyExtractor = new DependencyExtractor(new SourceElementIDsMap {}) with NoSourceElementsVisitor {
          def processDependency(src: Int, trgt: Int, dType: DependencyType) {
            /* DO NOTHING */
          }
        }

        it should ("be able to extract dependencies of class file " + zipentry.getName + " in " + zipfile.getName) in {
          var classFile = Java7Framework.ClassFile(() ⇒ zipfile.getInputStream(zipentry))
          dependencyExtractor.process(classFile)
        }

      }
    }
  }

}