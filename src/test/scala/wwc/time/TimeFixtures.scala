package wwc.time

import java.time.temporal.ChronoUnit
import java.time.Instant

trait TimeFixtures {
  protected val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
}
