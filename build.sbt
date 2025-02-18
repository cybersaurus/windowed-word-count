import Versions.*

name := "windowed-word-count"

version := "0.0.1"

scalaVersion := "3.6.3"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsCore,
  "org.typelevel" %% "cats-effect" % catsEffect,
  "co.fs2" %% "fs2-core" % fs2,
  "co.fs2" %% "fs2-io" % fs2,
  "io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-parser" % circe,
  "org.http4s" %% "http4s-circe" % http4s,
  "org.http4s" %% "http4s-dsl" % http4s,
  "org.http4s" %% "http4s-ember-server" % http4s,
  "com.disneystreaming" %% "weaver-cats" % weaver % Test
)

testFrameworks += new TestFramework("weaver.framework.CatsEffect")

Compile / run / fork := true

assemblyJarName := "event-listener.jar"
