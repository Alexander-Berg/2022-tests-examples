package ru.auto.salesman.service.tskv.dealer.logger.impl

import org.joda.time.DateTime
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.service.broker.LogsBrokerService
import ru.auto.salesman.test.BaseSpec

import scala.util.Try
import scala.util.Failure

class TskvApplyActionLoggerImplSpec extends BaseSpec {

  val now = DateTime.now()
  val brokerService = mock[LogsBrokerService]
  val logger = new TskvApplyActionLoggerImpl(brokerService)

  "BrokerTskvApplyActionLogger" should {
    "send log to broker" in {
      (brokerService
        .sendTasksTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(
          *,
          Map(
            "isQuota" -> "true",
            "action" -> "action",
            "product" -> "call",
            "service" -> "autoru.salesman-test",
            "method" -> "apply",
            "timestamp" -> s"$now",
            "tskv_format" -> "vertis-aggregated-log",
            "result" -> "Success",
            "unixtime" -> s"${now.getMillis / 1000}"
          ),
          now
        )
        .returningZ(())
      logger
        .log(Request(), Try(Response()))
        .provideConstantClock(now)
        .success
        .value
    }

    "send log to broker result Failure" in {
      (brokerService
        .sendTasksTskvLogEntry(
          _: MessageId,
          _: Map[String, String],
          _: DateTime
        ))
        .expects(
          *,
          Map(
            "isQuota" -> "true",
            "product" -> "call",
            "service" -> "autoru.salesman-test",
            "method" -> "apply",
            "timestamp" -> s"$now",
            "tskv_format" -> "vertis-aggregated-log",
            "result" -> "Failure",
            "unixtime" -> s"${now.getMillis / 1000}"
          ),
          now
        )
        .returningZ(())
      logger
        .log(Request(), Failure(new Exception("test")))
        .provideConstantClock(now)
        .success
        .value
    }
  }

}
