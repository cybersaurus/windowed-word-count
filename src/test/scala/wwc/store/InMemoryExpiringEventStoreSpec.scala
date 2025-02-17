package wwc.store

import cats.effect.IO
import cats.effect.Ref
import cats.instances.list.*
import cats.syntax.traverse.*
import wwc.model.Event

import java.time.temporal.ChronoUnit
import java.time.Instant
import scala.concurrent.duration.*

object InMemoryExpiringEventStoreSpec extends weaver.SimpleIOSuite {
  private val initialInstant: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(2, ChronoUnit.HOURS)

  private val initialEvents: List[Event] = List.tabulate(5) { n =>
    Event("event_type", s"data $n", initialInstant.plus(n, ChronoUnit.HOURS))
  }

  test("append() adds all Events to the EventStore") {
    val eventsRef: Ref[IO, List[Event]] = Ref.unsafe(List.empty)

    val testInstance = InMemoryExpiringEventStore(15.minutes, eventsRef)

    for {
      _ <- initialEvents.traverse(testInstance.append)
      appendedEvents <- eventsRef.get
    } yield expect.same(initialEvents, appendedEvents)
  }

  test("getAllUnexpired() returns initialEvents without expired Events") {
    val testInstance = InMemoryExpiringEventStore(15.minutes, Ref.unsafe(initialEvents))
    val expectedUnexpiredEvents: List[Event] = initialEvents.takeRight(3)

    for {
      _ <- testInstance.removeExpired()
      unexpiredEvents <- testInstance.getAllUnexpired()
    } yield expect.same(expectedUnexpiredEvents, unexpiredEvents)
  }

  test("removeExpired() removes expired Events") {

    val eventsRef: Ref[IO, List[Event]] = Ref.unsafe(initialEvents)

    val testInstance = InMemoryExpiringEventStore(15.minutes, eventsRef)
    val expectedUnexpiredEvents: List[Event] = initialEvents.takeRight(3)

    for {
      _ <- testInstance.removeExpired()
      unexpiredEvents <- eventsRef.get
    } yield expect.same(expectedUnexpiredEvents, unexpiredEvents)
  }

  pureTest("purgeExpiredEvents() returns a List containing only unexpired Events") {

    val expectedUnexpiredEvents: List[Event] = initialEvents.takeRight(3)

    expect.same(
      expectedUnexpiredEvents,
      InMemoryExpiringEventStore.purgeExpiredEvents(initialInstant.plus(2, ChronoUnit.HOURS))(initialEvents)
    )
  }

}
