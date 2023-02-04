package ru.yandex.vertis.passport.service.ban

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.WordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.UserReseller
import ru.yandex.vertis.passport.dao.impl.mysql.AutoruUsersQuotaDao
import ru.yandex.vertis.passport.dao.{FullUserDao, UserModerationStatusDao}
import ru.yandex.vertis.passport.integration.email.{EmailTemplates, TemplatedLetter}
import ru.yandex.vertis.passport.loc.AutoruStringResources
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.service.ban.ModerationBanReasonsService.BanReasonDescription
import ru.yandex.vertis.passport.service.ban.ModerationServiceImpl.DomainModerationOpinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.passport.service.ban.ModerationServiceImpl.{DomainModerationOpinion, _}
import ru.yandex.vertis.passport.service.communication.UserCommunicationService
import ru.yandex.vertis.passport.service.user.UserBackendService
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.curator.DynamicPropertyView
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class ModerationServiceImplSpec extends WordSpec with SpecBase with MockitoSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val moderationDomain = "CARS"
  val SomeReason = "LOW_PRICE"

  val AllNotificationReason: BanReasonDescription = BanReasonDescription(
    textUserBan = Some("some text"),
    textUserBanSms = Some("other text"),
    textChat = Some("chat text")
  )

  val SmsNotificationReason: BanReasonDescription = BanReasonDescription(
    textUserBanSms = Some("other text")
  )

  val EmailNotificationReason: BanReasonDescription = BanReasonDescription(
    textUserBan = Some("other text")
  )

  val EmailAndChatNotification: BanReasonDescription = BanReasonDescription(
    textUserBan = Some("email text"),
    textChat = Some("chat text")
  )

  class Context {
    val banService: BanService = mock[BanService]
    val userDao: FullUserDao = mock[FullUserDao]
    val quotaDao: AutoruUsersQuotaDao = mock[AutoruUsersQuotaDao]
    val moderationStatusDao: UserModerationStatusDao = mock[UserModerationStatusDao]
    val commService: UserCommunicationService = mock[UserCommunicationService]
    val userBackendService: UserBackendService = mock[UserBackendService]
    val banReasonsService: ModerationBanReasonsService = mock[ModerationBanReasonsService]
    val userNotificationsEnabled: DynamicPropertyView[Boolean] = () => true

    val userModerationStatusProvider = new UserModerationStatusProviderImpl(
      moderationStatusDao,
      banService,
      quotaDao,
      banReasonsService
    )

    val service = new ModerationServiceImpl(
      banService,
      quotaDao,
      userDao,
      userBackendService,
      userModerationStatusProvider,
      commService,
      banReasonsService,
      AutoruStringResources,
      new EmailTemplates("localhost"),
      userNotificationsEnabled
    )
  }

  def context(f: Context => Unit): Unit = {
    f(new Context)
  }

  "ModerationService.processUserOpinions" should {

    "ban user and notify him about it" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(AllNotificationReason)))
      when(ctx.commService.sendEmailToUser(?, ?)(?)).thenReturn(Future.successful(true))
      when(ctx.commService.sendSmsToUser(?, ?)(?)).thenReturn(Future.successful(true))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(None))
      stub(ctx.moderationStatusDao.upsert(_: UserId, _: UserModerationStatus)(_: Traced)) {
        case (user.id, status, _) if status.bans.size == 2 => Future.unit
      }

      val opinions = Map(
        domain -> Failed(Seq(SomeReason), None),
        moderationDomain -> Failed(Seq(SomeReason), None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.banService).banUser(eq(user), eq(domain), ?)(?)
      verify(ctx.commService).sendEmailToUser(eq(user), ?)(?)
      verify(ctx.commService).sendSmsToUser(eq(user), ?)(?)
      verify(ctx.moderationStatusDao).upsert(eq(user.id), ?)(?)
    }

    "not do anything if user already banned" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      val opinions = Map(
        domain -> Failed(Seq(SomeReason), None)
      )

      val currentStatus = UserModerationStatus(bans = Map(domain -> DomainBan(Set(SomeReason), None)))

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?))
        .thenReturn(Future.successful(Some(currentStatus)))

      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(AllNotificationReason)))

      stub(ctx.moderationStatusDao.upsert(_: UserId, _: UserModerationStatus)(_: Traced)) {
        case (user.id, status, _) => Future.unit
      }

      ctx.service.processUserOpinions(user.id, opinions).futureValue

      Mockito.verifyNoMoreInteractions(ctx.banService)
      Mockito.verifyNoMoreInteractions(ctx.commService)
    }

    "not send email if no reason is resolved to text" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(SmsNotificationReason)))
      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.commService.sendSmsToUser(?, ?)(?)).thenReturn(Future.successful(true))
      defaultStatusStub(ctx, user)

      val opinions = Map(
        domain -> Failed(Seq("I_DON'T_KNOW"), None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.banService).banUser(eq(user), eq(domain), ?)(?)
      verify(ctx.commService).sendSmsToUser(eq(user), ?)(?)
    }

    "send different email in case reason is COMMERCIAL" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      val notif = EmailNotificationReason
        .copy(passportSenderTemplate = Some("moderation.block_commercial"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?)).thenReturn(Future.successful(Some(notif)))
      defaultStatusStub(ctx, user)

      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (_, template, _) if template.templateName == "moderation.block_commercial" =>
          Future.successful(true)
      }

      val opinions = Map(
        domain -> Failed(Seq("COMMERCIAL"), None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.banService).banUser(eq(user), eq(domain), ?)(?)
      verify(ctx.commService).sendEmailToUser(eq(user), ?)(?)
    }

    "send common email in case reason is USER_HACKED" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      val notif = EmailNotificationReason
        .copy(passportSenderTemplate = Some("moderation.block_cheat"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(notif)))
      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.userBackendService.cleanHackedUser(eq(user.id))(?)).thenReturn(Future.unit)
      defaultStatusStub(ctx, user)

      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (_, template, _) if template.templateName == "moderation.block_cheat" =>
          Future.successful(true)
      }

      val reasons = Seq("USER_HACKED", "ANOTHER_REASON")
      val opinions = Map(
        domain -> Failed(reasons, None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.banService).banUser(eq(user), eq(domain), eq(reasons.mkString(", ")))(?)
      verify(ctx.commService).sendEmailToUser(eq(user), ?)(?)
      verify(ctx.userBackendService).cleanHackedUser(eq(user.id))(?)
    }

    "unban user" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll
      val currentStatus = UserModerationStatus(bans = Map(domain -> DomainBan(Set(SomeReason), None)))

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      val notif = EmailAndChatNotification
        .copy(passportSenderTemplate = Some("moderation.unblock_account"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(notif)))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?))
        .thenReturn(Future.successful(Some(currentStatus)))
      when(ctx.moderationStatusDao.upsert(eq(user.id), ?)(?)).thenReturn(Future.unit)
      when(ctx.banService.removeUserBan(?, ?)(?)).thenReturn(Future.unit)
      when(ctx.commService.sendToTechSupportChat(eq(user), ?, ?)(?))
        .thenReturn(Future.unit)

      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (_, template, _) if template.templateName == "moderation.unblock_account" =>
          Future.successful(true)
      }

      val opinions = Map(
        domain -> Ok
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.banService).removeUserBan(eq(user.id), eq(domain))(?)
      verify(ctx.commService).sendEmailToUser(eq(user), ?)(?)
      verify(ctx.commService).sendToTechSupportChat(eq(user), ?, ?)(?)
      verify(ctx.moderationStatusDao)
        .upsert(eq(user.id), argThat[UserModerationStatus](v => v.bans.isEmpty && v.bansUpdated.isDefined))(?)
    }

    "add new bans only" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll
      val otherDomain = AutoruBanServiceImpl.DomainUsers
      val otherDomain2 = "ONE_MORE_DOMAIN"
      val currentStatus = UserModerationStatus(bans = Map(domain -> DomainBan(Set(SomeReason), None)))

      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.moderationStatusDao.upsert(eq(user.id), ?)(?)).thenReturn(Future.unit)
      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?))
        .thenReturn(Future.successful(Some(currentStatus)))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(AllNotificationReason)))

      val opinions = Map(
        domain -> Failed(Seq(SomeReason), None),
        otherDomain -> Failed(Seq(SomeReason), None),
        otherDomain2 -> Failed(Seq(SomeReason), None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      val expectedBannedDomains = Set(domain, otherDomain, otherDomain2)
      verify(ctx.banService).banUser(eq(user), eq(otherDomain), ?)(?)
      verify(ctx.moderationStatusDao)
        .upsert(eq(user.id), argThat[UserModerationStatus](_.bans.keySet == expectedBannedDomains))(?)
    }

    "not try to unban if user wasn't banned by moderation" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(None))

      val opinions = Map(
        domain -> Unknown
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue
    }

    "should process 'USER_RESELLER' and remove quota" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = "CARS"

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      val notif = EmailNotificationReason
        .copy(passportSenderTemplate = Some("moderation.block_quota"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(notif)))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(None))
      stub(ctx.moderationStatusDao.upsert(_: UserId, _: UserModerationStatus)(_: Traced)) {
        case (user.id, status, _) if status.bans.nonEmpty && status.reseller => Future.unit
      }
      when(ctx.quotaDao.removeQuota(eq(user.id), eq(domain))(?)).thenReturn(Future.unit)
      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (`user`, template, _) if template.templateName == "moderation.block_quota" =>
          Future.successful(true)
      }

      val opinions = Map(
        domain -> Failed(Seq("USER_RESELLER"), Some(UserReseller.ResellerType.FAST_RESALE))
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.moderationStatusDao).upsert(
        eq(user.id),
        argThat[UserModerationStatus] { v =>
          v.reseller &&
          v.resellerFlagUpdated.isDefined &&
          v.resellerFlagUpdatedByDomain.contains(domain)
        }
      )(?)

      verify(ctx.quotaDao).removeQuota(eq(user.id), ?)(?)
      Mockito.verifyNoMoreInteractions(ctx.banService)
    }

    "should ban user on 'MOS_RU_VALIDATION'" in context { ctx =>
      val reason = "MOS_RU_VALIDATION"
      val user = ModelGenerators.fullUser.next
      val domain = AutoruBanServiceImpl.DomainAll

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      val notif = EmailNotificationReason
        .copy(passportSenderTemplate = Some("moderation.block_quota"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?))
        .thenReturn(Future.successful(Some(notif)))
      when(ctx.banService.banUser(?, ?, ?)(?)).thenReturn(Future.unit)
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(None))
      stub(ctx.moderationStatusDao.upsert(_: UserId, _: UserModerationStatus)(_: Traced)) {
        case (user.id, status, _) if status.bans.nonEmpty && !status.reseller => Future.unit
      }
      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (`user`, template, _) if template.templateName == "moderation.block_quota" =>
          Future.successful(true)
      }

      val opinions = Map(
        domain -> Failed(Seq(reason), None)
      )
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.moderationStatusDao).upsert(
        eq(user.id),
        argThat[UserModerationStatus] { v =>
          !v.reseller && v.bans(domain).reasons == Set(reason)
        }
      )(?)
      verify(ctx.commService).sendEmailToUser(eq(user), ?)(?)
      verify(ctx.banService).banUser(eq(user), eq(domain), ?)(?)
    }

    "should restore quota" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = "CARS"
      val domain2 = "SPECIAL"
      val bans = Map(
        domain -> DomainBan(Set(ReasonReseller), Some(UserReseller.ResellerType.FAST_RESALE)),
        domain2 -> DomainBan(Set(ReasonReseller), Some(UserReseller.ResellerType.FAST_RESALE))
      )
      val currentStatus = UserModerationStatus(bans = bans, reseller = true)

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?))
        .thenReturn(Future.successful(Some(currentStatus)))
      val notif = EmailAndChatNotification
        .copy(passportSenderTemplate = Some("moderation.return_quota"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?)).thenReturn(Future.successful(Some(notif)))
      when(ctx.moderationStatusDao.upsert(?, ?)(?)).thenReturn(Future.unit)
      when(ctx.quotaDao.restoreQuota(eq(user.id), ?)(?)).thenReturn(Future.unit)
      when(ctx.commService.sendToTechSupportChat(eq(user), ?, ?)(?))
        .thenReturn(Future.unit)
      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (`user`, template, _) if template.templateName == "moderation.return_quota" =>
          Future.successful(true)
      }

      val opinions = Map.empty[String, DomainModerationOpinion]
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.quotaDao, times(2)).restoreQuota(eq(user.id), ?)(?)
      verify(ctx.commService, times(1)).sendToTechSupportChat(eq(user), ?, ?)(?)
      Mockito.verifyNoMoreInteractions(ctx.banService)
      verify(ctx.moderationStatusDao).upsert(
        eq(user.id),
        argThat[UserModerationStatus] { v =>
          !v.reseller &&
          v.resellerFlagUpdated.isDefined &&
          v.resellerFlagUpdatedByDomain.keySet == Set(domain, domain2)
        }
      )(?)
    }
    "should restore quota if user is reseller without new statuses" in context { ctx =>
      val user = ModelGenerators.fullUser.next
      val domain = "CARS"
      val domain2 = "SPECIAL"
      val bans = Map(
        domain -> DomainBan(Set(ReasonReseller), None),
        domain2 -> DomainBan(Set(ReasonReseller), None)
      )
      val currentStatus = UserModerationStatus(bans = bans, reseller = true)

      when(ctx.userDao.get(eq(user.id))(?)).thenReturn(Future.successful(user))
      when(ctx.moderationStatusDao.get(eq(user.id), ?)(?))
        .thenReturn(Future.successful(Some(currentStatus)))
      val notif = EmailAndChatNotification
        .copy(passportSenderTemplate = Some("moderation.return_quota"))
      when(ctx.banReasonsService.getReasonDescription(?, ?)(?)).thenReturn(Future.successful(Some(notif)))
      when(ctx.moderationStatusDao.upsert(?, ?)(?)).thenReturn(Future.unit)
      when(ctx.quotaDao.restoreQuota(eq(user.id), ?)(?)).thenReturn(Future.unit)
      when(ctx.commService.sendToTechSupportChat(eq(user), ?, ?)(?))
        .thenReturn(Future.unit)
      stub(ctx.commService.sendEmailToUser(_: FullUser, _: TemplatedLetter)(_: RequestContext)) {
        case (`user`, template, _) if template.templateName == "moderation.return_quota" =>
          Future.successful(true)
      }

      val opinions = Map.empty[String, DomainModerationOpinion]
      ctx.service.processUserOpinions(user.id, opinions).futureValue

      verify(ctx.quotaDao, times(2)).restoreQuota(eq(user.id), ?)(?)
      verify(ctx.commService, times(1)).sendToTechSupportChat(eq(user), ?, ?)(?)
      Mockito.verifyNoMoreInteractions(ctx.banService)
      verify(ctx.moderationStatusDao).upsert(
        eq(user.id),
        argThat[UserModerationStatus] { v =>
          !v.reseller &&
          v.resellerFlagUpdated.isDefined &&
          v.resellerFlagUpdatedByDomain.keySet == Set(domain, domain2)
        }
      )(?)
    }
  }

  private def defaultStatusStub(ctx: Context, user: FullUser) = {
    when(ctx.moderationStatusDao.get(eq(user.id), ?)(?)).thenReturn(Future.successful(None))
    when(ctx.moderationStatusDao.upsert(eq(user.id), ?)(?)).thenReturn(Future.unit)
  }
}
