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
package findrealbugs

import resolved._
import resolved.analyses._
import java.net.URL

/**
 * This object provides an interface for the Eclipse plugin.
 *
 * @author Florian Brandherm
 */
object FindRealBugsPluginInterface {
    import FindRealBugs._

    /**
     * Analyzes a project consisting of the given files. Throws a
     * `FindRealBugsException` if an error occurs.
     *
     * @param inputFileNames The .class/.jar files that should be analyzed together.
     * @param inputLibraryFileNames The .class/.jar files that should be included in the
     * analysis as library class files.
     * @param disabledAnalyses Names of analyses that should not be run (default: empty).
     * @param progressListener A `ProgressListener` object that will be notified about the
     * analysis progress.
     * @param additionalAnalyses External analyses that should be added to the list of
     * analyses to run.
     * @return The analyses' reports.
     */
    def runAnalysis(
        inputFileNames: Iterable[String],
        inputLibraryFileNames: Iterable[String],
        disabledAnalyses: Iterable[String] = Nil,
        progressListener: ProgressListener,
        additionalAnalyses: Map[String, Analysis]): Array[(String, AnalysisReports)] = {

        if (inputFileNames.size == 0) {
            throw new FindRealBugsException("No input files!")
        }

        def loadClassFilesForPlugin(
            fileNames: Iterable[String],
            loadAsLibrary: Boolean): Iterable[(ClassFile, URL)] = {
            loadClassFiles(
                fileNames,
                loadAsLibrary,
                error ⇒ throw new FindRealBugsException(error),
                file ⇒ {}
            )
        }

        val classFiles = loadClassFilesForPlugin(inputFileNames, false)
        val libraryClassFiles = loadClassFilesForPlugin(inputLibraryFileNames, true)

        // Create project
        val project = Project(classFiles, libraryClassFiles)

        // Determine analyses that should be run
        val allAnalyses = builtInAnalyses ++ additionalAnalyses
        val analysesToRun =
            for {
                analysisName ← allAnalyses.keys
                if (!disabledAnalyses.exists(_ == analysisName))
            } yield {
                analysisName
            }

        // Analyze
        analyze(project,
            analysesToRun,
            progressListener,
            allAnalyses).toArray
    }
}

/**
 * Exception that will be thrown if the `FindRealBugsPluginInterface` encounters an error.
 *
 * @param message message that will be passed by the exception.
 */
class FindRealBugsException(message: String) extends Exception(message)
