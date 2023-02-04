package ru.auto.api.routes.v1.dealer.requisites

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito.{reset, verify}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.managers.dealer.RequisitesManager
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators.{dealerAccessGroupWithGrantGen, DealerSessionResultGen, PhoneGen, ReadableStringGen}
import ru.auto.api.model.gen.NetGenerators
import ru.auto.api.services.MockedClients
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, ResourceAlias}
import ru.yandex.vertis.billing.model.proto.{Individual, Requisites, RequisitesIdResponse, RequisitesProperties, RequisitesResponse}

import scala.jdk.CollectionConverters._

class RequisitesHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {
  override lazy val requisitesManager: RequisitesManager = mock[RequisitesManager]
  override lazy val passportManager: PassportManager = mock[PassportManager]

  "GET /dealer/requisites" should {
    "return requisites given clientId in request" in {

      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong

      val indRequisites = Individual
        .newBuilder()
        .setEmail(NetGenerators.emailGen.next)
        .setFirstName(ReadableStringGen.next)
        .setMidName(ReadableStringGen.next)
        .setLastName(ReadableStringGen.next)
        .setPhone(PhoneGen.next)
        .build()
      val requisitesProps = RequisitesProperties.newBuilder().setIndividual(indRequisites).build()
      val requisites = Requisites.newBuilder().setId(1).setClientId(clientId).setProperties(requisitesProps).build()
      val response = RequisitesResponse.newBuilder().addAllRequisitesList(Seq(requisites).asJava).build()

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.SALON_REQUISITES, AccessLevel.READ_ONLY).next

      val sessionWithGrants = session.toBuilder
        .setAccess {
          AccessGrants
            .newBuilder()
            .setGroup(accessGroup.toBuilder.clearGrants())
            .addAllGrants(accessGroup.getGrantsList)
        }
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
      when(requisitesManager.getRequisites(eq(clientId))(?)).thenReturnF(response)

      Get(s"/1.0/dealer/requisites") ~> xAuthorizationHeader ~>
        addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
        route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[RequisitesResponse] shouldBe response
      }

      verify(requisitesManager).getRequisites(eq(clientId))(?)
      reset(requisitesManager, passportManager)
    }
  }

  "POST /dealer/requisites" should {
    "create requisites for given clientId" in {
      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong

      val requisites = RequisitesProperties
        .newBuilder()
        .setIndividual {
          Individual
            .newBuilder()
            .setEmail(NetGenerators.emailGen.next)
            .setFirstName(ReadableStringGen.next)
            .setMidName(ReadableStringGen.next)
            .setLastName(ReadableStringGen.next)
            .setPhone(PhoneGen.next)
        }
        .build()

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.SALON_REQUISITES, AccessLevel.READ_WRITE).next

      val sessionWithGrants = session.toBuilder
        .setAccess {
          AccessGrants
            .newBuilder()
            .setGroup(accessGroup.toBuilder.clearGrants())
            .addAllGrants(accessGroup.getGrantsList)
        }
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

      val response = RequisitesIdResponse.newBuilder().setId(1L).build()
      when(requisitesManager.createRequisites(?, ?)(?)).thenReturnF(response)

      Post(s"/1.0/dealer/requisites", requisites) ~> xAuthorizationHeader ~>
        addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
        route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[RequisitesIdResponse] shouldBe response
      }

      verify(requisitesManager).createRequisites(eq(clientId), eq(requisites))(?)
      reset(requisitesManager, passportManager)
    }

    "fail on invalid access level" in {
      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong

      val requisites = RequisitesProperties
        .newBuilder()
        .setIndividual {
          Individual
            .newBuilder()
            .setEmail(NetGenerators.emailGen.next)
            .setFirstName(ReadableStringGen.next)
            .setMidName(ReadableStringGen.next)
            .setLastName(ReadableStringGen.next)
            .setPhone(PhoneGen.next)
        }
        .build()

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.SALON_REQUISITES, AccessLevel.READ_ONLY).next

      val sessionWithGrants = session.toBuilder
        .setAccess {
          AccessGrants
            .newBuilder()
            .setGroup(accessGroup.toBuilder.clearGrants())
            .addAllGrants(accessGroup.getGrantsList)
        }
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

      Post(s"/1.0/dealer/requisites", requisites) ~> xAuthorizationHeader ~>
        addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
        route ~> check {
        status shouldBe StatusCodes.Forbidden
      }

      reset(passportManager)
    }

  }

  "PUT /1.0/dealer/requisites/{requisites_id}" should {
    "update payment requisites" in {
      reset(requisitesManager, passportManager)

      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong
      val requisitesId = 1L

      val expectedRequisitesId = RequisitesIdResponse
        .newBuilder()
        .setId(requisitesId)
        .build()

      val indRequisites = Individual
        .newBuilder()
        .setEmail(NetGenerators.emailGen.next)
        .setFirstName(ReadableStringGen.next)
        .setMidName(ReadableStringGen.next)
        .setLastName(ReadableStringGen.next)
        .setPhone(PhoneGen.next)
        .build()
      val requisitesProps = RequisitesProperties.newBuilder().setIndividual(indRequisites).build()

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.SALON_REQUISITES, AccessLevel.READ_WRITE).next

      val sessionWithGrants = session.toBuilder
        .setAccess {
          AccessGrants
            .newBuilder()
            .setGroup(accessGroup.toBuilder.clearGrants())
            .addAllGrants(accessGroup.getGrantsList)
        }
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
      when(requisitesManager.updateRequisites(?, ?, ?)(?))
        .thenReturnF(expectedRequisitesId)

      Put(s"/1.0/dealer/requisites/$requisitesId", requisitesProps) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
        route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[RequisitesIdResponse] shouldBe expectedRequisitesId
      }

      verify(requisitesManager).updateRequisites(eq(clientId), eq(requisitesId), eq(requisitesProps))(?)
      reset(passportManager)
    }

  }
}
