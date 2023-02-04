package ru.yandex.vertis.passport.proto

import org.joda.time.DateTime
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import ru.yandex.vertis.passport.Domains
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.test.ModelGenerators._
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.SpecBase
import ru.yandex.vertis.tracing.Traced

class EventsProtoFormatsSpec extends WordSpec with SpecBase with BeforeAndAfterEach with GeneratorDrivenPropertyChecks {
  "EventEnvelopeFormat" should {
    "trim session" in {
      val requestContext: RequestContext = RequestContext(ApiPayload("test"), Traced.empty)
      val userLoggedInEvent = userLoggedIn.next
      val event = EventEnvelope(Domains.Auto, DateTime.now(), userLoggedInEvent, requestContext)
      val proto = EventsProtoFormats.EventEnvelopeFormat.write(event)
      proto.getPayload.getUserLoggedIn.getSessionId should endWith("...")
    }
  }
}
