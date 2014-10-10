import AssemblyKeys._

name := "BugPicker"

version := "ALWAYS-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - BugPicker")

libraryDependencies += "org.scalafx"  %% "scalafx"   % "1.0.0-R8"

// We want to use different VM settings for OPAL
fork in run := true

//javaOptions in run := Seq("-Xmx2G", "-Xms1024m", "-XX:+AggressiveOpts", "-Xnoclassgc", "-XX:InlineSmallCode=1500", "-XX:MaxInlineSize=52")
//19.9secs... javaOptions in run := Seq("-Xmx2G", "-Xms1014m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc", "-XX:InlineSmallCode=2048", "-XX:MaxInlineSize=64")
javaOptions in run := Seq("-Xmx2G", "-Xms1024m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc")

scalaVersion := "2.11.1"

assemblySettings

jarName in assembly := "bugpicker.jar"

test in assembly := {}

mainClass in assembly := Some("org.opalj.bugpicker.BugPicker")
