package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.entities.UserRoles
import ru.auto.cabinet.dao.jdbc.RolesDao
import ru.auto.cabinet.model.{Role, RoleRow, Uid}
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.test.JdbcSpecTemplate
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _, _}

/** Specs [[RolesDao]]
  */
class RolesDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  implicit val instr = new EmptyInstr("test")
  val dao = new RolesDao(office7Database, office7Database)

  def user1Id: Uid = 1

  val dummy = RoleRow(user1Id, Role.Manager.value)

  "JdbcRolesDao" should "get user roles" in {
    for {
      _ <- office7Database.run(UserRoles.table += dummy)
      roles <- dao.userRoles(user1Id)
      _ <- office7Database.run(
        UserRoles.table.filter(_.userId === user1Id).delete)
    } yield {
      roles.userId should be(user1Id)
      roles.roles should be(Vector(Role.Manager))
    }
  }

  it should "return empty roles if not exist" in {
    for {
      roles <- dao.userRoles(user1Id)
    } yield {
      roles.userId should be(user1Id)
      roles.roles shouldBe empty
    }
  }

}
