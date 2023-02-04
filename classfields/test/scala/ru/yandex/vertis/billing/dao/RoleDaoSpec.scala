package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.Uid
import ru.yandex.vertis.billing.service.RoleService

import scala.util.Success

/**
  * Specs on [[RoleDao]]
  */
trait RoleDaoSpec extends AnyWordSpec with Matchers {

  protected def roleDao: RoleDao

  import RoleService.Roles._

  "RoleDao" should {
    "provide no role" in {
      roleDao.get(Uid(1)) should be(Success(None))
    }
    "set SuperUser role to uid 1" in {
      roleDao.set(Uid(1), SuperUser) should be(Success(()))
      roleDao.get(Uid(1)) should be(Success(Some(SuperUser)))
    }
    "set RegularUser role to uid 1" in {
      roleDao.set(Uid(1), RegularUser) should be(Success(()))
      roleDao.get(Uid(1)) should be(Success(Some(RegularUser)))
    }
    "set SuperUser role again to uid 1" in {
      roleDao.set(Uid(1), SuperUser) should be(Success(()))
      roleDao.get(Uid(1)) should be(Success(Some(SuperUser)))
    }
    "set RegularUser role to uid 2" in {
      roleDao.set(Uid(2), RegularUser) should be(Success(()))
      roleDao.get(Uid(2)) should be(Success(Some(RegularUser)))
    }
  }
}
