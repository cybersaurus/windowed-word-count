import Dependencies.*

ThisBuild / name := "windowed-word-count"

ThisBuild / version := "0.0.1"

ThisBuild / scalaVersion := "3.6.3"

lazy val root = project
  .in(file("."))
  .settings(
    Compile / run / fork := true,
    libraryDependencies ++= catsCore ++ catsEffect ++ fs2 ++ circe ++ http4s ++ weaver.map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    assembly / assemblyJarName := "event-listener.jar",
    assembly / mainClass := Some("wwc.app.WindowedWordCountApp")
  )

lazy val integration = project
  .in(file("integration"))
  .dependsOn(root % "test->compile")
  .settings(
    Test / fork := true,
    libraryDependencies ++= catsCore ++ catsEffect ++ fs2 ++ circe ++ http4s ++
      (http4sClient ++ weaver).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
