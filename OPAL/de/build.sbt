name := "Dependencies Extraction Library"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Dependencies Extraction Library") 