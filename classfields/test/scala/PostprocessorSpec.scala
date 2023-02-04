import mockups.CassandraClientMockup
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.archive.scheduler.updater.diff._
import ru.yandex.realty.archive.scheduler.updater.enrich.CassandraEnricher
import ru.yandex.realty.archive.scheduler.updater.managers.{
  CassandraPossibleHeadersManager,
  PossibleHeadersManager,
  PostprocessorImpl
}
import ru.yandex.realty.archive.scheduler.updater.persistence.{
  ArchiveUpdatePatch,
  WritableClusterRecord,
  WritableClusteringState
}
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.archive.{ArchiveOfferClusterBrother, _}
import ru.yandex.realty.model.offer._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-04-01
  */

@RunWith(classOf[JUnitRunner])
class PostprocessorSpec extends AsyncSpecBase with Logging with PropertyChecks {

  private lazy val cassandraClient = new CassandraClientMockup
  private lazy val enricher = new CassandraEnricher(cassandraClient)

  private lazy val startDate = LocalDate.parse("2016-01-15")
  private lazy val endDate = startDate.plusDays(1)
  private lazy val startDateTime = startDate.toDateTimeAtStartOfDay
  private lazy val endDateTime = endDate.toDateTimeAtStartOfDay
  private lazy val possibleHeaders = new CassandraPossibleHeadersManager(cassandraClient)
  private lazy val postprocessor = new PostprocessorImpl(possibleHeaders)

  object TestIds extends Enumeration {
    val Template: Value = Value("123000")
    val BasicOfferInsert: Value = Value("123001")
    val SimilarOfferInsert: Value = Value("123002")
    val AddressDiffs: Value = Value("123006")
  }

  object TestAddresses extends Enumeration {
    val Template: Value = Value("test-template-address")
    val BasicOfferInsert: Value = Value("test-basic-address-" ++ TestIds.BasicOfferInsert.toString)
    val SameOfferAddressDiffs: Value = Value("test-address-diffs1-" ++ TestIds.BasicOfferInsert.toString)
  }

  val testOfferTemplate = ArchiveOfferHolocron(
    offerId = TestIds.Template.toString,
    date = startDateTime,
    price = ArchiveOfferPrice(value = 12000, period = PricingPeriod.PER_MONTH, currency = Currency.RUR),
    offerType = OfferType.RENT,
    offerCategory = CategoryType.APARTMENT,
    offerInfo = ArchiveOfferHolocronOfferInfo(
      agentFee = None,
      premoderation = false,
      partnerId = "111333",
      internal = None,
      clusterId = None,
      hidden = Some(false)
    ),
    areaInfo = ArchiveOfferAreaInfo(
      area = ArchiveOfferAreaValue(value = 13, unit = AreaUnit.SQUARE_METER),
      totalArea = None,
      kitchenSpace = None,
      livingSpace = None
    ),
    images = ArchiveOfferImages(totalImages = 0, urls = None),
    location = ArchiveOfferHolocronLocation(
      localityName = None,
      subjectFederationId = None,
      address = TestAddresses.Template.toString
    ),
    building = ArchiveOfferBuilding(buildingId = None, buildingSeriesId = None, buildingType = None, parkingType = None),
    floorsTotal = Some(10),
    floorsOffered = 3,
    openPlan = false,
    roomsOffered = 1,
    roomsTotal = 1,
    flatType = Some(FlatType.SECONDARY),
    house = ArchiveOfferHouse(balconyType = None, studio = false),
    apartment = ArchiveOfferApartment(newFlat = None, renovation = None, apartments = None, ceilingHeight = None),
    description = Some("non-existent offer"),
    error0 = None,
    error1 = None
  )

  case class ShortenedHolo(
    offerId: String,
    date: DateTime,
    price: Long = testOfferTemplate.price.value,
    rooms: Int = testOfferTemplate.roomsOffered,
    address: String = testOfferTemplate.location.address,
    floorsOffered: Int = testOfferTemplate.floorsOffered
  ) {

    def toFull: ArchiveOfferHolocron = {
      testOfferTemplate.copy(
        offerId = offerId,
        date = date,
        price = testOfferTemplate.price.copy(value = price),
        roomsOffered = rooms,
        roomsTotal = rooms,
        location = testOfferTemplate.location.copy(address = address),
        floorsOffered = floorsOffered
      )
    }
  }

