package org.opalj
package bugpicker

import scala.collection.JavaConversions
import java.io.File
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8LibraryFrameworkWithCaching
import org.opalj.br.ClassFile
import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.geometry.Insets
import scalafx.scene.web.WebView
import scalafx.event.ActionEvent
import scala.language.implicitConversions
import scalafx.scene.Scene

object ProjectHelper {
    def setupProject(loadedFiles: List[List[File]], parentStage: Stage): (Project[URL], Seq[File]) = {
        val files = loadedFiles(0)
        val sources = loadedFiles(1)
        val libs = loadedFiles(2)
        val project = setupProject(files, libs, parentStage)
        (project, sources)
    }

    def setupProject(
        cpFiles: Iterable[File],
        libcpFiles: Iterable[File],
        parentStage: Stage): Project[URL] = {
        println("[info] Reading class files (found in):")
        val cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
        val Java8ClassFileReader = new Java8FrameworkWithCaching(cache)
        val Java8LibraryClassFileReader = new Java8LibraryFrameworkWithCaching(cache)

        val (classFiles, exceptions1) =
            br.reader.readClassFiles(
                cpFiles,
                Java8ClassFileReader.ClassFiles,
                (file) ⇒ println("[info]\t"+file))

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                println("[info] Reading library class files (found in):")
                br.reader.readClassFiles(
                    libcpFiles,
                    Java8LibraryClassFileReader.ClassFiles,
                    (file) ⇒ println("[info]\t"+file))
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }
        val allExceptions = exceptions1 ++ exceptions2
        if (allExceptions.nonEmpty) {
            val out = new java.io.ByteArrayOutputStream
            val pout = new java.io.PrintStream(out)
            for (exception ← exceptions1 ++ exceptions2) {
                pout.println(s"<h3>${exception.getMessage}</h3>")
                exception.getStackTrace.foreach { ste ⇒
                    pout.append(ste.toString).println("<br/>")
                }
            }
            pout.flush
            val message = new String(out.toByteArray)
            val dialog = new DialogStage(parentStage) {
                scene = new Scene {
                    root = new BorderPane {
                        top = new Label {
                            text = "The following exceptions occurred while reading the specified files:"
                        }
                        val wv = new WebView
                        center = wv
                        wv.engine.loadContent(message)
                        bottom = new HBox {
                            content = new Button {
                                id = "Close"
                                text = "Close"
                                padding = Insets(5, 10, 5, 10)
                            }
                        }
                    }
                }
            }
            dialog.scene().lookup("#Close").asInstanceOf[javafx.scene.control.Button].onAction = { e: ActionEvent ⇒
                dialog.close()
            }
            dialog.showAndWait()
        }

        var project = Project(classFiles, libraryClassFiles)
        print(
            project.statistics.map(kv ⇒ "- "+kv._1+": "+kv._2).toList.sorted.
                mkString("[info] Project statistics:\n[info]\t", "\n[info]\t", "\n")
        )
        project
    }
}