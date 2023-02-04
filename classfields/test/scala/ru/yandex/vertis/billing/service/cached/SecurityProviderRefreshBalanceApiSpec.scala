package ru.yandex.vertis.billing.service.cached

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DefaultPropertyChecks
import ru.yandex.vertis.billing.balance.model.{Balance, ClientId, OperatorId, UserId}
import ru.yandex.vertis.billing.model_core.{AutoRuUid, CustomerId, Uid, User}
import ru.yandex.vertis.billing.security.{
  CachedSecurityProvider,
  Grant,
  GrantModes,
  SecurityContext,
  SecurityContextImpl,
  SecurityProvider
}
import ru.yandex.vertis.billing.service.cached.SecurityProviderRefreshBalanceApiSpec.{
  sourceCache,
  Check,
  Load,
  RefreshAction,
  RefreshByAdd,
  RefreshByRemove,
  SecurityProviderMock,
  SupClientId,
  SupCustomer,
  SupUserUid,
  UserAutoRuUid,
  UserUid
}
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

//TODO do not work on minSuccessful = 100 workers=5
class SecurityProviderRefreshBalanceApiSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with DefaultPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10)

  val contextName = "security-provider-refresh-balance-api-spec"
  implicit val oc = OperatorContext(contextName, UserUid)

  val balanceMock = {
    val m = mock[Balance]
    stub(m.createUserClientAssociation(_: ClientId, _: UserId)(_: OperatorId)) { case _ =>
      Success(())
    }
    stub(m.removeUserClientAssociation(_: ClientId, _: UserId)(_: OperatorId)) { case _ =>
      Success(())
    }
    m
  }

  val securityProvider = new SecurityProviderMock() with CachedSecurityProvider {

    override def support: Cache = sourceCache

    override def serviceNamespace: String = "test"

  }

  val refresh = new SecurityProviderRefreshBalanceApi(balanceMock, securityProvider)

  var UidInCache = false
  val AutoRuUidSet = mutable.Set.empty[Grant]

  private def wrapToCacheKey(user: User, grant: Grant): String = {
    val key = user match {
      case _: AutoRuUid =>
        s"$user:${Grant.toKey(grant)}"
      case _ =>
        user.toString
    }
    securityProvider.usingServiceNamespace(CachedSecurityProvider.userSecurityContextKey(key))
  }

  private def checkedGet(user: User, grant: Grant): Unit = {
    val shouldPresent = user match {
      case UserAutoRuUid =>
        AutoRuUidSet(grant)
      case UserUid =>
        UidInCache
      case _ =>
        false
    }
    val key = wrapToCacheKey(user, grant)
    sourceCache.get[SecurityContext](key).get match {
      case Some(c) if c.contains(grant) && !shouldPresent =>
        fail(s"Value present but should not.$c")
      case None if shouldPresent =>
        fail("Value not present but should.")
      case _ =>
        ()
    }
  }

  private def checkedRefresh(user: User, grant: Grant, action: RefreshAction): Unit = user match {
    case UserAutoRuUid =>
      sourceCache.delete(wrapToCacheKey(user, grant)).get
      AutoRuUidSet -= grant
      checkedGet(UserAutoRuUid, grant)
    case UserUid =>
      action match {
        case RefreshByAdd =>
          refresh.assignUser(UserUid, SupClientId).get
        case RefreshByRemove =>
          refresh.deassignUser(UserUid, SupClientId).get
      }
      UidInCache = false
      checkedGet(UserUid, grant)
    case _ =>
  }

  private def checkedLoad(user: User, grant: Grant): Unit = {
    securityProvider.get(user, Some(grant)).get
    user match {
      case UserAutoRuUid =>
        AutoRuUidSet += grant
      case UserUid =>
        UidInCache = true
      case _ =>
    }
    checkedGet(user, grant)
  }

  "SecurityProviderRefreshBalanceApiSpec" should {
    "correctly update cache" in {
      val userGrantGen = for {
        user <- Gen.oneOf(UserUid, UserAutoRuUid)
        grantGen <- Gen.oneOf(
          Grant.OnClient(SupClientId, _: GrantModes.Value),
          Grant.OnUser(SupUserUid, _: GrantModes.Value),
          Grant.OnCustomer(SupCustomer, _: GrantModes.Value),
          (_: GrantModes.Value) => Grant.All
        )
        mode <- Gen.oneOf(GrantModes.values.toSeq)
        action <- Gen.oneOf(Load, RefreshByAdd, RefreshByRemove, Check)
      } yield (user, grantGen(mode), action)

      forAll(userGrantGen) {
        case (user, grant, Load) =>
          checkedGet(user, grant)
          checkedLoad(user, grant)
        case (user, grant, r: RefreshAction) =>
          checkedGet(user, grant)
          checkedRefresh(user, grant, r)
        case (user, grant, Check) =>
          checkedGet(user, grant)
        case _ =>
      }
    }
  }

}

object SecurityProviderRefreshBalanceApiSpec {

  val UserUid = Uid(123)
  val UserAutoRuUid = AutoRuUid("321")

  val SupClientId: ClientId = 666
  val SupUserUid = Uid(666)
  val SupCustomer = CustomerId(SupClientId.toString)

  val sourceCache = new NeverExpireCache()

  class SecurityProviderMock extends SecurityProvider {

    override def get(user: User, grantOpt: Option[Grant])(implicit rc: OperatorContext): Try[SecurityContext] =
      user match {
        case UserUid | UserAutoRuUid =>
          val grants: Set[Grant] = grantOpt match {
            case Some(grant) =>
              Set(grant)
            case _ =>
              Set.empty
          }
          Success(SecurityContextImpl(user, grants))
        case _ =>
          Failure(new IllegalArgumentException("Unknown user"))
      }

  }

  sealed trait Action
  case object Load extends Action
  case object Check extends Action

  sealed trait RefreshAction extends Action
  case object RefreshByAdd extends RefreshAction
  case object RefreshByRemove extends RefreshAction

}
