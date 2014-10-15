package org.opalj
package bugpicker
package analysis

case class AnalysisParameters(
        maxEvalTime: Int = DeadCodeAnalysis.defaultMaxEvalTime,
        maxEvalFactor: Double = DeadCodeAnalysis.defaultMaxEvalFactor,
        maxCardinalityOfIntegerRanges: Int = DeadCodeAnalysis.defaultMaxCardinalityOfIntegerRanges) {

    def toStringParameters: Seq[String] = Seq(
        s"-maxEvalFactor=$maxEvalFactor",
        s"maxEvalTime=$maxEvalTime",
        s"-maxCardinalityOfIntegerRanges=$maxCardinalityOfIntegerRanges")
}