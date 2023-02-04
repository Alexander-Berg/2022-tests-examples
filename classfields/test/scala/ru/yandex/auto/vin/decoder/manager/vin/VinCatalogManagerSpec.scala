package ru.yandex.auto.vin.decoder.manager.vin

import auto.carfax.common.utils.tracing.Traced
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.message.CatalogSchema.CatalogCardMessage
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.model.CarCard
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.CarsCatalog
import ru.yandex.auto.vin.decoder.manager.vin.catalog.VinToCatalogManager
import ru.yandex.auto.vin.decoder.model.offer.OfferGroup
import ru.yandex.auto.vin.decoder.model.{ColorsSelectorHolder, ResolutionData, VinCode}
import ru.yandex.auto.vin.decoder.proto.TtxSchema.CommonTtx
import ru.yandex.auto.vin.decoder.proto.VinHistory.{VinInfo, VinInfoHistory}
import ru.yandex.auto.vin.decoder.providers.engine.EngineProvider
import ru.yandex.auto.vin.decoder.providers.options.OptionsProvider
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

class VinCatalogManagerSpec extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfter {
  val optionsProvider = mock[OptionsProvider]
  val enginesProvider = mock[EngineProvider]
  val colorsProvider = mock[ColorsSelectorHolder]
  val catalog = mock[CarsCatalog]
  val rd = mock[ResolutionData]
  val feature = mock[Feature[Boolean]]

  implicit val t = Traced.empty

  before {
    reset(catalog)
    reset(rd)
  }

  implicit val m = TestOperationalSupport
  val manager = new VinToCatalogManager(catalog, optionsProvider, enginesProvider, colorsProvider, feature)

  val HONDA = "HONDA"
  val ACCORD = "ACCORD"
  val ACCORD_S = "ACCORD_S"
  val BMW = "BMW"
  val X1 = "X1"
  val X3 = "X3"

  val offerId = "32143421-sa4f4352ra"
  val vin = VinCode("XW7BF4HKX0S118638")
  val techParamsId = 123L
  val confId = 345L

  def offer(
      mark: String,
      model: String,
      dateOfPlacement: Long,
      offerId: String = "1234-fsd43",
      configuration: String = "",
      techParam: String = ""): VinInfo = {
    VinInfo
      .newBuilder()
      .setMark(mark)
      .setModel(model)
      .setDateOfPlacement(dateOfPlacement)
      .setConfigurationId(configuration)
      .setTechParamId(techParam)
      .build()
  }

  def registration(
      mark: String,
      model: String,
      year: Int = 2000,
      power: Int = 100,
      displacement: Int = 1500): VinInfoHistory = {
    val b = VinInfoHistory.newBuilder()
    b.getRegistrationBuilder
      .setMark(mark)
      .setModel(model)
      .setYear(year)
      .setPowerHp(power)
      .setDisplacement(displacement)
    b.build()
  }

  def offerGroup(offers: VinInfo*): OfferGroup = {
    OfferGroup(offers.toList, EventType.AUTORU_OFFER)
  }

  def vinHist(offers: VinInfo*): VinInfoHistory = {
    val b = VinInfoHistory.newBuilder()
    offers.foreach(b.addRecords)
    b.build()
  }

  def resData(
      offers: List[VinInfo],
      registration: Option[Prepared],
      ttx: Option[VinInfoHistory]): ResolutionData = {
    ResolutionData
      .empty(vin)
      .copy(
        registration = registration,
        offers = offers,
        autocodeTtx = ttx
      )
  }

  def carCard(
      mark: String,
      model: String,
      power: Int = 100,
      displacement: Int = 1500,
      techParamsId: Long = techParamsId,
      confId: Long = confId): CarCard = {
    val b = CatalogCardMessage.newBuilder().setVersion(1)
    b.getMarkBuilder.setCode(mark).setVersion(1)
    b.getModelBuilder.setCode(model).setVersion(1)
    b.getTechparameterBuilder
      .setPower(power)
      .setDisplacement(displacement)
      .setId(techParamsId)
      .setVersion(1)
    b.getConfigurationBuilder.setId(confId).setVersion(1)

    CarCard(b.build())
  }

  "getCardFromOffer" should {
    "use tech param if present" in {
      val of = offer(BMW, X1, 0, techParam = "555")
      when(catalog.getCardByTechParamId(555L))
        .thenReturn(Some(carCard(BMW, X1, techParamsId = 555L)))
      when(feature.value).thenReturn(true)
      val res = manager.getCardFromOffer(of, List.empty, CommonTtx.newBuilder().build())
      res.techParamId.get shouldBe 555L
      res.configurationId.get shouldBe confId

    }
  }

}
