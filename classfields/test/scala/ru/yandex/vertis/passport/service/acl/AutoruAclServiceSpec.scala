package ru.yandex.vertis.passport.service.acl

import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruAclDao
import ru.yandex.vertis.passport.model.UserId
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import slick.jdbc.MySQLProfile.api._

/**
  *
  * @author zvez
  */
class AutoruAclServiceSpec extends FreeSpec with SpecBase with MySqlSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao = new AutoruAclDao(dbs.legacyAcl)
  val aclService = new AutoruAclService(dao)

  //from legacy.acl.sql
  val LoginAsId = 1
  val LoginAsDenyId = 2

  "AutoruAclService" - {
    "canImpersonate" - {
      "should be false for common folks" in {
        val userId = ModelGenerators.userId.next
        aclService.canImpersonate(userId).futureValue shouldBe false
      }

      "should be true if user is part of a group with access to 'users8.login_as'" in {
        val userId = ModelGenerators.userId.next
        val groupId = 5
        addResourceToGroup(groupId, LoginAsId)
        addUserToGroup(groupId, userId)

        aclService.canImpersonate(userId).futureValue shouldBe true
      }

      "should be false if user is part of a group without 'users8.login_as'" in {
        val userId = ModelGenerators.userId.next
        val groupId = 6
        addResourceToGroup(groupId, LoginAsDenyId)
        addUserToGroup(groupId, userId)

        aclService.canImpersonate(userId).futureValue shouldBe false
      }

      "should be true if user has direct access to 'users8.login_as'" in {
        val userId = ModelGenerators.userId.next
        addResourceToUser(userId, LoginAsId)

        aclService.canImpersonate(userId).futureValue shouldBe true
      }
    }

    "canBeImpersonated" - {
      "should be true for common folks" in {
        val userId = ModelGenerators.userId.next
        aclService.canBeImpersonated(userId).futureValue shouldBe true
      }

      "should be false if user has access to 'users8.login_as.deny'" in {
        val userId = ModelGenerators.userId.next
        val groupId = 7
        addResourceToGroup(groupId, LoginAsDenyId)
        addUserToGroup(groupId, userId)

        aclService.canBeImpersonated(userId).futureValue shouldBe false
      }
    }

    def addResourceToGroup(groupId: Int, resourceId: Int): Unit = {
      dbs.legacyAcl
        .run(
          sqlu"INSERT INTO groups_resources(group_id, resource_id) VALUES ($groupId, $resourceId)"
        )
        .futureValue
    }

    def addResourceToUser(userId: UserId, resourceId: Int): Unit = {
      dbs.legacyAcl
        .run(
          sqlu"INSERT INTO resources_users(user_id, resource_id) VALUES ($userId, $resourceId)"
        )
        .futureValue
    }

    def addUserToGroup(groupId: Int, userId: UserId): Unit = {
      dbs.legacyAcl
        .run(
          sqlu"INSERT INTO groups_users(user_id, group_id) VALUES ($userId, $groupId)"
        )
        .futureValue
    }
  }

}
