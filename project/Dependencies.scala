import sbt.*

object Dependencies {
  lazy val catsCore = Seq(
    "org.typelevel" %% "cats-core" % Versions.catsCore
  )
  lazy val catsEffect = Seq(
    "org.typelevel" %% "cats-effect" % Versions.catsEffect
  )
  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Versions.fs2,
    "co.fs2" %% "fs2-io" % Versions.fs2
  )
  lazy val circe = Seq(
    "io.circe" %% "circe-core" % Versions.circe,
    "io.circe" %% "circe-parser" % Versions.circe
  )
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-circe" % Versions.http4s,
    "org.http4s" %% "http4s-dsl" % Versions.http4s,
    "org.http4s" %% "http4s-ember-server" % Versions.http4s
  )
  lazy val http4sClient = Seq(
    "org.http4s" %% "http4s-ember-client" % Versions.http4s
  )
  lazy val weaver = Seq(
    "com.disneystreaming" %% "weaver-cats" % Versions.weaver
  )
}
