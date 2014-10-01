package org.opalj
package bugpicker

import org.w3c.dom.events.EventListener
import org.w3c.dom.Node
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener
import org.opalj.br.ObjectType
import javafx.concurrent.Worker.State
import org.opalj.br.analyses.Project
import java.net.URL
import scalafx.scene.web.WebView
import scala.language.reflectiveCalls
import java.io.FilenameFilter
import java.io.File

class DOMNodeClickListener(
        project: Project[URL],
        sourceDir: File,
        node: Node,
        sourceWebview: WebView) extends EventListener {

    type Renderable = {
        def toXHTML: scala.xml.Node
    }

    private val nodeAttributes = node.getAttributes

    private def getAttribute(name: String): Option[String] =
        if (nodeAttributes.getNamedItem(name) != null)
            Some(nodeAttributes.getNamedItem(name).getTextContent)
        else
            None

    def classFileOrSourceFile(
        sourceDir: java.io.File,
        project: Project[URL],
        theType: ObjectType,
        lineOption: Option[String]): Option[Renderable] = {

        val classFile = project.classFile(theType)
        if (!classFile.isDefined) return None

        val cf = classFile.get

        val sourceFileName = cf.sourceFile.getOrElse(theType.simpleName)
        val sourcePackagePath = theType.packageName

        val sourceFile: Option[File] =
            if (!lineOption.isDefined) None
            else if (cf.sourceFile.isDefined) {
                Some(new File(sourceDir, sourcePackagePath+"/"+cf.sourceFile.get))
            } else {
                val name = theType.simpleName
                val packageDir = new File(sourceDir, sourcePackagePath)
                val candidateFiles = packageDir.listFiles(new FilenameFilter {
                    override def accept(file: File, filename: String): Boolean =
                        filename.matches("^"+name+"\\.\\w+$")
                })
                if (candidateFiles.isEmpty) None else Some(candidateFiles(0))
            }

        if (sourceFile.isDefined && sourceFile.get.exists) {
            val wrapper = new SourceFileWrapper(sourceFile.get, lineOption.getOrElse(""))
            Some(wrapper)
        } else project.source(theType).map { url ⇒
            val inStream = url.openStream
            val cf = org.opalj.da.ClassFileReader.ClassFile(() ⇒ inStream)
            inStream.close
            cf.head
        }
    }

    override def handleEvent(event: org.w3c.dom.events.Event) {
        val className = getAttribute("data-class").get
        val sourceType = ObjectType(className)
        val lineOption = getAttribute("data-line")
        val sourceFile: Option[Renderable] = classFileOrSourceFile(sourceDir, project, sourceType, lineOption)
        if (sourceFile.isDefined) {
            val methodOption = getAttribute("data-method")
            val pcOption = getAttribute("data-pc")
            val sourceDoc = sourceFile.get.toXHTML
            sourceWebview.engine.loadContent(sourceDoc.toString)
            new JumpToProblemListener(sourceWebview, methodOption, pcOption, lineOption)
        }
    }
}