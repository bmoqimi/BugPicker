package org.opalj
package bugpicker
package dialogs

import scalafx.Includes._
import scalafx.stage.Stage
import org.opalj.bugpicker.analysis.AnalysisParameters
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.scene.layout.VBox
import scalafx.scene.layout.HBox
import scalafx.scene.control.Button
import scalafx.geometry.Insets
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos
import scalafx.scene.layout.GridPane
import scalafx.scene.control.Label
import scalafx.scene.control.TextField
import scalafx.event.ActionEvent
import org.opalj.bugpicker.analysis.DeadCodeAnalysis
import scalafx.scene.control.TitledPane

class AnalysisParametersDialog(owner: Stage) extends DialogStage(owner) {
    theStage ⇒
    title = "Set analysis parameters"

    var parameters: Option[AnalysisParameters] = None
    val buttonMinWidth = 80
    val buttonMargin = Insets(10)

    width = 640

    val maxEvalFactorField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }
    val maxEvalTimeField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }
    val maxCardinalityOfIntegerRangesField = new TextField {
        hgrow = Priority.ALWAYS
        alignment = Pos.BASELINE_RIGHT
    }

    scene = new Scene {
        root = new BorderPane {
            center = new GridPane {
                add(new Label("Maximum evaluation factor:"), 0, 0)
                add(maxEvalFactorField, 1, 0)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalFactorField.text = DeadCodeAnalysis.defaultMaxEvalFactor.toString
                    }
                }, 2, 0)

                add(new Label("Maximum evaluation time:"), 0, 1)
                add(maxEvalTimeField, 1, 1)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxEvalTimeField.text = DeadCodeAnalysis.defaultMaxEvalTime.toString
                    }
                }, 2, 1)

                add(new Label("Maximum cardinality of integer ranges:"), 0, 2)
                add(maxCardinalityOfIntegerRangesField, 1, 2)
                add(new Button {
                    text = "Default"
                    onAction = { e: ActionEvent ⇒
                        maxCardinalityOfIntegerRangesField.text = DeadCodeAnalysis.defaultMaxCardinalityOfIntegerRanges.toString
                    }
                }, 2, 2)

                children foreach (c ⇒ GridPane.setMargin(c, Insets(10)))

                style = "-fx-border-width: 0 0 1 0; -fx-border-color: #ccc;"
            }
            bottom = new HBox {
                content = Seq(
                    new Button {
                        text = "_Defaults"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            maxEvalFactorField.text = DeadCodeAnalysis.defaultMaxEvalFactor.toString
                            maxEvalTimeField.text = DeadCodeAnalysis.defaultMaxEvalTime.toString
                            maxCardinalityOfIntegerRangesField.text = DeadCodeAnalysis.defaultMaxCardinalityOfIntegerRanges.toString
                        }
                    },
                    new Button {
                        text = "_Cancel"
                        mnemonicParsing = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒ close() }
                    },
                    new Button {
                        text = "_Ok"
                        mnemonicParsing = true
                        defaultButton = true
                        minWidth = buttonMinWidth
                        margin = buttonMargin
                        onAction = { e: ActionEvent ⇒
                            var interrupt = false
                            val maxEvalFactor = try {
                                maxEvalFactorField.text().toDouble
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum evaluation factor!",
                                        theStage)
                                    interrupt = true
                                    Double.NaN
                                }
                            }
                            val maxEvalTime = try {
                                maxEvalTimeField.text().toInt
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum evaluation time!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }
                            val maxCardinalityOfIntegerRanges = try {
                                maxCardinalityOfIntegerRangesField.text().toInt
                            } catch {
                                case _ ⇒ {
                                    DialogStage.showMessage("Error",
                                        "You entered an illegal value for maximum cardinality of integer ranges!",
                                        theStage)
                                    interrupt = true
                                    Int.MinValue
                                }
                            }

                            if (!interrupt) {
                                parameters = Some(new AnalysisParameters(
                                    maxEvalTime = maxEvalTime,
                                    maxEvalFactor = maxEvalFactor,
                                    maxCardinalityOfIntegerRanges = maxCardinalityOfIntegerRanges))
                                close()
                            }
                        }
                    }
                )
                alignment = Pos.CENTER
            }
        }
    }

    def show(parameters: AnalysisParameters): Option[AnalysisParameters] = {
        maxEvalFactorField.text = parameters.maxEvalFactor.toString
        maxEvalTimeField.text = parameters.maxEvalTime.toString
        maxCardinalityOfIntegerRangesField.text = parameters.maxCardinalityOfIntegerRanges.toString
        showAndWait()
        this.parameters
    }
}