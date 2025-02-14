#!/usr/bin/env scala-cli run

//> using scala "3.3.3"
//> using dep "org.typelevel::cats-effect::3.5.7"
//> using dep "co.fs2::fs2-io::3.11.0"
//> using dep "io.circe::circe-generic::0.14.10"
//> using dep "io.circe::circe-parser::0.14.10"

//> using file src/main/scala/wwc/app/EventListenerApp.scala
//> using file src/main/scala/wwc/model/Event.scala

object EventListenerAppWrapper extends wwc.app.EventListenerApp
