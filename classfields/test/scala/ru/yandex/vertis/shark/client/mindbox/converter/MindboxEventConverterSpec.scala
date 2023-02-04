package ru.yandex.vertis.shark.client.mindbox.converter

import java.net.URL
import java.time.Instant
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.common_model.PhotoClass
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.shark.client.mindbox.MindboxClient.Event
import ru.yandex.vertis.shark.model.Entity.NameEntity
import ru.yandex.vertis.shark.proto.model.CreditApplication.Communication.AutoruExternal
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.AutoruCreditApplication.ExternalCommunication.ClaimEntity
import ru.yandex.vertis.shark.model.event.CreditApplicationAutoruExternalCommunicationEvent
import ru.yandex.vertis.shark.model.event.CreditApplicationAutoruExternalCommunicationEvent._
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.AutoUser
import zio.test.Assertion.equalTo
import zio.test.DefaultRunnableSpec

object MindboxEventConverterSpec extends DefaultRunnableSpec {

  import MindboxEventConverter._

  private val ts: Instant = Instant.now()
  private val creditApplicationId: CreditApplicationId = "credit-application-id".taggedWith[Tag.CreditApplicationId]

  private val event: CreditApplicationAutoruExternalCommunicationEvent =
    CreditApplicationAutoruExternalCommunicationEvent(
      timestamp = ts,
      requestId = "request-id".some,
      idempotencyKey = "idempotency-key",
      externalCommunication = AutoruCreditApplication.ExternalCommunication.forTest(
        updated = ts,
        lastEvent = ts.some,
        eventScheduledAt = ts.some,
        completenessState = AutoruExternal.CompletenessState.MINIMUM,
        objectCommunicationState = AutoruExternal.ObjectCommunicationState.SELECTED,
        claimEntities = Seq(
          ClaimEntity.forTest(
            creditProductId = "tinkoff-1".taggedWith[Tag.CreditProductId],
            state = AutoruExternal.ClaimCommunicationState.SENT,
            claimState = proto.CreditApplication.Claim.ClaimState.NEW.some
          )
        )
      ),
      creditApplicationId = creditApplicationId,
      creditApplicationRequirements = CreditApplication
        .Requirements(
          maxAmount = 5000000L.taggedWith[Tag.MoneyRub],
          initialFee = 1000000L.taggedWith[Tag.MoneyRub],
          termMonths = 36.taggedWith[Tag.MonthAmount],
          geobaseIds = Seq(1L.taggedWith[zio_baker.Tag.GeobaseId])
        )
        .some,
      creditApplicationState = proto.CreditApplication.State.ACTIVE,
      objectPayload = ObjectPayload(
        offerUrl = new URL("http://host.com/path/offer-id"),
        priceRub = 6000000L.taggedWith[Tag.MoneyRub],
        mark = "MERCEDES",
        model = "A-Klasse",
        photos = Seq(
          ObjectPayload.Photo(
            sizes = Map(
              ObjectPayload.Size320x240 -> new URL("http://host.com/photo/1.jpg"),
              ObjectPayload.Size1200x900n -> new URL("http://host.com/photo/2.jpg"),
              "another-size" -> new URL("http://host.com/photo/3.jpg")
            ),
            photoClass = PhotoClass.AUTO_VIEW_3_4_BACK_LEFT
          )
        )
      ).some,
      userPayload = UserPayload(
        user = AutoUser("5".taggedWith[zio_baker.Tag.UserId]).some,
        name = NameEntity(
          name = "Имя".taggedWith[Tag.Name],
          surname = "Фамилия".taggedWith[Tag.Surname],
          patronymic = "Отчество".taggedWith[Tag.Patronymic].some
        ).some,
        phone = Phone("+79991234567").some,
        email = Email("mailbox@host.com").some
      )
    )

  override def spec =
    suite("MindboxEventConverter") {
      val expected = Event(
        customer = Event.Customer(
          ids = Event.Customer.Ids(autoruUserId = "5", clientRequestId = creditApplicationId.some),
          lastName = "Фамилия",
          firstName = "Имя",
          middleName = "Отчество",
          email = "mailbox@host.com",
          mobilePhone = "79991234567",
          subscriptions = DefaultSubscriptions
        ),
        order = Event.Order(
          ids = Event.Order.Ids(creditApplicationId),
          customFields = Event.Order.CustomFields(
            requestEnv = "testing",
            requestOfferUrl = "http://host.com/path/offer-id".some,
            requestPriceRub = 6000000L.some,
            requestMark = "MERCEDES".some,
            requestModel = "A-Klasse".some,
            requestMaxAmount = 5000000L.some,
            requestInitialFee = 1000000L.some,
            requestTermMonths = 36.some,
            requestGeobaseIds = Seq(1L),
            requestState = proto.CreditApplication.State.ACTIVE,
            requestCompletnessState = AutoruExternal.CompletenessState.MINIMUM,
            requestObjectState = AutoruExternal.ObjectCommunicationState.SELECTED,
            photo320x240Url = "http://host.com/photo/1.jpg".some,
            photo1200x900Url = "http://host.com/photo/2.jpg".some
          ),
          lines = Seq(
            Event.Order.Line(
              product = Event.Order.Line.ProductItem(
                Event.Order.Line.ProductItem.Ids("tinkoff-1")
              ),
              status = proto.CreditApplication.Claim.ClaimState.NEW.toString
            )
          )
        ),
        executionDateTimeUtc = ts
      )
      val result = event.toClient(Environments.Testing)
      test("toEvent")(zio.test.assert(result)(equalTo(expected)))
    }
}