  implicit private def shortenedToFull(shortened: ShortenedHolo): ArchiveOfferHolocron = shortened.toFull

  private def mergeWithStartDate(
    startState: ArchiveOfferHolocron,
    endState: ArchiveOfferHolocron,
    isActive: Boolean
  ): ArchiveOfferCassandraRecord = {
    mergeWithStartDate(endState, startState.date, startState.price, isActive)
  }

  private def mergeWithStartDate(
    endState: ArchiveOfferHolocron,
    startDate: DateTime,
    startPrice: ArchiveOfferPrice,
    isActive: Boolean
  ): ArchiveOfferCassandraRecord = {
    val newOffer = endState.toCassandra(isActive)

    val fixedOffer = newOffer.copy(
      dates = newOffer.dates.copy(creationDate = startDate),
      prices = newOffer.prices.copy(firstPrice = startPrice)
    )

    val newBros = Map(endState.offerId -> ArchiveOfferClusterBrother(fixedOffer))

    ArchiveOfferCassandraRecord(
      offer = fixedOffer,
      address = endState.location.address,
      offerId = endState.offerId,
      clusterBrothers = newBros
    )
  }

  implicit def mergeActiveHoloOffers[V](tpl: (V, V))(implicit f: V => ArchiveOfferHolocron): ArchiveOfferCassandra = {

    val (startState, endState) = tpl match {
      case (firstOffer, secondOffer) => (f(firstOffer), f(secondOffer))
    }
    mergeWithStartDate(startState, endState, isActive = true).offer
  }

  def mergeClosedHolo(startState: ArchiveOfferHolocron, endState: ArchiveOfferHolocron): ArchiveOfferCassandra = {
    mergeWithStartDate(startState, endState, isActive = false).offer
  }

  def toActiveRecord(offer: ArchiveOfferHolocron): ArchiveOfferCassandraRecord = {
    mergeWithStartDate(offer, offer, isActive = true)
  }

  def toInactiveRecord(offer: ArchiveOfferHolocron): ArchiveOfferCassandraRecord = {
    mergeWithStartDate(offer, offer, isActive = false)
  }

  def addBro(
    record: ArchiveOfferCassandraRecord,
    broOffer: ArchiveOfferCassandra,
    updateMaster: Boolean
  ): ArchiveOfferCassandraRecord = {
    val newBro = ArchiveOfferClusterBrother(broOffer)
    val newOffer = if (!updateMaster) {
      record.offer
    } else {
      val fixedStart = if (broOffer.dates.creationDate.isBefore(record.offer.dates.creationDate)) {
        record.offer.copy(
          dates = record.offer.dates.copy(creationDate = broOffer.dates.creationDate),
          prices = record.offer.prices.copy(firstPrice = broOffer.prices.firstPrice)
        )
      } else {
        record.offer
      }

      fixedStart
    }
    record.copy(offer = newOffer, clusterBrothers = record.clusterBrothers ++ Map(broOffer.offerId -> newBro))
  }

  def writeRecord(record: ArchiveOfferCassandraRecord): Unit = {
    val f1 = cassandraClient.writeRecord(record)
    val f2 = for {
      broId <- record.clusterBrothers.keys
    } yield cassandraClient.writeNewMasterId(broId, record.offerId)
    Await.result(f1.zip(Future.sequence(f2)), 10.seconds)
  }

  //type of enrichment data:
  //1. no offer exists
  //2. close offer exists
  //3. the same offer exists
  //4. address diffs
  //insert, close, delete, update

