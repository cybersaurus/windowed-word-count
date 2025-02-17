# Windowed Word Count

### Running the tests
```shell
sbt clean compile test
```

### Start the Event Publisher and Event Listener

##### Using `jar` command
```shell
sbt assembly
java -jar external/blackbox.jar | java -jar target/scala-3.6.3/event-listener.jar
```
##### Using `scala-cli` command
```shell
java -jar external/blackbox.jar | scala-cli event-listener.scala
```