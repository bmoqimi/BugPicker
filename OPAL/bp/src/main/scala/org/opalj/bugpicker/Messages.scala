package org.opalj
package bugpicker

import scala.io.Source

object Messages {
    def getMessage(path: String): String = process(getClass.getResourceAsStream(path))(Source.fromInputStream(_).mkString)

    final val ANALYSIS_RUNNING = getMessage("/org/opalj/bugpicker/messages/analysisrunning.html")
    final val ANALYSIS_FINISHED = getMessage("/org/opalj/bugpicker/messages/analysisfinished.html")
    final val LOADING_FINISHED = getMessage("/org/opalj/bugpicker/messages/projectloadingfinished.html")
    final val APP_STARTED = getMessage("/org/opalj/bugpicker/messages/appstarted.html")
    final val LOADING_STARTED = getMessage("/org/opalj/bugpicker/messages/projectloadingstarted.html")
    final val ANALYSES_CANCELLING = getMessage("/org/opalj/bugpicker/messages/analysescancelling.html")
    final val LOAD_CLASSES_FIRST = getMessage("/org/opalj/bugpicker/messages/loadclassesfirst.html")
    final val NO_BYTECODE_FOUND = getMessage("/org/opalj/bugpicker/messages/nobytecodefound.html")
    final val GET_HELP = getMessage("/org/opalj/bugpicker/messages/gethelp.html")

    val helpTopics: Seq[HelpTopic] = Seq(
        new HelpTopic(GET_HELP),
        new HelpTopic(APP_STARTED),
        new HelpTopic(LOADING_FINISHED),
        new HelpTopic(ANALYSIS_FINISHED)
    )
}

class HelpTopic(val content: String) {
    val title = "<h2>(.*)</h2>".r.findFirstMatchIn(content).map(_.group(1)).getOrElse("Untitled")

    override def toString: String = title
}