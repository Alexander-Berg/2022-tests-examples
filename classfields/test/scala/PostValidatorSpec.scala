import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.archive.scheduler.updater.diff._
import ru.yandex.realty.archive.scheduler.updater.managers.PostValidatorImpl
import ru.yandex.realty.archive.scheduler.updater.managers.PostValidatorImpl.{MaxAllowedRent, MinAllowedSell}
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.archive.{ArchiveOfferClusterBrother, _}
import ru.yandex.realty.model.offer._

import scala.language.implicitConversions

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-06-19
  */

@RunWith(classOf[JUnitRunner])
class PostValidatorSpec extends AsyncSpecBase with Logging with PropertyChecks {

  private lazy val postvalidator = new PostValidatorImpl()

  val testOfferTemplate = ArchiveOfferHolocron(
    offerId = "123321",
    date = DateTime.parse("2016-01-15T00:00:00.000"),
    price = ArchiveOfferPrice(value = 300000, period = PricingPeriod.PER_MONTH, currency = Currency.RUR),
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
    location = ArchiveOfferHolocronLocation(localityName = None, subjectFederationId = None, address = "test-address"),
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

  var lastPartnerId = 111333

  def nextUniquePartnerId(): String = {
    lastPartnerId += 1
    lastPartnerId.toString
  }

  def holo(offerType: OfferType, date: String, price: Long = testOfferTemplate.price.value): ArchiveOfferHolocron = {
    testOfferTemplate.copy(
      date = LocalDate.parse(date).toDateTimeAtStartOfDay,
      price = testOfferTemplate.price.copy(value = price),
      offerType = offerType,
      offerInfo = testOfferTemplate.offerInfo.copy(partnerId = nextUniquePartnerId())
    )
  }

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

  def toActiveRecord(offer: ArchiveOfferHolocron): ArchiveOfferCassandraRecord = {
    mergeWithStartDate(offer, offer, isActive = true)
  }

  def insert(offer: ArchiveOfferHolocron): EnrichedOfferDiffInsert = {
    EnrichedOfferDiffInsert(
      EnrichedOptOffer(offer, key = EnrichedOfferKey.getEnrichedOfferKey(offer, None), clusterRecord = None)
    )
  }

  def delete(startState: ArchiveOfferHolocron, endState: ArchiveOfferHolocron): EnrichedOfferDiffDelete = {
    EnrichedOfferDiffDelete(
      EnrichedOffer(
        endState,
        key = EnrichedOfferKey.getEnrichedOfferKey(endState, Some(startState.offerId)),
        clusterRecord = toActiveRecord(startState)
      )
    )
  }

  def close(startState: ArchiveOfferHolocron, endState: ArchiveOfferHolocron): EnrichedOfferDiffClose = {
    EnrichedOfferDiffClose(
      EnrichedOffer(
        endState,
        key = EnrichedOfferKey.getEnrichedOfferKey(endState, Some(startState.offerId)),
        clusterRecord = toActiveRecord(startState)
      )
    )
  }

  def update(startState: ArchiveOfferHolocron, endState: ArchiveOfferHolocron): EnrichedOfferDiffUpdateData = {
    val almostEndState = startState.copy(date = endState.date.minusDays(1))
    EnrichedOfferDiffUpdateData(
      startState = EnrichedOffer(
        almostEndState,
        key = EnrichedOfferKey.getEnrichedOfferKey(almostEndState, Some(startState.offerId)),
        clusterRecord = toActiveRecord(startState)
      ),
      endState = EnrichedOptOffer(
        endState,
        key = EnrichedOfferKey.getEnrichedOfferKey(endState, Some(startState.offerId)),
        clusterRecord = Some(toActiveRecord(startState))
      )
    )
  }

  def deleteInsteadUpdate(startState: ArchiveOfferHolocron, endState: ArchiveOfferHolocron): EnrichedOfferDiffDelete = {
    val almostEndState = startState.copy(date = endState.date.minusDays(1))
    delete(startState, almostEndState)
  }

  "PostValidator on insert" should {
    "do nothing" in {
      val diff = insert(holo(OfferType.RENT, "2019-01-01"))
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

  }

  "PostValidator on delete" should {
    "do nothing" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-15")
      val diff = delete(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
  }

  "PostValidator on close" should {
    "do nothing if offer is valid" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-15")
      val diff = close(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
    "delete offer if too short exposition on sell" in {
      val startOffer = holo(OfferType.SELL, "2019-01-01")
      val endOffer = holo(OfferType.SELL, "2019-01-02")
      val diff = close(startOffer, endOffer)
      val expectedDiff = delete(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
    "delete offer if too long exposition on rent" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-01")
      val diff = close(startOffer, endOffer)
      val expectedDiff = delete(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if too short exposition on rent" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-02")
      val diff = close(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
    "do nothing if too long exposition on sell" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = holo(OfferType.SELL, "2019-01-01")
      val diff = close(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "delete if exposition on rent is more than edge" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MaxAllowedRent).plusDays(1))
      val diff = close(startOffer, endOffer)
      val expectedDiff = delete(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if exposition on rent is less or equal than edge" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MaxAllowedRent))
      val diff = close(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "delete if exposition on sell is less than edge" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MinAllowedSell).minusDays(1))
      val diff = close(startOffer, endOffer)
      val expectedDiff = delete(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if exposition on sell is more or equal than edge" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MinAllowedSell))
      val diff = close(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "delete if too big price change" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01", price = 1000)
      val endOffer = holo(OfferType.RENT, "2019-01-15", price = 100000)
      val diff = close(startOffer, endOffer)
      val expectedDiff = delete(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
  }

  "PostValidator on update" should {
    "do nothing if offer is valid" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-15")
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if too short exposition on sell" in {
      val startOffer = holo(OfferType.SELL, "2019-01-01")
      val endOffer = holo(OfferType.SELL, "2019-01-03")
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
    "delete offer if too long exposition on rent" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = holo(OfferType.RENT, "2018-01-01")
      val diff = update(startOffer, endOffer)
      val expectedDiff = deleteInsteadUpdate(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if too short exposition on rent" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01")
      val endOffer = holo(OfferType.RENT, "2019-01-02")
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
    "do nothing if too long exposition on sell" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = holo(OfferType.SELL, "2019-01-01")
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "delete if exposition on rent is more than edge" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MaxAllowedRent).plusDays(1))
      val diff = update(startOffer, endOffer)
      val expectedDiff = deleteInsteadUpdate(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if exposition on rent is less or equal than edge" in {
      val startOffer = holo(OfferType.RENT, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MaxAllowedRent))
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if exposition on sell is less than edge" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MinAllowedSell).minusDays(1))
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "do nothing if exposition on sell is more or equal than edge" in {
      val startOffer = holo(OfferType.SELL, "2017-01-01")
      val endOffer = startOffer.copy(date = startOffer.date.plus(MinAllowedSell))
      val diff = update(startOffer, endOffer)
      val expectedDiff = diff
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }

    "delete if too big price change" in {
      val startOffer = holo(OfferType.RENT, "2019-01-01", price = 1000)
      val endOffer = holo(OfferType.RENT, "2019-01-15", price = 100000)
      val diff = update(startOffer, endOffer)
      val expectedDiff = deleteInsteadUpdate(startOffer, endOffer)
      val realDiff = postvalidator.getValid(diff)
      realDiff shouldEqual expectedDiff
    }
  }
}
