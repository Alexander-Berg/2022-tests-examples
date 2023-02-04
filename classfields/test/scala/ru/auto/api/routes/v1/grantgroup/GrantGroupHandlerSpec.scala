package ru.auto.api.routes.v1.grantgroup

import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import akka.http.scaladsl.model.{HttpHeader, MediaTypes, StatusCodes, Uri}
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel._
import ru.auto.api.auth.{Grants, Groups}
import ru.auto.api.managers.chat.ChatUserRef
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.routes.v1.grantgroup.GrantGroupHandlerSpec._
import ru.auto.api.services.MockedClients
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Specs on [[GrantGroupHandlerSpec]].
  *
  * @author alnovis
  */
class GrantGroupHandlerSpec extends ApiSpec with MockedClients with BeforeAndAfter {

  override lazy val passportManager: PassportManager = mock[PassportManager]

  before {
    when(passportManager.getClientId(?)(?)).thenReturn(None)
  }

  "/grants" should {
    val base = "/1.0/grants"

    "get grant groups" in {
      val user = ChatUserRef.from(PrivateUserRefGen.next)
      val response = GrantsListResponse
        .newBuilder()
        .addAllGrant(
          Grants.all.iterator
            .map(grant =>
              GrantsListResponse.Grant
                .newBuilder()
                .setName(grant.name)
                .setDescription(grant.description)
                .build()
            )
            .toList
            .asJava
        )
        .addAllGroup(
          Groups.all.iterator
            .map(group =>
              GrantsListResponse.Group
                .newBuilder()
                .setName(group.name)
                .addAllGrant(group.grants.map(_.name).asJava)
                .build()
            )
            .toList
            .asJava
        )
        .setStatus(ResponseStatus.SUCCESS)
        .build()

      SupportedAccepts.foreach { accept =>
        Get(Uri(base)) ~>
          addHeader(accept) ~>
          addHeader(testAuthorizationHeader) ~>
          addHeader(userHeader(user)) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[GrantsListResponse] shouldBe response
            responseAs[GrantsListResponse].getGroupCount shouldBe 6
            responseAs[GrantsListResponse].getGrantCount shouldBe 92
            responseAs[GrantsListResponse].getGroupList.asScala.toSeq
              .filter(_.getName.equals("Каталог транспортных средств"))
              .flatMap(_.getGrantList.asScala.toSeq) should contain theSameElementsAs Seq("catalog", "breadcrumbs")
            responseAs[GrantsListResponse].getGroupList.asScala.toSeq
              .filter(_.getName.equals("Поиск по базе объявлений Авто.ру"))
              .flatMap(_.getGrantList.asScala.toSeq) should contain theSameElementsAs Seq(
              "search",
              "catalog",
              "breadcrumbs"
            )
            responseAs[GrantsListResponse].getStatus shouldBe ResponseStatus.SUCCESS
          }
      }
    }
  }
}

object GrantGroupHandlerSpec {
  implicit def asFuture[A](value: A): Future[A] = Future.successful(value)

  val AcceptJson = Accept(MediaTypes.`application/json`)
  val AcceptProtobuf = Accept(Protobuf.mediaType)

  val SupportedAccepts = Seq(AcceptJson, AcceptProtobuf)

  def userHeader(user: ChatUserRef): HttpHeader =
    RawHeader("x-uid", user.toRaw)
}
