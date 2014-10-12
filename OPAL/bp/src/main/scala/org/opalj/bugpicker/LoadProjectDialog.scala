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
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyEvent
import javafx.event.EventHandler
import scala.collection.mutable.ListBuffer
import java.io.File

class LoadProjectDialog(
        preloadJars: Seq[File] = Seq.empty,
        preloadLibs: Seq[File] = Seq.empty,
        preloadSources: Seq[File] = Seq.empty) extends Stage {
    private final val buttonWidth = 200
    private final val buttonMargin = Insets(5)

    private final val boxMargin = Insets(10)
    private final val boxPadding = Insets(10)

    val jars = ListBuffer[File]() ++ preloadJars
    val sources = ListBuffer[File]() ++ preloadSources
    val libs = ListBuffer[File]() ++ preloadLibs

    var cancelled = false
    val jarListview = new ListView[String] {
        items() ++= jars.map(_.toString)
        hgrow = Priority.ALWAYS
    }
    val libsListview = new ListView[String] {
        items() ++= libs.map(_.toString)
        hgrow = Priority.ALWAYS
    }
    val sourceListview = new ListView[String] {
        items() ++= sources.map(_.toString)
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
                                                jars += file
                                                jarListview.items.get.add(file.toString())
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
                                                jars += file
                                                jarListview.items() += file.toString()
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
                                            val selection = jarListview.selectionModel().getSelectedIndices
                                            if (!selection.isEmpty()) {
                                                selection.reverse.foreach { i ⇒
                                                    if (jars.isDefinedAt(i)) jars.remove(i)
                                                }
                                                jarListview.items().clear()
                                                jarListview.items() ++= jars.map(_.toString)
                                            }
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
                                                libs += file
                                                libsListview.items() += file.toString()
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
                                            val selection = libsListview.selectionModel().getSelectedIndices
                                            if (!selection.isEmpty()) {
                                                selection.reverse.foreach { i ⇒
                                                    if (libs.isDefinedAt(i)) libs.remove(i)
                                                }
                                                libsListview.items().clear()
                                                libsListview.items() ++= libs.map(_.toString())
                                            }
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
                                                sources += file
                                                sourceListview.items() += file.toString()
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
                                            val selection = sourceListview.selectionModel().getSelectedIndices
                                            if (!selection.isEmpty()) {
                                                selection.reverse.foreach { i ⇒
                                                    if (sources.isDefinedAt(i)) sources.remove(i)
                                                }
                                                sourceListview.items().clear()
                                                sourceListview.items() ++= sources.map(_.toString)
                                            }
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
                                self.close()
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
    scene().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
        override def handle(e: KeyEvent) {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                cancelled = true
                close()
            } else if (e.getCode().equals(KeyCode.ENTER)) {
                close()
            }
        }
    })

    def show(owner: Stage): Option[(List[File], List[File], List[File])] = {
        initModality(Modality.WINDOW_MODAL)
        initOwner(owner.scene().windowProperty().get)
        initStyle(StageStyle.UTILITY)
        centerOnScreen
        showAndWait
        if (cancelled) {
            None
        } else {
            Some((jars.toList, libs.toList, sources.toList))
        }
    }
}