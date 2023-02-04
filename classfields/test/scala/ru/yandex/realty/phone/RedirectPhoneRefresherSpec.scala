package ru.yandex.realty.phone

import org.junit.runner.RunWith
import org.scalatest.OneInstancePerTest
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.phone.PhoneRedirect.PhoneRedirectStrategyInitialStepNumber
import ru.yandex.realty.model.phone.PhoneType
import ru.yandex.realty.model.serialization.phone.PhoneRedirectProtoConverter
import ru.yandex.realty.proto.phone.PhoneRedirectStrategyAlgorithmType

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class RedirectPhoneRefresherSpec
  extends AsyncSpecBase
  with OneInstancePerTest
  with PhoneGenerators
  with RedirectPhoneServiceTestComponents {

  val redirectPhoneRefresher = new RedirectPhoneRefresher(redirectPhoneService)

  "RedirectPhoneRefresher" should {

    "refresh redirect with one step strategy" in {
      val targetPhone = phoneGen.next
      val original = phoneRedirectGen(
        targetPhone,
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
      ).next

      expectTeleponyCall(original.tag, original.phoneType, Success(original))

      val refreshed =
        redirectPhoneRefresher.refreshRedirect(PhoneRedirectProtoConverter.toMessage(original)).futureValue

      refreshed shouldEqual original
    }

    "refresh redirect with prioritize local strategy" in {
      val targetPhone = phoneGen.next
      val original = phoneRedirectGen(
        targetPhone,
        phoneType = None,
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL
      ).next

      expectTeleponyCall(original.tag, Some(PhoneType.Local), Success(original))

      val refreshed =
        redirectPhoneRefresher.refreshRedirect(PhoneRedirectProtoConverter.toMessage(original)).futureValue

      refreshed shouldEqual original
    }

    "refresh redirect with prioritize local strategy and remove old when succeeded step was changed" in {
      val targetPhone = phoneGen.next
      val original = phoneRedirectGen(
        targetPhone,
        phoneType = None,
        strategy = PhoneRedirectStrategyAlgorithmType.PRS_PRIORITIZE_LOCAL,
        strategyStepNumber = 2
      ).next

      expectTeleponyCall(original.tag, Some(PhoneType.Local), Success(original))
      expectTeleponyDeleteCall()

      val refreshed =
        redirectPhoneRefresher.refreshRedirect(PhoneRedirectProtoConverter.toMessage(original)).futureValue

      refreshed.strategy.getStrategy shouldEqual original.strategy.getStrategy
      refreshed.strategy.getStepNumber shouldEqual PhoneRedirectStrategyInitialStepNumber
    }

  }

}
