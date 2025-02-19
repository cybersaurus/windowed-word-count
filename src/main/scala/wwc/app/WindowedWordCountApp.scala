package wwc.app

import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import org.http4s.server.Server
import wwc.model.Event
import wwc.service.EventListener
import wwc.service.EventService
import wwc.store.ExpiringEventStore
import wwc.store.InMemoryExpiringEventStore

import scala.concurrent.duration.*

object WindowedWordCountApp extends cats.effect.IOApp.Simple {

  override final val run: IO[Unit] = WindowedWordCount.resource.useForever
}

object WindowedWordCount {
  private val WINDOW: FiniteDuration = 2.minutes

  val resource: Resource[IO, Unit] =
    Ref
      .of[IO, List[Event]](List.empty)
      .toResource
      .map(eventsRef => InMemoryExpiringEventStore(expirationWindow = WINDOW, eventsRef))
      .flatMap { eventStore =>
        Resource
          .both(
            IO.both(
              EventListener(inputStream = System.in, eventStore).consume,
              (IO.sleep(WINDOW) >> eventStore.removeExpired()).start.void
            ).toResource,
            buildServer(eventStore)
          )
          .flatMap(_ => Resource.unit)
      }

  private def buildServer(eventStore: ExpiringEventStore): Resource[IO, Server] = {
    import com.comcast.ip4s.port
    import org.http4s.ember.server.EmberServerBuilder
    import org.http4s.server.Router

    EmberServerBuilder
      .default[IO]
      .withPort(port"8080")
      .withHttpApp(Router[IO]("/" -> EventService.routes(eventStore)).orNotFound)
      .build
  }
}
