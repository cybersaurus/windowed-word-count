package wwc.service

import cats.data.Kleisli
import cats.effect.IO
import io.circe.Decoder
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.implicits.*
import org.http4s.Request
import org.http4s.Response
import wwc.model.Event
import wwc.service.EventService.WordCount
import wwc.store.ExpiringEventStore

import scala.concurrent.duration.*

object EventServiceSpec extends weaver.SimpleIOSuite with wwc.time.TimeFixtures {

  private given Decoder[EventService.WordCount] = Decoder.forProduct2("word", "count")(WordCount.apply)
  private given Decoder[EventService.WordCountsByEventType] =
    Decoder.forProduct1("wordCountsByEventType")(EventService.WordCountsByEventType.apply)

  private val request: Request[IO] = Request(uri = uri"/wordcount")

  test("return empty map of WordCounts when zero Events have been received") {
    val service: Kleisli[IO, Request[IO], Response[IO]] = EventService.routes(StubbedEventStore(List.empty)).orNotFound

    val expectedWordCounts = EventService.WordCountsByEventType(Map.empty)

    for {
      response <- service.run(request)
      responseBody <- response.as[EventService.WordCountsByEventType]
    } yield expect.same(expectedWordCounts, responseBody)
  }

  test("return map of WordCounts for the given Events") {
    val events = List(
      Event("eventType1", "one two three", now),
      Event("eventType2", "four four two", now),
      Event("eventType1", "two three", now.plusSeconds(1)),
      Event("eventType2", "four", now.plusSeconds(1)),
      Event("eventType1", "three", now.plusSeconds(2)),
      Event("eventType2", "four two", now.plusSeconds(2)),
      Event("eventTypeWithNoWords", "", now.plusSeconds(3))
    )
    val expectedWordCounts = EventService.WordCountsByEventType(
      Map(
        "eventType1" -> Set(
          WordCount("one", 1),
          WordCount("two", 2),
          WordCount("three", 3)
        ),
        "eventType2" -> Set(
          WordCount("two", 2),
          WordCount("four", 4)
        ),
        "eventTypeWithNoWords" -> Set.empty
      )
    )
    val service: Kleisli[IO, Request[IO], Response[IO]] = EventService.routes(StubbedEventStore(events)).orNotFound

    for {
      response <- service.run(request)
      responseBody <- response.as[EventService.WordCountsByEventType]
    } yield expect.same(expectedWordCounts, responseBody)
  }

  pureTest("wordCountsByEventType") {
    val events = List(
      Event("eventType1", "one two three", now),
      Event("eventType2", "four four two", now),
      Event("eventType1", "two three", now.plusSeconds(1)),
      Event("eventType2", "four", now.plusSeconds(1)),
      Event("eventType1", "three", now.plusSeconds(2)),
      Event("eventType2", "four two", now.plusSeconds(2)),
      Event("eventTypeWithNoWords", "", now.plusSeconds(3))
    )

    val expectedWordCounts = EventService.WordCountsByEventType(
      Map(
        "eventType1" -> Set(
          WordCount("one", 1),
          WordCount("two", 2),
          WordCount("three", 3)
        ),
        "eventType2" -> Set(
          WordCount("two", 2),
          WordCount("four", 4)
        ),
        "eventTypeWithNoWords" -> Set.empty
      )
    )

    expect.same(
      expected = expectedWordCounts,
      found = EventService.wordCountsByEventType(events)
    )
  }
}

class StubbedEventStore(events: List[Event], val expirationWindow: FiniteDuration = 15.minutes)
    extends ExpiringEventStore {
  override def getAllUnexpired(): IO[List[Event]] = IO.pure(events)

  override def append(event: Event): IO[Unit] = ???
  override def removeExpired(): IO[Unit] = ???
}
