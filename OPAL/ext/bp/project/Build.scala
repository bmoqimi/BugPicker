import sbt._
import Keys._

import sbtassembly.Plugin.AssemblyKeys._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object BugPickerBuild extends Build {
	lazy val buildSettings = Defaults.defaultSettings ++
		SbtScalariform.scalariformSettingsWithIt ++
		Seq(ScalariformKeys.preferences <<= baseDirectory.apply(getScalariformPreferences)) ++
		Seq(Defaults.itSettings : _*) ++
		Seq(EclipseKeys.configurations := Set(Compile, Test, IntegrationTest)) ++
		Seq(libraryDependencies ++= Seq(
			"de.opal-project" % "abstract-interpretation-framework_2.11" % "0.0.1-SNAPSHOT",
			"de.opal-project" % "bytecode-disassembler_2.11" % "0.1.0-SNAPSHOT"
		))
	
	def getScalariformPreferences(dir: File) = PreferencesImporterExporter.loadPreferences(
		(file("Scalariform Formatter Preferences.properties").getPath))

	lazy val bugpicker = Project(
		id = "BugPicker",
		base = file("."),
		settings = buildSettings ++ Seq(publishArtifact := false)
	)
}
