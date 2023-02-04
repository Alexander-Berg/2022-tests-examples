package ru.yandex.vos2.autoru.feedprocessor

import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.{Category, OfferStatus, Section, Offer => Form}
import ru.auto.feedprocessor.FeedprocessorModel._
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.request.RequestContext
import ru.yandex.vertis.ydb.skypper.settings.TransactionSettings
import ru.yandex.vertis.ydb.skypper.{YdbQueryExecutor, YdbWrapper}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferService}
import ru.yandex.vos2.api.model.WithFailures
import ru.yandex.vos2.autoru.dao.ActivationSuccess
import ru.yandex.vos2.autoru.dao.feeds.AutoruFeedProcessorDao
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.dao.proxy.{AdditionalData, FormsBatchWriter, OffersReader, OffersWriter}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.RichFeedprocessorTask
import ru.yandex.vos2.autoru.model.extdata.BanReasons
import ru.yandex.vos2.autoru.model.{OfferInfo, SalonPoi, TestUtils}
import ru.yandex.vos2.autoru.utils.FeedprocessorHashUtils
import ru.yandex.vos2.autoru.utils.moderation.ModerationProtection
import ru.yandex.vos2.autoru.utils.options.OptionsEnricher
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.model.{OfferRef, UserRef, UserRefAutoruClient}
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import ru.yandex.vos2.{getNow, OfferID, OfferModel}

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

// scalastyle:off multiple.string.literals
class AutoruOffersRequestHandlerSpec extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfter {

  private val formsBatchWriter = mock[FormsBatchWriter]
  private val offersReader = mock[OffersReader]
  private val feedDao = mock[AutoruFeedProcessorDao]
  private val mdsPhotoUtils = mock[MdsPhotoUtils]
  private val feedprocessorHashCheckOnly = mock[Feature[Boolean]]
  private val truckMotoWaitActivation = mock[Feature[Boolean]]
  private val moderationProtection = mock[ModerationProtection]
  private val banReason = mock[BanReasons]

