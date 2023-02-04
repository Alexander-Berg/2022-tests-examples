package ru.auto.api.managers.auction

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.auction.CallAuction.CallAuctionRequestContext
import ru.auto.api.exceptions.CallAuctionException
import ru.auto.api.extdata.DataService
import ru.auto.api.geo.Tree
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.model.ModelGenerators.RegionGen
import ru.auto.api.model.{AutoruDealer, CategorySelector, RequestParams}
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel.{MarkCard, ModelCard, RawCatalog}
import ru.yandex.vertis.mockito.MockitoSupport
import vsmoney.auction.CommonModel.{AuctionContext, CriteriaValue}

import scala.jdk.CollectionConverters._

class AuctionProtoConverterSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  val vosClientMock: VosClient = mock[VosClient]
  val treeMock: Tree = mock[Tree]
  val dataServiceMock: DataService = mock[DataService]
  val catalogManagerMock: CatalogManager = mock[CatalogManager]

  val auctionProtoConverter = new AuctionProtoConverterImpl(
    vosClient = vosClientMock,
    dataService = dataServiceMock,
    catalogManager = catalogManagerMock
  )

  private val DefaultUser = AutoruDealer(123)

  implicit private val defaultRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setUser(DefaultUser)
    r
  }

  "AuctionProtoConverter" should {
    "success convert front auction context to back auction context" in {

      val request = CallAuctionRequestContext
        .newBuilder()
        .setMarkCode("BMW")
        .setModelCode("X6")
        .build()

      val criteriaValues = List(
        CriteriaValue.newBuilder().setKey(CallAuctionManager.RegionCriteriaName).setValue("42").build(),
        CriteriaValue.newBuilder().setKey(CallAuctionManager.MarkCriteriaName).setValue("BMW").build(),
        CriteriaValue.newBuilder().setKey(CallAuctionManager.ModelCriteriaName).setValue("X6").build()
      )

      val criteriaContext = AuctionContext.CriteriaContext
        .newBuilder()
        .addAllCriteriaValues(criteriaValues.asJava)
        .build()

      val response = AuctionContext
        .newBuilder()
        .setCriteriaContext(criteriaContext)
        .build()

      when(vosClientMock.getDealerRegionId(eq(DefaultUser))(?)).thenReturnF(42L)
      when(dataServiceMock.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(42L)).thenReturn(RegionGen.next.copy(id = 42L))
      val catalogFilter = CatalogFilter.newBuilder().setMark("BMW").setModel("X6").build()

      val markEntity = ru.auto.api.BreadcrumbsModel.Entity.newBuilder().setId("BMW").build()
      val modelEntity = ru.auto.api.BreadcrumbsModel.Entity.newBuilder().setId("X6").build()
      val markCard = MarkCard
        .newBuilder()
        .setEntity(markEntity)
        .putAllModel(
          Map("X6" -> ModelCard.newBuilder().setEntity(modelEntity).build()).asJava
        )
        .build()

      when(
        catalogManagerMock
          .exactByCatalogFilter(CategorySelector.Cars, None, Seq(catalogFilter), failNever = false, legacyMode = true)
      ).thenReturnF(
        RawCatalog
          .newBuilder()
          .putAllMark(Map("BMW" -> markCard).asJava)
          .build()
      )
      val result = auctionProtoConverter.frontToBackContext(request, DefaultUser.asDealer).await
      result shouldBe response
    }
    "generate failre if mark model not found in catalog" in {
      val request = CallAuctionRequestContext
        .newBuilder()
        .setMarkCode("BMW")
        .setModelCode("X6")
        .build()

      when(vosClientMock.getDealerRegionId(eq(DefaultUser))(?)).thenReturnF(42L)
      when(dataServiceMock.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(42L)).thenReturn(RegionGen.next.copy(id = 42L))
      val catalogFilter = CatalogFilter.newBuilder().setMark("BMW").setModel("X6").build()

      when(
        catalogManagerMock
          .exactByCatalogFilter(CategorySelector.Cars, None, Seq(catalogFilter), failNever = false, legacyMode = true)
      ).thenReturnF(
        RawCatalog
          .newBuilder()
          .build()
      )
      val resultError = auctionProtoConverter.frontToBackContext(request, DefaultUser.asDealer).failed.futureValue
      resultError shouldBe an[CallAuctionException]
    }
    "schould fill auction context" in {
      val region = "42"
      val mark = "BMW"
      val model = "T2"

      val criteriaValues = List(
        CriteriaValue.newBuilder().setKey(CallAuctionManager.RegionCriteriaName).setValue(region).build(),
        CriteriaValue.newBuilder().setKey(CallAuctionManager.MarkCriteriaName).setValue(mark).build(),
        CriteriaValue.newBuilder().setKey(CallAuctionManager.ModelCriteriaName).setValue(model).build()
      )

      val criteriaContext = AuctionContext.CriteriaContext
        .newBuilder()
        .addAllCriteriaValues(criteriaValues.asJava)
        .build()

      val response = AuctionContext
        .newBuilder()
        .setCriteriaContext(criteriaContext)
        .build()

      auctionProtoConverter.fillAuctionContext(region, mark, model) shouldBe response
    }
  }
}
