package wwc.app

import cats.effect.IO
import cats.effect.Ref
import wwc.model.Event
import wwc.service.EventListener
import wwc.store.InMemoryExpiringEventStore

import scala.concurrent.duration.*

class WindowedWordCountApp extends cats.effect.IOApp.Simple {

  override final val run: IO[Unit] =
    Ref
      .of[IO, List[Event]](List.empty)
      .map(eventsRef => InMemoryExpiringEventStore(expirationWindow = 1.minutes, eventsRef))
      .flatMap { eventStore =>
        IO.both(
          EventListener(inputStream = System.in, eventStore).consume,
          eventStore.removeExpired().foreverM
        )
      }
      .void
}