  "Postprocessor on the do-nothing" should {
    "do nothing" in {
      val doNothingDiff = OfferDiffDoNothing(offerId = "123321", reason = "test do-nothing reason")
      val enrichedDiff = enricher.enrich(doNothingDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      patches shouldBe empty
    }
  }

  "Postprocessor on the insert" should {

    val offerStart = ShortenedHolo(
      offerId = TestIds.BasicOfferInsert.toString,
      date = startDateTime.minusDays(3),
      address = TestAddresses.BasicOfferInsert.toString
    )
    val offerEnd = offerStart.copy(date = endDateTime)
    val offerWrittenActive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = true)
    val offerWrittenInactive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)
    val similarOffer = offerStart.copy(offerId = TestIds.SimilarOfferInsert.toString, date = endDateTime, rooms = 3)
    val sameOfferNewState = offerStart.copy(date = endDateTime, price = offerStart.price + 100)
    val sameOfferNewClusteringState: ArchiveOfferHolocron =
      offerStart.copy(date = endDateTime, floorsOffered = offerStart.floorsOffered + 3, price = offerStart.price * 2)

    "insert if no offer exists" in {
      val basicDiff = OfferDiffInsert(offerStart)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)
      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(toActiveRecord(offerStart)),
        WritableClusteringState(offerStart.offerId, offerStart.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer state as slave if similar offer is active" in {
      val offerWritten = offerWrittenActive

      writeRecord(offerWritten)
      val basicDiff = OfferDiffInsert(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedMergedRecord = offerWritten.copy(
        clusterBrothers = offerWritten.clusterBrothers
          ++ Map(similarOffer.offerId -> ArchiveOfferClusterBrother(similarOffer, isActive = true))
      )
      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedMergedRecord),
        WritableClusteringState(similarOffer.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer state as master if similar offer is closed" in {
      val offerWritten = offerWrittenInactive
      writeRecord(offerWritten)
      val basicDiff = OfferDiffInsert(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val soloRecord = toActiveRecord(similarOffer)

      val offerHidden = offerEnd.withHidden(Some(true))
      val expectedOldHiddenRecord = mergeWithStartDate(offerStart, offerHidden, isActive = false)
      val expectedMergedRecord = addBro(soloRecord, offerWritten.offer, updateMaster = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedMergedRecord),
        WritableClusteringState(offerWritten.offerId, similarOffer.offerId),
        WritableClusteringState(similarOffer.offerId, similarOffer.offerId),
        WritableClusterRecord(expectedOldHiddenRecord)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer if existing master is hidden" in {
      val offerHidden = offerEnd.withHidden(Some(true))
      val offerWritten = mergeWithStartDate(offerStart, offerHidden, isActive = false)
      writeRecord(offerWritten)
      val basicDiff = OfferDiffInsert(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val soloRecord = toActiveRecord(similarOffer)
      val expectedMergedRecord = addBro(soloRecord, offerWritten.offer, updateMaster = false)
      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedMergedRecord),
        WritableClusteringState(offerWritten.offerId, similarOffer.offerId),
        WritableClusteringState(similarOffer.offerId, similarOffer.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer state if prev state is closed" in {
      val offerWritten = offerWrittenInactive
      writeRecord(offerWritten)
      val basicDiff = OfferDiffInsert(sameOfferNewState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedRecord = mergeWithStartDate(offerStart, sameOfferNewState, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer state if prev state is hidden" in {
      val offerHidden = offerEnd.withHidden(Some(true))
      val offerWritten = mergeWithStartDate(offerStart, offerHidden, isActive = false)
      writeRecord(offerWritten)

      val basicDiff = OfferDiffInsert(sameOfferNewState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedRecord = mergeWithStartDate(offerStart, sameOfferNewState, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "insert new offer if clustering changed" in {
      // we can do nothing with old offer state, we even can't read it to determine first-date and first-price
      // if we are lucky, new clustering state has the same address as old one;
      // in that case, we'll just rewrite old state with new one.
      // else, two offer states would be stored in C* independently.
      // Read more at REALTY-16058
      val offerWritten = offerWrittenInactive
      writeRecord(offerWritten)

      val basicDiff = OfferDiffInsert(sameOfferNewClusteringState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = toActiveRecord(sameOfferNewClusteringState)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(sameOfferNewClusteringState.offerId, sameOfferNewClusteringState.offerId)
      )

      patches shouldEqual expectedPatches
    }
  }

  "Postprocessor on close" should {
    val offerStart = ShortenedHolo(
      offerId = TestIds.BasicOfferInsert.toString,
      date = startDateTime.minusDays(3),
      address = TestAddresses.BasicOfferInsert.toString
    )
    val offerEnd = offerStart.copy(date = endDateTime)
    val offerWrittenActive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = true)
    val offerWrittenInactive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)
    val similarOffer = offerStart.copy(
      offerId = TestIds.SimilarOfferInsert.toString,
      date = endDateTime,
      price = offerStart.price + 100,
      rooms = 3
    )

    "close existing active state" in {
      val offerWritten = offerWrittenActive
      writeRecord(offerWritten)

      val basicDiff = OfferDiffClose(offerEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }

    "close existing active state if it is slave" in {
      val offerWritten = offerWrittenActive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOffer).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffClose(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = addBro(offerWritten, toInactiveRecord(similarOffer).offer, updateMaster = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }
    "close existing active state if it is slave and is last active bro" in {
      val offerWritten = offerWrittenInactive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOffer).offer, updateMaster = true)

      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffClose(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = addBro(offerWritten, toInactiveRecord(similarOffer).offer, updateMaster = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }
  }

  "Postprocessor on delete" should {
    val offerStart = ShortenedHolo(
      offerId = TestIds.BasicOfferInsert.toString,
      date = startDateTime.minusDays(3),
      address = TestAddresses.BasicOfferInsert.toString
    )
    val offerEnd = offerStart.copy(date = endDateTime)
    val offerWrittenActive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = true)
    val offerWrittenInactive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)
    val similarOffer = offerStart.copy(
      offerId = TestIds.SimilarOfferInsert.toString,
      date = endDateTime,
      price = offerStart.price + 100,
      rooms = 3
    )

    "hide existing active state" in {
      val offerWritten = offerWrittenActive
      writeRecord(offerWritten)

      val basicDiff = OfferDiffDelete(offerEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = mergeWithStartDate(offerStart, offerEnd.withHidden(Some(true)), isActive = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }
    "hide existing active state if it is slave" in {
      val offerWritten = offerWrittenActive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOffer).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffDelete(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord =
        addBro(offerWritten, toInactiveRecord(similarOffer.withHidden(Some(true))).offer, updateMaster = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }
    "hide existing active state if it is slave and is last active bro" in {
      val offerWritten = offerWrittenInactive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOffer).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffDelete(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord =
        addBro(offerWritten, toInactiveRecord(similarOffer.withHidden(Some(true))).offer, updateMaster = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }

    "hide existing active state if it is slave and is first (most earlier) active bro" in {
      val masterStart =
        ShortenedHolo(offerId = TestIds.BasicOfferInsert.toString, date = startDateTime.minusDays(3), price = 14123)
      val masterEnd = masterStart.copy(date = startDateTime, price = 15000)
      val slaveStart =
        ShortenedHolo(offerId = TestIds.SimilarOfferInsert.toString, date = startDateTime.minusDays(10), price = 14456)
      val slaveEnd = slaveStart.copy(date = endDateTime, price = 16000)

      val masterRecord = mergeWithStartDate(masterStart, masterEnd, isActive = false)
      val slaveRecord = mergeWithStartDate(slaveStart, slaveEnd, isActive = true)

      val writtenRecordWithSlave = addBro(masterRecord, slaveRecord.offer, updateMaster = true)
      writeRecord(writtenRecordWithSlave)

      val basicDiff = OfferDiffDelete(slaveEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedSlaveHiddenOffer =
        mergeWithStartDate(slaveStart, slaveEnd.withHidden(Some(true)), isActive = false).offer
      val expectedNewRecord = addBro(masterRecord, expectedSlaveHiddenOffer, updateMaster = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord)
      )

      patches shouldEqual expectedPatches
    }
  }

  "Postprocessor on update" should {
    val offerStart = ShortenedHolo(
      offerId = TestIds.BasicOfferInsert.toString,
      date = startDateTime.minusDays(3),
      address = TestAddresses.BasicOfferInsert.toString
    )
    val offerEnd = offerStart.copy(date = endDateTime)
    val offerWrittenActive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = true)
    val offerWrittenInactive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)
    val sameOfferNewState = offerStart.copy(date = endDateTime, price = offerStart.price + 100, rooms = 3)
    val similarOfferStart =
      offerStart.copy(offerId = TestIds.SimilarOfferInsert.toString, date = endDateTime, rooms = 3)
    val similarOfferEnd = similarOfferStart.copy(date = endDateTime, price = similarOfferStart.price + 100, rooms = 2)

    "update existing active state" in {
      val offerWritten = offerWrittenActive
      writeRecord(offerWrittenActive)

      val basicDiff = OfferDiffUpdateData(offerEnd, sameOfferNewState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedRecord = mergeWithStartDate(offerStart, sameOfferNewState, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "update existing active state if it is slave" in {
      val offerWritten = offerWrittenActive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOfferStart).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(similarOfferStart, similarOfferEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = addBro(
        offerWritten,
        mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true).offer,
        updateMaster = true
      )

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(similarOfferEnd.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "update existing active state if it is slave and master is hidden" in {
      val offerHidden = offerEnd.withHidden(Some(true))
      val offerWritten = mergeWithStartDate(offerStart, offerHidden, isActive = false)
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOfferStart).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(similarOfferStart, similarOfferEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val soloRecord = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val expectedNewRecord = addBro(soloRecord, offerWritten.offer, updateMaster = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(offerWritten.offerId, similarOfferEnd.offerId),
        WritableClusteringState(similarOfferEnd.offerId, similarOfferEnd.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "update existing active state and last-price if it is slave and is last active bro" in {
      val offerWritten = offerWrittenInactive
      val offerWrittenWithSlave = addBro(offerWritten, toActiveRecord(similarOfferStart).offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(similarOfferStart, similarOfferEnd)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = addBro(
        mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true),
        offerWritten.offer,
        updateMaster = true
      )

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(offerWritten.offerId, similarOfferEnd.offerId),
        WritableClusteringState(similarOfferEnd.offerId, similarOfferEnd.offerId)
      )

      patches shouldEqual expectedPatches
    }
  }

  "Postprocessor on update with new clustering state" should {
    val offerStart = ShortenedHolo(
      offerId = TestIds.BasicOfferInsert.toString,
      date = startDateTime.minusDays(3),
      address = TestAddresses.BasicOfferInsert.toString
    )
    val offerEnd = offerStart.copy(date = endDateTime)
    val offerWrittenActive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = true)
    val offerWrittenInactive: ArchiveOfferCassandraRecord = mergeWithStartDate(offerStart, offerEnd, isActive = false)
    val sameOfferNewAddress = offerStart.copy(
      address = TestAddresses.SameOfferAddressDiffs.toString,
      date = endDateTime,
      price = offerStart.price + 100,
      floorsOffered = offerStart.floorsOffered + 3
    )
    val sameOfferNewClusteringState =
      offerStart.copy(date = endDateTime, price = offerStart.price + 100, floorsOffered = offerStart.floorsOffered + 3)
    val similarOfferStart =
      offerStart.copy(offerId = TestIds.SimilarOfferInsert.toString, date = endDateTime, rooms = 3)
    val similarOfferEnd = similarOfferStart.copy(date = endDateTime, price = similarOfferStart.price + 200, rooms = 2)
    val similarOfferNewAddress = similarOfferEnd.copy(
      date = similarOfferEnd.date.plusDays(1),
      price = similarOfferStart.price + 300,
      floorsOffered = offerStart.floorsOffered + 3,
      address = TestAddresses.SameOfferAddressDiffs.toString
    )
    val newOfferNewAddress = sameOfferNewAddress.copy(date = startDateTime, offerId = TestIds.AddressDiffs.toString)

    "hide prev state and move offer to new record" in {
      val offerWritten = offerWrittenActive
      writeRecord(offerWritten)

      val basicDiff = OfferDiffUpdateData(offerEnd, sameOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedOldRecord = mergeWithStartDate(offerStart, offerEnd.withHidden(Some(true)), isActive = false)
      val expectedNewRecord = mergeWithStartDate(offerStart, sameOfferNewAddress, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "rewrite prev state if address the same" in {
      val offerWritten = offerWrittenActive
      writeRecord(offerWritten)

      val basicDiff = OfferDiffUpdateData(offerEnd, sameOfferNewClusteringState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = mergeWithStartDate(offerStart, sameOfferNewClusteringState, isActive = true)
      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId)
      )

      patches shouldEqual expectedPatches
    }

    "rewrite prev state if address the same but bros are left" in {
      val offerWritten = offerWrittenActive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(offerEnd, sameOfferNewClusteringState)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedNewRecord = mergeWithStartDate(offerStart, sameOfferNewClusteringState, isActive = true)
      val expectedOldRecord = mergeWithStartDate(
        offerStart.copy(offerId = similarOfferSlave.offerId),
        offerEnd.copy(offerId = similarOfferSlave.offerId).withHidden(Some(true)),
        isActive = false
      )

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(offerWritten.offerId, offerWritten.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "remove prev slave state and move offer to new record" in {
      val offerWritten = offerWrittenActive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(similarOfferStart, similarOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedOldRecord = offerWritten
      val expectedNewRecord = mergeWithStartDate(similarOfferStart, similarOfferNewAddress, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(similarOfferStart.offerId, similarOfferStart.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "update last-price if it is slave and is last active bro" in {
      val offerWritten = offerWrittenInactive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)

      val basicDiff = OfferDiffUpdateData(similarOfferEnd, similarOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedOldRecord = offerWritten
      val expectedNewRecord = mergeWithStartDate(similarOfferStart, similarOfferNewAddress, isActive = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(similarOfferStart.offerId, similarOfferStart.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "move to already existing new record" in {
      val offerWritten = offerWrittenActive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)
      val newAddressWritten = toActiveRecord(newOfferNewAddress)
      writeRecord(newAddressWritten)

      val basicDiff = OfferDiffUpdateData(similarOfferEnd, similarOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedSimilarOfferNewAddress =
        mergeWithStartDate(similarOfferStart, similarOfferNewAddress, isActive = true)

      val expectedOldRecord = offerWritten
      val expectedNewRecord = addBro(newAddressWritten, expectedSimilarOfferNewAddress.offer, updateMaster = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(similarOfferStart.offerId, newOfferNewAddress.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "move to already existing but hidden new record" in {
      val offerWritten = offerWrittenActive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)
      val newAddressWritten = toInactiveRecord(newOfferNewAddress.withHidden(Some(true)))
      writeRecord(newAddressWritten)

      val basicDiff = OfferDiffUpdateData(similarOfferEnd, similarOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedSimilarOfferNewAddress =
        mergeWithStartDate(similarOfferStart, similarOfferNewAddress, isActive = true)

      val expectedOldRecord = offerWritten
      val expectedNewRecord = addBro(expectedSimilarOfferNewAddress, newAddressWritten.offer, updateMaster = false)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(newAddressWritten.offerId, similarOfferStart.offerId),
        WritableClusteringState(similarOfferStart.offerId, similarOfferStart.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
    "move to already existing but closed new record" in {
      val offerWritten = offerWrittenActive
      val similarOfferSlave = mergeWithStartDate(similarOfferStart, similarOfferEnd, isActive = true)
      val offerWrittenWithSlave = addBro(offerWritten, similarOfferSlave.offer, updateMaster = true)
      writeRecord(offerWrittenWithSlave)
      val newAddressWritten = toInactiveRecord(newOfferNewAddress)
      writeRecord(newAddressWritten)

      val basicDiff = OfferDiffUpdateData(similarOfferEnd, similarOfferNewAddress)
      val enrichedDiff = enricher.enrich(basicDiff)
      val patches = postprocessor.getArchiveUpdatePatches(enrichedDiff)

      val expectedSimilarOfferNewAddress =
        mergeWithStartDate(similarOfferStart, similarOfferNewAddress, isActive = true)

      val expectedOldRecord = offerWritten
      val expectedNewRecord = addBro(expectedSimilarOfferNewAddress, newAddressWritten.offer, updateMaster = true)

      val expectedPatches = Seq[ArchiveUpdatePatch](
        WritableClusterRecord(expectedNewRecord),
        WritableClusteringState(newAddressWritten.offerId, similarOfferStart.offerId),
        WritableClusteringState(similarOfferStart.offerId, similarOfferStart.offerId),
        WritableClusterRecord(expectedOldRecord)
      )

      patches shouldEqual expectedPatches
    }
  }
}
