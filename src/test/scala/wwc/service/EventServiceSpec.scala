package wwc.service

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.implicits.*
import org.http4s.Request
import org.http4s.Response
import wwc.model.Event
import wwc.model.EventType
import wwc.service.EventService.WordCount
import wwc.store.ExpiringEventStore

import scala.concurrent.duration.*

object EventServiceSpec extends weaver.SimpleIOSuite with wwc.time.TimeFixtures with wwc.json.DecoderFixtures {

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
      Event(EventType("eventType1"), "one two three", now),
      Event(EventType("eventType2"), "four four two", now),
      Event(EventType("eventType1"), "two three", now.plusSeconds(1)),
      Event(EventType("eventType2"), "four", now.plusSeconds(1)),
      Event(EventType("eventType1"), "three", now.plusSeconds(2)),
      Event(EventType("eventType2"), "four two", now.plusSeconds(2)),
      Event(EventType("eventTypeWithNoWords"), "", now.plusSeconds(3))
    )
    val expectedWordCounts = EventService.WordCountsByEventType(
      Map(
        EventType("eventType1") -> Set(
          WordCount("one", 1),
          WordCount("two", 2),
          WordCount("three", 3)
        ),
        EventType("eventType2") -> Set(
          WordCount("two", 2),
          WordCount("four", 4)
        ),
        EventType("eventTypeWithNoWords") -> Set.empty
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
      Event(EventType("eventType1"), "one two three", now),
      Event(EventType("eventType2"), "four four two", now),
      Event(EventType("eventType1"), "two three", now.plusSeconds(1)),
      Event(EventType("eventType2"), "four", now.plusSeconds(1)),
      Event(EventType("eventType1"), "three", now.plusSeconds(2)),
      Event(EventType("eventType2"), "four two", now.plusSeconds(2)),
      Event(EventType("eventTypeWithNoWords"), "", now.plusSeconds(3))
    )

    val expectedWordCounts = EventService.WordCountsByEventType(
      Map(
        EventType("eventType1") -> Set(
          WordCount("one", 1),
          WordCount("two", 2),
          WordCount("three", 3)
        ),
        EventType("eventType2") -> Set(
          WordCount("two", 2),
          WordCount("four", 4)
        ),
        EventType("eventTypeWithNoWords") -> Set.empty
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
