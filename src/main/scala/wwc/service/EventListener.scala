package wwc.service

import cats.effect.IO
import fs2.Pipe
import io.circe.parser.parse
import io.circe.Decoder
import io.circe.Json
import io.circe.ParsingFailure
import wwc.model.Event
import wwc.store.ExpiringEventStore

import java.io.InputStream

class EventListener(inputStream: InputStream, eventStore: ExpiringEventStore) {
  private val toJson: Pipe[IO, String, Either[ParsingFailure, Json]] = _.map(parse)
  private val dropParsingFailures: Pipe[IO, Either[ParsingFailure, Json], Json] = _.collect { case Right(json) => json }
  private val toEvent: Pipe[IO, Json, Decoder.Result[Event]] = _.map(_.as[Event])
  private val dropDecodingFailures: Pipe[IO, Decoder.Result[Event], Event] = _.collect { case Right(event) => event }

  final val consume: IO[Unit] =
    fs2.io
      .readInputStream(fis = IO.pure(inputStream), chunkSize = 1024)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .through(toJson)
      .through(dropParsingFailures)
      .through(toEvent)
      .through(dropDecodingFailures)
      .evalTap(IO.println)
      .evalMap(event => eventStore.append(event))
      .onFinalize(IO.println("") >> IO.println("Shutting down EventListener..."))
      .compile
      .drain
}
