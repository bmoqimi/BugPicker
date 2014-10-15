import AssemblyKeys._

name := "BugPicker"

version := "ALWAYS-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - BugPicker")

libraryDependencies += "org.scalafx"  %% "scalafx"   % "1.0.0-R8"

javaOptions ++= Seq("-source", "1.7", "-target", "1.7")

jfxSettings

unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/jfxrt.jar"))

JFX.mainClass := Some("org.opalj.bugpicker.BugPicker")

// We want to use different VM settings for OPAL
fork in run := true

javaOptions in run := Seq("-Xmx2G", "-Xms1024m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc")

assemblySettings

jarName in assembly := "bugpicker.jar"

test in assembly := {}

mainClass in assembly := Some("org.opalj.bugpicker.BugPicker")
