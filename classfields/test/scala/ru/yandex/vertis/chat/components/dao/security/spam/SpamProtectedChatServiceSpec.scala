package ru.yandex.vertis.chat.components.dao.security.spam

import ru.yandex.vertis.chat.SlagGenerators.userRequestContext
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, JvmAuthorityService}
import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.security.spam.CleanWebService.CleanWebResponse
import ru.yandex.vertis.chat.components.dao.security.spam.impl.{AlwaysSpamDetectionService, FixedCleanWebService, FixedImageSpamDetectionService, NoSpamDetectionService}
import ru.yandex.vertis.chat.components.dao.statistics.{InStorageStatisticsService, StatisticsService}
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.features.ChatFeatures
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.impl.{ChatServiceWrapper, TestDomainAware}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.{SpecBase, UserRequestContext}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.chat.components.dao.authority.BanScope

class SpamProtectedChatServiceSpec extends SpecBase with ProducerProvider with RequestContextAware {

  trait Fixture {
    val timeService: TimeService = new DefaultTimeServiceImpl

    val state = JvmChatState.empty()
    val authority = new JvmAuthorityService(state, timeService)
    val detector = AlwaysSpamDetectionService
    val detectorNever = NoSpamDetectionService

    val cleanWebNever = FixedCleanWebService.NoOp
    val cleanWeb = new FixedCleanWebService(CleanWebResponse.empty.copy(hasForbiddenLink = true))

    val imageDetectorNever = FixedImageSpamDetectionService(false)
    val imageDetector = FixedImageSpamDetectionService(true)

    val registry = new InMemoryFeatureRegistry(BasicFeatureTypes)

    val features = new ChatFeatures {
      override def featureRegistry: FeatureRegistry = registry
    }

    val effectiveService = new JvmChatService(state)

    def makeService(cleanWeb: CleanWebService = cleanWebNever,
                    spamOborona: SpamDetectionService = detector,
                    imageSpamDetector: ImageSpamDetectionService = imageDetectorNever): SpamProtectedChatService =
      new ChatServiceWrapper(effectiveService)
        with SpamProtectedChatService
        with SameThreadExecutionContextSupport
        with TestDomainAware {

        override def autoBanLimit = 2

        override val imageSpamService: DMap[ImageSpamDetectionService] = DMap.forAllDomains(imageSpamDetector)

        override val authorityService: DMap[AuthorityService] =
          DMap.forAllDomains(authority)

        override val spamService: DMap[SpamDetectionService] =
          DMap.forAllDomains(spamOborona)

        val cleanWebService: DMap[CleanWebService] = DMap.forAllDomains(cleanWeb)

        override val features: DMap[ChatFeatures] = DMap.forAllDomains(Fixture.this.features)

        override val statisticsService: DMap[StatisticsService] =
          DMap.forDomainAware(d => {
            new InStorageStatisticsService with d.DomainAwareImpl {

              override val chatStorage: DMap[ChatStorage] =
                DMap.forAllDomains(JvmStorage(state))
            }
          })
      }

    def service: SpamProtectedChatService

    registry.updateFeature(features.CleanWebSpamDetection.name, true).futureValue
  }

  trait FixtureWithDefaultService extends Fixture {
    val service = makeService()
  }

  "SpamProtectedChatService" should {
    "automatically ban user after too many spam messages" in new FixtureWithDefaultService {
      val user = userId.next
      withUserContext(user) { implicit rc =>
        val room =
          service
            .createRoom(createRoomParameters.next.withUserId(user))
            .futureValue
        sendMessageParameters(room)
          .next(service.autoBanLimit)
          .map(_.copy(author = user))
          .foreach(parameters => {
            service.sendMessage(parameters).futureValue
          })
        authority.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe true
      }
    }

    "not check message from tech support" in new FixtureWithDefaultService {
      val user = userId.next
      withUserContext(user) { implicit rc =>
        val overloadMessage = TechSupportUtils.privateOverloadMessage(user, "text")
        val sendMessageResult = service.sendMessage(overloadMessage).futureValue
        sendMessageResult.message.isSpam shouldBe false
      }
    }

    "not mark message as spam if clean web marks it as spam but spam oborona doesn't and feature is false" in new Fixture {
      implicit val rc: UserRequestContext = userRequestContext.next
      val service = makeService(cleanWeb, detectorNever)
      val user = userId.next
      val room = service
        .createRoom(createRoomParameters.next.withUserId(user))
        .futureValue
      val result = service.sendMessage(sendMessageParameters(room).next).futureValue.message

      result.isSpam shouldBe false
    }

    "mark message as spam if clean web marks it as spam but spam oborona doesn't and feature is true" in new Fixture {
      registry.updateFeature(features.CleanWebForbiddenLink.name, true).futureValue
      implicit val rc: UserRequestContext = userRequestContext.next
      val service = makeService(cleanWeb, detectorNever)
      val user = userId.next
      val room = service
        .createRoom(createRoomParameters.next.withUserId(user))
        .futureValue
      val result = service.sendMessage(sendMessageParameters(room).next).futureValue.message

      result.isSpam shouldBe true
    }

    "not mark message as spam if clean web doesnt mark it as spam and spam oborona also doesn't" in new Fixture {
      val service = makeService(cleanWebNever, detectorNever)
      val user = userId.next
      val room = service
        .createRoom(createRoomParameters.next.withUserId(user))
        .futureValue
      val result = service.sendMessage(sendMessageParameters(room).next).futureValue.message

      result.isSpam shouldBe false
    }

    "mark message as spam if feature is on and image spam detector returns true" in new Fixture {
      registry.updateFeature(features.SpamImageInEmptyChat.name, true).futureValue
      implicit val rc: UserRequestContext = userRequestContext.next
      val service = makeService(cleanWebNever, detectorNever, imageDetector)
      val user = userId.next
      val room = service
        .createRoom(createRoomParameters.next.withUserId(user))
        .futureValue
      val result = service.sendMessage(sendMessageParameters(room).next).futureValue.message

      result.isSpam shouldBe true
    }

    "not check message from telepony, suggest_call, bumblebee_vin, bumblebee_not_vin" in new FixtureWithDefaultService {
      for (platform <- Seq(
             "telepony",
             "suggest_call",
             "bumblebee_vin",
             "bumblebee_not_vin",
             "man_in_black_vin",
             "man_in_black_not_vin"
           )) {
        val user = userId.next
        withUserContext(user) { rc =>
          implicit val teleponyRc: UserRequestContext =
            rc.copy(requester = rc.requester.copy(platform = Some(platform)))
          val room = service
            .createRoom(createRoomParameters.next.withUserId(user))
            .futureValue
          sendMessageParameters(room)
            .next(service.autoBanLimit)
            .map(_.copy(author = user))
            .foreach(parameters => {
              val sendMessageResult =
                service.sendMessage(parameters).futureValue
              sendMessageResult.message.isSpam shouldBe false
            })
          authority.getBanStatus(user).futureValue.isBannedAt(timeService.getNow, BanScope.AllUserChats) shouldBe false
        }
      }
    }
  }
}
