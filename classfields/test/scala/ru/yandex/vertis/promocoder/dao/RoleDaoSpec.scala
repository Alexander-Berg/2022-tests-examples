package ru.yandex.vertis.promocoder.dao

import ru.yandex.vertis.promocoder.service.RoleService.Roles.{RegularUser, SuperUser}

import scala.concurrent.ExecutionContext

/** Specs on [[RoleDao]]
  *
  * @author alex-kovalenko
  */
trait RoleDaoSpec extends DaoSpecBase {

  protected def roleDao: RoleDao

  "RoleDao" should {
    import ExecutionContext.Implicits.global

    "provide no role" in {
      asyncTest {
        for {
          role <- roleDao.get("1")
          _ = role shouldBe empty
        } yield ()
      }

    }
    "set SuperUser role to uid 1" in {
      asyncTest {
        for {
          _ <- roleDao.set("1", SuperUser)
          got <- roleDao.get("1")
          _ = got should contain(SuperUser)
        } yield ()
      }
    }
    "set RegularUser role to uid 1" in {
      asyncTest {
        for {
          _ <- roleDao.set("1", RegularUser)
          got <- roleDao.get("1")
          _ = got should contain(RegularUser)
        } yield ()
      }
    }
    "set SuperUser role again to uid 1" in {
      asyncTest {
        for {
          _ <- roleDao.set("1", SuperUser)
          got <- roleDao.get("1")
          _ = got should contain(SuperUser)
        } yield ()
      }
    }
    "set RegularUser role to uid 2" in {
      asyncTest {
        for {
          _ <- roleDao.set("2", RegularUser)
          got <- roleDao.get("2")
          _ = got should contain(RegularUser)
        } yield ()
      }
    }
  }
}
