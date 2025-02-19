package wwc

import cats.effect.IO
import cats.effect.Resource
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.client.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.syntax.literals.*
import wwc.app.WindowedWordCount
import wwc.model.EventType
import wwc.service.EventService
import wwc.service.EventService.WordCount

import java.io.InputStream

object WindowedWordCountAppTest extends weaver.IOSuite with wwc.json.DecoderFixtures {

  case class SharedResources(client: Client[IO])
  override type Res = SharedResources

  override def sharedResource: Resource[IO, SharedResources] = {
    import java.io.ByteArrayInputStream

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
    System.setIn(inputStream)

    for {
      client <- EmberClientBuilder.default[IO].build
      _ <- WindowedWordCount.resource
    } yield SharedResources(client)
  }

  test("return map of WordCounts for the given Events") { sharedResources =>

    val expectedResponse = EventService.WordCountsByEventType(
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

    for {
      response <- sharedResources.client.expect[EventService.WordCountsByEventType]("http://localhost:8080/wordcount")
    } yield expect.same(expectedResponse, response)
  }
}
