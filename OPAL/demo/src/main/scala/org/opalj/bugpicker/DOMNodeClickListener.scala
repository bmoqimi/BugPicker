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

import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxActionEvent2sfx
import scalafx.Includes.jfxNode2sfx
import scalafx.Includes.jfxObjectBinding2sfx
import scalafx.Includes.jfxSceneProperty2sfx
import scalafx.Includes.jfxWindow2sfx
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

class DOMNodeClickListener(
        project: Project[URL],
        sourceDir: File,
        node: Node,
        bytecodeWebview: WebView,
        sourceWebview: WebView,
        focus: WebView ⇒ Unit) extends EventListener {

    private final val MESSAGE_NO_BYTECODE_FOUND =
        <html><h1>No source- or bytecode for this class could be found!</h1></html>

    private val nodeAttributes = node.getAttributes

    private def getAttribute(name: String): Option[String] =
        if (nodeAttributes.getNamedItem(name) != null)
            Some(nodeAttributes.getNamedItem(name).getTextContent)
        else
            None

    def findSourceFile(
        sourceDir: java.io.File,
        project: Project[URL],
        theType: ObjectType,
        lineOption: Option[String]): Option[SourceFileWrapper] = {

        val classFile = project.classFile(theType)
        if (!classFile.isDefined) return None

        val cf = classFile.get

        val sourceFileName = cf.sourceFile.getOrElse(theType.simpleName)
        val sourcePackagePath = theType.packageName

        val sourceFile: Option[File] =
            if (cf.sourceFile.isDefined) {
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
            case _                  ⇒ (true, true)
        }

        var noSourceFound = false
        if (loadSource) {
            val sourceFile = findSourceFile(sourceDir, project, sourceType, lineOption)
            if (sourceFile.isDefined) {
                sourceWebview.engine.loadContent(sourceFile.get.toXHTML.toString)
                new JumpToProblemListener(webview = sourceWebview, methodOption = methodOption, pcOption = None, lineOption = lineOption)
                if (!loadBytecode) focus(sourceWebview)
            } else {
                noSourceFound = true
                showWarning(s"Could not find source code for type $className.\nShowing bytecode instead.")
                sourceWebview.engine.loadContent("")
            }
        }
        if (loadBytecode || noSourceFound) {
            val classFile = decompileClassFile(project, sourceType)
            bytecodeWebview.engine.loadContent(classFile.map(_.toXHTML).getOrElse(MESSAGE_NO_BYTECODE_FOUND).toString)
            new JumpToProblemListener(webview = bytecodeWebview, methodOption = methodOption, pcOption = pcOption, lineOption = None)
            if (!loadSource || noSourceFound) focus(bytecodeWebview)
        }
    }

    def showWarning(msg: String) {
        val dialog: Stage = new Stage {
            title = "Warning"

            scene = new Scene {
                root = new BorderPane {
                    center = new Label {
                        text = msg
                        wrapText = true
                        maxWidth = 600
                        margin = Insets(5, 5, 5, 5)
                    }
                    bottom = new Button {
                        text = "Ok"
                        override val labelPadding = ObjectProperty(Insets(2, 5, 2, 5))
                        minWidth = 80
                        margin = Insets(5, 5, 5, 5)
                        onAction = { e: ActionEvent ⇒
                            close
                        }
                    }
                    BorderPane.setAlignment(bottom.value, Pos.BOTTOM_CENTER)
                }
            }
        }
        dialog.initStyle(StageStyle.UTILITY)
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(sourceWebview.scene.window.value)
        dialog.centerOnScreen
        dialog.showAndWait
    }
}