package ru.yandex.realty.traffic

import com.yandex.yoctodb.immutable.{FilterableIndex, IndexedDatabase, SortableIndex}
import com.yandex.yoctodb.query.{DocumentProcessor, Query}
import com.yandex.yoctodb.util.buf.Buffer
import org.mockito.invocation.InvocationOnMock
import play.api.libs.json._
import ru.yandex.common.util.IOUtils
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.extdata.provider.loader.DataLoader
import ru.yandex.realty.application.service.Prometheus.Prometheus
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.canonical.base.request.Request
import ru.yandex.realty.clients.router.parser.ListingFiltersParser
import ru.yandex.realty.context.ExtDataLoaders
import ru.yandex.realty.context.street.StreetStorage
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.message.ExtDataSchema.RailwayStation
import ru.yandex.realty.model.message.RealtySchema.OfferMessage
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.serialization.OfferProtoConverter
import ru.yandex.realty.model.sites.Company
import ru.yandex.realty.parser.RegionNamesTranslationsJsonParser
import ru.yandex.realty.proto.unified.offer.UnifiedOffer
import ru.yandex.realty.proto.village.Village
import ru.yandex.realty.railway.RailwayStationsStorage
import ru.yandex.realty.sites.stat.SiteInfoStorage
import ru.yandex.realty.sites.stat.model.{SiteInfoItem, SiteOffersStat, SiteRooms, SiteRoomsStat}
import ru.yandex.realty.sites.{CompaniesStorage, SitesGroupingService, SitesGroupingServiceImpl}
import ru.yandex.realty.storage.RegionDocumentsStatisticsStorage
import ru.yandex.realty.traffic.service.FrontendRouter.FrontendRouter
import ru.yandex.realty.traffic.service.RegionService.RegionService
import ru.yandex.realty.traffic.service.live.LiveRegionService
import ru.yandex.realty.traffic.service.{FrontendRouter, RegionService}
import ru.yandex.realty.traffic.testkit.FakeController
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration.FilterName
import ru.yandex.realty.urls.router.model.filter.{FilterDeclaration, ListingFilters}
import ru.yandex.realty.urls.router.model.{RouterUrlRequest, RouterUrlResponse, ViewType}
import ru.yandex.realty.util.IOUtil
import ru.yandex.realty.util.Mappings._
import ru.yandex.realty.util.geo.{GeoHelper, GeoHelperProvider}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.SimpleCompositeCollector
import zio._

import java.io.InputStream
import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

object TestData extends MockitoSupport {

  private def processResource[T](name: String)(f: InputStream => T): T =
    IOUtil.using(getClass.getClassLoader.getResourceAsStream(name))(f)

  lazy val routerFilters: ListingFilters =
    processResource("frontend-router-filters.json") { is =>
      ListingFiltersParser
        .parse(
          Json.parse(is)
        )
        .get
    }

  lazy val filtersMap: Map[FilterName, FilterDeclaration] =
    routerFilters.normalizedMap.values
      .flatMap(_.values.flatMap(_.values.flatten))
      .map(f => f.name -> f)
      .toMap

  lazy val regionGraph: RegionGraph =
    processResource("region_graph-8-2.data")(is => RegionGraphProtoConverter.deserialize(IOUtils.gunzip(is)))

  private lazy val regionNameTraanslations =
    processResource("region_names_translations-1-1.data")(RegionNamesTranslationsJsonParser.parse)

  lazy val regionDocumentsStatisticsStorage: RegionDocumentsStatisticsStorage =
    processResource("region_documents_statistics-2-49841.data")(ExtDataLoaders.createRegionDocumentsStatisticsStorage)

  lazy val geoHelperProvider: Provider[GeoHelper] =
    new GeoHelperProvider(
      () => regionGraph,
      () => regionDocumentsStatisticsStorage
    ).applySideEffect(_.update().get)

  val railwayStationsStorage = mock[RailwayStationsStorage]
  val station = RailwayStation.newBuilder().setEsr(196216L).setTitle("Красный Балтиец").build()
  when(railwayStationsStorage.getStation(?)).thenReturn(station)

  lazy val railwayStationsProvider: Provider[RailwayStationsStorage] = () => railwayStationsStorage

