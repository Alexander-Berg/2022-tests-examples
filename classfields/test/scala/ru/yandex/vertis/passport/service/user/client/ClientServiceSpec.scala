package ru.yandex.vertis.passport.service.user.client

import org.scalatest.{BeforeAndAfterAll, FreeSpec}
import ru.yandex.vertis.passport.model.{ConfirmationCode, Identity, RequestContext, UserId}
import ru.yandex.vertis.passport.service.user.UserService
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.UserIsAlreadyLinkedToClientException

trait ClientServiceSpec extends FreeSpec with SpecBase with BeforeAndAfterAll {
  def userService: UserService
  def clientService: ClientService

  implicit val ctx = RequestContext("test")

  private var userId: UserId = _
  private val email = ModelGenerators.emailAddress.next
  private val password = ModelGenerators.readableString.next
  private val clientId = ModelGenerators.clientId.next

  override protected def beforeAll() = {
    super.beforeAll()
    val source = ModelGenerators.userSource.next.copy(email = Some(email), password = password)
    val result = userService.create(source).futureValue
    userId = result.user.id
    userService
      .confirmIdentity(
        ConfirmationCode(Identity.Email(email), result.confirmCode.get)
      )
      .futureValue
  }

  "link to client" in {
    clientService.linkUser(clientId, userId, clientGroup = None).futureValue
    val user = userService.get(userId).futureValue.user
    user.profile.clientId shouldBe Some(clientId)
    user.profile.autoru.clientGroup shouldBe None
  }

  "link to client with group" in {
    clientService.linkUser(clientId, userId, clientGroup = Some("custom-group")).futureValue
    val user = userService.get(userId).futureValue.user
    user.profile.clientId shouldBe Some(clientId)
    user.profile.autoru.clientGroup shouldBe Some("custom-group")
  }

  "fail linking to client when already linked" in {
    val newClientId = ModelGenerators.clientId.next
    clientService
      .linkUser(newClientId, userId, clientGroup = None)
      .failed
      .futureValue shouldBe an[UserIsAlreadyLinkedToClientException]
  }

  "unlink from client" in {
    clientService.unlinkUser(userId).futureValue
    val user = userService.get(userId).futureValue.user
    user.profile.clientId shouldBe None
    user.profile.autoru.clientGroup shouldBe None
  }
}
