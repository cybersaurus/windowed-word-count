package wwc.store

import cats.effect.Clock
import cats.effect.IO
import cats.effect.Ref
import wwc.model.Event

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters.*

/** Simple in-memory implementation of an EventStore. An expirationWindow defines how long store Events are deemed to be
  * valid. After this window has elapsed Events are eligible for eviction from the EventStore on the next invocation of
  * getAllUnexpired() or removeExpired().
  */
class InMemoryExpiringEventStore(override val expirationWindow: FiniteDuration, eventsRef: Ref[IO, List[Event]])
    extends ExpiringEventStore {

  import InMemoryExpiringEventStore.purgeExpiredEvents

  private val expiryTime: IO[Instant] =
    Clock[IO].realTimeInstant
      .map(now => now.minus(expirationWindow.toJava))

  /*
   * Adds the supplied Event to the EventStore
   */
  override def append(event: Event): IO[Unit] =
    eventsRef.update(events => events.appended(event))

  /*
   * Return all currently unexpired Events. As a side effect, expired Events are purged - this is to evict Events that
   * have expired since the last call to removeExpired().
   */
  override def getAllUnexpired(): IO[List[Event]] =
    expiryTime
      .flatMap { expiredIfBefore =>
        eventsRef.updateAndGet(purgeExpiredEvents(expiredIfBefore))
      }

  /*
   * Purges expired Events from the EventStore. Intended to be called periodically on a background thread. (This is to
   * prevent the simple in-memory store from exhausting memory resources.)
   */
  override def removeExpired(): IO[Unit] =
    expiryTime
      .flatMap(expiredIfBefore => eventsRef.update(purgeExpiredEvents(expiredIfBefore)))
}

object InMemoryExpiringEventStore {
  private[store] def purgeExpiredEvents(expiredIfBefore: Instant)(events: List[Event]): List[Event] =
    events.dropWhile { event => event.timestamp.isBefore(expiredIfBefore) }
}
