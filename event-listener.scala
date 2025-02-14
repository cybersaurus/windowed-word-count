#!/usr/bin/env scala-cli run

//> using scala "3.3.3"
//> using dep "org.typelevel::cats-effect::3.5.7"
//> using dep "co.fs2::fs2-io::3.11.0"

//> using file src/main/scala/wwc/EventListenerApp.scala

//import cats.effect.IO

object EventListenerAppWrapper extends wwc.EventListenerApp
