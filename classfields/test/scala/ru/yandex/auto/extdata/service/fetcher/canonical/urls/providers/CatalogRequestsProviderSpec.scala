package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers

import org.junit.runner.RunWith
import org.scalatest.concurrent.TimeLimits
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.DummyPreparedQueryProvider
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.catalog.model.{ConfigurationImpl, MarkImpl, ModelImpl, SuperGenerationImpl}
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.core.region.{Region, RegionService, RegionTree, RegionType}
import ru.yandex.auto.eds.catalog.cars.structure.{
  ConfigurationStructure,
  MarkStructure,
  ModelStructure,
  SuperGenStructure
}
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.service.canonical.router.model.Params._
import ru.yandex.auto.extdata.service.canonical.router.model.{CanonicalUrlRequest, CanonicalUrlRequestType}
import ru.yandex.auto.index.consumer.YoctoSearcher
import ru.yandex.auto.message.CarAdSchema.CarAdMessage
import ru.yandex.auto.traffic.utils.ColorUtils
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CatalogRequestsProviderSpec extends FlatSpecLike with Matchers with TimeLimits with MockitoSupport {

  private val ConfId: Long = 1
  private val SuperGenId: Long = 2
  private val Model: String = "a7"
  private val Mark: String = "audi"
  private val Mark2: String = "renault"
  private val RegionCode = 213
  private val Colors = ColorUtils.getKnownColorCodes

  private val configEntity = {
    val conf = mock[ConfigurationImpl]
    when(conf.getId).thenReturn(ConfId)
    conf
  }

  private val superGenEntity = {
    val conf = mock[SuperGenerationImpl]
    when(conf.getId).thenReturn(SuperGenId)
    conf
  }

  private val modelEntity = {
    val model = mock[ModelImpl]
    when(model.getCode).thenReturn(Model)
    model
  }

  private val markEntity = {
    val mark = mock[MarkImpl]
    when(mark.getCode).thenReturn(Mark)
    mark
  }

  private val service = mock[CarsCatalogGroupingService]
  private val mockedMark = mock[MarkStructure]
  private val mockedModel = mock[ModelStructure]
  private val mockedSuperGen = mock[SuperGenStructure]
  private val mockedConfiguration = mock[ConfigurationStructure]

  when(mockedConfiguration.entity).thenReturn(configEntity)

  when(mockedSuperGen.entity).thenReturn(superGenEntity)
  when(mockedSuperGen.members).thenReturn(Seq(mockedConfiguration))

  when(mockedModel.entity).thenReturn(modelEntity)
  when(mockedModel.members).thenReturn(Seq(mockedSuperGen))

  when(mockedMark.entity).thenReturn(markEntity)
  when(mockedMark.members).thenReturn(Seq(mockedModel))

  when(service.buildGroupByMarks).thenReturn(Seq(mockedMark))

  private val regionTree: RegionTree = mock[RegionTree]
  when(regionTree.getRegion(?)).thenReturn(new Region(RegionCode, "", RegionType.SUBJECT_FEDERATION, null, 0, 0))

  private val carAdMessage = CarAdMessage
    .newBuilder()
    .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
    .setRegionCode(RegionCode.toString)
    .setMark(Mark)
    .setSearchState(Search.NEW.name())
    .setUrlHashId("")
    .setCreationDate(12345L)
    .setUrl("qwerty")
    .build()

  private val messages = Seq(
    carAdMessage,
    carAdMessage.toBuilder
      .setMark(Mark2)
      .addAllColor((Colors ++ Seq("SOME_UNKNOWN")).asJava)
      .build()
  )

  private val carsPreparedQuery = new DummyPreparedQueryProvider(messages).get()
  private val carsSearcher: YoctoSearcher[CarAdMessage] = mock[YoctoSearcher[CarAdMessage]]
  when(carsSearcher.prepare(?)).thenReturn(carsPreparedQuery)

  private val CatalogModelListing: Seq[CanonicalUrlRequest] = {
    val base = CanonicalUrlRequest(CanonicalUrlRequestType.CatalogModelListing, Set(new MarkParam(Mark)))

    Seq(base, base.mobileRequest)
  }

  private val CatalogGenerationListing: Seq[CanonicalUrlRequest] = {
    val modelOnly = CanonicalUrlRequest(
      CanonicalUrlRequestType.CatalogGenerationListing,
      Set(new MarkParam(Mark), new ModelParam(Model))
    )

    Seq(modelOnly, modelOnly.withParam(new SuperGenParam(SuperGenId)))
  }

  private val CatalogCard: Seq[CanonicalUrlRequest] = {
    val base = CanonicalUrlRequest(
      CanonicalUrlRequestType.CatalogCard,
      Set(new MarkParam(Mark), new ModelParam(Model), new SuperGenParam(SuperGenId), new ConfigurationParam(ConfId))
    )

    val notMobile = Seq(
      base,
      base.withType(CanonicalUrlRequestType.CatalogCardEquipment),
      base.withType(CanonicalUrlRequestType.CatalogCardSpecification)
    )

    notMobile ++ notMobile.map(_.mobileRequest)
  }

  private val Listing: Seq[CanonicalUrlRequest] = {
    def requests(mark: String, colors: Iterable[String]) = {
      val r = CanonicalUrlRequest(CanonicalUrlRequestType.ListingType)
        .withParam(new GeoIdParam(RegionCode))
        .withParam(CategoryParam.Cars)
        .withParam(new MarkParam(mark))
        .withParam(SectionParam(Search.NEW))

      Seq(r) ++ colors.map(c => r.withParam(new ColorParam(c)))
    }

    requests(Mark, colors = Seq.empty) ++ requests(Mark2, Colors.flatMap(ColorUtils.getColorTranslation))
  }

  it should "correctly build requests" in {

    val expected =
      CatalogModelListing ++
        CatalogGenerationListing ++
        CatalogCard ++
        Listing

    val regionService = mock[RegionService]

    when(regionService.getRegionTree).thenReturn(regionTree)

    val provider = new CatalogRequestsProvider(service, carsSearcher, regionService)

    provider.get().toSeq should contain theSameElementsAs expected
  }
}