  lazy val regionServiceLayer: ULayer[RegionService] =
    ZLayer.succeed[RegionService.Service](
      new LiveRegionService(
        () => regionGraph,
        () => regionNameTraanslations,
        geoHelperProvider,
        railwayStationsProvider
      )
    )

  val filtersProvider: Provider[ListingFilters] = () => routerFilters

  private object RouterStub extends FrontendRouter.Service {
    override def parse(urlPath: String, viewType: ViewType): Task[Try[Request]] =
      Task.succeed(Failure(new NotImplementedError()))

    override def translate(requests: Iterable[RouterUrlRequest]): Task[Iterable[RouterUrlResponse]] =
      Task.succeed {
        requests.map { req =>
          RouterUrlResponse(
            req,
            Some(req.req.key)
          )
        }
      }
  }

  lazy val routerLayer: ULayer[FrontendRouter] = ZLayer.succeed(RouterStub)

  val Salarevo: Long = 375274L
  val VillagePresident: Long = 1808980L

  lazy val testPrometheus: ULayer[Prometheus] = ZLayer.succeed(new SimpleCompositeCollector)

  private def readOffer(name: String): UnifiedOffer =
    processResource(name)(is => OfferProtoConverter.fromMessage(OfferMessage.parseFrom(is))).extractNewModelObject()

  lazy val officeOffer: UnifiedOffer =
    readOffer("offer-office.proto.bytes")

  lazy val shortRentOffer: UnifiedOffer =
    readOffer("offer-short-rent.proto.bytes")

  lazy val VillagePresidentModel: Village =
    processResource("village_1808980.proto.bytes")(Village.parseFrom)

  lazy val oneRoomSellOffer: UnifiedOffer =
    readOffer("offer-one-room-sell-economy.proto.bytes")

  lazy val studioSellOffer: UnifiedOffer =
    readOffer("offer-studio-sell-economy.proto.bytes")

  private class ResourceDataLoader(path: String) extends DataLoader {
    override def load(): InputStream = getClass.getClassLoader.getResourceAsStream(path)
  }

  lazy val sitesService: SitesGroupingService = {
    val x = new SitesGroupingServiceImpl(FakeController)
    x.setLoader(new ResourceDataLoader("sites.data"))
    x.update().get
    x
  }

  lazy val companies: CompaniesStorage = {
    new CompaniesStorage(
      Seq(
        new Company(52308L)
          .applySideEffect(_.setName("Company 1"))
          .applySideEffect(_.setAllSites(List(Salarevo).map(Long.box).asJava))
      ).asJavaCollection
    )
  }

  def providerLayer[T: Tag](a: => T): ULayer[Has[Provider[T]]] =
    ZLayer.succeed(() => a)

  lazy val streetStorage: StreetStorage = {
    val storage = mock[StreetStorage]

    when(storage.getByAddress(?)).thenAnswer { (inv: InvocationOnMock) =>
      val addr = inv.getArgument[String](0)
      if (addr == "Россия, Москва, поселение Московский, Саларьевская улица") Some(Int.box(199508))
      else None
    }
    storage
  }

  lazy val siteInfoStorage: SiteInfoStorage =
    SiteInfoStorage(
      Map(
        Salarevo -> SiteInfoItem(
          Map(
            SiteRooms.OneRoom -> SiteRoomsStat(
              fromDeveloperStat = SiteOffersStat.create(
                1000000,
                10000000,
                10,
                100,
                100
              ),
              secondaryStat = None
            )
          ),
          isPaid = true,
          populatedRgid = Some(NodeRgid.MOSCOW_AND_MOS_OBLAST),
          kitchenSpaceMin = None,
          kitchenSpaceMax = None
        )
      )
    )

  object EmptyYocto extends IndexedDatabase {
    override def getFilter(fieldName: String): FilterableIndex = null

    override def execute(query: Query, processor: DocumentProcessor): Unit = ()

    override def executeAndUnlimitedCount(query: Query, processor: DocumentProcessor): Int = 0

    override def count(query: Query): Int = 0

    override def getDocumentCount: Int = 0

    override def getDocument(i: Int): Buffer = null

    override def getFieldValue(document: Int, fieldName: String): Buffer = null

    override def getSorter(fieldName: String): SortableIndex = null
  }

  lazy val EmptyBuildingStorage = new BuildingStorage(() => EmptyYocto)
}
