import mockups.CassandraClientMockup
import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.archive.scheduler.updater.diff._
import ru.yandex.realty.archive.scheduler.updater.enrich.CassandraEnricher
import ru.yandex.realty.archive.scheduler.updater.persistence.cassandra.CassandraClient
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.archive._
import ru.yandex.realty.model.offer._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-02-05
  */
@RunWith(classOf[JUnitRunner])
class CassandraEnricherSpec extends AsyncSpecBase with Logging with PropertyChecks {

  object TestIds extends Enumeration {
    val Template: Value = Value("123000")
    val NoOfferExists: Value = Value("123001")
    val SimilarOffer1: Value = Value("123002")
    val SimilarOffer2: Value = Value("123003")
    val SameOffer: Value = Value("123004")
    val SameOfferDiffDetails: Value = Value("123005")
    val AddressDiffs: Value = Value("123006")
  }

  object TestAddresses extends Enumeration {
    val Template: Value = Value("test-template-address")
    val NoOfferExists: Value = Value("test-non-existent-address-" ++ TestIds.NoOfferExists.toString)
    val SimilarOffer: Value = Value("test-address-" ++ TestIds.SimilarOffer1.toString)
    val SameOffer: Value = Value("test-address-" ++ TestIds.SameOffer.toString)
    val SameOfferDiffDetails: Value = Value("test-address-" ++ TestIds.SameOfferDiffDetails.toString)
    val AddressDiffs1: Value = Value("test-address-diffs1-" ++ TestIds.AddressDiffs.toString)
    val AddressDiffs2: Value = Value("test-address-diffs2-" ++ TestIds.AddressDiffs.toString)
  }

  val testOfferTemplate = ArchiveOfferHolocron(
    offerId = TestIds.Template.toString,
    date = LocalDate.parse("2016-01-15").toDateTimeAtStartOfDay,
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
    building = ArchiveOfferBuilding(
      buildingId = Some("123321"),
      buildingSeriesId = None,
      buildingType = None,
      parkingType = None
    ),
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

  def testHolocronToRecord(offer: ArchiveOfferHolocron): ArchiveOfferCassandraRecord = {
    val cassandraOffer = offer.toCassandra(isActive = false)

    ArchiveOfferCassandraRecord(
      address = offer.location.address,
      offerId = offer.offerId,
      offer = cassandraOffer,
      clusterBrothers = Map(offer.offerId -> ArchiveOfferClusterBrother(cassandraOffer))
    )
  }

  def testRecordToCassandra(cassandraClient: CassandraClient, record: ArchiveOfferCassandraRecord): Unit = {
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

  "Cassandra Enricher on the non-existent offer" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val noOfferExists: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.NoOfferExists.toString,
      location = testOfferTemplate.location.copy(address = TestAddresses.NoOfferExists.toString)
    )

    "enrich insert with emptiness" in {
      val basicDiff = OfferDiffInsert(noOfferExists)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = noOfferExists,
          key = EnrichedOfferKey.getEnrichedOfferKey(noOfferExists, masterId = None),
          clusterRecord = None
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }

    "enrich close up to do-nothing" in {
      val basicDiff = OfferDiffClose(noOfferExists)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich delete up to do-nothing" in {
      val basicDiff = OfferDiffDelete(noOfferExists)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich update up to do-nothing" in {
      val endOffer = noOfferExists.copy(date = noOfferExists.date.plusDays(1))
      val basicDiff = OfferDiffUpdateData(startOffer = noOfferExists, endOffer = endOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }
  }

  "Cassandra Enricher on the similar offer" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val similarOffer: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.SimilarOffer1.toString,
      date = LocalDate.parse("2016-01-15").toDateTimeAtStartOfDay,
      location = testOfferTemplate.location.copy(address = TestAddresses.SimilarOffer.toString)
    )

    val similarOfferCassandra: ArchiveOfferHolocron = similarOffer.copy(
      offerId = TestIds.SimilarOffer2.toString,
      date = LocalDate.parse("2016-01-20").toDateTimeAtStartOfDay,
      roomsOffered = 3
    )

    testRecordToCassandra(cassandraClient, testHolocronToRecord(similarOfferCassandra))

