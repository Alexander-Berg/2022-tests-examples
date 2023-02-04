package ru.auto.api.routes

import akka.http.scaladsl.testkit.ScalatestRouteTest
import ru.auto.api.BaseSpec
import ru.auto.api.model._
import ru.auto.api.routes.BootstrapHandler.Access
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, Resource, ResourceAlias}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils.RichBoolean

import scala.jdk.CollectionConverters._

class BootstrapHandlerSpec extends BaseSpec with ScalatestRouteTest {

  class TestHandler extends BootstrapHandler

  val handler = new TestHandler

  private def userInfo(userRef: UserRef, dealerRef: Option[AutoruDealer], grants: List[Resource]): UserInfo = {
    val sessionWithGrants =
      grants.nonEmpty.toOption {
        SessionResultGen.next.toBuilder
          .setAccess {
            AccessGrants
              .newBuilder()
              .addAllGrants(grants.asJava)
          }
          .build()
      }

    UserInfoGen.next.copy(userRef = userRef, dealerRef = dealerRef, session = sessionWithGrants)
  }

  private def grant(resource: ResourceAlias, access: AccessLevel): Resource = {
    Resource
      .newBuilder()
      .setAlias(resource)
      .setAccess(access)
      .build()
  }

  "check access method [user]" should {
    "work read access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), dealerRef = None, grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }

    "fail on no access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), dealerRef = None, grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.DASHBOARD,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe false
    }

    "work on no access but skip" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), dealerRef = None, grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.DASHBOARD,
        Access.Read,
        checkGuests = true,
        checkUsers = false,
        checkDealers = true
      )

      result shouldBe true
    }

    "work read-write access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_WRITE), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), dealerRef = None, grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.ReadWrite,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }

    "work read-write access on read" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_WRITE), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), dealerRef = None, grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }
  }

  "check access method [guest]" should {
    "work on skip" in {
      val info =
        userInfo(NoUser, dealerRef = None, grants = Nil)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = false,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }

    "fail on empty session" in {
      val info =
        userInfo(NoUser, dealerRef = None, grants = Nil)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe false
    }
  }

  "check access method [dealer]" should {
    "work read access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), Some(AutoruDealer(555L)), grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }

    "fail on no access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), Some(AutoruDealer(555L)), grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.DASHBOARD,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe false
    }

    "work on no access but skip" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_ONLY), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), Some(AutoruDealer(555L)), grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.DASHBOARD,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = false
      )

      result shouldBe true
    }

    "work read-write access" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_WRITE), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), Some(AutoruDealer(555L)), grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.ReadWrite,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }

    "work read-write access on read" in {
      val grants =
        List(grant(ResourceAlias.OFFERS, AccessLevel.READ_WRITE), grant(ResourceAlias.WALLET, AccessLevel.READ_ONLY))

      val info =
        userInfo(AutoruUser(123L), Some(AutoruDealer(555L)), grants)

      val result = handler.checkUserAccess(
        info,
        ResourceAlias.OFFERS,
        Access.Read,
        checkGuests = true,
        checkUsers = true,
        checkDealers = true
      )

      result shouldBe true
    }
  }
}