  private val featuresRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featuresRegistry)

  featuresRegistry.updateFeature(featuresManager.FeedProcessorDeduplication.name, true)

  private val optionsEnricher = {
    val op = mock[OptionsEnricher]
    stub(op.enrichEquipmentForEntities(_: UserRef, _: Map[Entity, Option[Offer]])(_: Traced)) {
      case (_, entities, _) =>
        entities.keys.toSeq
    }
    op
  }

  private val formTemplate = Form
    .newBuilder()
    .setCategory(Category.CARS)
    .setSection(Section.USED)
    .setUserRef("ac_26352")
    .build()

  private val offerTemplate = OfferModel.Offer
    .newBuilder()
    .setUserRef("ac_26352")
    .setOfferService(OfferService.OFFER_AUTO)
    .setTimestampUpdate(123)
    .build()

  private val messageTemplate = OffersRequest
    .newBuilder()
    .setTimestamp(getNow)
    .setType(MessageType.OFFER_STREAM_BATCH)
    .addEntities(Entity.newBuilder().setPosition(0).setAutoru(formTemplate))
    .setTask(
      Task
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .setId(1)
        .setFeedId("11")
        .setClientId(26352)
    )
    .build()
  private val additionalData = AdditionalData()
  private val registry = TestOperationalSupport.prometheusRegistry

  private val ydb = mock[YdbWrapper]

  stub(
    ydb.transaction(_: String, _: TransactionSettings)(_: YdbQueryExecutor => _)(_: Traced, _: RequestContext)
  ) {
    case (_, _, executor, _, _) =>
      ()
  }

  before {
    reset(formsBatchWriter)
    reset(banReason)
  }

  when(offersReader.loadAdditionalData(?, any[Map[String, Form]]())(?)).thenReturn(additionalData)
  when(feedprocessorHashCheckOnly.value).thenReturn(true)
  when(truckMotoWaitActivation.value).thenReturn(true)
  when(feedDao.findCntErrorByTaskId(?, ?)).thenReturn(Some(0))
  doNothing().when(feedDao).upsertCntError(?, ?, ?)
  doNothing().when(feedDao).upsertCntError(?, ?, ?)
  doNothing().when(feedDao).deleteCntError(?, ?)
  doNothing().when(feedDao).checkAndCreateDeduplicationRecord(?, ?)(?)

  private val expectedFeedprocessorHash =
    FeedprocessorHashUtils.getFeedprocessorHash(UserRefAutoruClient(26352), Category.CARS, formTemplate)

  private def buildHandler(offerDao: AutoruOfferDao, offersWriter: OffersWriter) = {
    val failedOfferHandler = new FailedOfferHandler(() => offerDao)
    val createFeedUploader = new FeedUploader(
      formsBatchWriter,
      () => offerDao,
      offersReader,
      offersWriter,
      feedDao,
      failedOfferHandler,
      mdsPhotoUtils,
      moderationProtection,
      optionsEnricher,
      banReason
    )(_, _)
    val streamEndHandler = new StreamEndHandler(() => offerDao, offersWriter, feedDao)
    new AutoruOffersRequestHandler(
      createFeedUploader,
      failedOfferHandler,
      streamEndHandler,
      registry
    )
  }

  "AutoruOffersRequestHandler" should {
    "restore user removed offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = offerTemplate.toBuilder.setOfferID("aabb-123").addFlag(OfferFlag.OF_DELETED).build()
      stub((userRef: UserRef, feedprocessorIds: Seq[String], notAllowedStatuses: Seq[CompositeStatus], trace: Traced) =>
        offerDao.findAllByFeedprocessorId(userRef, feedprocessorIds, notAllowedStatuses)(trace)
      ) {
        case (_, ids, statuses, _) =>
          statuses shouldNot contain(CompositeStatus.CS_REMOVED)
          ids.toList shouldEqual List(expectedFeedprocessorHash)
          Map(ids.head -> Seq(offer))
      }

      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))

      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map.empty[String, formsBatchWriter.CreateOrUpdateResult])
      when(offersWriter.setArchiveBatch(?, ?, ?, ?, ?, ?)(?))
        .thenReturn(true)
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)

      handler.handle(messageTemplate, 0)

      lazy val formsMatcher = ArgumentMatchers.argThat { forms: Map[String, Form] =>
        forms.size == 1 && forms.head._1 == "aabb-123"
      }

      verify(offersWriter).setArchiveBatch(?, eq(Seq("aabb-123")), ?, ?, ?, ?)(?)
      verify(formsBatchWriter)
        .batchUpdate(?, eq(Category.CARS), formsMatcher, ?, eq(additionalData), ?, eq("feedprocessor"))(?)
      verify(offerDao).findAllByFeedprocessorId(?, ?, ?)(?)
    }

    "not match offers of banned users (STATUS_FREEZED)" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      stub((userRef: UserRef, feedprocessorIds: Seq[String], notAllowedStatuses: Seq[CompositeStatus], trace: Traced) =>
        offerDao.findAllByFeedprocessorId(userRef, feedprocessorIds, notAllowedStatuses)(trace)
      ) {
        case (_, ids, statuses, _) =>
          statuses should be(empty)
          ids.toList shouldEqual List(expectedFeedprocessorHash)
          Map()
      }
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)

      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map.empty[String, formsBatchWriter.CreateOrUpdateResult])

      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(messageTemplate, 0)

      lazy val formsMatcher = ArgumentMatchers.argThat { forms: Map[String, Form] =>
        forms.size == 1 && forms.head._2 == formTemplate
      }
      verify(formsBatchWriter)
        .batchCreate(?, eq(Category.CARS), formsMatcher, ?, eq(additionalData), ?, eq("feedprocessor"))(?)
      verify(offerDao).findAllByFeedprocessorId(?, ?, ?)(?)
    }

    // оффер expired
    "activate expired offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).addFlag(OfferFlag.OF_EXPIRED).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationSuccess(CompositeStatus.CS_ACTIVE))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val response = handler.handle(messageTemplate, 0)
      response.getEntitiesList should have size 1
      response.getEntities(0).getOfferStatus should (equal(OfferStatus.NEED_ACTIVATION) or equal(OfferStatus.ACTIVE))
      response.getEntities(0).getCompositeStatusObsolete should (equal(CompositeStatus.CS_NEED_ACTIVATION.getNumber) or
        equal(CompositeStatus.CS_ACTIVE.getNumber))

      verify(offersWriter).activate(eq(offer.getOfferID), ?, ?, ?, ?, ?, ?)(?)
    }

    // оффер скрыт и expired
    "don't activate hidden expired offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils
        .createOffer(dealer = true)
        .addFlag(OfferFlag.OF_EXPIRED)
        .addFlag(OfferFlag.OF_INACTIVE)
        .build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationSuccess(CompositeStatus.CS_ACTIVE))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(messageTemplate, 0)

      verify(offersWriter, never()).activate(?, ?, ?, ?, ?, ?, ?)(?)
    }

    // оффер скрыт и expired - но пользователь хочет его показать (action=show)
    "activate showing hidden expired offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils
        .createOffer(dealer = true)
        .addFlag(OfferFlag.OF_EXPIRED)
        .addFlag(OfferFlag.OF_INACTIVE)
        .build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationSuccess(CompositeStatus.CS_ACTIVE))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.SHOW)
      handler.handle(message.build(), 0)

      verify(offersWriter).activate(eq(offer.getOfferID), ?, ?, ?, ?, ?, ?)(?)
    }

    // оффер expired - но пользователь хочет его скрыть
    "recall hiding expired offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).addFlag(OfferFlag.OF_EXPIRED).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.recall(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(true)
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.HIDE)
      handler.handle(message.build(), 0)

      verify(offersWriter, never()).activate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter).recall(eq(offer.getOfferID), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)
    }

    // оффер скрыт и пользователь хочет его "скрыть" повторно
    "don't touch already hidden offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).addFlag(OfferFlag.OF_INACTIVE).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.HIDE)
      handler.handle(message.build(), 0)

      verify(offersWriter, never()).recall(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)
    }

    // оффер не скрыт и пользователь хочет его "показать" повторно
    "don't touch already active offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.SHOW)
      handler.handle(message.build(), 0)

      verify(offersWriter, never()).activate(?, ?, ?, ?, ?, ?, ?)(?)
    }

    "hide active offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.recall(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(true)
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.HIDE)
      handler.handle(message.build(), 0)

      verify(offersWriter).recall(eq(offer.getOfferID), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)
    }

    "show hidden offers" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).addFlag(OfferFlag.OF_INACTIVE).build()
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(expectedFeedprocessorHash -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(offersWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationSuccess(CompositeStatus.CS_ACTIVE))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)

      val handler = buildHandler(offerDao, offersWriter)
      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setAction(Action.SHOW)
      handler.handle(message.build(), 0)

      verify(offersWriter).activate(eq(offer.getOfferID), ?, ?, ?, ?, ?, ?)(?)
    }

    "don't any update for banned offer with not editable reason" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils
        .createOffer(dealer = true)
        .addFlag(OfferFlag.OF_BANNED)
        .addFlag(OfferFlag.OF_INACTIVE)
        .addReasonsBan("duplicate")
        .build()

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map(expectedFeedprocessorHash -> Seq(offer)))
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)
      when(banReason.canEdit(any[Offer]())).thenReturn(false)

      val handler = buildHandler(offerDao, offersWriter)
      val response = handler.handle(messageTemplate, 0)

      verify(formsBatchWriter, never()).batchCreate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(formsBatchWriter, never()).batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).activate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).setArchive(?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).recall(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)
      verify(feedDao).upsertCntError(?, ?, ?)

      assert(response.getEntitiesCount == 1)
      assert(response.getEntities(0).getStatus == UpdateStatus.ERROR)
      assert(response.getEntities(0).getErrorsCount == 1)
      assert(
        response.getEntities(0).getErrors(0) == Entity.Error
          .newBuilder()
          .setMessage(s"Объявление ${offer.getOfferID} заблокировано")
          .setContext("vos")
          .build()
      )
    }

    "update banned offer with editable reason" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils
        .createOffer(dealer = true)
        .addFlag(OfferFlag.OF_BANNED)
        .addFlag(OfferFlag.OF_INACTIVE)
        .addReasonsBan("not_verified")
        .build()

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map(expectedFeedprocessorHash -> Seq(offer)))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Seq("123"), Seq("abc"))))
      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?))
        .thenReturn(Map(offer.getOfferID -> Right(FormsBatchWriter.Update(offer, Nil))))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)
      when(banReason.canEdit(any[Offer]())).thenReturn(true)

      val handler = buildHandler(offerDao, offersWriter)
      val response = handler.handle(messageTemplate, 0)

      verify(formsBatchWriter, never()).batchCreate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(formsBatchWriter, times(1)).batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).activate(?, ?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).setArchive(?, ?, ?, ?, ?, ?)(?)
      verify(offersWriter, never()).recall(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)(?)

      assert(response.getEntitiesCount == 1)
      assert(response.getEntities(0).getStatus == UpdateStatus.UPDATE)
    }

    "touch invalid offer by VIN" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).build()

      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setVin("VIN123").getAutoruBuilder.getDocumentsBuilder.setVin("VIN123")
      val form = message.getEntities(0).getAutoru

      val offerHash = FeedprocessorHashUtils.getFeedprocessorHash(
        UserRef.refAutoruClient(message.getTask.getClientId),
        Category.CARS,
        form
      )

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?)).thenReturn(Map.empty[String, Seq[Offer]])
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(formsBatchWriter.batchCreate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Map(offerHash -> Left(Nil)))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Nil, Nil)))
      when(offerDao.findByFeedAndVin(?, ?, eq(Seq("VIN123")), ?)(?))
        .thenReturn(Map("VIN123" -> OfferInfo(OfferRef(offer), isRemoved = false, CompositeStatus.CS_ACTIVE)))
      doNothing().when(offerDao).updateFeedprocessorTask(?, ?, ?, ?, ?)(?)

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(message.build(), 0)

      verify(offerDao).updateFeedprocessorTask(
        ?,
        eq(message.getTask.getFeedprocessorFeedId),
        eq(message.getTask.getId),
        ?,
        eq(Seq(offer.getOfferID))
      )(?)
    }

    "touch invalid offer by unique_id" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offer = TestUtils.createOffer(dealer = true).build()

      val message = messageTemplate.toBuilder
      message.getEntitiesBuilder(0).setUniqueId("UID123").getAutoruBuilder.setFeedprocessorUniqueId("UID123")
      val form = message.getEntities(0).getAutoru

      val offerHash = FeedprocessorHashUtils.getFeedprocessorHash(
        UserRef.refAutoruClient(message.getTask.getClientId),
        Category.CARS,
        form
      )

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?))
        .thenReturn(Map(offerHash -> Seq(offer)))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Map(offer.getOfferID -> Left(Nil)))
      when(mdsPhotoUtils.getOrigMdsPhotoInfo(?)).thenReturn(None)
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Try(WithFailures(Nil, Nil)))
      when(offerDao.findByFeedAndUniqueId(?, ?, eq(Seq("UID123")), ?)(?))
        .thenReturn(Map("UID123" -> OfferInfo(OfferRef(offer), isRemoved = false, CompositeStatus.CS_ACTIVE)))
      doNothing().when(offerDao).updateFeedprocessorTask(?, ?, ?, ?, ?)(?)

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(message.build(), 0)

      verify(offerDao).updateFeedprocessorTask(
        ?,
        eq(message.getTask.getFeedprocessorFeedId),
        eq(message.getTask.getId),
        ?,
        eq(Seq(offer.getOfferID))
      )(?)
    }

    "preserve current photo sorting after update" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offerBuilder = TestUtils.createOffer(dealer = true)
      offerBuilder.getOfferAutoruBuilder.getSalonBuilder
        .setAllowPhotoReorder(true)
        .setSalonId("1")
        .setTitle("foo")
      val salonPoi = mock[SalonPoi]
      when(salonPoi.allowPhotoReorder).thenReturn(true)

      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo1")
        .setSmartOrder(1)
        .setOrder(3)
        .setIsMain(true)
        .setCreated(111) // hand uploaded
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo2")
        .setSmartOrder(2)
        .setOrder(4)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo3")
        .setSmartOrder(3)
        .setOrder(2)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo4")
        .setSmartOrder(4)
        .setOrder(1)
        .setIsMain(false)
        .setCreated(111) // hand uploaded
      val offer = offerBuilder.build()

      val message = messageTemplate.toBuilder
      message.getTaskBuilder.setLeaveHandUploadedPhoto(true)

      val entity = message.getEntitiesBuilder(0)
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo3")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new1")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo2")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new2")

      val form = message.getEntities(0).getAutoru

      val offerHash = FeedprocessorHashUtils.getFeedprocessorHash(
        UserRef.refAutoruClient(message.getTask.getClientId),
        Category.CARS,
        form
      )

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?))
        .thenReturn(Map(offerHash -> Seq(offer)))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Map(offer.getOfferID -> Left(Nil)))
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Success(WithFailures(Nil)))
      when(offersReader.loadAdditionalData(?, any[Map[String, Form]]())(?)).thenReturn(AdditionalData(salonPoi))

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(message.build(), 0)

      val formsCaptor = ArgumentCaptor.forClass[Map[OfferID, Form], Map[OfferID, Form]](classOf[Map[OfferID, Form]])
      verify(formsBatchWriter).batchUpdate(?, ?, formsCaptor.capture(), ?, ?, ?, ?)(?)

      val forms = formsCaptor.getValue
      forms should have size 1
      val (_, formForUpdate) = forms.head

      val images = formForUpdate.getState.getImageUrlsList.asScala.map(_.getName).toList
      images shouldEqual List("photo1", "photo2", "photo3", "photo4", "photo_new1", "photo_new2")
    }

    "rearrange photos after update if smart_order disabled for offer" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offerBuilder = TestUtils.createOffer(dealer = true)
      offerBuilder.getOfferAutoruBuilder.getSalonBuilder
        .setAllowPhotoReorder(false)
        .setSalonId("1")
        .setTitle("foo")
      val salonPoi = mock[SalonPoi]
      when(salonPoi.allowPhotoReorder).thenReturn(false)

      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo1")
        .setSmartOrder(1)
        .setOrder(3)
        .setIsMain(true)
        .setCreated(111) // hand uploaded
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo2")
        .setSmartOrder(2)
        .setOrder(4)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo3")
        .setSmartOrder(3)
        .setOrder(2)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo4")
        .setSmartOrder(4)
        .setOrder(1)
        .setIsMain(false)
        .setCreated(111) // hand uploaded
      val offer = offerBuilder.build()

      val message = messageTemplate.toBuilder
      message.getTaskBuilder.setLeaveHandUploadedPhoto(true)

      val entity = message.getEntitiesBuilder(0)
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo3")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new1")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo2")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new2")

      val form = message.getEntities(0).getAutoru

      val offerHash = FeedprocessorHashUtils.getFeedprocessorHash(
        UserRef.refAutoruClient(message.getTask.getClientId),
        Category.CARS,
        form
      )

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?))
        .thenReturn(Map(offerHash -> Seq(offer)))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Map(offer.getOfferID -> Left(Nil)))
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Success(WithFailures(Nil)))
      when(offersReader.loadAdditionalData(?, any[Map[String, Form]]())(?)).thenReturn(AdditionalData(salonPoi))

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(message.build(), 0)

      val formsCaptor = ArgumentCaptor.forClass[Map[OfferID, Form], Map[OfferID, Form]](classOf[Map[OfferID, Form]])
      verify(formsBatchWriter).batchUpdate(?, ?, formsCaptor.capture(), ?, ?, ?, ?)(?)

      val forms = formsCaptor.getValue
      forms should have size 1
      val (_, formForUpdate) = forms.head

      val images = formForUpdate.getState.getImageUrlsList.asScala.map(_.getName).toList
      images shouldEqual List("photo1", "photo4", "photo3", "photo_new1", "photo2", "photo_new2")
    }

    "remove hand uploaded photos if no flag" in {
      val offerDao = mock[AutoruOfferDao]
      val offersWriter = mock[OffersWriter]
      val offerBuilder = TestUtils.createOffer(dealer = true)
      offerBuilder.getOfferAutoruBuilder.getSalonBuilder
        .setAllowPhotoReorder(false)
        .setSalonId("1")
        .setTitle("foo")
      val salonPoi = mock[SalonPoi]
      when(salonPoi.allowPhotoReorder).thenReturn(false)

      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo1")
        .setSmartOrder(1)
        .setOrder(3)
        .setIsMain(true)
        .setCreated(111) // hand uploaded
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo2")
        .setSmartOrder(2)
        .setOrder(4)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo3")
        .setSmartOrder(3)
        .setOrder(2)
        .setIsMain(false)
        .setCreated(111)
        .setCreatedByFeedprocessor(true)
      offerBuilder.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("photo4")
        .setSmartOrder(4)
        .setOrder(1)
        .setIsMain(false)
        .setCreated(111) // hand uploaded
      val offer = offerBuilder.build()

      val message = messageTemplate.toBuilder
      message.getTaskBuilder.setLeaveHandUploadedPhoto(false)

      val entity = message.getEntitiesBuilder(0)
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo3")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new1")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo2")
      entity.getAutoruBuilder.getStateBuilder.addImageUrlsBuilder().setName("photo_new2")

      val form = message.getEntities(0).getAutoru

      val offerHash = FeedprocessorHashUtils.getFeedprocessorHash(
        UserRef.refAutoruClient(message.getTask.getClientId),
        Category.CARS,
        form
      )

      when(offerDao.findAllByFeedprocessorId(?, ?, ?)(?))
        .thenReturn(Map(offerHash -> Seq(offer)))
      when(offerDao.getOffersByParams(?, ?)(?)).thenReturn(Seq.empty)
      when(moderationProtection.matchOfferByVitalFields(?, ?)).thenReturn(Some(offer))
      when(formsBatchWriter.batchUpdate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(Map(offer.getOfferID -> Left(Nil)))
      when(offerDao.useWithUser(?, ?, ?, ?, ?)(?)(?)).thenReturn(Success(WithFailures(Nil)))
      when(offersReader.loadAdditionalData(?, any[Map[String, Form]]())(?)).thenReturn(AdditionalData(salonPoi))

      val handler = buildHandler(offerDao, offersWriter)
      handler.handle(message.build(), 0)

      val formsCaptor = ArgumentCaptor.forClass[Map[OfferID, Form], Map[OfferID, Form]](classOf[Map[OfferID, Form]])
      verify(formsBatchWriter).batchUpdate(?, ?, formsCaptor.capture(), ?, ?, ?, ?)(?)

      val forms = formsCaptor.getValue
      forms should have size 1
      val (_, formForUpdate) = forms.head

      val images = formForUpdate.getState.getImageUrlsList.asScala.map(_.getName).toList
      images shouldEqual List("photo3", "photo_new1", "photo2", "photo_new2")
    }
  }
}
