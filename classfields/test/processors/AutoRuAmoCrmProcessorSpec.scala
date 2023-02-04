package auto.dealers.amoyak.logic.processors

import auto.dealers.amoyak.consumers.logic.AutoRuAmoCrmProcessor

import java.time.{OffsetDateTime, ZoneOffset, ZonedDateTime}
import cats.syntax.option._
import ru.auto.amoyak.internal_service_model.{AmoyakDto, LoyaltyData, Office7Data, SalesmanData}
import ru.auto.amoyak.internal_service_model.Office7Data.{ClientDealers, Clients, ClientsComments, Moderation}
import auto.dealers.amoyak.model.{AmoJson, AmoMessageType, ClientType, Tariff, Tariffs}
import ru.auto.amoyak.common_service_model._
import auto.dealers.amoyak.model.model.defaultAmoDateTimeFormatter
import auto.dealers.amoyak.storage.testkit.AmoIntegratorClientMock
import zio.test.{DefaultRunnableSpec, _}
import zio.test.environment.TestEnvironment
import zio.test.mock.MockClock
import zio.test.Assertion._
import zio.test.mock.Expectation._
import auto.dealers.amoyak.model.AmoCrmPatch.{CabinetClientPatch, CabinetCompanyPatch, LoyaltyPatch, SalesmanPatch}

object AutoRuAmoCrmProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    val clockMock = MockClock.CurrentDateTime {
      value(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
    }

    suite("AutoRuAmoCrmProcessor")(
      testM("process should send client Office7Data to AmoIntegratorClient") {
        val origin = AmoyakDto(
          clientId = 1L,
          source = "source",
          ts = None,
          payload = AmoyakDto.Payload.Office7Data(
            Office7Data(
              clients = Clients(
                clientId = 1L,
                origin = "origin".some,
                responsibleManagerEmail = "manager@mail".some,
                customerType = CustomerType.CLIENT,
                agency = 1L.some,
                headCompany = 1L.some,
                status = ClientStatus.ACTIVE,
                createdTime = "2020-01-01 03:00:00".some,
                firstModeration = false.some,
                name = "name".some,
                city = "city".some,
                region = "region".some,
                address = "address".some,
                phoneNumber = Seq("1", "2"),
                website = "website".some
              ).some,
              moderation = Moderation(1, false.some).some,
              clientDealers = ClientDealers(Seq("1")).some,
              clientsComments = ClientsComments(moderationComment = "comment".some).some
            )
          )
        )

        val expected: AmoJson[CabinetClientPatch] with AmoMessageType = CabinetClientPatch(
          clientId = 1L,
          clientType = ClientType.Client,
          origin = "origin",
          name = "name".some,
          agency = 1L.some,
          headCompany = 1L.some,
          responsibleManagerEmail = "manager@mail".some,
          status = "active",
          region = "region".some,
          firstModeration = false,
          timestamp = ZonedDateTime.parse("2020-01-01 03:00:00", defaultAmoDateTimeFormatter)
        )

        val amoIntegratorClientMock =
          AmoIntegratorClientMock.PushMessage
            .of[AmoJson[CabinetClientPatch] with AmoMessageType](equalTo(expected), unit)

        assertM(AutoRuAmoCrmProcessor.process(origin))(isUnit)
          .provideLayer((clockMock ++ amoIntegratorClientMock) >>> AutoRuAmoCrmProcessor.live)
      },
      testM("process should send company Office7Data to AmoIntegratorClient") {
        val origin = AmoyakDto(
          clientId = 1L,
          customerType = CustomerType.COMPANY_GROUP,
          source = "source",
          ts = None,
          payload = AmoyakDto.Payload.Office7Data(
            Office7Data(
              clients = Clients(
                clientId = 1L,
                customerType = CustomerType.COMPANY_GROUP,
                createdTime = "2020-01-01 03:00:00".some,
                name = "company name".some
              ).some
            )
          )
        )

        val expected: AmoJson[CabinetCompanyPatch] with AmoMessageType = CabinetCompanyPatch(
          clientId = 1L,
          name = "company name".some,
          createdTime = ZonedDateTime.parse("2020-01-01 03:00:00", defaultAmoDateTimeFormatter).some,
          timestamp = ZonedDateTime.parse("2020-01-01 03:00:00", defaultAmoDateTimeFormatter)
        )

        val amoIntegratorClientMock =
          AmoIntegratorClientMock.PushMessage
            .of[AmoJson[CabinetCompanyPatch] with AmoMessageType](equalTo(expected), unit)

        assertM(AutoRuAmoCrmProcessor.process(origin))(isUnit)
          .provideLayer((clockMock ++ amoIntegratorClientMock) >>> AutoRuAmoCrmProcessor.live)
      }
    )
  }
}
