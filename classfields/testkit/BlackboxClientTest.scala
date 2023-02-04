package common.clients.blackbox.testkit

import common.clients.blackbox.BlackboxClient
import common.clients.blackbox.BlackboxClient.DefaultIp
import common.clients.blackbox.model.{
  Email,
  Emails,
  OAuthCheckResponse,
  OAuthResult,
  Phone,
  PhoneAttribute,
  Phones,
  UserInfo,
  UserInfoResponse
}
import common.tvm.model.UserTicket.TicketBody
import zio.{Task, ULayer, ZLayer}

object BlackboxClientTest extends BlackboxClient.Service {

  /** Мок для тестирования интеграции с блекбоксом.
    * Возвращает одного пользователя для любого запроса.
    * Для отрицательных id не возвращает ничего.
    */
  val Test: ULayer[BlackboxClient.BlackboxClient] = ZLayer.succeed(BlackboxClientTest)

  val TestUser: UserInfo = UserInfo(
    0,
    Some("test-user"),
    Some("avatar"),
    Some(0),
    List(Email(address = "test@yandex.ru")),
    List(
      Phone(Map(PhoneAttribute.E164 -> "+70000000003")),
      Phone(Map(PhoneAttribute.E164 -> "+70000000002", PhoneAttribute.Confirmed -> "1500000000")),
      Phone(Map(PhoneAttribute.E164 -> "+70000000001", PhoneAttribute.Confirmed -> "1600000000"))
    ),
    publicId = Some("publicId")
  )

  override def userInfoByTicket(
      userTicket: TicketBody,
      uid: Long,
      avatars: Boolean,
      publicName: Boolean,
      phones: Option[Phones],
      phoneAttributes: Seq[PhoneAttribute],
      emails: Option[Emails],
      getPublicId: Boolean): Task[Seq[UserInfoResponse]] = {
    Task.succeed(List(TestUser.copy(id = uid)).filter(_.id >= 0))
  }

  override def userInfoByIds(
      uids: Seq[Long],
      userIp: String = DefaultIp,
      avatars: Boolean,
      publicName: Boolean,
      phones: Option[Phones],
      phoneAttributes: Seq[PhoneAttribute],
      emails: Option[Emails],
      getPublicId: Boolean): Task[Seq[UserInfoResponse]] = {
    Task.succeed(uids.map(uid => TestUser.copy(id = uid)).filter(_.id >= 0))
  }

  override def getUserByPublicId(
      publicId: String,
      userIp: String,
      avatars: Boolean,
      publicName: Boolean,
      phones: Option[Phones],
      phoneAttributes: Seq[PhoneAttribute],
      emails: Option[Emails],
      getPublicId: Boolean): Task[Seq[UserInfoResponse]] = {
    Task.succeed(Seq(TestUser.copy(id = 34589729865L, publicId = Some(publicId))))
  }

  override def checkOAuth(token: String, userIp: String, getUserTicket: Boolean): Task[OAuthCheckResponse] = {
    Task.succeed {
      OAuthResult(Some("test_token"))
    }
  }
}
