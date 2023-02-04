package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.yandex.ydb.ValueProtos.Type.PrimitiveTypeId
import com.yandex.ydb.ValueProtos.{Column, ResultSet, Type}
import com.yandex.ydb.table.YdbTable.ExecuteQueryResult
import com.yandex.ydb.table.query.DataQueryResult
import com.yandex.ydb.table.values.PrimitiveValue
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import ru.auto.api.ApiOfferModel.Category
import ru.vertis.holocron.autoru.VehiclePhoto
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.ydb.skypper.result.{ResultSetWrapper, ResultSetWrapperImpl}
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.services.meta.PhotoMetaSender
import ru.yandex.vos2.autoru.utils.time.TimeService
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.dao.offers.ng.YdbRequests.getWorkerShardId
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import ru.yandex.vos2.{BasicsModel, OfferModel}

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SendMetaWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport with PrivateMethodTester {

  implicit val traced: Traced = Traced.empty

  def nextCheckAnswer(dateTime: DateTime): Iterator[ResultSetWrapper] = {
    val epoch = dateTime.getMillis / 1000
    val resultSetsList = ExecuteQueryResult
      .newBuilder()
      .addResultSets(
        ResultSet
          .newBuilder()
          .addColumns(
            Column
              .newBuilder()
              .setName("next_check")
              .setType(Type.newBuilder().setTypeId(PrimitiveTypeId.TIMESTAMP))
              .build()
          )
          .addRows(
            PrimitiveValue.timestamp(epoch).toPb
          )
      )
      .build()
      .getResultSetsList
    val data = new DataQueryResult("2", resultSetsList)
    val resultSet: ResultSetWrapper = new ResultSetWrapperImpl(
      data.getResultSet(0)
    )
    Iterator(
      resultSet
    )
  }

  private val timeService = mock[TimeService]
  when(timeService.getNow).thenReturn(new DateTime(1000))

  abstract private class Fixture {
    val offer: Offer
    val photoMetaSender = mock[PhotoMetaSender]
    val mdsPhotoUtils: MdsPhotoUtils = mock[MdsPhotoUtils]
    val mockedFeatureManager = mock[FeaturesManager]
    val daoMocked = mock[AutoruOfferDao]

    val worker = new SendMetaWorkerYdb(
      photoMetaSender,
      mdsPhotoUtils,
      timeService
    ) with YdbWorkerTestImpl {
      override def offerDaoVos: AutoruOfferDao = daoMocked
      override def features = mockedFeatureManager
      implicit override def ec: ExecutionContext = Threads.SameThreadEc
    }
  }

  "SendMetaWorker YDB" should {

    "toCarPhoto " in new Fixture {
      val offer = createOffer().build
      val offerId = offer.getOfferID
      val p = BasicsModel.Photo.newBuilder()
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.addNumbersBuilder().setConfidence(0.892).setNumber("M958KB159").setWidthPercent(0.1)
      p.getMetaBuilder.getAutoruNnetFeaturesBuilder.addAllProdV6EncToloka96(Seq(0.1, 0.2, 0.3).map(Double.box).asJava)

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(Some("http://example.com"))

      val carPhoto = worker.toCarPhoto(offer, p, 1000)

      assert(carPhoto.getOfferId == offerId)
      assert(carPhoto.getCategory == Category.CARS)
      assert(carPhoto.getMark == "FORD")
      assert(carPhoto.getModel == "FOCUS")
      assert(carPhoto.getPhotoUrl == "http://example.com")
      assert(carPhoto.getTimestamp.getSeconds == 1)
      assert(carPhoto.getMeta.getLicensePlateCount == 2)
      assert(carPhoto.getMeta.getLicensePlate(0).getText == "Y062ME777")
      assert(carPhoto.getMeta.getLicensePlate(0).getConfidence == 0.88)
      assert(carPhoto.getMeta.getLicensePlate(1).getText == "M958KB159")
      assert(carPhoto.getMeta.getLicensePlate(1).getConfidence == 0.892)
      assert(carPhoto.getMeta.getProdV6EncToloka96Count == 3)
      assert(carPhoto.getMeta.getProdV6EncToloka96(0) == 0.1)
      assert(carPhoto.getMeta.getProdV6EncToloka96(1) == 0.2)
      assert(carPhoto.getMeta.getProdV6EncToloka96(2) == 0.3)

    }

    "toCarPhoto: year" in new Fixture {
      val offerBuilder = createOffer()
      offerBuilder.getOfferAutoruBuilder.getEssentialsBuilder.setYear(1994)
      val offer = offerBuilder.build()
      val p = BasicsModel.Photo.newBuilder()

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(Some("http://example.com"))

      val carPhoto = worker.toCarPhoto(offer, p, 1000)
      assert(carPhoto.getYear == 1994)
    }

    "toCarPhoto: color" in new Fixture {
      val offerBuilder = createOffer()
      offerBuilder.getOfferAutoruBuilder.setColorHex("color")
      val offer = offerBuilder.build()

      val p = BasicsModel.Photo.newBuilder()

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(Some("http://example.com"))

      val carPhoto = worker.toCarPhoto(offer, p, 1000)
      assert(carPhoto.getColor == "color")
    }

    "toCarPhoto: broken, interior, left-side" in new Fixture {
      val offerBuilder = createOffer()
      offerBuilder.getOfferAutoruBuilder.setColorHex("color")
      val offer = offerBuilder.build()
      val p = BasicsModel.Photo.newBuilder()
      p.getMetaBuilder.getAutoClassificationBuilder.setAutoBrokenWeight(1)
      p.getMetaBuilder.getAutoClassificationBuilder.setAutoInteriorWeight(2)
      p.getMetaBuilder.getAutoClassificationBuilder.setAutoSideLeftWeight(3)

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(Some("http://example.com"))

      val carPhoto = worker.toCarPhoto(offer, p, 1000)
      assert(carPhoto.getMeta.getAutoBrokenWeight == 1)
      assert(carPhoto.getMeta.getAutoInteriorWeight == 2)
      assert(carPhoto.getMeta.getAutoSideLeftWeight == 3)
    }

    "toCarPhoto: trucks" in new Fixture {
      val offer = createOffer(category = Category.TRUCKS).build
      val offerId = offer.getOfferID
      val p = BasicsModel.Photo.newBuilder()
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.addNumbersBuilder().setConfidence(0.892).setNumber("M958KB159").setWidthPercent(0.1)
      p.getMetaBuilder.getAutoruNnetFeaturesBuilder.addAllProdV6EncToloka96(Seq(0.1, 0.2, 0.3).map(Double.box).asJava)

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(Some("http://example.com"))

      val carPhoto = worker.toCarPhoto(offer, p, 1000)

      assert(carPhoto.getOfferId == offerId)
      assert(carPhoto.getCategory == Category.TRUCKS)
      assert(carPhoto.getMark == "URAL")
      assert(carPhoto.getModel == "4320_TRAKTOR")
      assert(carPhoto.getPhotoUrl == "http://example.com")
      assert(carPhoto.getTimestamp.getSeconds == 1)
      assert(carPhoto.getMeta.getLicensePlateCount == 2)
      assert(carPhoto.getMeta.getLicensePlate(0).getText == "Y062ME777")
      assert(carPhoto.getMeta.getLicensePlate(0).getConfidence == 0.88)
      assert(carPhoto.getMeta.getLicensePlate(1).getText == "M958KB159")
      assert(carPhoto.getMeta.getLicensePlate(1).getConfidence == 0.892)
      assert(carPhoto.getMeta.getProdV6EncToloka96Count == 3)
      assert(carPhoto.getMeta.getProdV6EncToloka96(0) == 0.1)
      assert(carPhoto.getMeta.getProdV6EncToloka96(1) == 0.2)
      assert(carPhoto.getMeta.getProdV6EncToloka96(2) == 0.3)
    }

    "toCarPhoto: failed to return main photo" in new Fixture {
      val offer = createOffer().build
      val offerId = offer.getOfferID
      val p = BasicsModel.Photo.newBuilder()
      p.setName("100500-hash")
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.getAutoruNnetFeaturesBuilder.addAllProdV6EncToloka96(Seq(0.1, 0.2, 0.3).map(Double.box).asJava)

      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)

      val carPhoto = worker.toCarPhoto(offer, p, 1000)

      assert(carPhoto.getOfferId == offerId)
      assert(carPhoto.getCategory == Category.CARS)
      assert(carPhoto.getMark == "FORD")
      assert(carPhoto.getModel == "FOCUS")
      assert(carPhoto.getPhotoUrl == "100500-hash")
      assert(carPhoto.getTimestamp.getSeconds == 1)
      assert(carPhoto.getMeta.getLicensePlateCount == 1)
      assert(carPhoto.getMeta.getLicensePlate(0).getText == "Y062ME777")
      assert(carPhoto.getMeta.getLicensePlate(0).getConfidence == 0.88)
      assert(carPhoto.getMeta.getProdV6EncToloka96Count == 3)
      assert(carPhoto.getMeta.getProdV6EncToloka96(0) == 0.1)
      assert(carPhoto.getMeta.getProdV6EncToloka96(1) == 0.2)
      assert(carPhoto.getMeta.getProdV6EncToloka96(2) == 0.3)
    }

    "action: no photos" in new Fixture {

      val offerBuilder = createOffer()

      val offer: OfferModel.Offer = offerBuilder.build()

      when(daoMocked.findById(?, ?, ?)(?)).thenReturn(Some(offer))
      verifyNoMoreInteractions(daoMocked)
      val result = worker.process(offer, None)

      assert(!worker.shouldProcess(offer, None).shouldProcess)
      verifyNoMoreInteractions(photoMetaSender)
    }

    "action: no meta" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)

      val offer: OfferModel.Offer = b.build()
      assert(!worker.shouldProcess(offer, None).shouldProcess)

      verifyNoMoreInteractions(daoMocked)
      verifyNoMoreInteractions(photoMetaSender)
    }

    "action: meta not finished" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(false)

      val offer: OfferModel.Offer = b.build()
      val result = worker.process(offer, None)
      assert(!worker.shouldProcess(offer, None).shouldProcess)
      verifyNoMoreInteractions(photoMetaSender)
    }

    "action: meta already sent" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(true)

      val offer: OfferModel.Offer = b.build()
      val shardId = getWorkerShardId(offer.getOfferID)
      verifyNoMoreInteractions(daoMocked)
      verifyNoMoreInteractions(photoMetaSender)
      assert(!worker.shouldProcess(offer, None).shouldProcess)

    }

    "action: no numbers" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      val shardId = getWorkerShardId(offer.getOfferID)
      when(daoMocked.findById(?, ?, ?)(?)).thenReturn(Some(offer))
      verifyNoMoreInteractions(daoMocked)
      worker.process(offer, None)
      verifyNoMoreInteractions(photoMetaSender)
    }

    "action: failed to send" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer, offer.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.failed(new RuntimeException("Error!")))

      val shardId = getWorkerShardId(offer.getOfferID)
      when(daoMocked.findById(?, ?, ?)(?)).thenReturn(Some(offer))
      verifyNoMoreInteractions(daoMocked)
      val result = worker.process(offer, None)
      assert(result.updateOfferFunc.isEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RetryDelay.toMillis)
        .getMillis +- 1000)
      verify(photoMetaSender).send(eqq(carPhoto))
    }

    "action: sent successfully" in new Fixture {
      reset(daoMocked)
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer, offer.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.successful(()))

      val result = worker.process(offer, None)
      assert(result.updateOfferFunc.nonEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      val update1 = result.updateOfferFunc.get(offer)
      assert(update1.getOfferAutoru.getPhoto(0).getMeta.getIsSent)
      verify(photoMetaSender).send(eqq(carPhoto))
    }

    "action: duplicate photos sent successfully" in new Fixture {
      reset(daoMocked)
      val b = createOffer()

      val p1 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p1.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000).setLegacyId(1)
      p1.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p1.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val p2 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p2.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1001).setLegacyId(2)
      p2.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p2.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer, offer.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.successful(()))

      val result = worker.process(offer, None)
      assert(result.updateOfferFunc.nonEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      val update1 = result.updateOfferFunc.get(offer)
      assert(update1.getOfferAutoru.getPhoto(0).getMeta.getIsSent)
      assert(update1.getOfferAutoru.getPhoto(1).getMeta.getIsSent)
      verify(photoMetaSender, times(2)).send(eqq(carPhoto))
    }

    "action: sent successfully, but meta has changed" in new Fixture {
      reset(daoMocked)
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()

      b.getOfferAutoruBuilder.getPhotoBuilder(0).getMetaBuilder.setIsFinished(false)
      val offer2: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer2, offer2.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.successful(()))

      val result = worker.process(offer2, None)
      assert(result.updateOfferFunc.isEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      assert(result.updateOfferFunc.isEmpty)

    }

    "action: sent successfully, but meta has changed 2" in new Fixture {
      reset(daoMocked)
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()

      b.getOfferAutoruBuilder.getPhotoBuilder(0).getMetaBuilder.getAutoruNnetFeaturesBuilder.addProdV6EncToloka96(0.1)
      val offer2: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer2, offer2.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.successful(()))
      val result = worker.process(offer2, None)
      assert(result.updateOfferFunc.nonEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      val update1 = result.updateOfferFunc.get(offer)
      verify(photoMetaSender).send(eqq(carPhoto))
      assert(!update1.getOfferAutoru.getPhoto(0).getMeta.getIsSent)
    }

    "action: one sent successfully, one failed" in new Fixture {
      reset(daoMocked)
      val b = createOffer()
      val p1 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p1.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p1.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p1.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)
      val p2 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p2.setIsMain(true).setOrder(0).setName("100501-hash").setCreated(1000)
      p2.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p2.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer, offer.getOfferAutoru.getPhoto(0), 1000)

      stub(photoMetaSender.send(_: VehiclePhoto)) {
        case cp =>
          if (cp.getPhotoUrl == "100500-hash") Future.successful(())
          else Future.failed(new RuntimeException("Error!"))
      }

      val result = worker.process(offer, None)
      assert(result.updateOfferFunc.nonEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      val update1 = result.updateOfferFunc.get(offer)
      assert(update1.getOfferAutoru.getPhoto(0).getMeta.getIsSent)
      assert(!update1.getOfferAutoru.getPhoto(1).getMeta.getIsSent)
      verify(photoMetaSender).send(eqq(carPhoto))
    }

    "action: one sent successfully, one: meta has changed" in new Fixture {
      reset(daoMocked, photoMetaSender)
      val b = createOffer()
      val p1 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p1.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p1.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p1.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)
      val p2 = b.getOfferAutoruBuilder.addPhotoBuilder()
      p2.setIsMain(true).setOrder(0).setName("100501-hash").setCreated(1000)
      p2.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p2.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()

      b.getOfferAutoruBuilder.getPhotoBuilder(1).getMetaBuilder.getAutoruNnetFeaturesBuilder.addProdV6EncToloka96(0.1)
      val offer2: OfferModel.Offer = b.build()
      when(mdsPhotoUtils.getMainPhotoUrl(?)).thenReturn(None)
      val carPhoto = worker.toCarPhoto(offer, offer.getOfferAutoru.getPhoto(0), 1000)

      when(photoMetaSender.send(?)).thenReturn(Future.successful(()))
      val result = worker.process(offer2, None)
      assert(result.updateOfferFunc.nonEmpty)
      result.nextCheck.get.getMillis shouldBe (new DateTime()
        .plus(SendMetaWorkerYdb.RescheduleDelay.toMillis)
        .getMillis +- 1000)
      val update1 = result.updateOfferFunc.get(offer)
      assert(update1.getOfferAutoru.getPhoto(0).getMeta.getIsSent)
      assert(!update1.getOfferAutoru.getPhoto(1).getMeta.getIsSent)
      verify(photoMetaSender).send(eqq(carPhoto))
    }

    "shouldProcess: no photos" in new Fixture {
      val offerBuilder = createOffer()

      val offer: OfferModel.Offer = offerBuilder.build()
      when(daoMocked.findById(?, ?, ?)(?)).thenReturn(Some(offer))
      val shardId = getWorkerShardId(offer.getOfferID)
      assert(!worker.shouldProcess(offer, None).shouldProcess)

    }

    "shouldProcess: no meta" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)

      val offer: OfferModel.Offer = b.build()
      worker.process(offer, None)

    }

    "shouldProcess: meta not finished" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(false)

      val offer: OfferModel.Offer = b.build()
      worker.process(offer, None)
    }

    "shouldProcess: meta already sent" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(true)

      val offer: OfferModel.Offer = b.build()
      assert(!worker.shouldProcess(offer, None).shouldProcess)

    }

    "shouldProcess: no numbers" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()

      assert(!worker.shouldProcess(offer, None).shouldProcess)

    }

    "shouldProcess: true" in new Fixture {
      val b = createOffer()
      val p = b.getOfferAutoruBuilder.addPhotoBuilder()
      p.setIsMain(true).setOrder(0).setName("100500-hash").setCreated(1000)
      p.addNumbersBuilder().setConfidence(0.88).setNumber("Y062ME777").setWidthPercent(0.1)
      p.getMetaBuilder.setVersion(1).setIsFinished(true).setIsSent(false)

      val offer: OfferModel.Offer = b.build()
      assert(worker.shouldProcess(offer, None).shouldProcess)

    }

  }
}
