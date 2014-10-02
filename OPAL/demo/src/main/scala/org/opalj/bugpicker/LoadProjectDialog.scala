package org.opalj
package bugpicker

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.stage.Modality
import scalafx.scene.Scene
import scalafx.scene.control.ListView
import scalafx.stage.StageStyle
import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafx.event.ActionEvent
import scalafx.stage.FileChooser
import scalafx.stage.DirectoryChooser
import scalafx.geometry.Insets
import scalafx.scene.layout.Priority
import scalafx.geometry.Pos
import scalafx.scene.control.Tooltip
import scalafx.scene.control.TitledPane

class LoadProjectDialog extends Stage {
    private final val buttonWidth = 200
    private final val buttonMargin = Insets(5)

    private final val boxMargin = Insets(10)
    private final val boxPadding = Insets(10)

    var jars: List[java.io.File] = List()
    var sources: java.io.File = null
    var libs: java.io.File = null
    var cancelled = false
    val jarListview = new ListView[String] {
        hgrow = Priority.ALWAYS
    }
    val libsListview = new ListView[String] {
        hgrow = Priority.ALWAYS
    }
    val sourceListview = new ListView[String] {
        hgrow = Priority.ALWAYS
    }

    val self = this

    title = "Load project files"
    width = 800
    height = 600
    maxWidth = 800
    maxHeight = 600
    scene = new Scene {
        root = new VBox {
            content = Seq(
                new TitledPane {
                    text = "Select files(jars/.class/directory) to be analysed"
                    collapsible = false
                    padding = boxPadding
                    margin = boxMargin

                    content = new HBox {
                        content = Seq(
                            jarListview,
                            new VBox {
                                content = Seq(
                                    new Button {
                                        text = "Add _Jar/Class Files"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            val fcb = new FileChooser {
                                                title = "Open Dialog"
                                            }
                                            fcb.extensionFilters.addAll(
                                                new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                                new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                            val file = fcb.showOpenDialog(scene().getWindow())
                                            if (file != null) {
                                                jars :::= List(file)
                                                jarListview.items.get.add(jars(0).toString())
                                            }

                                        }
                                    },
                                    new Button {
                                        text = "Add _Directory"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            val dc = new DirectoryChooser {
                                                title = "Select Directory"
                                            }
                                            val file = dc.showDialog(scene().window())
                                            if (file != null) {

                                                jars :::= List(file)
                                                jarListview.items() += jars(0).toString()
                                            }
                                        }
                                    },
                                    new Button {
                                        text = "_Remove"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            val removed = jarListview.selectionModel().getSelectedItem()
                                            val temp = jars
                                            jars = List[java.io.File]()
                                            temp.foreach {
                                                file ⇒
                                                    {
                                                        if (file.toString != removed) {
                                                            jars :::= List(file)
                                                        }
                                                    }
                                            }
                                            jarListview.items() -= removed
                                        }
                                    })
                            })
                    }
                },
                new TitledPane {
                    text = "Choose the  class directory containing jar of class files"
                    collapsible = false
                    padding = boxPadding
                    margin = boxMargin

                    content = new HBox {
                        content = Seq(
                            libsListview,
                            new VBox {
                                content = Seq(
                                    new Button {
                                        text = "Add _Library Jar"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            val fcb = new FileChooser {
                                                title = "Open Dialog"
                                            }
                                            fcb.extensionFilters.addAll(
                                                new FileChooser.ExtensionFilter("Jar Files", "*.jar"),
                                                new FileChooser.ExtensionFilter("Class Files", "*.class"))
                                            val file = fcb.showOpenDialog(scene().getWindow())
                                            if (file != null) {
                                                libs = file
                                                libsListview.items() += libs.toString()
                                            }

                                        }
                                    },
                                    new Button {
                                        text = "R_emove"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            libsListview.items() -= libs.toString()
                                            libs = null
                                        }
                                    })
                            })
                    }
                },
                new TitledPane {
                    text = "select the source directory of your project"
                    collapsible = false
                    padding = boxPadding
                    margin = boxMargin

                    content = new HBox {
                        content = Seq(
                            sourceListview,
                            new VBox {
                                content = Seq(
                                    new Button {
                                        text = "Add _source Directory"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            val dc = new DirectoryChooser {
                                                title = "Open Dialog"
                                            }
                                            val file = dc.showDialog(scene().window())
                                            if (file != null) {
                                                sources = file
                                                sourceListview.items() += sources.toString()
                                            }
                                        }
                                    },
                                    new Button {
                                        text = "Re_move"
                                        mnemonicParsing = true
                                        maxWidth = buttonWidth
                                        minWidth = buttonWidth
                                        margin = buttonMargin
                                        onAction = { e: ActionEvent ⇒
                                            sourceListview.items() -= sources.toString()
                                            sources = null
                                        }
                                    })
                            })
                    }
                },
                new HBox {
                    alignment = Pos.CENTER
                    padding = boxPadding
                    margin = boxMargin

                    content = Seq(
                        new Button {
                            text = "_Cancel"
                            mnemonicParsing = true
                            margin = buttonMargin
                            minWidth = 80
                            onAction = { e: ActionEvent ⇒
                                cancelled = true
                                self.close
                            }
                        },
                        new Button {
                            text = "_Finish"
                            mnemonicParsing = true
                            margin = buttonMargin
                            defaultButton = true
                            minWidth = 80
                            onAction = { e: ActionEvent ⇒ self.close() }
                        }
                    )

                }
            )

        }
    }

    def show(owner: Stage): List[List[java.io.File]] = {
        initModality(Modality.WINDOW_MODAL)
        initOwner(owner.scene().windowProperty().get)
        initStyle(StageStyle.UTILITY)
        centerOnScreen
        showAndWait
        if (cancelled) {
            null
        } else {
            val results = List(jars, List(sources), List(libs))
            results
        }
    }
}