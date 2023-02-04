package ru.auto.api.managers.redemption

import org.mockito.Mockito._
import ru.auto.api.BaseSpec
import ru.auto.api.managers.callback.PhoneCallbackManager
import ru.auto.api.model.RequestParams
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.util.{ManagerUtils, Request, RequestImpl}
import ru.auto.cabinet.Redemption.RedemptionForm
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * @author Nikita Shaldenkov <shaldnikita@yandex-team.ru>
  *         Created on 2018-12-17
  */
class RedemptionManagerSpec extends BaseSpec with MockitoSupport {

  private val phoneCallbackManager = mock[PhoneCallbackManager]
  private val cabinetApiClient = mock[CabinetApiClient]
  private val redemptionManager = new RedemptionManager(phoneCallbackManager, cabinetApiClient)

  implicit val trace: Traced = Traced.empty

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }
  private val form = RedemptionForm.newBuilder().setDesiredOfferId("12345").build()

  "RedemptionManager" should {

    "throw exception if unable to request cabinetApi" in {
      reset(cabinetApiClient, phoneCallbackManager)
      when(cabinetApiClient.submitRedemption(?, ?)(?))
        .thenReturn(Future.failed(new IllegalStateException()))

      intercept[IllegalStateException](redemptionManager.createRedemption(1L, form).await)
      verifyNoMoreInteractions(phoneCallbackManager)
    }

    "throw exception if unable to request callKeeperApi" in {
      reset(cabinetApiClient, phoneCallbackManager)
      when(cabinetApiClient.submitRedemption(?, ?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future.failed(new IllegalStateException()))

      intercept[IllegalStateException](redemptionManager.createRedemption(1L, form).await)
    }

    "return create redemption and return SuccessResponse" in {
      reset(cabinetApiClient, phoneCallbackManager)
      when(cabinetApiClient.submitRedemption(?, ?)(?))
        .thenReturn(Future.unit)
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturn(Future(ManagerUtils.SuccessResponse))

      val result = redemptionManager.createRedemption(1L, form).await
      val expectedResult = ManagerUtils.SuccessResponse
      result shouldBe expectedResult
    }
  }
}
