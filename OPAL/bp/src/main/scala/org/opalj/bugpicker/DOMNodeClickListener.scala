package org.opalj
package bugpicker

import java.io.File
import java.io.FilenameFilter
import java.net.URL
import scala.language.reflectiveCalls
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener
import scalafx.Includes._
import scalafx.beans.property.ObjectProperty
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.geometry.Pos.sfxEnum2jfx
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.control.Label
import scalafx.scene.layout.BorderPane
import scalafx.scene.web.WebView
import scalafx.stage.Modality
import scalafx.stage.Stage
import scalafx.stage.StageStyle
import scala.collection.immutable.Stream

class DOMNodeClickListener(
        project: Project[URL],
        sources: Seq[File],
        node: Node,
        bytecodeWebview: WebView,
        sourceWebview: WebView,
        focus: WebView ⇒ Unit) extends EventListener {

    private val nodeAttributes = node.getAttributes

    private def getAttribute(name: String): Option[String] =
        if (nodeAttributes.getNamedItem(name) != null)
            Some(nodeAttributes.getNamedItem(name).getTextContent)
        else
            None

    def findSourceFile(
        theType: ObjectType,
        lineOption: Option[String]): Option[SourceFileWrapper] = {

        val classFile = project.classFile(theType)
        if (!classFile.isDefined) return None

        val cf = classFile.get

        val sourceFileName = cf.sourceFile.getOrElse(theType.simpleName)
        val sourcePackagePath = theType.packageName

        val sourceFile: Option[File] =
            if (cf.sourceFile.isDefined) {
                sources.toStream.map(dir ⇒ new File(dir, sourcePackagePath+"/"+cf.sourceFile.get)).find(_.exists())
            } else {
                val name = theType.simpleName
                val packageDir = sources.toStream.map(dir ⇒ new File(dir, sourcePackagePath)).find(_.exists())
                packageDir.map(_.listFiles(new FilenameFilter {
                    override def accept(file: File, filename: String): Boolean =
                        filename.matches("^"+name+"\\.\\w+$")
                })(0))
            }

        if (sourceFile.isDefined && sourceFile.get.exists) {
            val wrapper = new SourceFileWrapper(sourceFile.get, lineOption.getOrElse(""))
            Some(wrapper)
        } else {
            None
        }
    }

    def decompileClassFile(project: Project[URL], theType: ObjectType): Option[ClassFile] = {
        project.source(theType).map { url ⇒
            val inStream = url.openStream
            val cf = org.opalj.da.ClassFileReader.ClassFile(() ⇒ inStream)
            inStream.close
            cf.head
        }
    }

    override def handleEvent(event: org.w3c.dom.events.Event) {
        val className = getAttribute("data-class").get
        val sourceType = ObjectType(className)
        val methodOption = getAttribute("data-method")
        val pcOption = getAttribute("data-pc")
        val lineOption = getAttribute("data-line")
        val (loadBytecode, loadSource) = getAttribute("data-load") match {
            case Some("bytecode")   ⇒ (true, false)
            case Some("sourcecode") ⇒ (false, true)
            case _                  ⇒ (true, sources.nonEmpty)
        }

        var noSourceFound = false
        if (loadSource) {
            val sourceFile = findSourceFile(sourceType, lineOption)
            if (sourceFile.isDefined) {
                sourceWebview.engine.loadContent(sourceFile.get.toXHTML.toString)
                new JumpToProblemListener(webview = sourceWebview, methodOption = methodOption, pcOption = None, lineOption = lineOption)
                if (!loadBytecode) focus(sourceWebview)
            } else {
                noSourceFound = true
                val msg = s"Could not find source code for type $className.\nShowing bytecode instead."
                DialogStage.showMessage("Info", msg, sourceWebview.scene().window())
                sourceWebview.engine.loadContent("")
            }
        }
        if (loadBytecode || noSourceFound) {
            val classFile = decompileClassFile(project, sourceType)
            if (classFile.isDefined)
                bytecodeWebview.engine.loadContent(classFile.get.toXHTML.toString)
            else
                bytecodeWebview.engine.loadContent(Messages.NO_BYTECODE_FOUND)
            new JumpToProblemListener(webview = bytecodeWebview, methodOption = methodOption, pcOption = pcOption, lineOption = None)
            if (!loadSource || noSourceFound) focus(bytecodeWebview)
        }
    }
}