package wwc.model

import io.circe.Decoder

import java.time.Instant

opaque type EventType = String
object EventType {
  def apply(eventType: String): EventType = eventType

  given Decoder[EventType] = Decoder.forProduct1("event_type")(EventType.apply)
}

case class Event(eventType: EventType, data: String, timestamp: Instant) {
  lazy val words: List[String] = data.split(" ").toList.filterNot(_.isBlank)
}

object Event {
  private given Decoder[Instant] = Decoder.decodeLong.map(Instant.ofEpochMilli)
  private given Decoder[EventType] = Decoder.decodeString.map(EventType.apply)
  given Decoder[Event] = Decoder.forProduct3("event_type", "data", "timestamp")(Event.apply)
}
