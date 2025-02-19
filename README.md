# Windowed Word Count

### Problem

Given incoming JSON events of the following shape:
```shell
{ "event_type": "baz", "data": "sit", "timestamp": 1739467746293 }
{ "event_type": "baz", "data": "ipsum", "timestamp": 1739467746293 }
```
count the number of words contained within `data`, grouped by `event_type`.

Invalid input records are discarded.

Results are counted over a rolling 2 minute window.
i.e. events received more than 2 minutes ago are automatically purged.

Results can be viewed via a REST API:
```shell
GET /wordcount
```

### Solution

The main components of the application are:

##### WindowedWordCountApp
This class provides the `main` function that is invoked to start the application.
It is abstract and singleton instances are defined in the companion class and via the scala-cli wrapper in [event-listener.scala](event-listener.scala).
(See: [WindowedWordCountApp](src/main/scala/wwc/app/WindowedWordCountApp.scala))

The app is responsible for:
- instantiating an event listener to consume events from `stdin` and store in a transient event store. (See: [EventListener](src/main/scala/wwc/service/EventListener.scala))
- instantiating a REST API service to vend word count results. (See: [EventService](src/main/scala/wwc/service/EventService.scala))
- creating a background task to manage purging expired events from the event store.

##### EventStore
`InMemoryExpiringEventStore` transiently "stores" a sequence of incoming events.

The store follows Event Sourcing principles in vending an "append-only" interface - meaning that existing events cannot be modified.
(Although expired events can still be purged, of course.).

EventStore clients are responsible for converting the sequence of Events into whatever response format is required.

(See: [InMemoryExpiringEventStore](src/main/scala/wwc/store/InMemoryExpiringEventStore.scala))

### Running tests

##### Unit tests
```shell
sbt clean compile test
```
##### Integration tests
The integration tests:
- start instances of the EventListener & EventService
- consume Events from the input stream
- call the EventService REST API and verify the received word counts match the expected values
```shell
sbt integration/test
```
To run both the unit and integration tests, use the supplied alias:
```shell
sbt validate
```

### Running the application

The application can be started in one of two ways. Either by first building the codebase and running the resultant jar file.
Or in a more scripting-like manner, via [Scala-CLI](https://scala-cli.virtuslab.org/).

##### Using `jar` command
```shell
sbt assembly
java -jar external/blackbox.jar | java -jar target/scala-3.6.3/event-listener.jar
```
##### Using `scala-cli` command
(Assumes scala-cli is installed locally. See: [Installing Scala-CLI](https://scala-cli.virtuslab.org/install))
```shell
java -jar external/blackbox.jar | scala-cli event-listener.scala
```
Piping the `blackbox` output events into `event-listener` enables the two process to remain decoupled.
This allows for easier testing the application components in isolation for the blackbox events producer.

### Calling the Event Service REST API
The current word count can be accessed via:

```shell
curl localhost:8080/wordcount
```

and will return output similar to:

```shell
{
  "wordCountsByEventType": {
    "bar": [
      {
        "word": "amet",
        "count": 3
      },
      {
        "word": "dolor",
        "count": 8
      }
    ],
    "baz": [
      {
        "word": "amet",
        "count": 3
      },
      {
        "word": "ipsum",
        "count": 5
      }
    ],
    "foo": [
      {
        "word": "lorem",
        "count": 6
      },
      {
        "word": "amet",
        "count": 4
      }
    ]
  }
}
```
