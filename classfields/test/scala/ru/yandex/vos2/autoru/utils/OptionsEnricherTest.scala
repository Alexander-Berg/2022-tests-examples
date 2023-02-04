package ru.yandex.vos2.autoru.utils

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.vin.VinReportModel.RawVinEssentialsReport
import ru.auto.catalog.model.api.ApiModel.DescriptionParseResult
import ru.auto.feedprocessor.FeedprocessorModel.Entity
import ru.yandex.auto.searcher.filters.EquipmentFilters.OptionMessage
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.api.model.WithFailures
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.catalog.CatalogClient
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.autoru.utils.options.OptionsEnricher
import ru.yandex.vos2.dao.offers.OfferUpdate.OfferUpdate
import ru.yandex.vos2.model.{UserRef, UserRefAutoruClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._
import scala.util.Success
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OptionsEnricherTest extends AnyFunSuite with MockitoSupport {

  private val catalogClient = mock[CatalogClient]
  private val vinDecoderClient = mock[VinDecoderClient]
  private val offersDao = mock[AutoruOfferDao]

  private val enrichOfferFromDescriptionFeature = {
    val f = mock[Feature[Boolean]]
    when(f.value).thenReturn(true)
    f
  }

  private val enrichEquipmentsFromVinDecoderFeature = {
    val f = mock[Feature[Boolean]]
    when(f.value).thenReturn(true)
    f
  }

  private val optionsEnricher = new OptionsEnricher(
    () => offersDao,
    catalogClient,
    vinDecoderClient,
    enrichOfferFromDescriptionFeature,
    enrichEquipmentsFromVinDecoderFeature
  )
  implicit val trace: Traced = Traced.empty

  private val carOffer = TestUtils.createOffer(dealer = true, category = Category.CARS).build
  private val carForm = ApiOfferModel.Offer.newBuilder.setCategory(Category.CARS).build

  private val carEntity = Entity.newBuilder
    .setAutoru(carForm)
    .setPosition(0)
    .setOfferId(carOffer.getOfferID)
    .setVin("some_vin_code")
    .build

  test("enrich entity and new offer") {
    val user = UserRefAutoruClient(42)
    when(catalogClient.parseDescription(?)(?)).thenReturn {
      val result = DescriptionParseResult.newBuilder()
      result
        .addOptionsBuilder()
        .setValue(OptionMessage.newBuilder().setCode("option_code").setName("option_name"))
      result.build()
    }

    when(vinDecoderClient.getRawEssentialsReport(?)(?)).thenReturn {
      val builder = RawVinEssentialsReport.newBuilder
      builder.getVehicleBuilder.getCarInfoBuilder.putAllEquipment {
        Map(
          "equipment_from_vin_decoder" -> Boolean.box(true),
          "halogen" -> Boolean.box(true),
          "electro-window-all" -> Boolean.box(true)
        ).asJava
      }
      Success(Some(builder.build))
    }

    var updates: Iterable[OfferUpdate] = Iterable.empty
    stub(
      offersDao.useWithUser(_: UserRef, _: Iterable[OfferID], _: Boolean, _: Boolean, _: String)(
        _: Iterable[Offer] => Iterable[OfferUpdate]
      )(_: Traced)
    ) {
      case (_, _, _, _, _, f, _) =>
        updates = updates ++ f(Seq(carOffer))
        Success(WithFailures.Empty)
    }

    val res = optionsEnricher.enrichEquipmentForEntities(user, Map(carEntity -> None))

    assertResult(1)(res.head.getErrorsCount)
    assertResult(Entity.Error.Type.NOTICE)(res.head.getErrors(0).getType)
    assert(res.head.getErrors(0).getMessage.contains("option_name"))

    val firstUpdate = updates.head
    val firstCarInfo = firstUpdate.getUpdate.get.getOfferAutoru.getCarInfo
    assert(firstCarInfo.getEquipmentList.asScala.exists(_.getName == "option_code"))
    assert(firstCarInfo.getEquipmentsMeta.getDescriptionParsed)
    val firstEquipmentsMap = firstCarInfo.getEquipmentsMeta.getEquipmentMetaMap.asScala
    assert(firstEquipmentsMap.contains("option_code"))

    val lastUpdate = updates.last
    assert(lastUpdate.getUpdate.isDefined)
    val lastCarInfo = lastUpdate.getUpdate.get.getOfferAutoru.getCarInfo
    val expectedOptions = Seq("equipment_from_vin_decoder", "electro-window-back", "electro-window-front")
    assert {
      val actualEuipments = lastCarInfo.getEquipmentList.asScala.map(_.getName)
      expectedOptions.forall(actualEuipments.contains)
    }
    val lastEquipmentsMap = lastCarInfo.getEquipmentsMeta.getEquipmentMetaMap.asScala
    assert(lastEquipmentsMap.contains("equipment_from_vin_decoder"))
  }
}
