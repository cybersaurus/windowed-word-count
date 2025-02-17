//> using scala "3.3.3"
//> using jvm 21

//> using dep "org.typelevel::cats-effect::3.5.7"
//> using dep "co.fs2::fs2-io::3.11.0"
//> using dep "io.circe::circe-parser::0.14.10"

//> using file src/main/scala/wwc/app/WindowedWordCountApp.scala
//> using file src/main/scala/wwc/model/Event.scala
//> using file src/main/scala/wwc/service/EventListener.scala
//> using file src/main/scala/wwc/store/ExpiringEventStore.scala
//> using file src/main/scala/wwc/store/InMemoryExpiringEventStore.scala

object EventListenerAppWrapper extends wwc.app.WindowedWordCountApp
