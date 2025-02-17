package wwc.store

import cats.effect.Clock
import cats.effect.IO
import cats.effect.Ref
import wwc.model.Event

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

class InMemoryExpiringEventStore(override val expirationWindow: FiniteDuration, eventsRef: Ref[IO, List[Event]])
    extends ExpiringEventStore {

  import InMemoryExpiringEventStore.purgeExpiredEvents

  private val expiryTime: IO[Instant] =
    Clock[IO].realTimeInstant
      .map(now => now.minus(expirationWindow.toJava))

  override def append(event: Event): IO[Unit] =
    eventsRef.update(events => events.appended(event))

  override def getAllUnexpired(): IO[List[Event]] =
    expiryTime
      .flatMap { expiredIfBefore =>
        eventsRef.updateAndGet(purgeExpiredEvents(expiredIfBefore))
      }

  override def removeExpired(): IO[Unit] =
    expiryTime
      .flatMap(expiredIfBefore => eventsRef.update(purgeExpiredEvents(expiredIfBefore)))
}

object InMemoryExpiringEventStore {
  private[store] def purgeExpiredEvents(expiredIfBefore: Instant)(events: List[Event]): List[Event] =
    events.dropWhile { event => event.timestamp.isBefore(expiredIfBefore) }
}
