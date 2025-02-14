package wwc

import cats.effect.IO

class EventListenerApp extends cats.effect.IOApp.Simple {

  override final val run: IO[Unit] =
    fs2.io.stdinUtf8[IO](1024)
    .through(fs2.text.lines)
    .evalMap(line => IO.println(s"Received line: [$line]"))
    .onFinalize(IO.println("Stream finished. Bye!"))
    .compile.drain
}
