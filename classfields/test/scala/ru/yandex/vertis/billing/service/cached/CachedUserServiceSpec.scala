package ru.yandex.vertis.billing.service.cached

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.RoleService
import ru.yandex.vertis.billing.service.UserService.Position

/**
  * Specs on codec working on [[Position]].
  *
  * @author dimas
  */
class CachedUserServiceSpec extends AnyWordSpec with Matchers {

  import ru.yandex.vertis.billing.service.cached.impl.KryoCodec._

  "Codec" should {
    "round trip OnlyRole" in {
      val position = Position.OnlyRole(Uid(1), RoleService.Roles.RegularUser)
      val restored = deserialize[Position](serialize(position))
      restored should be(position)
    }

    "round trip HasBalanceClient" in {
      val client = gens.ClientGen.next
      val position = Position.HasBalanceClient(Uid(2), client)
      val restored = deserialize[Position](serialize(position))
      restored should be(position)
    }

    "round trip HasCustomers" in {
      val resource = PartnerRef("100500")
      val clients = gens.ClientGen.next(5).toSet
      val customers = clients.map(c => Customer(c, None, Seq(resource)))
      val position = Position.HasCustomers(Uid(2), customers)
      val restored = deserialize[Position](serialize(position))
      restored should be(position)
    }
  }
}
