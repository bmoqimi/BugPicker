package org.opalj
package bugpicker

import java.io.File
import scala.io.Source
import scala.xml.Unparsed
import scala.xml.NodeSeq
import scala.xml.Text

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
                <style type="text/css">{ Unparsed(SourceFileWrapper.PRISM_JS_CSS) }</style>
            </head>
            <body>
                <pre class="line-numbers" data-line={ highlightLines }><code class={ language }>{ source }</code></pre>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.PRISM_JS) }</script>
                <script type="text/javascript">{ Unparsed(SourceFileWrapper.ADD_LINE_ANCHORS) }</script>
            </body>
        </html>
}

object SourceFileWrapper {
    final lazy val PRISM_JS: String = Source.fromURL(getClass.getResource("prism.js")).mkString
    final lazy val PRISM_JS_CSS: String = Source.fromURL(getClass.getResource("prism.css")).mkString
    final lazy val ADD_LINE_ANCHORS: String = Source.fromURL(getClass.getResource("add-line-anchors.js")).mkString
}