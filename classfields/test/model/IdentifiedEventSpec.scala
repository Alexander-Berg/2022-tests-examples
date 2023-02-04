package vertistraf.notification_center.events_broker.main.test.model

import ru.vertistraf.notification_center.events_broker.model.Event
import ru.vertistraf.notification_center.events_broker.model.IdentifiedEvent._
import zio.random.Random
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.magnolia._

object IdentifiedEventSpec extends DefaultRunnableSpec {
  import Assertion._

  override def spec: ZSpec[TestEnvironment, Any] = suite("IdentifiedEventSpec")(
    suite("eventId")(
      testM("Different events have different event ids")(
        check(eventGen, eventGen) { (event1, event2) =>
          assert(event1.eventId)(not(equalTo(event2.eventId)))
        }
      ),
      testM("Depends only on the event content")(
        check(eventGen) { event1 =>
          assert(event1.eventId)(equalTo(event1.eventId))
        }
      )
    )
  )

  private val eventGen: Gen[Random with Sized, Event] = DeriveGen[Event]
}
