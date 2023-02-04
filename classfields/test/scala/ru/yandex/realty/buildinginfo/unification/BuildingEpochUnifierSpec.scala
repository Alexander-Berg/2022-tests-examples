package ru.yandex.realty.buildinginfo.unification

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.building.model.BuildingEpoch
import ru.yandex.realty.buildinginfo.model.OfferBuilding
import ru.yandex.realty.model.offer._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BuildingEpochUnifierSpec extends FlatSpec with Matchers with PropertyChecks {

  private val buildingEpochUnifier = new BuildingEpochUnifier {}

  import BuildingEpochUnifierSpec._

  "BuildingEpochUnifier" should "calculate isStalinka to true for text 'сталинка'" in {
    for (text <- Set("сталинка", "Сталинка", "в сталинском")) {
      val offer = createOfferBuilding(Some(text))
      val isStalinka = BuildingEpochUnifier.isStalinkaFromDescription(offer)
      isStalinka shouldBe true
    }
  }

  it should "calculate isStalinka to false for text 'не сталинка'" in {
    for (text <- Set("не сталинка", "не\tсталинка", "не \"сталинка")) {
      val offer = createOfferBuilding(Some(text))
      val isStalinka = BuildingEpochUnifier.isStalinkaFromDescription(offer)
      isStalinka shouldBe false
    }
  }

  it should "calculate isKhrushchevka to true for text 'хрущёвка'" in {
    for (text <- Set("хрущёвка", "Хрущёвка", "хрущевка", "хрущ")) {
      val offer = createOfferBuilding(Some(text))
      val isKhrushchevka = BuildingEpochUnifier.isKhrushchevkaFromDescription(offer)
      isKhrushchevka shouldBe true
    }
  }

  it should "calculate isKhrushchevka to false for text 'не хрущёвка'" in {
    for (text <- Set("не хрущёвка", "не\tхрущевка", "не \"хрущёвка")) {
      val offer = createOfferBuilding(Some(text))
      val isKhrushchevka = BuildingEpochUnifier.isKhrushchevkaFromDescription(offer)
      isKhrushchevka shouldBe false
    }
  }

  it should "calculate isBrezhnevka to true for text 'брежневка'" in {
    for (text <- Set("брежневка", "Брежневка", "в брежневском")) {
      val offer = createOfferBuilding(Some(text))
      val isBrezhnevka = BuildingEpochUnifier.isBrezhnevkaFromDescription(offer)
      isBrezhnevka shouldBe true
    }
  }

  it should "calculate isBrezhnevka to false for text 'не брежневка'" in {
    for (text <- Set("не брежневка", "не\tбрежневка", "не \"брежневка")) {
      val offer = createOfferBuilding(Some(text))
      val isBrezhnevka = BuildingEpochUnifier.isBrezhnevkaFromDescription(offer)
      isBrezhnevka shouldBe false
    }
  }

  it should "calculate building epoch by epoch verba correction" in {
    for (epoch <- BuildingEpoch.values()) {
      val ctx = BuildingEpochContext(
        buildingId = 0,
        buildYear = None,
        ceilingHeight = None,
        floorsCount = None,
        buildingType = None,
        hasStalinOffers = false,
        hasBrezhnevOffers = false,
        hasKhrushchevOffers = false,
        isStalinSeries = false,
        isKhrushchevSeries = false,
        isBrezhnevSeries = false
      )
      val unifiedEpoch = buildingEpochUnifier.unifyBuildingEpoch(buildingEpochCorrection = Some(epoch), ctx)
      unifiedEpoch shouldEqual Some(epoch)
    }
  }

  private def checkUnificationBySeriesNames(
    buildingEpochFromSeries: BuildingEpoch,
    expectedBuildingEpoch: BuildingEpoch
  ): Unit = {
    val ctx = BuildingEpochContext(
      buildingId = 0,
      buildYear = None,
      ceilingHeight = None,
      floorsCount = None,
      buildingType = None,
      hasStalinOffers = false,
      hasBrezhnevOffers = false,
      hasKhrushchevOffers = false,
      isStalinSeries = buildingEpochFromSeries == BuildingEpoch.BUILDING_EPOCH_STALIN,
      isKhrushchevSeries = buildingEpochFromSeries == BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV,
      isBrezhnevSeries = buildingEpochFromSeries == BuildingEpoch.BUILDING_EPOCH_BREZHNEV
    )

    val unifiedEpoch = buildingEpochUnifier.unifyBuildingEpoch(buildingEpochCorrection = None, ctx)
    unifiedEpoch shouldEqual Some(expectedBuildingEpoch)
  }

  it should "calculate building epoch by building series" in {
    checkUnificationBySeriesNames(BuildingEpoch.BUILDING_EPOCH_STALIN, BuildingEpoch.BUILDING_EPOCH_STALIN)
    checkUnificationBySeriesNames(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV, BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
    checkUnificationBySeriesNames(BuildingEpoch.BUILDING_EPOCH_BREZHNEV, BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
  }

  private def unifyByBuildYearAndSeries(
    year: Int,
    epochFromSeries: Option[BuildingEpoch] = None
  ): Option[BuildingEpoch] = {
    val ctx = BuildingEpochContext(
      buildingId = 0,
      buildYear = Some(year),
      ceilingHeight = None,
      floorsCount = None,
      buildingType = None,
      hasStalinOffers = false,
      hasBrezhnevOffers = false,
      hasKhrushchevOffers = false,
      isStalinSeries = epochFromSeries.contains(BuildingEpoch.BUILDING_EPOCH_STALIN),
      isKhrushchevSeries = epochFromSeries.contains(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV),
      isBrezhnevSeries = epochFromSeries.contains(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
    )
    buildingEpochUnifier.unifyBuildingEpoch(buildingEpochCorrection = None, ctx)
  }

  it should "calculate stalin building epoch for buildYear in (1923, 1953) without buildingSeries" in {
    forAll(Gen.choose(1923, 1953)) { year =>
      val unifiedEpoch = unifyByBuildYearAndSeries(year)
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
    }
  }

  it should "calculate epoch UNRECOGNIZED for buildYear in (1923, 1953) with brezhnev, khrushchev buildingSeries" in {
    val inconsistentBuildingEpochs = Set(BuildingEpoch.BUILDING_EPOCH_BREZHNEV, BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
    for (buildingEpochFromSeries <- inconsistentBuildingEpochs) {
      forAll(Gen.choose(1923, 1953)) { year =>
        val unifiedEpoch = unifyByBuildYearAndSeries(year, Some(buildingEpochFromSeries))
        unifiedEpoch shouldEqual Some(BuildingEpoch.UNRECOGNIZED)
      }
    }
  }

  it should "calculate building epoch None for buildYear in (1954, 1957) with brezhnev buildingSeries" in {
    forAll(Gen.choose(1954, 1957)) { year =>
      val unifiedEpoch = unifyByBuildYearAndSeries(year, Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV))
      unifiedEpoch shouldEqual Some(BuildingEpoch.UNRECOGNIZED)
    }
  }

  private def unifyByText(
    year: Int,
    text: String,
    ceilingHeight: Option[Int] = None,
    floorCount: Option[Int] = None
  ): Option[BuildingEpoch] = {
    val offerBuilding = createOfferBuilding(description = Some(text))
    val ctx = BuildingEpochContext(
      buildingId = 0,
      buildYear = Some(year),
      ceilingHeight = ceilingHeight,
      floorsCount = floorCount,
      buildingType = None,
      hasStalinOffers = BuildingEpochUnifier.isStalinkaFromDescription(offerBuilding),
      hasBrezhnevOffers = BuildingEpochUnifier.isBrezhnevkaFromDescription(offerBuilding),
      hasKhrushchevOffers = BuildingEpochUnifier.isKhrushchevkaFromDescription(offerBuilding),
      isStalinSeries = false,
      isKhrushchevSeries = false,
      isBrezhnevSeries = false
    )
    buildingEpochUnifier.unifyBuildingEpoch(buildingEpochCorrection = None, ctx)
  }

  private def unifyByTextAndSize(
    text: String,
    ceilingHeight: Option[Int] = None,
    floorCount: Option[Int] = None
  ): Option[BuildingEpoch] = {
    val offerBuilding = createOfferBuilding(description = Some(text))
    val ctx = BuildingEpochContext(
      buildingId = 0,
      buildYear = None,
      ceilingHeight = ceilingHeight,
      floorsCount = floorCount,
      buildingType = None,
      hasStalinOffers = BuildingEpochUnifier.isStalinkaFromDescription(offerBuilding),
      hasBrezhnevOffers = BuildingEpochUnifier.isBrezhnevkaFromDescription(offerBuilding),
      hasKhrushchevOffers = BuildingEpochUnifier.isKhrushchevkaFromDescription(offerBuilding),
      isStalinSeries = false,
      isKhrushchevSeries = false,
      isBrezhnevSeries = false
    )
    buildingEpochUnifier.unifyBuildingEpoch(buildingEpochCorrection = None, ctx)
  }

  it should "calculate building epoch stalin for buildYear in (1954, 1957) if no khrushev in description" in {
    forAll(Gen.choose(1954, 1957)) { year =>
      val unifiedEpoch = unifyByText(year, "не хрущёвка")
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
    }
  }

  it should "calculate building epoch khrushchev for buildYear in (1954, 1957) if khrushev in description" in {
    forAll(Gen.choose(1954, 1957)) { year =>
      forAll(Gen.option(Gen.choose(200, 279))) { ceilingHeight =>
        val unifiedEpoch = unifyByText(year, "хрущёвка не сталинка", ceilingHeight)
        unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
      }
    }
  }

  it should "calculate epoch None for buildYear (1954, 1957) if khrushchev in text and ceiling height >= 280" in {
    forAll(Gen.choose(1954, 1957)) { year =>
      forAll(Gen.some(Gen.choose(280, 300))) { ceilingHeight =>
        val unifiedEpoch = unifyByText(year, "хрущёвка", ceilingHeight)
        unifiedEpoch shouldEqual None
      }
    }
  }

  it should "calculate epoch khrushchev for buildYear (1958, 1961) and khrushchev in text and ceiling height < 280" in {
    forAll(Gen.choose(1958, 1961)) { year =>
      val unifiedEpoch = unifyByText(year, "хрущёвка не сталинка", ceilingHeight = Some(279))
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
    }
  }

  it should "calculate epoch khrushchev for (1958, 1961) and not stalinka in text and ceiling height < 280" in {
    forAll(Gen.choose(1958, 1961)) { year =>
      forAll(Gen.choose(231, 279)) { height =>
        val unifiedEpoch = unifyByText(year, "не сталинка", ceilingHeight = Some(height))
        unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
      }
    }
  }

  it should "calculate epoch stalin for (1958, 1961) and stalinka in text and ceiling height > 280" in {
    forAll(Gen.choose(1958, 1961)) { year =>
      for (ceilingHeight <- Seq(None, Some(281))) {
        val unifiedEpoch = unifyByText(year, "сталинка", ceilingHeight)
        unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
      }
    }
  }

  it should "calculate epoch stalin for (1958, 1961) and no khrushchev in text and ceiling height > 280" in {
    forAll(Gen.choose(1958, 1961)) { year =>
      val unifiedEpoch = unifyByText(year, "foo", ceilingHeight = Some(281))
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
    }
  }

  it should "calculate epoch khrushchev for (1962, 1964) and no stalinka in text and ceiling height < 280" in {
    forAll(Gen.choose(1962, 1964)) { year =>
      for (ceilingHeight <- Seq(None, Some(279))) {
        val unifiedEpoch = unifyByText(year, "bar", ceilingHeight)
        unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
      }
    }
  }

  it should "calculate epoch stalin for (1962, 1964) and stalinka in text and ceiling height > 280" in {
    forAll(Gen.choose(1962, 1964)) { year =>
      val unifiedEpoch = unifyByText(year, "сталинка", ceilingHeight = Some(281))
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
    }
  }

  it should "calculate building epoch correctly for (1965, 1972)" in {
    forAll(Gen.choose(1965, 1972)) { year =>
      for (ceilingHeight <- Seq(None, Some(279))) {
        unifyByText(year, "хрущёвка", ceilingHeight) shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
      }

      for (ceilingHeight <- Seq(None, Some(289))) {
        unifyByText(year, "брежневка", ceilingHeight) shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
      }

      unifyByText(year, "сталинка", Some(281)) shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)

      for (ceilingHeight <- Seq(None, Some(289))) {
        unifyByText(year, "some", ceilingHeight, Some(6)) shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
      }

      for (ceilingHeight <- 231 until 280 by 10) {
        for (floorCount <- 4 to 5) {
          val res = unifyByText(year, "some", Some(ceilingHeight), Some(floorCount))
          res shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
        }
      }
    }
  }

  it should "calculate building epoch correctly for (1973, 1982)" in {
    forAll(Gen.choose(1973, 1982)) { year =>
      for (ceilingHeight <- Seq(None, Some(279))) {
        unifyByText(year, "some text", ceilingHeight) shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
      }

      for (ceilingHeight <- 231 until 280 by 10) {
        for (floorCount <- 4 to 5) {
          val res = unifyByText(year, "хрущёвка не сталинка не брежневка", Some(ceilingHeight), Some(floorCount))
          res shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
        }
      }
    }
  }

  it should "calculate building epoch correctly for (1983, 1990)" in {
    forAll(Gen.choose(1983, 1990)) { year =>
      for (ceilingHeight <- Seq(None, Some(289))) {
        val res = unifyByText(year, "брежневка не сталинка не хрущёвка", ceilingHeight, Some(6))
        res shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
      }
    }
  }

  it should "calculate building epoch correctly without build year" in {
    for (ceilingHeight <- Seq(None, Some(289))) {
      for (floorCount <- Seq(None, Some(6))) {
        val res = unifyByTextAndSize("брежневка не хрущёвка не сталинка", ceilingHeight, floorCount)
        res shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_BREZHNEV)
      }
    }
    unifyByTextAndSize("брежневка не хрущёвка не сталинка", Some(291)) shouldEqual None
    unifyByTextAndSize("брежневка не хрущёвка не сталинка", Some(289), Some(4)) shouldEqual None

    for (ceilingHeight <- Seq(None, Some(279))) {
      for (floorCount <- Seq(None, Some(4), Some(5))) {
        val res = unifyByTextAndSize("хрущёвка не сталинка не брежневка", ceilingHeight, floorCount)
        res shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_KHRUSHCHEV)
      }
    }
    unifyByTextAndSize("хрущёвка не сталинка не брежневка", Some(281)) shouldEqual None
    unifyByTextAndSize("хрущёвка не сталинка не брежневка", Some(279), Some(6)) shouldEqual None

    for (ceilingHeight <- Seq(None, Some(281))) {
      val unifiedEpoch = unifyByTextAndSize("сталинка не хрущёвка не брежневка", ceilingHeight)
      unifiedEpoch shouldEqual Some(BuildingEpoch.BUILDING_EPOCH_STALIN)
    }
    unifyByTextAndSize("сталинка не хрущёвка не брежневка", Some(279)) shouldEqual None
  }
}

