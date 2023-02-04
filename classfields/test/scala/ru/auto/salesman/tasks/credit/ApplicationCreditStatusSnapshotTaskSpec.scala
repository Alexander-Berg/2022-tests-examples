package ru.auto.salesman.tasks.credit

import org.joda.time.DateTime
import ru.auto.salesman.model.AutoruDealer
import ru.auto.salesman.service.application_credit.{
  ApplicationCreditSnapshot,
  CreditTariffReadService,
  DealerActiveApplicationCreditInfo,
  GroupedDealers
}
import ru.auto.salesman.service.broker.ApplicationCreditBrokerService
import ru.auto.salesman.test.BaseSpec

import scala.util.control.NoStackTrace

class ApplicationCreditStatusSnapshotTaskSpec extends BaseSpec {
  import ApplicationCreditStatusSnapshotTaskSpec._
  import ApplicationCreditStatusSnapshotTask._

  private val creditTariffService = mock[CreditTariffReadService]
  private val brokerService = mock[ApplicationCreditBrokerService]

  private val task =
    new ApplicationCreditStatusSnapshotTask(creditTariffService, brokerService)

  private val testDateTime = DateTime.now

  "create right DealerActiveCreditInfo" in {

    val expected = List(
      DealerActiveApplicationCreditInfo(
        testDateTime,
        AutoruDealer(1),
        singleForUsedActive = false,
        singleForNewActive = true,
        accessForUsedActive = true,
        accessForNewActive = true
      ),
      DealerActiveApplicationCreditInfo(
        testDateTime,
        AutoruDealer(2),
        singleForUsedActive = true,
        singleForNewActive = false,
        accessForUsedActive = true,
        accessForNewActive = false
      ),
      DealerActiveApplicationCreditInfo(
        testDateTime,
        AutoruDealer(3),
        singleForUsedActive = true,
        singleForNewActive = false,
        accessForUsedActive = false,
        accessForNewActive = false
      ),
      DealerActiveApplicationCreditInfo(
        testDateTime,
        AutoruDealer(4),
        singleForUsedActive = false,
        singleForNewActive = true,
        accessForUsedActive = false,
        accessForNewActive = false
      )
    )

    val result = testInfo.toDealerActiveCreditInfo(testDateTime)

    result should contain theSameElementsAs expected
  }

  "send data to broker" in {
    (creditTariffService.getApplicationCreditSnapshot _)
      .expects()
      .once()
      .returningZ(testInfo)

    val mustCallSendTimes =
      testInfo.toDealerActiveCreditInfo(testDateTime).toList.length

    (brokerService.sendActiveCreditStatistics _)
      .expects(*)
      .repeated(mustCallSendTimes)
      .returningZ(())

    task
      .execute()
      .success
  }

  "fail if creditTariffService fail" in {
    (creditTariffService.getApplicationCreditSnapshot _)
      .expects()
      .once()
      .throwingZ(TestException)

    (brokerService.sendActiveCreditStatistics _)
      .expects(*)
      .never()

    task
      .execute()
      .failure
      .exception shouldEqual TestException
  }

  "fail if broker fail and dont call broker again" in {
    (creditTariffService.getApplicationCreditSnapshot _)
      .expects()
      .once()
      .returningZ(testInfo)

    (brokerService.sendActiveCreditStatistics _)
      .expects(*)
      .once()
      .throwingZ(TestException)

    task
      .execute()
      .failure
      .exception shouldEqual TestException
  }

}

object ApplicationCreditStatusSnapshotTaskSpec {

  val testInfo: ApplicationCreditSnapshot =
    ApplicationCreditSnapshot(
      singleCredit = GroupedDealers(
        dealersWithCarsNew = Set(AutoruDealer(1), AutoruDealer(4)),
        dealersWithCarsUsed = Set(AutoruDealer(2), AutoruDealer(3))
      ),
      accessCredit = GroupedDealers(
        dealersWithCarsNew = Set(AutoruDealer(1)),
        dealersWithCarsUsed = Set(AutoruDealer(1), AutoruDealer(2))
      )
    )

  case object TestException extends Exception with NoStackTrace
}
