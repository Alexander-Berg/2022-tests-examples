package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.AclResponse.{AccessLevel, Group, Resource, ResourceAlias}
import ru.auto.cabinet.dao.entities.acl.{
  AclGroup,
  AclGroupResource,
  AclResource
}
import ru.auto.cabinet.dao.jdbc.AccessDao
import ru.auto.cabinet.model.CustomerTypes
import ru.auto.cabinet.service.BadRequestException
import ru.auto.cabinet.test.JdbcSpecTemplate

class AccessDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  private val dao = new AccessDao(office7Database, office7Database)

  "AccessDao.getGroupGrants" should "get group grants" in {
    val expectedResult = Map(
      AclGroup(
        id = 2L,
        "Custom",
        CustomerTypes.Client,
        Some(20101),
        isEditable = true) ->
        List(
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.OFFERS,
              AccessLevel.READ_ONLY)),
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.TARIFFS,
              AccessLevel.READ_ONLY))
        )
    )

    for {
      result <- dao.getGroupGrants(groupId = 2L)
    } yield result shouldBe expectedResult
  }

  "AccessDao.getGroupGrants" should "return empty map on unknown group" in {
    for {
      result <- dao.getGroupGrants(groupId = 100L)
    } yield result shouldBe Map()
  }

  "AccessDao.getClientGroupsGrants" should "get client groups with grants" in {
    val expectedResult = Map(
      AclGroup(
        id = 1L,
        "Admins",
        CustomerTypes.Client,
        customerId = None,
        isEditable = false) ->
        List(
          Right(
            AclGroupResource(
              groupId = 1L,
              ResourceAlias.OFFERS,
              AccessLevel.READ_WRITE)),
          Right(
            AclGroupResource(
              groupId = 1L,
              ResourceAlias.TARIFFS,
              AccessLevel.READ_WRITE))
        ),
      AclGroup(
        id = 2L,
        "Custom",
        CustomerTypes.Client,
        Some(20101),
        isEditable = true) ->
        List(
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.OFFERS,
              AccessLevel.READ_ONLY)),
          Right(
            AclGroupResource(
              groupId = 2L,
              ResourceAlias.TARIFFS,
              AccessLevel.READ_ONLY))
        )
    )

    for {
      result <- dao.getCustomerGroupsGrants(20101, CustomerTypes.Client)
    } yield result shouldBe expectedResult
  }

  "AccessDao.getAllResources" should "get all resources" in {
    val expectedResources = List(
      Right(
        AclResource(
          ResourceAlias.DASHBOARD,
          "Dashboard",
          "Dashboard resource")),
      Right(AclResource(ResourceAlias.OFFERS, "Offers", "Offers resource"))
    )

    for {
      result <- dao.getAllResources()
    } yield result shouldBe expectedResources
  }

  "AccessDao.getGroup" should "get group by id" in {
    val expected = AclGroup(
      id = 2L,
      name = "Custom",
      CustomerTypes.Client,
      customerId = Some(20101L),
      isEditable = true)

    for {
      result <- dao.getGroup(2L)
    } yield result shouldBe Some(expected)
  }

  "AccessDao.getGroup" should "return None on group not found" in {
    for {
      result <- dao.getGroup(-1L)
    } yield result shouldBe None
  }

  "AccessDao.deleteGroup" should "delete group by id" in {
    val groupId = 2L
    for {
      beforeDeleteResult <- dao.getGroup(groupId)
      deleteResult <- dao.deleteGroup(groupId)
      afterDeleteResult <- dao.getGroup(groupId)
    } yield {
      beforeDeleteResult.map(_.id) shouldBe Some(groupId)

      deleteResult.id shouldBe groupId
      deleteResult.rowCount shouldBe 1

      afterDeleteResult shouldBe None
    }
  }

  "AccessDao.putCustomerGroup" should "create group" in {
    val group =
      Group
        .newBuilder()
        .setName("New group")
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.OFFERS)
            .setAccess(AccessLevel.READ_WRITE)
        }

    val clientId = 111L

    for {
      beforePutResult <- dao.getCustomerGroupsGrants(
        clientId,
        CustomerTypes.Client)
      _ <- dao.putCustomerGroup(clientId, CustomerTypes.Client, group.build())
      afterPutResult <- dao.getCustomerGroupsGrants(
        clientId,
        CustomerTypes.Client)
    } yield {
      beforePutResult.values.size shouldBe 1
      afterPutResult.values.size shouldBe 2

      val created =
        afterPutResult.find { case (group, _) =>
          group.name == "New group"
        }

      created.get._1.name shouldBe "New group"
      created.get._2.size shouldBe 1
    }
  }

  "AccessDao.putCustomerGroup" should "update group" in {
    val group =
      Group
        .newBuilder()
        .setName("New group")
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.OFFERS)
            .setAccess(AccessLevel.READ_WRITE)
        }

    val clientId = 111L

    for {
      beforePutResult <- dao.getCustomerGroupsGrants(
        clientId,
        CustomerTypes.Client)
      createResult <- dao.putCustomerGroup(
        clientId,
        CustomerTypes.Client,
        group.build())

      _ <- dao.putCustomerGroup(
        clientId,
        CustomerTypes.Client,
        group
          .setId(createResult.id)
          .setName("Brand new group")
          .build())

      afterPutResult <- dao.getCustomerGroupsGrants(
        clientId,
        CustomerTypes.Client)
    } yield {
      beforePutResult.values.size shouldBe 2
      afterPutResult.values.size shouldBe 3

      val created =
        afterPutResult.find { case (group, _) =>
          group.id == createResult.id
        }

      created.get._1.name shouldBe "Brand new group"
      created.get._2.size shouldBe 1
    }
  }

  "AccessDao.validateGroup" should "validate group" in {
    val group =
      Group
        .newBuilder()
        .setName("Access group")
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.FEEDS)
            .setAccess(AccessLevel.READ_ONLY)
        }
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.CHATS)
            .setAccess(AccessLevel.READ_WRITE)
        }
        .build()

    for {
      result <- dao.validateGroup(group)
    } yield result shouldBe (())
  }

  "AccessDao.validateGroup" should "fail on group without name" in {
    val group =
      Group
        .newBuilder()
        .setId(1L)
        .build()

    for {
      failure <- dao.validateGroup(group).failed
    } yield failure shouldBe a[BadRequestException]
  }

  "AccessDao.validateGroup" should "fail on group without grants" in {
    val group =
      Group
        .newBuilder()
        .setName("Access group")
        .build()

    for {
      failure <- dao.validateGroup(group).failed
    } yield failure shouldBe a[BadRequestException]
  }

  "AccessDao.validateGroup" should "fail on group grants without access" in {
    val group =
      Group
        .newBuilder()
        .setName("Access group")
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.FEEDS)
            .setAccess(AccessLevel.READ_ONLY)
        }
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.CHATS)
        }
        .build()

    for {
      failure <- dao.validateGroup(group).failed
    } yield failure shouldBe a[BadRequestException]
  }

  "AccessDao.validateGroup" should "fail on group grants without alias" in {
    val group =
      Group
        .newBuilder()
        .setName("Access group")
        .addGrants {
          Resource
            .newBuilder()
            .setAlias(ResourceAlias.FEEDS)
            .setAccess(AccessLevel.READ_ONLY)
        }
        .addGrants {
          Resource
            .newBuilder()
            .setAccess(AccessLevel.READ_WRITE)
        }
        .build()

    for {
      failure <- dao.validateGroup(group).failed
    } yield failure shouldBe a[BadRequestException]
  }

}