object BuildingEpochUnifierSpec {

  def createEmptyOfferBuilding(): OfferBuilding = {
    val builder = new OfferBuilding.Builder
    builder.offerId = 1L
    builder.updateTimestamp = 2L
    builder.latitude = 123f
    builder.longitude = 321f
    builder.build
  }

  def createOfferBuilding(description: Option[String]): OfferBuilding = {
    val builder = new OfferBuilding.Builder
    builder.offerId = 1L
    builder.updateTimestamp = 2L
    builder.latitude = 123f
    builder.longitude = 321f
    builder.buildingName = "Greenland"
    builder.buildYear = 2016
    builder.buildingType = BuildingType.MONOLIT
    builder.buildingSeries = None.orNull
    builder.totalFloors = 21
    builder.hasParking = false
    builder.hasLift = true
    builder.hasRubbishChute = true
    builder.hasSecurity = false
    builder.isGuarded = None.orNull
    builder.ceilingHeight = 2.75f
    builder.offerType = OfferType.SELL
    builder.categoryType = CategoryType.APARTMENT
    builder.pricingPeriod = PricingPeriod.PER_MONTH
    builder.rooms = Rooms.STUDIO
    builder.sqmInRubles = Money.of(Currency.RUR, 30000.0)
    builder.offerDescription = description.orNull
    builder.build
  }
}
