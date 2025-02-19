package wwc.json

import io.circe.Decoder
import io.circe.KeyDecoder
import wwc.model.EventType
import wwc.service.EventService

trait DecoderFixtures {
  protected given KeyDecoder[EventType] = KeyDecoder.instance(EventType.apply andThen Some.apply)

  protected given Decoder[EventService.WordCount] = Decoder.forProduct2("word", "count")(EventService.WordCount.apply)

  protected given Decoder[EventService.WordCountsByEventType] =
    Decoder.forProduct1("wordCountsByEventType")(EventService.WordCountsByEventType.apply)

}
