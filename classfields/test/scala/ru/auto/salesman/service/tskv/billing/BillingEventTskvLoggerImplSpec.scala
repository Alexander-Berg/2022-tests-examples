package ru.auto.salesman.service.tskv.billing

import org.joda.time.DateTime
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.service.broker.LogsBrokerService
import ru.auto.salesman.service.tskv.billing.impl.BillingEventTskvLoggerImpl
import ru.auto.salesman.test.BaseSpec

class BillingEventTskvLoggerImplSpec extends BaseSpec {

  val now = DateTime.now()
  val brokerService = mock[LogsBrokerService]
  val logger = new BillingEventTskvLoggerImpl(brokerService)
  val billingEventDateTime = DateTime.parse("2021-09-20T15:00:00")

  "BrokerBillingEventTskvLogger" should {
    "send log to broker with timestamp from billingEvent" in {
      (brokerService
        .sendBillingTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(*, *, billingEventDateTime)
        .returningZ(())

      logger
        .apply(billingEvent(billingEventDateTime), Map.empty)
        .provideConstantClock(now)
        .success
        .value
    }
  }

}
