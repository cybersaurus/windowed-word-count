package wwc.model

object EventSpec extends weaver.FunSuite with wwc.time.TimeFixtures {

  test("words() returns a list of words contained in the Event") {
    val event = Event("eventType", "one two three", now)
    expect.same(
      expected = List("one", "two", "three"),
      found = event.words
    )
  }

  test("words() returns an empty list of words when the Event contains no words") {
    val event = Event("emptyDataEventType", "", now)
    expect.same(
      expected = List.empty,
      found = event.words
    )
  }

  test("words() returns an empty list of words when the Event blank text") {
    val event = Event("blankDataEventType", "    ", now)
    expect.same(
      expected = List.empty,
      found = event.words
    )
  }
}
