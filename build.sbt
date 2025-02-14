name := "windowed-word-count"

version := "0.0.1"

scalaVersion := "3.6.3"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "co.fs2" %% "fs2-core" % "3.11.0",
  "co.fs2" %% "fs2-io" % "3.11.0",
  "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test
)

testFrameworks += new TestFramework("weaver.framework.CatsEffect")

Compile / run / fork := true
