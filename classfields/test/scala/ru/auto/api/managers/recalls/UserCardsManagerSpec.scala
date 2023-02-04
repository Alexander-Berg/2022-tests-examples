package ru.auto.api.managers.recalls

import org.mockito.Mockito.reset
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{RecallsUserCardResponse, RecallsUserCardsResponse}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.RecallsUserCardNotFound
import ru.auto.api.managers.carfax.CarfaxManager
import ru.auto.api.managers.carfax.CarfaxManager.VinResolveResult
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.{Paging, RequestParams}
import ru.auto.api.recalls.RecallsApiModel
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.recalls.RecallsClient
import ru.auto.api.util.ManagerUtils.SuccessResponse
import ru.auto.api.util.RequestImpl
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.UserEssentials
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class UserCardsManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {
  private val recallsClient = mock[RecallsClient]
  private val carfaxManager = mock[CarfaxManager]
  private val passportClient = mock[PassportClient]

  implicit private val trace: Traced = Traced.empty

  private val manager = new UserCardsManager(recallsClient, carfaxManager, passportClient)

  private val vin = "Z8T4C5S19BM005269"
  private val maskedVin = "Z8T4C5S1*BM*****9"
  private val paging = Paging.Default
  private val cardId = 1

  private val userEssentials = UserEssentials
    .newBuilder()
    .build()

  private val cardMock = RecallsApiModel.Card
    .newBuilder()
    .setVinCode(vin)

  private val card = cardMock
    .clearVinCode()
    .setVinCodeMasked(maskedVin)

  private val userCardsResponseMock = RecallsUserCardsResponse
    .newBuilder()
    .addCards(cardMock)
    .build()

  private val userCardsResponse = userCardsResponseMock.toBuilder
    .addCards(card)
    .build()

  private val userCardResponseMock = RecallsUserCardResponse
    .newBuilder()
    .setCard(cardMock)
    .build()

  private val userCardResponse = userCardResponseMock.toBuilder
    .setCard(cardMock)
    .build()

  before {
    reset(recallsClient, carfaxManager)
  }

  private def generateRequest: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(PrivateUserRefGen.next)
    r.setTrace(trace)
    r
  }

  def checkResponse[T](answer: Int, response: Future[T]): Assertion = {
    require(Set(1, 0).contains(answer))

    if (answer == 1) {
      response.futureValue shouldBe SuccessResponse
    } else {
      assertThrows[RecallsUserCardNotFound] {
        response.await
      }
    }
  }

  "UserCardsManager" should {
    "return cards" in {
      when(recallsClient.getUserCards(?, ?)(?)).thenReturnF(userCardsResponse)
      val response = manager.get(paging)(generateRequest).futureValue
      response shouldBe userCardsResponse
      response.getCards(0).getVinCode shouldBe empty
      response.getCards(0).getVinCodeMasked shouldBe maskedVin
    }

    "add card" in {
      when(carfaxManager.translateToVin(?)(?)).thenReturnF(VinResolveResult(vin, IdentifierType.VIN, None))
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)
      when(recallsClient.addUserCard(?, ?, ?)(?)).thenReturnF(userCardResponse)
      val response = manager.add(vin)(generateRequest).futureValue
      response shouldBe userCardResponse
      response.getCard.getVinCode shouldBe empty
      response.getCard.getVinCodeMasked shouldBe maskedVin
    }

    for (answer <- Set(1, 0)) {
      val cardType = if (answer == 1) "" else "missing"

      s"delete $cardType card" in {
        when(recallsClient.removeUserCard(?, ?)(?)).thenReturnF(answer)
        checkResponse(answer, manager.delete(cardId)(generateRequest))
      }

      s"subscribe to $cardType card" in {
        when(recallsClient.subscribeUserCard(?, ?)(?)).thenReturnF(answer)
        checkResponse(answer, manager.subscribe(cardId)(generateRequest))
      }

      s"unsubscribe from $cardType card" in {
        when(recallsClient.unsubscribeUserCard(?, ?)(?)).thenReturnF(answer)
        checkResponse(answer, manager.unsubscribe(cardId)(generateRequest))
      }
    }
  }
}
