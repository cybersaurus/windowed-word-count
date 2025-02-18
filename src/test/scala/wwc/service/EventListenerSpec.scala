package wwc.service

import cats.effect.IO
import cats.effect.Ref
import wwc.model.Event
import wwc.store.InMemoryExpiringEventStore

import java.io.ByteArrayInputStream
import java.io.InputStream
import scala.concurrent.duration.*

object EventListenerSpec extends weaver.SimpleIOSuite with wwc.time.TimeFixtures {

  test("EventListener consumes valid json events") {

    val inputStream: InputStream =
      new ByteArrayInputStream(
        s"""
           |{ "event_type": "baz", "data": "first", "timestamp": ${now.minusSeconds(3).toEpochMilli} }
           |{ "event_type": "baz", "data": "second", "timestamp": ${now.minusSeconds(2).toEpochMilli} }
           |{ "event_type": "baz", "data": "third", "timestamp": ${now.minusSeconds(1).toEpochMilli} }
           |{ "event_type": "baz", "data": "fourth", "timestamp": ${now.toEpochMilli} }""".stripMargin.getBytes
      )

    val eventStore = InMemoryExpiringEventStore(expirationWindow = 1.hours, Ref.unsafe(List.empty))
    val testInstance = EventListener(inputStream, eventStore)

    val expectedEvents = List(
      Event("baz", "first", now.minusSeconds(3)),
      Event("baz", "second", now.minusSeconds(2)),
      Event("baz", "third", now.minusSeconds(1)),
      Event("baz", "fourth", now)
    )

    for {
      _ <- testInstance.consume
      _ <- IO.sleep(10.milliseconds)
      consumed <- eventStore.getAllUnexpired()
    } yield expect.same(expectedEvents, consumed)
  }

  test("EventListener filters out invalid json events") {

    val inputStream: InputStream =
      new ByteArrayInputStream(
        s"""
           |{ "event_type": "baz", "data": "first", "timestamp": ${now.minusSeconds(2).toEpochMilli} }
           |{ not valid json
           |{ "event_type": "baz", "data": "second", "timestamp": ${now.minusSeconds(1).toEpochMilli} }
           |{ "event_type": "baz", "data": "second", "other": "invalidField" }
           |{ "event_type": "baz", "data": "third", "timestamp": ${now.toEpochMilli} }""".stripMargin.getBytes
      )

    val eventStore = InMemoryExpiringEventStore(expirationWindow = 1.hours, Ref.unsafe(List.empty))
    val testInstance = EventListener(inputStream, eventStore)

    val expectedEvents = List(
      Event("baz", "first", now.minusSeconds(2)),
      Event("baz", "second", now.minusSeconds(1)),
      Event("baz", "third", now)
    )

    for {
      _ <- testInstance.consume
      _ <- IO.sleep(10.milliseconds)
      consumed <- eventStore.getAllUnexpired()
    } yield expect.same(expectedEvents, consumed)
  }
}
