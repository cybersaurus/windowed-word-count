package wwc.store

import cats.effect.IO
import wwc.model.Event

import scala.concurrent.duration.FiniteDuration

trait ExpiringEventStore {
  val expirationWindow: FiniteDuration

  def append(event: Event): IO[Unit]
  def getAllUnexpired(): IO[List[Event]]

  def removeExpired(): IO[Unit]
}
