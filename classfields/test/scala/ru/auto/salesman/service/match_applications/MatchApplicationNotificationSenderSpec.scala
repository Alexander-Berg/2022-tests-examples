package ru.auto.salesman.service.match_applications

import java.time.{Instant, OffsetDateTime, ZoneId}

import ru.auto.api.match_applications.MatchApplicationsResponseModel.MatchApplication
import ru.auto.match_maker.model.api.ApiModel
import ru.auto.salesman.client.PublicApiClient.MatchMakerMatchApplicationNotFoundException
import ru.auto.salesman.client.{PublicApiClient, SenderClient}
import ru.auto.salesman.dao.ClientDao.ForId
import ru.auto.salesman.dao.ClientSubscriptionsDao.ClientSubscription
import ru.auto.salesman.dao.{ClientDao, ClientSubscriptionsDao}
import ru.auto.salesman.exceptions.ClientNotFoundException
import ru.auto.salesman.model.{CityId, Client, ClientStatuses, RegionId}
import ru.auto.salesman.service.match_applications.MatchApplicationNotificationSenderSpec._
import ru.auto.salesman.service.match_applications.MatchApplicationServiceSpec.now
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.MatchApplicationGenerators.{
  MatchApplicationIdGen,
  _
}
import ru.auto.salesman.util.AutomatedContext
import zio.test.environment.{TestClock, TestEnvironment}

class MatchApplicationNotificationSenderSpec extends BaseSpec {
  private val publicApiClient = mock[PublicApiClient]
  private val clientSubscriptionsDao = mock[ClientSubscriptionsDao]
  private val senderClient = mock[SenderClient]
  private val clientDao = mock[ClientDao]

  private val sender = new MatchApplicationsNotificationSender(
    publicApiClient,
    clientSubscriptionsDao,
    senderClient,
    clientDao
  )

  private val rc = AutomatedContext("test")

  "MatchApplicationNotificationService" should {
    "pass complete email args map" in {
      (clientSubscriptionsDao.byClientWithCategory _)
        .expects(clientId, "match-applications")
        .returningZ(Seq(createClientSubscription))
      (publicApiClient.getMatchApplication _)
        .expects(matchApplicationId, clientId)
        .returningZ(createMatchApplication)
      (clientDao.get _)
        .expects(ForId(clientId))
        .returningZ(List(createClient))

      val expectedEmailArgs = Map(
        "mark" -> mark,
        "model" -> model,
        "name" -> userName,
        "phone_number" -> userPhone,
        "dealer_name" -> dealerName,
        "trade_in_possible" -> "true",
        "credit_possible" -> "true"
      )
      (senderClient.sendLetter _)
        .expects("new_order_newcar", clientEmail, expectedEmailArgs)
        .returningZ(unit)

      val instant = Instant.ofEpochMilli(now.getMillis)
      val zoneId = ZoneId.of(now.getZone.getID, ZoneId.SHORT_IDS)
      (TestClock.setDateTime(OffsetDateTime.ofInstant(instant, zoneId)) *>
        sender
          .sendNotificationWithEmail(clientId, matchApplicationId)
          .provideRc(rc))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
    }

    "fail if none match application found by id" in {
      (clientSubscriptionsDao.byClientWithCategory _)
        .expects(clientId, "match-applications")
        .returningZ(Seq(createClientSubscription))
      (publicApiClient.getMatchApplication _)
        .expects(matchApplicationId, clientId)
        .throwingZ(
          MatchMakerMatchApplicationNotFoundException(
            matchApplicationId,
            clientId
          )
        )
      (clientDao.get _)
        .expects(ForId(clientId))
        .returningZ(List(createClient))

      sender
        .sendNotificationWithEmail(clientId, matchApplicationId)
        .failure
        .exception shouldBe MatchMakerMatchApplicationNotFoundException(
        matchApplicationId,
        clientId
      )
    }

    "fail if none client found by id" in {
      (clientSubscriptionsDao.byClientWithCategory _)
        .expects(clientId, "match-applications")
        .returningZ(Seq(createClientSubscription))
      (publicApiClient.getMatchApplication _)
        .expects(matchApplicationId, clientId)
        .returningZ(createMatchApplication)
      (clientDao.get _)
        .expects(ForId(clientId))
        .throwingZ(ClientNotFoundException(clientId))

      sender
        .sendNotificationWithEmail(clientId, matchApplicationId)
        .failure
        .exception shouldBe ClientNotFoundException(clientId)
    }

    "do nothing if subscription list is empty" in {
      (clientSubscriptionsDao.byClientWithCategory _)
        .expects(clientId, "match-applications")
        .returningZ(Nil)
      (publicApiClient.getMatchApplication _)
        .expects(*, *)
        .never()
      (clientDao.get _)
        .expects(*)
        .never()

      sender
        .sendNotificationWithEmail(clientId, matchApplicationId)
        .success
        .value shouldBe (())
    }
  }
}

object MatchApplicationNotificationSenderSpec {
  private val clientId = 123L
  private val clientEmail = "email@yandex.ru"
  private val mark = "mark"
  private val model = "model"
  private val userName = "username"
  private val userPhone = "7987654321"
  private val dealerName = "ya_dealer"
  private val matchApplicationId = MatchApplicationIdGen.next

  private def createMatchApplication: MatchApplication = {
    val userInfo =
      ApiModel.UserInfo
        .newBuilder()
        .setName(userName)
        .setPhone(userPhone)
        .setCreditInfo(ApiModel.CreditInfo.newBuilder().setIsPossible(true))

    val userProposal =
      MatchApplication.UserProposal.newBuilder().setMark(mark).setModel(model)

    MatchApplication
      .newBuilder()
      .setUserInfo(userInfo)
      .setUserProposal(userProposal)
      .setUserCarInfo(MatchApplication.UserCarInfo.getDefaultInstance)
      .build()
  }

  private def createClient: Client =
    Client(
      clientId,
      None,
      None,
      None,
      RegionId(1L),
      CityId(21735L),
      ClientStatuses.Active,
      Set(),
      firstModerated = true,
      Some(dealerName),
      paidCallsAvailable = true,
      priorityPlacement = true
    )

  private def createClientSubscription: ClientSubscription =
    ClientSubscription(0L, clientId, "match-applications", clientEmail)
}
