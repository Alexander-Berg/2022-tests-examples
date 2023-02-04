package ru.yandex.realty.phone

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.phone.PhoneRedirect.PhoneRedirectStrategyInitialStepNumber
import ru.yandex.realty.model.phone.PhoneType
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.proto.phone.PhoneRedirectStrategyAlgorithmType
import ru.yandex.vertis.telepony.model.proto.DomainNumberStatusEnum.DomainNumberStatus
import ru.yandex.vertis.telepony.model.proto.NumbersCountersResponse
import ru.yandex.vertis.telepony.model.proto.NumbersCountersResponse.CounterResult

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

@RunWith(classOf[JUnitRunner])
class RedirectPhoneServiceSpec
  extends AsyncSpecBase
  with OneInstancePerTest
  with PhoneGenerators
  with RedirectPhoneServiceTestComponents {

  "RedirectPhoneService" should {

    "create redirect with given data for one step strategy" in {
      val targetPhone = phoneGen.next
      val redirectData = redirectTeleponyInfoGen(
        targetPhone,
        phoneType = Some(PhoneType.Mobile),
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
      ).next
      val teleponyRedirect = phoneRedirectGen(
        targetPhone = redirectData.target,
        tag = redirectData.tag,
        phoneType = redirectData.phoneType,
        strategy = redirectData.strategy.getStrategy
      ).next

      expectTeleponyCall(redirectData.tag, redirectData.phoneType, Success(teleponyRedirect))

      val actualRedirect = redirectPhoneService.createRedirectAsync(redirectData).futureValue

      actualRedirect.domain shouldEqual teleponyRedirect.domain
      actualRedirect.id shouldEqual teleponyRedirect.id
      actualRedirect.objectId shouldEqual teleponyRedirect.objectId
      actualRedirect.tag shouldEqual teleponyRedirect.tag
      actualRedirect.createTime shouldEqual teleponyRedirect.createTime
      actualRedirect.deadline shouldEqual teleponyRedirect.deadline
      actualRedirect.source shouldEqual teleponyRedirect.source
      actualRedirect.target shouldEqual teleponyRedirect.target
      actualRedirect.phoneType shouldEqual teleponyRedirect.phoneType
      actualRedirect.geoId shouldEqual teleponyRedirect.geoId
      actualRedirect.ttl shouldEqual teleponyRedirect.ttl

      actualRedirect.strategy.getStrategy shouldEqual PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
      actualRedirect.strategy.getStepNumber shouldEqual PhoneRedirectStrategyInitialStepNumber
    }

    "create local redirect on first step for prioritize local strategy" in {
      val targetPhone = phoneGen.next
      val redirectData = redirectTeleponyInfoGen(
        targetPhone,
        phoneType = Some(PhoneType.Mobile),
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      ).next
      val teleponyRedirect = phoneRedirectGen(
        targetPhone = redirectData.target,
        tag = redirectData.tag,
        phoneType = Some(PhoneType.Local),
        strategy = redirectData.strategy.getStrategy
      ).next

      expectTeleponyCall(redirectData.tag, Some(PhoneType.Local), Success(teleponyRedirect))

      val actualRedirect = redirectPhoneService.createRedirectAsync(redirectData).futureValue

      actualRedirect.domain shouldEqual teleponyRedirect.domain
      actualRedirect.id shouldEqual teleponyRedirect.id
      actualRedirect.objectId shouldEqual teleponyRedirect.objectId
      actualRedirect.tag shouldEqual teleponyRedirect.tag
      actualRedirect.createTime shouldEqual teleponyRedirect.createTime
      actualRedirect.deadline shouldEqual teleponyRedirect.deadline
      actualRedirect.source shouldEqual teleponyRedirect.source
      actualRedirect.target shouldEqual teleponyRedirect.target
      actualRedirect.phoneType shouldEqual Some(PhoneType.Local)
      actualRedirect.geoId shouldEqual teleponyRedirect.geoId
      actualRedirect.ttl shouldEqual teleponyRedirect.ttl

      actualRedirect.strategy.getStrategy shouldEqual PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      actualRedirect.strategy.getStepNumber shouldEqual PhoneRedirectStrategyInitialStepNumber
    }

    "create mobile redirect on second step for prioritize local strategy" in {
      val targetPhone = phoneGen.next
      val redirectData = redirectTeleponyInfoGen(
        targetPhone,
        phoneType = Some(PhoneType.Mobile),
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      ).next
      val teleponyRedirect = phoneRedirectGen(
        targetPhone = redirectData.target,
        tag = redirectData.tag,
        phoneType = Some(PhoneType.Mobile),
        strategy = redirectData.strategy.getStrategy
      ).next

      expectTeleponyCallFailed(
        redirectData.tag,
        Some(PhoneType.Local),
        geoId = None,
        exception = new NoSuchElementException()
      )
      expectTeleponyCall(
        redirectData.tag,
        Some(PhoneType.Mobile),
        geoId = None,
        result = Success(teleponyRedirect)
      )

      val actualRedirect = redirectPhoneService.createRedirectAsync(redirectData).futureValue

      actualRedirect.domain shouldEqual teleponyRedirect.domain
      actualRedirect.id shouldEqual teleponyRedirect.id
      actualRedirect.objectId shouldEqual teleponyRedirect.objectId
      actualRedirect.tag shouldEqual teleponyRedirect.tag
      actualRedirect.createTime shouldEqual teleponyRedirect.createTime
      actualRedirect.deadline shouldEqual teleponyRedirect.deadline
      actualRedirect.source shouldEqual teleponyRedirect.source
      actualRedirect.target shouldEqual teleponyRedirect.target
      actualRedirect.phoneType shouldEqual Some(PhoneType.Mobile)
      actualRedirect.geoId shouldEqual None
      actualRedirect.ttl shouldEqual teleponyRedirect.ttl

      actualRedirect.strategy.getStrategy shouldEqual PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      actualRedirect.strategy.getStepNumber shouldEqual PhoneRedirectStrategyInitialStepNumber + 1
    }

    "create local msk redirect on third step for prioritize local strategy" in {
      val targetPhone = phoneGen.next
      val redirectData = redirectTeleponyInfoGen(
        targetPhone,
        phoneType = Some(PhoneType.Local),
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      ).next
      val teleponyRedirect = phoneRedirectGen(
        targetPhone = redirectData.target,
        tag = redirectData.tag,
        phoneType = Some(PhoneType.Local),
        strategy = redirectData.strategy.getStrategy,
        geoId = Some(Regions.MSK_AND_MOS_OBLAST)
      ).next

      expectTeleponyCallFailed(
        redirectData.tag,
        Some(PhoneType.Local),
        geoId = None,
        exception = new NoSuchElementException()
      )
      expectTeleponyCallFailed(
        redirectData.tag,
        Some(PhoneType.Mobile),
        geoId = None,
        exception = new NoSuchElementException()
      )
      expectTeleponyCall(
        redirectData.tag,
        Some(PhoneType.Local),
        geoId = Some(Regions.MSK_AND_MOS_OBLAST),
        result = Success(teleponyRedirect)
      )

      val actualRedirect = redirectPhoneService.createRedirectAsync(redirectData).futureValue

      actualRedirect.domain shouldEqual teleponyRedirect.domain
      actualRedirect.id shouldEqual teleponyRedirect.id
      actualRedirect.objectId shouldEqual teleponyRedirect.objectId
      actualRedirect.tag shouldEqual teleponyRedirect.tag
      actualRedirect.createTime shouldEqual teleponyRedirect.createTime
      actualRedirect.deadline shouldEqual teleponyRedirect.deadline
      actualRedirect.source shouldEqual teleponyRedirect.source
      actualRedirect.target shouldEqual teleponyRedirect.target
      actualRedirect.phoneType shouldEqual Some(PhoneType.Local)
      actualRedirect.geoId shouldEqual Some(Regions.MSK_AND_MOS_OBLAST)
      actualRedirect.ttl shouldEqual teleponyRedirect.ttl

      actualRedirect.strategy.getStrategy shouldEqual PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      actualRedirect.strategy.getStepNumber shouldEqual PhoneRedirectStrategyInitialStepNumber + 2
    }

    "fail to create redirect for prioritize local strategy when unexpected exception occurred" in {
      val targetPhone = phoneGen.next
      val redirectData = redirectTeleponyInfoGen(
        targetPhone,
        phoneType = Some(PhoneType.Mobile),
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      ).next

      expectTeleponyCallFailed(
        redirectData.tag,
        Some(PhoneType.Local),
        geoId = None,
        exception = new NoSuchElementException()
      )
      expectTeleponyCallFailed(
        redirectData.tag,
        Some(PhoneType.Local),
        geoId = Some(Regions.MSK_AND_MOS_OBLAST),
        exception = new RuntimeException
      )

      val failed = Try(redirectPhoneService.createRedirectAsync(redirectData).futureValue)

      failed.isFailure shouldBe true
    }

  }

  "RedirectPhoneService companion " should {
    "count vacant percentage by telepony response" in {
      val response = NumbersCountersResponse
        .newBuilder()
        .addAllResults(
          Seq(
            CounterResult.newBuilder().setGeoId(1).setStatus(DomainNumberStatus.READY).setCount(20).build(),
            CounterResult.newBuilder().setGeoId(1).setStatus(DomainNumberStatus.DOWNTIMED).setCount(20).build(),
            CounterResult.newBuilder().setGeoId(1).setStatus(DomainNumberStatus.BUSY).setCount(20).build()
          ).asJava
        )
        .build()
      (RedirectPhoneService.countVacantPercentage(response) * 100).round shouldBe 33
    }
  }
}
