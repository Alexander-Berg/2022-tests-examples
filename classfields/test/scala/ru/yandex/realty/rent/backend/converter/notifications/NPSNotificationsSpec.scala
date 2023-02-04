package ru.yandex.realty.rent.backend.converter.notifications

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.ContractParticipant
import ru.yandex.realty.rent.model.enums.{ContractStatus, Role}
import ru.yandex.realty.rent.util.NowMomentProvider
import ru.yandex.realty.util.protobuf.ProtobufFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class NPSNotificationsSpec extends SpecBase with RentModelsGen with ProtobufFormats {
  "NPSNotifications" should {
    "not show nps notification for tenant if he lives less than 3 months" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(cleanTenant),
          role = Role.Tenant,
          userContracts = Seq(newContract),
          addFallback = false
        )
        .getNotificationsList
        .isEmpty shouldBe (true)
    }

    "not show nps notification for owner if he uses service less than 3 months" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(cleanOwner),
          role = Role.Owner,
          userContracts = Seq(newContract),
          addFallback = false
        )
        .getNotificationsList
        .isEmpty shouldBe (true)
    }

    "show nps notification for tenant if he lives 3 months or more" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(cleanTenant),
          role = Role.Tenant,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .asScala
        .exists(_.hasNetPromoterScoreNotification) shouldBe (true)
    }

    "show nps notification for owner if he uses service 3 months or more" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(cleanOwner),
          role = Role.Owner,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .asScala
        .exists(_.hasNetPromoterScoreNotification) shouldBe (true)
    }

    "not show nps notification for tenant if last nps notification sent 6 month ago or less" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(tenantWithLastSent),
          role = Role.Tenant,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .isEmpty shouldBe (true)
    }

    "not show nps notification for owner if last nps notification sent 6 month ago or less" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(ownerWithLastSent),
          role = Role.Owner,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .isEmpty shouldBe (true)
    }

    "show nps notification for owner if last nps notification sent more than 6 month ago" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(ownerWithOldLastSent),
          role = Role.Owner,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .asScala
        .exists(_.hasNetPromoterScoreNotification) shouldBe (true)
    }

    "show nps notification for tenant if last nps notification sent more than 6 month ago" in new Data {
      NPSNotifications
        .buildNotifications(
          userOpt = Some(tenantWithOldLastSent),
          role = Role.Tenant,
          userContracts = Seq(contract),
          addFallback = false
        )
        .getNotificationsList
        .asScala
        .exists(_.hasNetPromoterScoreNotification) shouldBe (true)
    }
  }

  trait Data {

    import TestUtil.dt

    val now = dt(2022, 6, 11)
    implicit val nowMomentProvider: NowMomentProvider = NowMomentProvider(now)

    val OwnerUid = 1L
    val TenantUid = 2L

    val contract = {
      val c = rentContractGen(ContractStatus.Active).next
      c.copy(
        contractId = "договор1",
        owner = ContractParticipant(Some(OwnerUid), None, None, None),
        tenant = ContractParticipant(Some(TenantUid), None, None, None),
        data = c.data.toBuilder.setRentStartDate(now.minusMonths(3)).build()
      )
    }

    val newContract = {
      val c = rentContractGen(ContractStatus.Active).next
      c.copy(
        contractId = "новый-договор-1",
        owner = ContractParticipant(Some(OwnerUid), None, None, None),
        tenant = ContractParticipant(Some(TenantUid), None, None, None),
        data = c.data.toBuilder.setRentStartDate(now.minusMonths(2)).build()
      )
    }

    val sixMonthAgo = now.minusMonths(6)
    val beforeSixMonth = sixMonthAgo.minusDays(1)
    val afterSixMonth = sixMonthAgo.plusDays(1)

    val cleanOwner = userGen().next.copy(uid = OwnerUid)

    val ownerWithLastSent = {
      val u = userGen().next
      u.copy(uid = OwnerUid, data = u.data.toBuilder.setLastNpsFeedbackDate(afterSixMonth).build())
    }

    val ownerWithOldLastSent = {
      val u = userGen().next
      u.copy(uid = OwnerUid, data = u.data.toBuilder.setLastNpsFeedbackDate(beforeSixMonth).build())
    }

    val cleanTenant = userGen().next.copy(uid = TenantUid)

    val tenantWithLastSent = {
      val u = userGen().next
      u.copy(uid = TenantUid, data = u.data.toBuilder.setLastNpsFeedbackDate(afterSixMonth).build())
    }

    val tenantWithOldLastSent = {
      val u = userGen().next
      u.copy(uid = TenantUid, data = u.data.toBuilder.setLastNpsFeedbackDate(beforeSixMonth).build())
    }
  }
}
