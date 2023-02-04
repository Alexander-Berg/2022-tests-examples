package ru.yandex.realty.rent.chat.service.processing

import akka.kafka.ConsumerMessage.{GroupTopicPartition, PartitionOffset}
import org.scalamock.matchers.ArgCapture.{CaptureAll, CaptureOne}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers
import org.scalatest.concurrent.{AbstractPatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.canonical.base.params.RequestParameter.Source
import ru.yandex.realty.canonical.base.request.SearchRequest
import ru.yandex.realty.chat.model.{MessageId, RentOfferChatBotUser, RentOfferChatCallCenterUser, RoomId, Window}
import ru.yandex.realty.chat.service.crm.jivosite.Request
import ru.yandex.realty.chat.service.crm.jivosite.client.JivositeClient
import ru.yandex.realty.chat.service.history.HistoryLoader
import ru.yandex.realty.chat.service.processing.ChatEventProcessor
import ru.yandex.realty.chat.service.processing.context.MessageContextBuilder
import ru.yandex.realty.chat.service.processing.stages.TimingProcessor
import ru.yandex.realty.chat.service.revisit.RevisitQueueDao
import ru.yandex.realty.chat.util.RoomProperties
import ru.yandex.realty.clients.chat.{ChatClient, ChatRequestContext, RobotRequestContext}
import ru.yandex.realty.clients.rent.{
  RentFlatQuestionnaireServiceClient,
  RentFlatServiceClient,
  RentFlatShowingServiceClient
}
import ru.yandex.realty.clients.router.FrontendRouterClient
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.chat.model.{RentOfferChatEvent, RentOfferChatRoomState}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.{ApplicationData, BotState}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.BotState.StateCase
import ru.yandex.realty.rent.chat.model.offerchat.{BotConfig, FaqEntry}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category
import ru.yandex.realty.rent.chat.service.analytics.offerchat.startdateresponses.{
  StartDateResponseDao,
  StartDateResponseRecord
}
import ru.yandex.realty.rent.chat.service.analytics.offerchat.status.OfferChatAnalyticsDao
import ru.yandex.realty.rent.chat.service.crm.CrmSenderFactory
import ru.yandex.realty.rent.chat.service.pings.idleness.IdlenessPingQueueDao
import ru.yandex.realty.rent.chat.service.processing.context.{
  OfferChatMessageContext,
  OfferChatProcessingContext,
  OfferChatProcessingContextBuilder,
  OfferChatRoomContext
}
import ru.yandex.realty.rent.chat.service.processing.stages.{
  OfferChatActionProcessor,
  OfferChatBotGraphProcessor,
  OfferChatBotUserProcessor,
  OfferChatCommandProcessor,
  OfferChatDirectPassProcessor,
  OfferChatIdlePingProcessor,
  OfferChatSessionProcessor
}
import ru.yandex.realty.rent.chat.service.state.{OfferChatRoomStateAccessor, OfferChatRoomStateDao}
import ru.yandex.realty.rent.chat.util.ChatBackendUtils
import ru.yandex.realty.rent.proto.api.moderation.{FlatDetailedInfo, FlatRequestFeature}
import ru.yandex.realty.rent.proto.chatactionlog
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.urls.router.model.RouterUrlRequest
import ru.yandex.realty.util.{TestClock, TimeUtils}
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.util.tracing.NoopTracingProvider
import ru.yandex.realty.util.TimeUtils.instantToProtoTimestamp
import ru.yandex.vertis.MimeType.TEXT_PLAIN
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.chat.model.api.ApiModel.{
  CreateMessageParameters,
  Message,
  MessagePropertyType,
  Room,
  UpdateRoomParameters
}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.util.concurrent.Threads

import java.time.{Instant, ZoneId}
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object OfferChatBackendEventProcessorFixtures
  extends NoopTracingProvider
  with Matchers
  with ScalaFutures
  with AbstractPatienceConfiguration {

  implicit val testEc: ExecutionContext =
    Threads.newForkJoinPoolExecutionContext(2, "OfferChatBackendEventProcessorFixtures-ec")

  // copied from ru.yandex.realty.AsyncSpecBase
  implicit override def patienceConfig: OfferChatBackendEventProcessorFixtures.PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(15, Millis))

  trait FixtureMocks extends FeaturesStubComponent with MockFactory {

    implicit val traced: Traced = Traced.empty

    val ops: TestOperationalSupport = new TestOperationalSupport {}

    val roomStateDao = mock[OfferChatRoomStateDao]
    val chatClient = mock[ChatClient]
    val requestContextFactory = new RobotRequestContext.Factory(ChatBackendUtils.RobotNameForChatBackend)

    val historyLoader = mock[HistoryLoader]
    (historyLoader.robotUser _).expects().anyNumberOfTimes().returns(RentOfferChatBotUser)

    val rentFlatGrpcClient = mock[RentFlatServiceClient]
    val rentFlatShowingClient = mock[RentFlatShowingServiceClient]
    val flatQuestionnaireClient: RentFlatQuestionnaireServiceClient = mock[RentFlatQuestionnaireServiceClient]
    val jivositeClient = mock[JivositeClient]
    val mdsUrlBuilder = new MdsUrlBuilder("//avatars")
    val revisitQueueDao = mock[RevisitQueueDao]

    val statusAnalyticsDao = mock[OfferChatAnalyticsDao]
    val startDateResponseDao = mock[StartDateResponseDao]

    object IdleOperatorPingQueue {
      val dao = mock[IdlenessPingQueueDao]
      val enqueueAfter = CaptureOne[Instant]()

      def expectEnqueue(): Unit = {
        (dao
          .enqueue(_: String, _: Instant)(_: Traced))
          .expects(*, capture(enqueueAfter), *)
          .anyNumberOfTimes()
          .returns(Future.unit)
      }
    }

    object IdleUserPingQueue {
      val dao = mock[IdlenessPingQueueDao]
      val enqueueAfter = CaptureOne[Instant]()

      def expectEnqueue(): Unit = {
        (dao
          .enqueue(_: String, _: Instant)(_: Traced))
          .expects(*, capture(enqueueAfter), *)
          .anyNumberOfTimes()
          .returns(Future.unit)
      }
    }

    val idlePingProcessor = new OfferChatIdlePingProcessor(
      OfferChatIdlePingProcessor.TestingDurationUntilIdleUserPing1,
      OfferChatIdlePingProcessor.TestingDurationUntilIdleUserPing2
    )

    val frontendRouterClient: FrontendRouterClient = mock[FrontendRouterClient]

    val brokerClient: BrokerClient = mock[BrokerClient]

    val clock = new TestClock(ZoneId.systemDefault())
    clock.setInstant(Instant.ofEpochSecond(12 * 60 * 60))

    val processingContextBuilder = new OfferChatProcessingContextBuilder(
      clock,
      botConfigProvider = () => botConfig,
      roomStateDao,
      chatClient,
      requestContextFactory,
      historyLoader,
      rentFlatGrpcClient,
      rentFlatShowingClient,
      flatQuestionnaireClient,
      frontendRouterClient,
      features
    )

    val timingProcessor = new TimingProcessor(ops)

    val sessionProcessor = new OfferChatSessionProcessor

    val commandProcessor = new OfferChatCommandProcessor(
      reinitSession = sessionProcessor.resetSession(_),
      isProduction = false
    )
    val botUserProcessor = new OfferChatBotUserProcessor(chatClient, requestContextFactory)

    val botConfig = BotConfig
      .newBuilder()
      .setMIntroduction("MIntroduction")
      .setMGreetNoApplication("MGreetNoApplication")
      .setAGreetNoAppRent("AGreetNoAppRent")
      .setAGreetNoAppQuestions("AGreetNoAppQuestions")
      .setMGreetWithApplication("MGreetWithApplication")
      .setAGreetWithAppQuestion("AGreetWithAppQuestion")
      .setAGreetWithAppOther("AGreetWithAppOther")
      .setMFaq("MFaq")
      .addFaqEntries(
        FaqEntry
          .newBuilder()
          .setId("Id")
          .setButtonText("ButtonText")
          .setAnswer("Answer")
          .setEnabled(true)
      )
      .addFaqEntries(
        FaqEntry
          .newBuilder()
          .setId("DisabledId")
          .setButtonText("DisabledButtonText")
          .setAnswer("DisabledAnswer")
          .setEnabled(false)
      )
      .setAFaqQuestionAboutApplication("AFaqQuestionAboutApplication")
      .setAFaqSpeakToManager("AFaqSpeakToManager")
      .setMFaqQuestionAboutApplication("MFaqQuestionAboutApplication")
      .setMFaqCallingManager("MFaqCallingManager")
      .setMFallbackToManager("MFallbackToManager")
      .addMRentIntro("MRentIntro1")
      .addMRentIntro("MRentIntro2")
      .setAIntroContinue("AIntroContinue")
      .setAIntroQuestions("AIntroQuestions")
      .setMNightAutoresponse("MNightAutoresponse ${now} ${nightEnd}")
      .setMFailedPrompt("MFailedPrompt")
      .setAFailedPromptYes("AFailedPromptYes")
      .setAFailedPromptNo("AFailedPromptNo")
      .setMFailedBye("MFailedBye")
      .setMLongTermOnly("MLongTermOnly")
      .setALongTermOnlyAccept("ALongTermOnlyAccept")
      .setALongTermOnlyNo("ALongTermOnlyNo")
      .setMLongTermOnlyUnfit("MLongTermOnlyUnfit ${url}")
      .setMTenants("MTenants")
      .setATenantsOne("ATenantsOne")
      .setATenantsTwo("ATenantsTwo")
      .setATenantsThree("ATenantsThree")
      .setATenantsFour("ATenantsFour")
      .setATenantsFivePlus("ATenantsFivePlus")
      .setMTenantsUnfit("MTenantsUnfit ${url}")
      .setMPets("MPets")
      .setAPetsYes("APetsYes")
      .setAPetsNo("APetsNo")
      .setMPetsNotAllowed("MPetsNotAllowed ${url}")
      .setMPetsWhich("MPetsWhich")
      .setMStartDatePromptUnlimited("MStartDatePromptUnlimited")
      .addAStartDatePresets("AStartDatePresets 1")
      .addAStartDatePresets("AStartDatePresets 2")
      .setStartDatePresetsVersion(1)
      .setMCitizenship("MCitizenship")
      .setACitizenshipYes("ACitizenshipYes")
      .setACitizenshipNo("ACitizenshipNo")
      .setMCitizenshipUnfit("MCitizenshipUnfit")
      .setMShowingType("MShowingType")
      .addMShowingType2("MShowingType2 1")
      .addMShowingType2("MShowingType2 2")
      .setAShowingTypeWithout("AShowingTypeWithout")
      .setAShowingTypeOnline("AShowingTypeOnline")
      .setAShowingTypeOffline("AShowingTypeOffline")
      .setMIdleOperatorPing("MIdleOperatorPing")
      .setMShowingDiscoveredOnFreeInput("MShowingDiscoveredOnFreeInput")
      .setMReturningToMainFlow("MReturningToMainFlow")
      .setMIdleUserPing1("MIdleUserPing1")
      .setAIdleUserContinue1("AIdleUserContinue1")
      .setAIdleUserQuestion("AIdleUserQuestion")
      .setAIdleUserRefusal("AIdleUserRefusal")
      .setMIdleUserReturns("MIdleUserReturns")
      .setMIdleUserPing2("MIdleUserPing2")
      .setAIdleUserContinue2("AIdleUserContinue2")
      .build()

    val botProcessor = new OfferChatBotGraphProcessor(
      ops
    )

    val directPassProcessor = new OfferChatDirectPassProcessor

    val actionProcessor = new OfferChatActionProcessor(
      statusAnalyticsDao,
      startDateResponseDao,
      IdleOperatorPingQueue.dao,
      IdleUserPingQueue.dao,
      new CrmSenderFactory(
        jivositeClient,
        mdsUrlBuilder,
        JivositeClient.Channel("token", "channel")
      ),
      chatClient,
      brokerClient,
      enablePauseAfterContextMessage = false
    )

    val chatBackendEventProcessor =
      new OfferChatBackendEventProcessor(
        timingProcessor,
        sessionProcessor,
        botUserProcessor,
        commandProcessor,
        idlePingProcessor,
        botProcessor,
        directPassProcessor,
        actionProcessor
      )

    val messageContextBuilder = new MessageContextBuilder[OfferChatMessageContext] {
      override def build(message: Message)(implicit t: Traced): OfferChatMessageContext =
        new OfferChatMessageContext(message)(Traced.empty)
    }

    val processor: ChatEventProcessor[RentOfferChatEvent, RentOfferChatRoomState, RentOfferChatRoomState.Builder, OfferChatRoomContext, OfferChatMessageContext, OfferChatProcessingContext] =
      new ChatEventProcessor(
        new OfferChatEventInfoExtractor(clock),
        OfferChatRoomStateAccessor,
        messageContextBuilder,
        OfferChatSessionProcessor.SessionSilencePeriod,
        tracingSupport,
        focusedRoomIds = Set.empty,
        historyLoader,
        processingContextBuilder,
        chatBackendEventProcessor,
        revisitQueueDao,
        ops
      )

    val offset = PartitionOffset(GroupTopicPartition("group", "topic", 0), 0L)

    val roomId = "my-room"
    val roomCreationTime = clock.instant()
    val buyerUid = "1000"
    val ownerRequestId = "100"

    val savedRoomState = CaptureAll[RentOfferChatRoomState]()
    val startDateResponseRecord = CaptureOne[StartDateResponseRecord]()
    val jivositeRequests = CaptureAll[Request]()
    val chatBackendRequests = CaptureAll[CreateMessageParameters]()

    val remoteRoomObject = Room
      .newBuilder()
      .setId(roomId)
      .setCreated(instantToProtoTimestamp(roomCreationTime))
      .setCreator(buyerUid)
      .addUserIds(buyerUid)
      .addUserIds(RentOfferChatBotUser.toPlain)
      .addUserIds(RentOfferChatCallCenterUser.toPlain)
      .putProperties(RoomProperties.OwnerRequestId, ownerRequestId)
      .putProperties(RoomProperties.OfferId, "101")
      .build()

  }

  trait Fixture extends FixtureMocks {

    botProcessor.confusionCounter.clear()

    def stage: Int
    features.RentOfferChatBotStage.setNewState(newState = true, generation = stage)

    def initialRoomState: Option[RentOfferChatRoomState]
    @volatile private var roomStateInitialized: Boolean = false
    @volatile private var roomState: Option[RentOfferChatRoomState] = None
    (roomStateDao
      .get(_: String, _: Boolean)(_: Traced))
      .expects(roomId, true, *)
      .atLeastOnce()
      .onCall { (_, _, _) =>
        if (!roomStateInitialized) {
          roomState = initialRoomState
          roomStateInitialized = true
        }
        Future.successful(roomState)
      }
    inSequence {
      if (initialRoomState.isDefined) {
        (roomStateDao
          .update(_: String, _: RentOfferChatRoomState)(_: Traced))
          .expects(roomId, capture(savedRoomState), *)
          .onCall { (_, state, _) =>
            roomState = Some(state)
            Future.successful(true)
          }
      } else {
        (roomStateDao
          .update(_: String, _: RentOfferChatRoomState)(_: Traced))
          .expects(roomId, *, *)
          .returns(Future.successful(false))
        (roomStateDao
          .insert(_: String, _: String, _: RentOfferChatRoomState)(_: Traced))
          .expects(roomId, *, capture(savedRoomState), *)
          .onCall { (_, _, state, _) =>
            roomState = Some(state)
            Future.successful(true)
          }
      }
      (roomStateDao
        .update(_: String, _: RentOfferChatRoomState)(_: Traced))
        .expects(roomId, capture(savedRoomState), *)
        .onCall { (_, state, _) =>
          roomState = Some(state)
          Future.successful(true)
        }
    }

    (roomStateDao
      .getBreakTime(_: String)(_: Traced))
      .expects(roomId, *)
      .anyNumberOfTimes()
      .returns(Future.successful(None))

    (chatClient
      .getRoom(_: RoomId)(_: ChatRequestContext))
      .expects(RoomId(roomId), *)
      .anyNumberOfTimes()
      .returns(Future.successful(remoteRoomObject))
    (chatClient
      .updateRoom(_: RoomId, _: UpdateRoomParameters)(_: ChatRequestContext))
      .expects(RoomId(roomId), *, *)
      .anyNumberOfTimes()
      .returns(Future.successful(remoteRoomObject))
    (chatClient
      .sendMessage(_: CreateMessageParameters)(_: ChatRequestContext))
      .expects(capture(chatBackendRequests), *)
      .anyNumberOfTimes()
      .returns(Future.successful(Message.getDefaultInstance))

    def emulateMoreUnprocessedMessages: Boolean = false
    (chatClient
      .getMessages(_: RoomId, _: Window[MessageId], _: Option[Boolean])(_: ChatRequestContext))
      .expects(RoomId(roomId), *, *, *)
      .returns(
        Future.successful(
          if (emulateMoreUnprocessedMessages) {
            Seq(
              /* тут надо бы сделать сообщение с ID, который мы запросили, но мне лень */
              Message.getDefaultInstance,
              Message.getDefaultInstance
            )
          } else Seq.empty
        )
      )

    (rentFlatGrpcClient
      .getModerationFlatByOwnerRequestId(_: String, _: Set[FlatRequestFeature])(_: Traced))
      .expects(ownerRequestId, Set.empty[FlatRequestFeature], *)
      .anyNumberOfTimes()
      .returns(
        Future.successful(
          Some(
            FlatDetailedInfo
              .newBuilder()
              .applySideEffect(
                _.getFlatBuilder.getAddressBuilder
                  .setAddressWithFlat("address with flat")
                  .setSubjectFederationRgid(222)
              )
              .build()
          )
        )
      )

    def hasShowing: Boolean = false
    (rentFlatShowingClient
      .checkHasShowing(_: String, _: Long)(_: Traced))
      .expects(*, *, *)
      .anyNumberOfTimes()
      .returns(Future.successful(hasShowing))

    private def replyToFrontendRouterRequest(reply: String)(q: RouterUrlRequest)(traced: Traced): Future[String] = {
      val source = q.req
        .asInstanceOf[SearchRequest]
        .params
        .collectFirst {
          case Source(source) => source
        }
        .getOrElse(throw new IllegalArgumentException("missing expected router parameter: source"))
      Future.successful(s"$reply $source")
    }

    (frontendRouterClient
      .buildUrl(_: RouterUrlRequest)(_: Traced))
      .expects(
        where {
          case (q: RouterUrlRequest, _) =>
            q.req.asInstanceOf[SearchRequest].params.filterNot(_.isInstanceOf[Source]).map(_.toString) == Seq(
              "rgid: \"222\"",
              "type: \"RENT\"",
              "category: \"APARTMENT\"",
              "rentTime: \"SHORT\""
            )
        }
      )
      .anyNumberOfTimes()
      .onCall { replyToFrontendRouterRequest("/daily_rent_search_url")(_)(_) }
    (frontendRouterClient
      .buildUrl(_: RouterUrlRequest)(_: Traced))
      .expects(
        where {
          case (q: RouterUrlRequest, _) =>
            q.req.asInstanceOf[SearchRequest].params.filterNot(_.isInstanceOf[Source]).map(_.toString) == Seq(
              "rgid: \"222\"",
              "type: \"RENT\"",
              "category: \"APARTMENT\"",
              "yandexRent: \"YES\""
            )
        }
      )
      .anyNumberOfTimes()
      .onCall(replyToFrontendRouterRequest("/yandex_rent_search_url")(_)(_))
    (frontendRouterClient
      .buildUrl(_: RouterUrlRequest)(_: Traced))
      .expects(
        where {
          case (q: RouterUrlRequest, _) =>
            q.req.asInstanceOf[SearchRequest].params.filterNot(_.isInstanceOf[Source]).map(_.toString) == Seq(
              "rgid: \"222\"",
              "type: \"RENT\"",
              "category: \"APARTMENT\"",
              "yandexRent: \"YES\"",
              "withPets: \"YES\""
            )
        }
      )
      .anyNumberOfTimes()
      .onCall(replyToFrontendRouterRequest("/yandex_rent_with_pets_search_url")(_)(_))

    (jivositeClient
      .sendMessage(_: Request, _: JivositeClient.Channel)(_: Traced))
      .expects(capture(jivositeRequests), *, *)
      .anyNumberOfTimes()
      .returns(Future.successful(()))

    def expectNewConversationCategory: Option[Category] = None
    expectNewConversationCategory.foreach { category =>
      (statusAnalyticsDao
        .setCategory(_: String, _: Category)(_: Traced))
        .expects(roomId, category, *)
        .returns(Future.unit)
    }

    (statusAnalyticsDao
      .setHasMessagesFromUser(_: String)(_: Traced))
      .expects(roomId, *)
      .anyNumberOfTimes()
      .returns(Future.unit)
  }

  trait HasShowing {
    this: Fixture =>
    abstract override def hasShowing: Boolean = true
  }

  trait InitialBotState {
    this: Fixture =>

    def initBotState(b: BotState.Builder): Unit

    override def initialRoomState: Option[RentOfferChatRoomState] = Some(
      RentOfferChatRoomState
        .newBuilder()
        .applySideEffect(
          _.getSessionBuilder.getLimitsBuilder
            .setFrom(instantToProtoTimestamp(roomCreationTime))
            .setTo(instantToProtoTimestamp(roomCreationTime))
        )
        .applySideEffect(
          _.getSessionBuilder.getBotStateBuilder.applySideEffect(initBotState)
        )
        .build()
    )
  }

  trait InitialCategory extends Fixture {
    def currentConversationCategory: Category
    abstract override def initialRoomState: Option[RentOfferChatRoomState] = super.initialRoomState.map { rs =>
      rs.toBuilder
        .applySideEffect(_.getSessionBuilder.setCategory(currentConversationCategory))
        .build()
    }
  }

  trait ExpectIdleUserPingEnqueued extends ProcessAndCheck {
    this: Fixture =>

    abstract override def runBeforeProcessing(): Unit = {
      features.RentOfferChatIdleUserPings.setNewState(true)
      super.runBeforeProcessing()
      IdleUserPingQueue.expectEnqueue()
    }
  }

  trait Faq2State extends InitialBotState {
    this: Fixture =>
    override def initBotState(b: BotState.Builder): Unit = b.getFaq2Builder
  }

  trait DirectPassState extends InitialBotState {
    this: Fixture =>
    override def initBotState(b: BotState.Builder): Unit = b.getDirectPassBuilder
    override def initialRoomState: Option[RentOfferChatRoomState] =
      super.initialRoomState
        .map(
          _.toBuilder
            .applySideEffect(
              _.getSessionBuilder.setLastMessageSentToOperator("x")
            )
            .build()
        )
  }

  trait Closed extends DirectPassState {
    this: Fixture =>
    override def initialRoomState: Option[RentOfferChatRoomState] = super.initialRoomState.map(
      _.toBuilder.applySideEffect(_.getSessionBuilder.setClosedByOperator(true)).build()
    )
  }

  trait InitAppData extends InitialBotState {
    this: Fixture =>
    def initAppData(adb: ApplicationData.Builder): Unit
    abstract override def initialRoomState: Option[RentOfferChatRoomState] = {
      super.initialRoomState.map { rs =>
        rs.toBuilder
          .applySideEffect { rs =>
            initAppData(rs.getSessionBuilder.getApplicationDataBuilder)
          }
          .build()
      }
    }
  }

  trait ProcessAndCheck {
    this: Fixture =>

    def event: RentOfferChatEvent

    def expectBotState: StateCase
    def expectBotStateToRestore: StateCase = expectBotState

    def expectCBMessages: Seq[CBMessage] = Seq.empty

    def messageWithKeyboard(text: String, keyIds: String*): CBMessage = CBMessage(
      text,
      propType = MessagePropertyType.ACTIONS,
      keyboard = Seq(keyIds: _*)
    )

    def expectedSessionStartTime: Instant = roomCreationTime
    def expectedSessionStartMessageId: String = ""
    def expectAnalyticsRecordCreation: Boolean = false
    def expectStartDateResponseClearance: Boolean = false
    def expectStartDateResponseSetting: Boolean = false

    def expectChatClosed: Boolean = false

    if (expectAnalyticsRecordCreation) {
      (statusAnalyticsDao
        .create(_: String, _: Instant)(_: Traced))
        .expects(roomId, expectedSessionStartTime, *)
        .returns(Future.unit)
    }
    if (expectStartDateResponseClearance) {
      (startDateResponseDao
        .clear(_: String)(_: Traced))
        .expects(roomId, *)
        .returns(Future.unit)
    }
    if (expectStartDateResponseSetting) {
      (startDateResponseDao
        .set(_: StartDateResponseRecord)(_: Traced))
        .expects(capture(startDateResponseRecord), *)
        .returns(Future.unit)
    }

    def checkAfterProcessing(): Unit = {
      botProcessor.confusionCounter.get() shouldBe 0.0

      checkFinalRoomState()

      if (expectStartDateResponseSetting) {
        val initialBotState = initialRoomState.map(_.getSession.getBotState).get
        assume(initialBotState.hasStartDatePrompt)
        assume(initialBotState.getStartDatePrompt.getPresetsVersion != botConfig.getStartDatePresetsVersion)
        startDateResponseRecord.value.configVersion shouldBe initialBotState.getStartDatePrompt.getPresetsVersion
      }

      val expectedCbMessages = expectCBMessages
      chatBackendRequests.values.size shouldBe expectedCbMessages.size
      chatBackendRequests.values.zip(expectedCbMessages).zipWithIndex.foreach {
        case ((actual, expected), index) =>
          withClue(s"message index $index") {
            expected.matchAgainst(actual)
          }
      }
    }

    def checkFinalRoomState(): Unit = {
      savedRoomState.value.hasSession shouldBe true
      savedRoomState.value.getSession.hasLimits shouldBe true
      savedRoomState.value.getSession.getLimits.getFrom shouldBe
        instantToProtoTimestamp(expectedSessionStartTime)
      savedRoomState.value.getSession.getLimits.getFromMessage shouldBe expectedSessionStartMessageId

      savedRoomState.value.getSession.getBotState.getStateCase shouldBe expectBotState

      savedRoomState.value.getSession.getClosedByOperator shouldBe expectChatClosed

      savedRoomState.value.getSession.getBotStateToRestore.getStateCase shouldBe expectBotStateToRestore
    }

    def runBeforeProcessing(): Unit = {}

    runBeforeProcessing()
    whenReady(processor.process(offset, event)) { _ =>
      checkAfterProcessing()
    }
  }

  case class CBMessage(
    text: String,
    propType: MessagePropertyType = MessagePropertyType.MESSAGE_PROPERTY_TYPE_UNKNOWN,
    keyboard: Seq[String] = Seq.empty
  ) {

    def matchAgainst(m: CreateMessageParameters): Unit = {
      m.getPayload.getValue shouldBe text
      m.getProperties.getType shouldBe propType
      m.getProperties.getKeyboard.getButtonsList.asScala.map(_.getId) shouldBe keyboard
    }
  }

  trait OnRoomCreation extends ProcessAndCheck {
    this: Fixture =>

    override lazy val event: RentOfferChatEvent = {
      RentOfferChatEvent
        .newBuilder()
        .applySideEffect(
          _.getChatBackendEventBuilder.getPayloadBuilder.getRoomCreatedBuilder
            .setRoomId(roomId)
            .setCreator(buyerUid)
        )
        .build()
    }

    override def expectAnalyticsRecordCreation: Boolean = true

    abstract override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()

      (historyLoader
        .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
        .expects(roomId, None, false, roomCreationTime, *)
        .atLeastOnce()
        .returns(Future.successful(Seq.empty))
    }

    abstract override def checkFinalRoomState(): Unit = {
      super.checkFinalRoomState()
      savedRoomState.value.getSession.getLimits.getTo shouldBe instantToProtoTimestamp(roomCreationTime)
      savedRoomState.value.getSession.getLimits.getToMessage shouldBe ""
    }
  }

  trait OnMessage extends ProcessAndCheck {
    this: Fixture =>

    def eventMessageId: String = "1"
    def eventMessageAuthor: String = buyerUid
    def eventMessageCreated: Instant = roomCreationTime.plusSeconds(1)
    def eventMessageText: String = "Hello!"
    def eventMessageKeyboardResponse: Option[String] = None

    lazy val message: Message = {
      Message
        .newBuilder()
        .setId(eventMessageId)
        .setAuthor(eventMessageAuthor)
        .setCreated(instantToProtoTimestamp(eventMessageCreated))
        .applySideEffect(_.getPayloadBuilder.setContentType(TEXT_PLAIN).setValue(eventMessageText))
        .applySideEffects[String](
          eventMessageKeyboardResponse,
          _.getPropertiesBuilder.getKeyboardResponseBuilder.setButtonId(_)
        )
        .build()
    }

    override lazy val event: RentOfferChatEvent = {
      RentOfferChatEvent
        .newBuilder()
        .applySideEffect(
          _.getChatBackendEventBuilder.getPayloadBuilder.getMessageSentBuilder
            .setRoom(remoteRoomObject)
            .setMessage(message)
        )
        .build()
    }

    abstract override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()
      (historyLoader
        .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
        .expects(roomId, None, false, TimeUtils.toJavaInstant(message.getCreated), *)
        .atLeastOnce()
        .returns(Future.successful(Seq(message)))
      (historyLoader
        .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
        .expects(roomId, Some(message.getId), false, TimeUtils.toJavaInstant(message.getCreated), *)
        .anyNumberOfTimes()
        .returns(Future.successful(Seq(message)))
    }

    abstract override def checkFinalRoomState(): Unit = {
      super.checkFinalRoomState()

      savedRoomState.value.getSession.getLimits.getTo shouldBe message.getCreated
      savedRoomState.value.getSession.getLimits.getToMessage shouldBe message.getId

      checkIdleUserPingDataInRoomState()
    }

    def checkIdleUserPingDataInRoomState(): Unit = {
      savedRoomState.value.getSession.getUnansweredMessageId shouldBe ""
      savedRoomState.value.getSession.hasUnansweredEventTime shouldBe false
    }
  }

  trait OnDirectPassMessage extends OnMessage {
    this: Fixture =>
    override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()
      // такой запрос сделает DirectPassProcessor;
      // притворяемся, что сообщение "x" было сразу перед сообщением из события
      (historyLoader
        .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
        .expects(roomId, Some("x"), true, eventMessageCreated, *)
        .atLeastOnce()
        .returns(Future.successful(Seq(message)))
    }
  }

  trait OnOperatorMessage extends OnMessage {
    this: Fixture =>
    final override def eventMessageAuthor: String = RentOfferChatCallCenterUser.toPlain
    override def checkIdleUserPingDataInRoomState(): Unit = {
      savedRoomState.value.getSession.getUnansweredMessageId shouldBe eventMessageId
      savedRoomState.value.getSession.getUnansweredEventTime shouldBe instantToProtoTimestamp(eventMessageCreated)
      savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue) shouldBe
        Seq(eventMessageText)
    }
  }

  trait OnBotMessage extends OnMessage {
    this: Fixture =>
    final override def eventMessageAuthor: String = RentOfferChatBotUser.toPlain
    override def checkIdleUserPingDataInRoomState(): Unit = {
      savedRoomState.value.getSession.getUnansweredMessageId shouldBe eventMessageId
      savedRoomState.value.getSession.getUnansweredEventTime shouldBe instantToProtoTimestamp(eventMessageCreated)
      val mtr = savedRoomState.value.getSession.getMessagesToResendList.asScala.map(_.getPayload.getValue)
      expectCBMessages.takeRight(mtr.size).map(_.text) shouldBe mtr
    }
  }

  trait OnRevisit extends ProcessAndCheck {
    this: Fixture =>

    override lazy val event: RentOfferChatEvent = {
      RentOfferChatEvent
        .newBuilder()
        .applySideEffect(
          _.getRevisitBuilder
            .setRoomId(roomId)
            .setForce(false)
        )
        .build()
    }

    abstract override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()
      // такой запрос сделает DirectPassProcessor;
      // притворяемся, что сообщение "x" было сразу перед сообщением из события
      val toMessageId = initialRoomState.map(_.getSession.getLimits.getToMessage).filter(_.nonEmpty)
      (historyLoader
        .loadHistoryFrom(_: String, _: Option[String], _: Boolean, _: Instant)(_: Traced))
        .expects(roomId, toMessageId, toMessageId.isDefined, *, *)
        .returns(Future.successful(Seq.empty))
    }
  }

  trait ExpectActionRecord extends ProcessAndCheck {
    this: Fixture =>
    abstract override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()
      (brokerClient
        .send(
          _: Option[String],
          _: chatactionlog.OfferChatAction,
          _: Option[String]
        )(_: ProtoMarshaller[chatactionlog.OfferChatAction]))
        .expects(*, *, *, *)
        .atLeastOnce()
        .returns(Future.unit)
    }
  }

  trait ExpectMoveToDirectPass extends ProcessAndCheck with ExpectActionRecord {
    this: Fixture =>

    abstract override def runBeforeProcessing(): Unit = {
      super.runBeforeProcessing()
      (statusAnalyticsDao
        .setSwitchToOperatorTime(_: String, _: Instant)(_: Traced))
        .expects(roomId, clock.instant(), *)
        .returns(Future.unit)
    }

    final override def expectBotState: StateCase = StateCase.DIRECT_PASS
    def expectAnswerFromUser: Boolean = false

    abstract override def checkAfterProcessing(): Unit = {
      super.checkAfterProcessing()
      jivositeRequests.values.size should be > 0
      savedRoomState.value.getSession.getBotState.getDirectPass.getNotExpectingUserAnswer shouldBe !expectAnswerFromUser
    }
  }

  trait ExpectMoveToFaq extends ProcessAndCheck {
    this: Fixture =>
    final override def expectBotState: StateCase = StateCase.FAQ_CHOICE
    final override def expectCBMessages: Seq[CBMessage] =
      Seq(messageWithKeyboard("MFaq", "Id", "9", "10"))
  }

  trait ExpectMoveToFailedBye extends ProcessAndCheck {
    this: Fixture =>
    override def expectBotState: StateCase = StateCase.FAILED_BYE
    override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFailedBye"))
  }

  trait ExpectGreetingWithApplication extends ProcessAndCheck {
    this: Fixture =>
    final override def expectNewConversationCategory: Option[Category] = Some(Category.CLIENT)
    final override def expectBotState: StateCase = StateCase.Q1_WITH_APP
    def expectIntroduction: Boolean = false
    final override def expectCBMessages: Seq[CBMessage] =
      (if (expectIntroduction) Seq(CBMessage("MIntroduction")) else Seq.empty) :+
        messageWithKeyboard("MGreetWithApplication", "7", "8")
  }

  trait TestFallbackToManagerOnFreeInput extends Fixture with OnMessage with ExpectMoveToDirectPass {
    override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MFallbackToManager"))
  }

  trait TestFallbackToManagerOnFreeInputWithShowing
    extends Fixture
    with HasShowing
    with OnMessage
    with ExpectMoveToDirectPass {
    override def expectCBMessages: Seq[CBMessage] = Seq(CBMessage("MShowingDiscoveredOnFreeInput"))
  }

}
