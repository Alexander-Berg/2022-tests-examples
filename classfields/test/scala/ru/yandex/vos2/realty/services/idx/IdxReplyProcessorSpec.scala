package ru.yandex.vos2.realty.services.idx

import java.util.concurrent.ThreadLocalRandom

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.graph.core.{Name, Node, NodeMetro}
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.message.RealtySchema.{LocationMessage, MetroWithDistanceMessage, OfferMessage}
import ru.yandex.realty.proto.offer.vos.OfferPatch
import ru.yandex.realty.proto.offer.{IndexerErrors, OfferState, ProblemDescription}
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.vos2.BasicsModel.{Location, Metro}
import ru.yandex.vos2.OfferModel.IDXStatus.NoteType.{ERROR, WARN}
import ru.yandex.vos2.OfferModel.IDXStatus.{Note, NoteType}
import ru.yandex.vos2.realty.model.TestUtils

import scala.collection.JavaConverters._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class IdxReplyProcessorSpec extends WordSpec with Matchers {

  import IdxReplyProcessorSpec._

  private val regionGraph = new MutableRegionGraph()
  regionGraph.addNode(
    new Node()
      .applySideEffect(_.setId(DistrictId))
      .applySideEffect(_.setType("SUBJECT_FEDERATION_DISTRICT"))
      .applySideEffect(_.setName(new Name().applySideEffect(_.setDisplay("district"))))
  )
  regionGraph.addNode(
    new Node()
      .applySideEffect(_.setId(CityDistrictId))
      .applySideEffect(_.setType("CITY_DISTRICT"))
      .applySideEffect(_.setName(new Name().applySideEffect(_.setDisplay("Sub-locality"))))
  )
  regionGraph.addNode(
    new NodeMetro()
      .applySideEffect(_.setId(MetroId))
      .applySideEffect(_.setGeoId(MetroGeoId))
      .applySideEffect(_.setType("METRO_STATION"))
      .applySideEffect(_.setName(new Name().applySideEffect(_.setDisplay("Some name"))))
      .applySideEffect(_.setPoint(GeoPoint.getPoint(1, 2)))
  )

  private val processor = new IdxReplyProcessor(
    None,
    new Provider[RegionGraph] {
      override def get(): RegionGraph = regionGraph
    }
  )

  "Processor" should {
    "apply error returned by IDX" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val code = 47
      val text = "Some error text"
      val error = createProblem(code, text)
      val state = OfferState.newBuilder().addError(error).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(!idxStatus.getCorrect)
      assert(idxStatus.getNoteList.size() == 1)
      assert(idxStatus.getNoteList.asScala.exists(validateNote(ERROR, code, text)))
    }

    "apply warning returned by IDX" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val code = 1
      val text = "Some warning text"
      val warning = createProblem(code, text)
      val state = OfferState.newBuilder().addWarning(warning).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getCorrect)
      assert(idxStatus.getNoteList.size() == 1)
      assert(idxStatus.getNoteList.asScala.exists(validateNote(WARN, code, text)))
    }

    "apply empty states as valid" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val state = OfferState.newBuilder().build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getCorrect)
      assert(idxStatus.getNoteList.isEmpty)
    }

    "apply missed states as valid" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val message = createMessage(None, offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getCorrect)
      assert(idxStatus.getNoteList.isEmpty)
    }

    "ignore duplicate statuses" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val code = 47
      val text = "Some error text"
      val error = createProblem(code, text)
      val state = OfferState.newBuilder().addError(error).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getUpdate.isDefined)
      val updated = update.getUpdate.get
      val update2 = processor.applyResult(updated, message)
      assert(update2.getVisitDelay.isEmpty)
      assert(update2.getUpdate.isEmpty)
    }

    "ignore vos banned status" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val code = 47
      val text = "Some error text"
      val error = createProblem(code, text)
      val state = OfferState.newBuilder().addError(error).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getUpdate.isDefined)
      val updated = update.getUpdate.get

      val error2 = createProblem(IndexerErrors.VOS_STATUS_BANNED_VALUE, text + " banned")
      val state2 = OfferState.newBuilder().addError(error2).build()
      val message2 =
        createMessage(Some(state2), offer.getOfferID).toBuilder.setOfferVersion(message.getOfferVersion).build()
      val update2 = processor.applyResult(updated, message2)
      assert(update2.getVisitDelay.isEmpty)
      assert(update2.getUpdate.isEmpty)
    }

    "store hash in case of normal response" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val state = OfferState.newBuilder().build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getCorrect)
      assert(idxStatus.getHash == message.getOfferVersion)
    }

    "store hash in case of vos banned status" in {
      val offer = TestUtils.createOffer().build()
      val error = createProblem(IndexerErrors.VOS_STATUS_BANNED_VALUE, "banned")
      val state = OfferState.newBuilder().addError(error).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getUpdate.isDefined)
      assert(update.getUpdate.get.getIDXStatus.getHash == message.getOfferVersion)
    }

    "store hash in case of missed states" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val message = createMessage(None, offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getHash == message.getOfferVersion)
    }

    "store unified location from unified offer if provided" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val message1 = createMessage(None, offer.getOfferID)
      val update = processor.applyResult(offer, message1)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)

      val expectedLocation = createLocation2()
      val message2 = message1.toBuilder
        .setUnifiedOffer(
          OfferMessage
            .newBuilder()
            .setLocation(expectedLocation)
            .build()
            .toByteString
        )
        .build()
      val update2 = processor.applyResult(update.getUpdate.get, message2)
      assert(update2.getVisitDelay.isDefined)
      assert(update2.getVisitDelay.get.isFinite)
      assert(update2.getUpdate.isDefined)
      check(expectedLocation, update2.getUpdate.get.getOfferRealty.getUnifiedAddress)
    }

    "store premoderation flag" in {
      val offer = TestUtils.createOffer().clearIDXStatus().build()
      val state = OfferState.newBuilder.setPremoderation(true).build()
      val message = createMessage(Some(state), offer.getOfferID)
      val update = processor.applyResult(offer, message)
      assert(update.getVisitDelay.isDefined)
      assert(update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      val idxStatus = update.getUpdate.get.getIDXStatus
      assert(idxStatus.getPremoderation)
    }

  }

  private def check(idxLocation: LocationMessage, vosLocation: Location): Unit = {
    vosLocation.getDistrict shouldBe "district"
    vosLocation.getHouseNumber shouldBe idxLocation.getHouseNum
    vosLocation.getLocalityName shouldBe idxLocation.getLocalityName
    vosLocation.getRegion shouldBe idxLocation.getRegionName
    vosLocation.getRgid shouldBe idxLocation.getRegionGraphId
    vosLocation.getStreet shouldBe idxLocation.getStreet
    vosLocation.getSubLocalityName shouldBe "Sub-locality"
    vosLocation.getMetroCount shouldBe idxLocation.getMetroCount
    idxLocation.getMetroList.asScala.zip(vosLocation.getMetroList.asScala).foreach(p => check(p._1, p._2))
  }

  private def check(idxMetro: MetroWithDistanceMessage, vosMetro: Metro): Unit = {
    vosMetro.getGeoId shouldBe idxMetro.getGeoId
    vosMetro.getName shouldBe "Some name"
    vosMetro.getTimeOnFoot shouldBe idxMetro.getTimeOnFoot
    vosMetro.getTimeOnTransport shouldBe idxMetro.getTimeOnTransport
  }
}

