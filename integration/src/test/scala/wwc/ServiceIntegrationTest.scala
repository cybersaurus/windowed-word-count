package wwc

import cats.effect.IO
import cats.effect.Ref
import cats.effect.Resource
import io.circe.Decoder
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.server.Server
import org.http4s.syntax.literals.*
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import wwc.model.Event
import wwc.service.EventListener
import wwc.service.EventService
import wwc.service.EventService.WordCount
import wwc.store.InMemoryExpiringEventStore

import java.io.ByteArrayInputStream
import java.io.InputStream
import scala.concurrent.duration.*

object ServiceIntegrationTest extends weaver.IOSuite {

  private given Decoder[EventService.WordCount] = Decoder.forProduct2("word", "count")(WordCount.apply)

  private given Decoder[EventService.WordCountsByEventType] =
    Decoder.forProduct1("wordCountsByEventType")(EventService.WordCountsByEventType.apply)

  case class SharedResources(
      inputStream: InputStream,
      httpApp: HttpApp[IO], /*listener: EventListener,*/ server: Server
  )
  override type Res = SharedResources

  override def sharedResource: Resource[IO, SharedResources] = {
    import com.comcast.ip4s.port
    import org.http4s.ember.server.EmberServerBuilder
    import org.http4s.server.Router

    val now = java.time.Instant.now()

    val inputStream: InputStream =
      new ByteArrayInputStream(
        s"""
           |{ "event_type": "eventType1", "data": "one two three", "timestamp": ${now.minusSeconds(2).toEpochMilli} }
           |{ "event_type": "eventType2", "data": "four four two", "timestamp": ${now.minusSeconds(2).toEpochMilli} }
           |{ "event_type": "eventType1", "data": "two three", "timestamp": ${now.minusSeconds(1).toEpochMilli} }
           |{ "event_type": "eventType2", "data": "four", "timestamp": ${now.minusSeconds(1).toEpochMilli} }
           |{ "event_type": "eventType1", "data": "three", "timestamp": ${now.toEpochMilli} }
           |{ "event_type": "eventType2", "data": "four two", "timestamp": ${now.toEpochMilli} }
           |{ "event_type": "eventTypeWithNoWords", "data": "", "timestamp": ${now.toEpochMilli} }""".stripMargin.getBytes
      )

    for {
      eventsRef <- Ref.of[IO, List[Event]](List.empty).toResource
      eventStore = InMemoryExpiringEventStore(expirationWindow = 5.minutes, eventsRef)
      httpApp: HttpApp[IO] = Router[IO]("/" -> EventService.routes(eventStore)).orNotFound
      _ <- EventListener(inputStream, eventStore).consume.toResource
      server <- EmberServerBuilder
        .default[IO]
        .withPort(port"8080")
        .withHttpApp(httpApp)
        .build
    } yield SharedResources(inputStream, httpApp, server)
  }

  test("return map of WordCounts for the given Events") { sharedResources =>
    val client: Client[IO] = Client.fromHttpApp(sharedResources.httpApp)

    val expectedResponse = EventService.WordCountsByEventType(
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

    for {
      response <- client.expect[EventService.WordCountsByEventType](Request(uri = uri"/wordcount"))
    } yield expect.same(expectedResponse, response)
  }
}
