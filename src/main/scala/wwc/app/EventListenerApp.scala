package wwc.app

import cats.effect.IO
import fs2.Pipe
import io.circe.{Decoder, Json, ParsingFailure}
import io.circe.parser.parse
import wwc.model.Event

class EventListenerApp extends cats.effect.IOApp.Simple {

  private val toJson: Pipe[IO, String, Either[ParsingFailure, Json]] = _.map(parse)
  private val dropParsingFailures: Pipe[IO, Either[ParsingFailure, Json], Json] = _.collect { case Right(json) => json }
  private val toEvent: Pipe[IO, Json, Decoder.Result[Event]] = _.map(_.as[Event])
  private val dropDecodingFailures: Pipe[IO, Decoder.Result[Event], Event] = _.collect { case Right(event) => event }


  override final val run: IO[Unit] =
    fs2.io
      .stdinUtf8[IO](1024)
      .through(fs2.text.lines)
      .through(toJson)
      .through(dropParsingFailures)
      .through(toEvent)
      .through(dropDecodingFailures)
      .evalTap(IO.println)
      .onFinalize(IO.println("Bye!"))
      .compile.drain
}
