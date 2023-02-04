package ru.yandex.vertis.promocoder.security

import ru.yandex.vertis.promocoder.WordSpecBase
import ru.yandex.vertis.promocoder.model.User
import ru.yandex.vertis.promocoder.util.CacheControl.NoCache
import ru.yandex.vertis.promocoder.util.UserContext
import scala.language.implicitConversions

/** Specs on [[SecurityProvider]]
  *
  * @author alex-kovalenko
  */
trait SecurityProviderSpec extends WordSpecBase {

  val superUser: User = "SuperUser"
  val user1: User = "u1"
  val user2: User = "u2"

  def securityProvider: SecurityProvider

  implicit def asRequestContext(user: User): UserContext =
    UserContext("rq", user, NoCache)

  import Grant.Modes.{Read => R, ReadWrite => RW}

  "SecurityProvider" should {
    "provide SecurityContext for RegularUser with grants OnUser" in {
      val context = securityProvider.get(user1)(user1).futureValue
      context.contains(Grant.OnUser(user1, R)) shouldBe true
      context.contains(Grant.OnUser(user1, RW)) shouldBe true
      context.contains(Grant.All) shouldBe false
    }

    "provide SecurityContext for SuperUser with Grant.All" in {
      val context = securityProvider.get(superUser)(superUser).futureValue
      context.contains(Grant.All) shouldBe true
    }

    "provide SecurityContext for SuperUser with grants as user" in {
      val context = securityProvider.get(user1)(superUser).futureValue
      context.contains(Grant.OnUser(user1, R)) shouldBe true
      context.contains(Grant.OnUser(user1, RW)) shouldBe true
      context.contains(Grant.All) shouldBe false
      context.contains(Grant.OnUser(user2, R)) shouldBe false
      context.contains(Grant.OnUser(user2, RW)) shouldBe false
    }
  }
}
