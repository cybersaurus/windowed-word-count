package wwc.app

import cats.effect.IO
import cats.effect.Ref
import wwc.model.Event
import wwc.service.EventListener
import wwc.service.EventService
import wwc.store.ExpiringEventStore
import wwc.store.InMemoryExpiringEventStore

import scala.concurrent.duration.*

class WindowedWordCountApp extends cats.effect.IOApp.Simple {

  override final val run: IO[Unit] =
    Ref
      .of[IO, List[Event]](List.empty)
      .map(eventsRef => InMemoryExpiringEventStore(expirationWindow = 5.minutes, eventsRef))
      .flatMap { eventStore =>
        IO.both(
          IO.both(
            EventListener(inputStream = System.in, eventStore).consume,
            eventStore.removeExpired().foreverM
          ),
          buildServer(eventStore)
        )
      }
      .void

  private def buildServer(eventStore: ExpiringEventStore): IO[Nothing] = {
    import com.comcast.ip4s.port
    import org.http4s.ember.server.EmberServerBuilder
    import org.http4s.server.Router

    EmberServerBuilder
      .default[IO]
      .withPort(port"8080")
      .withHttpApp(Router[IO]("/" -> EventService.routes(eventStore)).orNotFound)
      .build
      .useForever
      .onCancel(IO.println("Service shutting down..."))
  }
}
