package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import ru.yandex.vertis.telepony.SampleHelper.rc
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer.generatorAsProducer
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.DomainPoolService.ReleaseRequest
import ru.yandex.vertis.telepony.service.SharedPoolService.{NotEnoughNumbersException, PhoneDomainFilter}
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.annotation.nowarn

/**
  *
  * @author zvez
  */
class DomainPoolServiceIntSpec extends SpecBase with IntegrationSpecTemplate {

  val availablePhoneGen = for {
    opn <- Generator.OperatorNumberGen
    status <- Gen.oneOf(Status.Ready(None), Status.New(None))
  } yield opn.copy(status = status)

  "DomainPoolService" should {

    "release nothing when no phones available" in {
      val req = ReleaseRequest(
        PhoneDomainFilter(PhoneTypes.Mobile, 2, OperatorAccounts.MtsShared, Set(StatusValues.New, StatusValues.Ready)),
        None,
        10
      )
      domainPoolService.releaseNumbers(req).failed.futureValue shouldBe an[NotEnoughNumbersException]
    }

    "release one phone" in {
      val opn = availablePhoneGen.next
      prepareNumber(opn)

      val req = ReleaseRequest(
        PhoneDomainFilter(opn.phoneType, opn.geoId, opn.account, Set(StatusValues.New, StatusValues.Ready)),
        None,
        1
      )
      domainPoolService.releaseNumbers(req).futureValue shouldBe 1

      val releasedNumber = operatorNumberServiceV2.get(opn.number).futureValue
      releasedNumber.status.value shouldBe StatusValues.Deleted

      val sharedNumber = sharedPoolService.get(opn.number).futureValue
      sharedNumber.domain shouldBe empty
      sharedNumber.status shouldBe SharedNumberStatusValues.Free
    }

    "release some numbers" in {
      val otherDomain = TypedDomains.autoru_sto
      val account = OperatorAccounts.MtsShared
      val originOperator = Operators.Mts
      val geoId = 1
      val phoneType = PhoneTypes.Mobile
      val gen =
        availablePhoneGen.map(
          _.copy(account = account, originOperator = originOperator, geoId = geoId, phoneType = phoneType)
        )

      val goodNumbersCount = 5
      val goodNumbers = gen.next(goodNumbersCount)
      goodNumbers.foreach(prepareNumber(_))

      val brokenNumbers = gen.next(2)
      brokenNumbers.foreach(prepareNumber(_, Some(otherDomain)))

      val req =
        ReleaseRequest(PhoneDomainFilter(phoneType, geoId, account, Set(StatusValues.New, StatusValues.Ready)), None, 6)
      domainPoolService.releaseNumbers(req).futureValue shouldBe goodNumbersCount

      goodNumbers.foreach { opn =>
        val domain = operatorNumberServiceV2.get(opn.number).futureValue
        domain.status.value shouldBe StatusValues.Deleted

        val shared = sharedPoolService.get(opn.number).futureValue
        shared.domain shouldBe empty
        shared.status shouldBe SharedNumberStatusValues.Free
      }

      brokenNumbers.foreach { opn =>
        //opn becomes deleted, maybe should make recovery
//        val domain = operatorNumberServiceV2.get(opn.number).futureValue
//        domain shouldBe opn

        //should stay the same
        val shared = sharedPoolService.get(opn.number).futureValue
        shared.domain shouldBe Some(otherDomain)
        shared.status shouldBe SharedNumberStatusValues.Assigned
      }
    }

    "move to other domain" in {
      val otherDomain = TypedDomains.autoru_sto
      val opn = availablePhoneGen.next
      prepareNumber(opn)

      val req = ReleaseRequest(
        PhoneDomainFilter(opn.phoneType, opn.geoId, opn.account, Set(StatusValues.New, StatusValues.Ready)),
        Some(otherDomain),
        1
      )
      domainPoolService.releaseNumbers(req).futureValue shouldBe 1

      val releasedNumber = operatorNumberServiceV2.get(opn.number).futureValue
      releasedNumber.status.value shouldBe StatusValues.Deleted

      val sharedNumber = sharedPoolService.get(opn.number).futureValue
      sharedNumber.domain shouldBe Some(otherDomain)
      sharedNumber.status shouldBe SharedNumberStatusValues.Assigned
    }

    "release phone with predefined status" in {

      @nowarn
      def releaseAndCheckOpn(opn: OperatorNumber, statusFilterForRelease: Set[StatusValue]): Unit = {
        val req =
          ReleaseRequest(PhoneDomainFilter(opn.phoneType, opn.geoId, opn.account, statusFilterForRelease), None, 1)
        domainPoolService.releaseNumbers(req).futureValue shouldBe 1

        val releasedNumber = operatorNumberServiceV2.get(opn.number).futureValue
        releasedNumber.status.value shouldBe StatusValues.Deleted

        val sharedNumber = sharedPoolService.get(opn.number).futureValue
        sharedNumber.domain shouldBe empty
        sharedNumber.status shouldBe SharedNumberStatusValues.Free
      }

      val goodOpns = availablePhoneGen.next(5)
      val downtimedOpns = availablePhoneGen.next(2).map(_.copy(status = Status.Downtimed(None)))
      val garbageOpns = availablePhoneGen.next(2).map(_.copy(status = Status.Garbage(None)))
      val allOpns = goodOpns ++ downtimedOpns ++ garbageOpns
      allOpns.foreach(prepareNumber(_))

      (garbageOpns ++ downtimedOpns).foreach(releaseAndCheckOpn(_, Set(StatusValues.Garbage, StatusValues.Downtimed)))

    }

  }

  @nowarn
  private def prepareNumber(opn: OperatorNumber, assignedToDomain: Option[TypedDomain] = Some(typedDomain)): Unit = {
    operatorNumberDaoV2.create(opn).databaseValue.futureValue

    val status =
      if (assignedToDomain.isDefined) {
        SharedNumberStatusValues.Assigned
      } else {
        SharedNumberStatusValues.Free
      }

    val sharedNumber = SharedOperatorNumber(
      number = opn.number,
      account = opn.account,
      originOperator = Some(opn.originOperator),
      geoId = opn.geoId,
      phoneType = opn.phoneType,
      domain = assignedToDomain,
      status = status
    )

    sharedOperatorNumberDao.upsert(sharedNumber)
  }

}
