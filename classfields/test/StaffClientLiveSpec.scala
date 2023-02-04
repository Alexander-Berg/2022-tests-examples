package common.clients.staff.test

import common.clients.staff.StaffClientLive.StaffAuthConfig
import common.clients.staff.model.StaffPerson
import common.clients.staff.model.StaffPerson.{Images, PhoneKind, PhoneType, RichStaffPhone}
import common.clients.staff.{StaffClient, StaffClientLive}
import common.zio.sttp.Sttp
import common.zio.sttp.endpoint.Endpoint
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.StatusCode
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object StaffClientLiveSpec extends DefaultRunnableSpec {

  val testingBackend =
    AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespondWithCode(
      StatusCode.Ok,
      """{"uid":"1","phones":[{"kind":"common","protocol":"all","description":"","number":"+7 777 777-77-77","for_digital_sign":true,"type":"mobile","is_main":true}],"images":{"photo":"https://center-robot.yandex-team.ru/api/v1/user/login/photo.jpg","avatar":"https://center-robot.yandex-team.ru/api/v1/user/login/avatar.jpg"},"work_email":"login@yandex-team.ru","login":"login"}"""
    )

  val token = "token"
  val login = "login"
  val email = s"$login@yandex-team.ru"

  val person = StaffPerson(
    uid = "1",
    phones = Seq(RichStaffPhone("+7 777 777-77-77", PhoneType.Mobile, PhoneKind.Common, description = "")),
    workEmail = email,
    images = Images(
      "https://center-robot.yandex-team.ru/api/v1/user/login/photo.jpg",
      "https://center-robot.yandex-team.ru/api/v1/user/login/avatar.jpg"
    )
  )

  val sttp = Sttp.fromStub(testingBackend)
  val staffConfig = ZIO.succeed(Endpoint("staff-api.test.yandex-team.ru", "/v3", 443, "https")).toLayer
  val staffAuth = ZIO.succeed(StaffAuthConfig(token)).toLayer
  val staffClient = sttp ++ staffConfig ++ staffAuth >>> StaffClientLive.live

  override def spec = {
    suite("StaffClientLive")(
      testM("do request, get response") {
        (for {
          result <-
            ZIO.serviceWith[StaffClient.Service](_.getPersonByLogin(login))
        } yield assert(result)(equalTo(person))).provideCustomLayer(staffClient)
      }
    )
  }
}
