package wwc.model

import io.circe.Decoder

import java.time.Instant

case class Event(eventType: String, data: String, timestamp: Instant) {
  lazy val words: List[String] = data.split(" ").toList.filterNot(_.isBlank)
}

object Event {
  private given Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  given Decoder[Event] = Decoder.forProduct3("event_type", "data", "timestamp")(Event.apply)
}
