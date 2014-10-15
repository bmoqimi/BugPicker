package org.opalj
package bugpicker
package codeview

import java.io.File
import scala.io.Source
import scala.xml.Unparsed

case class SourceFileWrapper(sourceFile: File, highlightLines: String) {
    private val source = Source.fromFile(sourceFile).mkString

    private val language: String = {
        val prefix = "language-"
        val name = sourceFile.getName
        val suffix = name.substring(name.lastIndexOf('.') + 1)
        prefix + suffix
    }

    def toXHTML: scala.xml.Node =
        <html>
            <head>
                <title>{ sourceFile.getName }</title>
                <style type="text/css">{ Unparsed(SourceFileWrapper.PRISM_CSS) }</style>
            </head>
            <body>
                <pre class="line-numbers" data-line={ highlightLines }><code class={ language }>{ source }</code></pre>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.PRISM_JS) }</script>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.ADD_LINE_ANCHORS) }</script>
            </body>
        </html>
}

object SourceFileWrapper {
    final val PRISM_JS_URL: String = "/org/opalj/bugpicker/codeview/prism.js"
    final lazy val PRISM_JS: String = Source.fromURL(getClass.getResource(PRISM_JS_URL)).mkString
    final val PRISM_CSS_URL: String = "/org/opalj/bugpicker/codeview/prism.css"
    final lazy val PRISM_CSS: String = Source.fromURL(getClass.getResource(PRISM_CSS_URL)).mkString
    final val ADD_LINE_ANCHORS_URL: String = "/org/opalj/bugpicker/codeview/add-line-anchors.js"
    final lazy val ADD_LINE_ANCHORS: String = Source.fromURL(getClass.getResource(ADD_LINE_ANCHORS_URL)).mkString
}