    "enrich insert with existing similar offer" in {
      val basicDiff = OfferDiffInsert(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = similarOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(similarOffer, masterId = Some(similarOfferCassandra.offerId)),
          clusterRecord = Some(testHolocronToRecord(similarOfferCassandra))
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }

    "enrich close up to do-nothing" in {
      val basicDiff = OfferDiffClose(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich delete up to do-nothing" in {
      val basicDiff = OfferDiffDelete(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich update up to do-nothing" in {
      val endOffer = similarOffer.copy(date = similarOffer.date.plusDays(1))
      val basicDiff = OfferDiffUpdateData(startOffer = similarOffer, endOffer = endOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }
  }

  "Cassandra Enricher on the similar offer" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val similarOffer: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.SimilarOffer2.toString,
      date = LocalDate.parse("2016-01-15").toDateTimeAtStartOfDay,
      location = testOfferTemplate.location.copy(address = TestAddresses.SimilarOffer.toString)
    )

    val similarOfferCassandra: ArchiveOfferHolocron = similarOffer.copy(
      offerId = TestIds.SimilarOffer1.toString,
      date = LocalDate.parse("2016-01-20").toDateTimeAtStartOfDay,
      roomsOffered = 3
    )

    testRecordToCassandra(cassandraClient, testHolocronToRecord(similarOfferCassandra))
    testRecordToCassandra(cassandraClient, testHolocronToRecord(similarOffer).withHidden(Some(true)))

    "enrich insert with not hidden similar offer, if there are both hidden and not hidden variants" in {
      val basicDiff = OfferDiffInsert(similarOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = similarOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(similarOffer, masterId = Some(similarOfferCassandra.offerId)),
          clusterRecord = Some(testHolocronToRecord(similarOfferCassandra))
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }
  }

  "Cassandra Enricher on the same offer w/ same details" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val sameOffer: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.SameOffer.toString,
      date = LocalDate.parse("2016-01-16").toDateTimeAtStartOfDay,
      location = testOfferTemplate.location.copy(address = TestAddresses.SameOffer.toString)
    )

    val sameOfferCassandra: ArchiveOfferHolocron = sameOffer.copy(
      price = sameOffer.price.copy(value = sameOffer.price.value + 100)
    )

    testRecordToCassandra(cassandraClient, testHolocronToRecord(sameOfferCassandra))

    "enrich insert with existing offer" in {
      val basicDiff = OfferDiffInsert(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = Some(testHolocronToRecord(sameOfferCassandra))
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }

    "enrich close up with existing offer" in {
      val basicDiff = OfferDiffClose(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffClose(
        EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }

    "enrich delete up with existing offer" in {
      val basicDiff = OfferDiffDelete(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      val expectedDiff = EnrichedOfferDiffDelete(
        EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }

    "enrich update with existing offer" in {
      val endOffer = sameOffer.copy(date = sameOffer.date.plusDays(1))
      val basicDiff = OfferDiffUpdateData(startOffer = sameOffer, endOffer = endOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      val expectedDiff = EnrichedOfferDiffUpdateData(
        startState = EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        ),
        endState = EnrichedOptOffer(
          offer = endOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = Some(testHolocronToRecord(sameOfferCassandra))
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }
  }

  "Cassandra Enricher on the same offer w/ different details" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val sameOffer: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.SameOfferDiffDetails.toString,
      date = LocalDate.parse("2016-01-16").toDateTimeAtStartOfDay,
      location = testOfferTemplate.location.copy(address = TestAddresses.SameOfferDiffDetails.toString)
    )

    //it has the same id, but different detail info
    val sameOfferCassandra: ArchiveOfferHolocron = sameOffer.copy(
      floorsOffered = sameOffer.floorsOffered + 3,
      price = sameOffer.price.copy(value = sameOffer.price.value * 2)
    )

    testRecordToCassandra(cassandraClient, testHolocronToRecord(sameOfferCassandra))

    "enrich insert with no offer" in {
      val basicDiff = OfferDiffInsert(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = None),
          clusterRecord = None
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }

    "enrich close up with existing offer" in {
      val basicDiff = OfferDiffClose(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffClose(
        EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }

    "enrich delete up with existing offer" in {
      val basicDiff = OfferDiffDelete(sameOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      val expectedDiff = EnrichedOfferDiffDelete(
        EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }

    "enrich update with existing offer" in {
      val endOffer = sameOffer.copy(date = sameOffer.date.plusDays(1))
      val basicDiff = OfferDiffUpdateData(startOffer = sameOffer, endOffer = endOffer)
      val enrichedDiff = enricher.enrich(basicDiff)

      val expectedDiff = EnrichedOfferDiffUpdateData(
        startState = EnrichedOffer(
          offer = sameOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = testHolocronToRecord(sameOfferCassandra)
        ),
        endState = EnrichedOptOffer(
          offer = endOffer,
          key = EnrichedOfferKey.getEnrichedOfferKey(sameOffer, masterId = Some(sameOfferCassandra.offerId)),
          clusterRecord = Some(testHolocronToRecord(sameOfferCassandra))
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }
  }

  "Cassandra Enricher on the same offer with different address" should {
    val cassandraClient = new CassandraClientMockup
    val enricher = new CassandraEnricher(cassandraClient)

    val offerAddressDiffs: ArchiveOfferHolocron = testOfferTemplate.copy(
      offerId = TestIds.AddressDiffs.toString,
      date = LocalDate.parse("2016-01-16").toDateTimeAtStartOfDay,
      location = testOfferTemplate.location.copy(address = TestAddresses.AddressDiffs1.toString)
    )

    val offerAddressDiffsCassandra: ArchiveOfferHolocron = offerAddressDiffs.copy(
      location = offerAddressDiffs.location.copy(address = TestAddresses.AddressDiffs2.toString)
    )

    testRecordToCassandra(cassandraClient, testHolocronToRecord(offerAddressDiffsCassandra))

    //TODO: @vykukushkin
    // here is a not-a-bug-but-a-problem: as far as we don't know actual address for this offer stored in C*,
    // The consequences:
    // 1. After second inserting offer is stored twice on old and new addresses. That cannot be solved here,
    // in scheduler. Instead there should be another job that makes full-scan over C* and removes such duplicates
    // 2. If by some reasons scheduler failed to process msg where offer changed its address (f.ex, such change
    // was not written to holocron), we expect offer to be located on new address (as in holocron we see new address
    // in both startState and endState). However, in C* offer is located on old address, that we don't know. Such
    // situations are not legal, but should be processed carefully with do-nothing actions and detailed logging
    // Overall, for details, see REALTY-16058

    "enrich insert with no offer" in {
      val basicDiff = OfferDiffInsert(offerAddressDiffs)
      val enrichedDiff = enricher.enrich(basicDiff)
      val expectedDiff = EnrichedOfferDiffInsert(
        EnrichedOptOffer(
          offer = offerAddressDiffs,
          key = EnrichedOfferKey.getEnrichedOfferKey(offerAddressDiffs, masterId = None),
          clusterRecord = None
        )
      )

      enrichedDiff shouldEqual expectedDiff
    }

    "enrich close up to do-nothing" in {
      val basicDiff = OfferDiffClose(offerAddressDiffs)
      val enrichedDiff = enricher.enrich(basicDiff)
      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich delete up to do-nothing" in {
      val basicDiff = OfferDiffDelete(offerAddressDiffs)
      val enrichedDiff = enricher.enrich(basicDiff)
      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }

    "enrich update with existing offer when start address == actual C* address (legal update)" in {
      val startState = offerAddressDiffsCassandra.copy(date = offerAddressDiffs.date.minusDays(1))
      val endState = offerAddressDiffs

      val basicDiff = OfferDiffUpdateData(startOffer = startState, endOffer = endState)
      val enrichedDiff = enricher.enrich(basicDiff)

      val expectedDiff = EnrichedOfferDiffUpdateData(
        startState = EnrichedOffer(
          offer = startState,
          key = EnrichedOfferKey.getEnrichedOfferKey(startState, masterId = Some(offerAddressDiffsCassandra.offerId)),
          clusterRecord = testHolocronToRecord(offerAddressDiffsCassandra)
        ),
        endState = EnrichedOptOffer(
          offer = endState,
          key = EnrichedOfferKey.getEnrichedOfferKey(endState, masterId = None),
          clusterRecord = None
        )
      )
      enrichedDiff shouldEqual expectedDiff
    }

    "enrich update with existing offer when start address != actual C* address (illegal update)" in {
      val endState = offerAddressDiffs.copy(date = offerAddressDiffs.date.plusDays(1))
      val basicDiff = OfferDiffUpdateData(startOffer = offerAddressDiffs, endOffer = endState)
      val enrichedDiff = enricher.enrich(basicDiff)

      enrichedDiff.diffAction shouldEqual DiffAction.DoNothing
    }
  }
}
