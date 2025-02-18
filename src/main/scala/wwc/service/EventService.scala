package wwc.service

import cats.effect.IO
import io.circe.Encoder
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.io.*
import org.http4s.HttpRoutes
import wwc.model.Event

import scala.util.chaining.*

object EventService {
  case class WordCountsByEventType(wordCountsByEventType: Map[String, Set[WordCount]])
  object WordCountsByEventType {
    given Encoder[WordCountsByEventType] = Encoder.forProduct1("wordCountsByEventType")(_.wordCountsByEventType)
  }

  case class WordCount(word: String, count: Int)
  object WordCount {
    given Encoder[WordCount] = Encoder.forProduct2("word", "count")(wc => wc.word -> wc.count)
  }

  def routes(eventStore: wwc.store.ExpiringEventStore): HttpRoutes[IO] =
    HttpRoutes
      .of[IO] { case GET -> Root / "wordcount" =>
        eventStore
          .getAllUnexpired()
          .map(wordCountsByEventType)
          .flatMap(wordCounts => Ok(wordCounts))
      }

  private[service] def wordCountsByEventType(allEvents: List[Event]): WordCountsByEventType =
    allEvents
      .groupBy(_.eventType)
      .map { (eventType, eventTypeEvents) =>
        eventType -> wordCounts(eventTypeEvents)
      }
      .pipe(WordCountsByEventType.apply)

  private def wordCounts(events: List[Event]): Set[WordCount] =
    events
      .flatMap(_.words)
      .groupBy(identity)
      .map { (word, occurrences) => WordCount(word, occurrences.length) }
      .toSet
}