object IdxReplyProcessorSpec {

  private val DistrictId = 1000
  private val CityDistrictId = 1010
  private val MetroId = 1011
  private val MetroGeoId = 11

  private def createLocation2(): LocationMessage = {
    LocationMessage
      .newBuilder()
      .addDistrict(DistrictId)
      .addDistrict(CityDistrictId)
      .setRegionGraphId(217)
      .setRegionName("Region")
      .setLocalityName("Locality")
      .setStreet("Street")
      .setHouseNum("77, лит. 'A', корп. 3")
      .applySideEffect(
        _.addMetroBuilder()
          .setGeoId(11)
          .setTimeOnFoot(1)
          .setTimeOnTransport(10)
      )
      .build()
  }

  private def createProblem(code: Int, text: String): ProblemDescription = {
    ProblemDescription
      .newBuilder()
      .setType(code)
      .setValue(text)
      .build()
  }

  private def createMessage(state: Option[OfferState], offerId: String): OfferPatch = {
    val builder = OfferPatch
      .newBuilder()
      .setVersion(1)
      .setOfferVersion(ThreadLocalRandom.current().nextInt())

    builder.getIdentityBuilder
      .setOfferId(offerId)

    state.foreach(builder.setState)
    builder.build()
  }

  private def validateNote(noteType: NoteType, code: Int, text: String)(note: Note): Boolean = {
    note.getNoteType == noteType &&
    note.getNoteCode == code &&
    note.getText == text
  }
}
