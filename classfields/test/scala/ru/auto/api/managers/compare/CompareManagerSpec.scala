package ru.auto.api.managers.compare

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.BreadcrumbsModel.Entity
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.extdata.DataService
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.decay.DecayManager
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.{CategorySelector, ModelGenerators, RequestParams}
import ru.auto.api.services.compare.CompareClient.CatalogCardInfo
import ru.auto.api.services.compare.DefaultCompareClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.catalog.model.api.ApiModel.{ConfigurationCard, RawCatalog, SuperGenCard, TechParamCard}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * Created by ndmelentev on 22.05.17.
  */
class CompareManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  trait mocks {
    val compareClient: DefaultCompareClient = mock[DefaultCompareClient]
    val offerLoader: OfferLoader = mock[OfferLoader]
    val offersCompareBuilder: OffersCompareBuilder = mock[OffersCompareBuilder]
    val modelsCompareBuilder: ModelsCompareBuilder = mock[ModelsCompareBuilder]
    val catalogManager: CatalogManager = mock[CatalogManager]
    val dataService: DataService = mock[DataService]
    val geoManager: GeoManager = mock[GeoManager]
    val decayManager: DecayManager = mock[DecayManager]

    val compareManager: CompareManager =
      new CompareManager(
        compareClient,
        offerLoader,
        offersCompareBuilder,
        modelsCompareBuilder,
        catalogManager,
        dataService,
        geoManager,
        decayManager
      )

    implicit val trace: Traced = Traced.empty

    implicit val request: Request = {
      val r = new RequestImpl
      r.setRequestParams(RequestParams.construct("1.1.1.1"))
      r.setTrace(trace)
      r.setUser(ModelGenerators.PersonalUserRefGen.next)
      r
    }
  }

  private val catalogResponse = {
    val catalog = RawCatalog.newBuilder()
    val techParam = List("3").map { tpId =>
      tpId -> TechParamCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setId(tpId))
        .setParentConfiguration("1")
        .build()
    }.toMap
    val configuration = List("1").map { confId =>
      confId -> ConfigurationCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setTechParamId(confId))
        .setParentSuperGen("1337")
        .build()
    }.toMap
    val superGen = List("1337").map { superGenId =>
      superGenId -> SuperGenCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setId(superGenId))
        .setParentModel("X1")
        .build()
    }.toMap
    catalog.putAllTechParam(techParam.asJava)
    catalog.putAllConfiguration(configuration.asJava)
    catalog.putAllSuperGen(superGen.asJava)
    catalog.build()
  }

  "CompareManager.getCatalogCards" should {
    "get catalog cards of user" in new mocks {
      val category = CategorySelector.Cars
      when(compareClient.getCatalogCards(?, ?)(?))
        .thenReturnF(Seq(CatalogCardInfo("1_2_3", 0, 0, None), CatalogCardInfo("4_5", 0, 0, None)))
      when(compareClient.moveCatalogCards(?, ?)(?))
        .thenReturn(Future.unit)

      val result = compareManager.getCatalogCards(category, request.user.personalRef)(request).futureValue
      result.getCatalogCardIds(0) shouldBe "1_2_3"
      result.getCatalogCardIds(1) shouldBe "4_5"
    }
  }

  "CompareManager.addCatalogCard" should {
    "post catalog card to users compare list" in new mocks {
      forAll(ModelGenerators.PersonalUserRefGen) { (user) =>
        val category = CategorySelector.Cars
        when(compareClient.addCatalogCard(?, ?, ?, ?)(?))
          .thenReturn(Future.unit)
        when(catalogManager.subtree(?, ?, ?, ?, ?)(?))
          .thenReturnF(catalogResponse)

        val result = compareManager.addCatalogCard(category, user, Seq("1_2_3")).futureValue
        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }
  }

  "CompareManager.upsert" should {
    "upsert catalog card to users compare list" in new mocks {
      forAll(ModelGenerators.PersonalUserRefGen) { user =>
        val category = CategorySelector.Cars
        when(compareClient.upsertCatalogCard(?, ?, ?, ?)(?))
          .thenReturn(Future.unit)
        when(catalogManager.subtree(?, ?, ?, ?, ?)(?))
          .thenReturnF(catalogResponse)

        val result = compareManager.upsertCatalogCard(category, user, Seq("1_2_3")).futureValue
        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }
  }

  "CompareManager.delete" should {
    "delete catalog card from users compare list" in new mocks {
      forAll(ModelGenerators.PersonalUserRefGen) { user =>
        val category = CategorySelector.Cars
        when(compareClient.deleteCatalogCard(?, ?, ?)(?))
          .thenReturn(Future.unit)

        val result = compareManager.deleteCatalogCard(category, user, Seq("1_2_3")).futureValue
        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }
  }

  "CompareManager.upsert" should {
    "validate catalog id parameter" in new mocks {
      forAll(ModelGenerators.PersonalUserRefGen) { user =>
        val category = CategorySelector.Cars
        when(compareClient.upsertCatalogCard(?, ?, ?, ?)(?))
          .thenReturn(Future.unit)
        when(catalogManager.subtree(?, ?, ?, ?, ?)(?))
          .thenReturnF(catalogResponse)

        val result = compareManager.upsertCatalogCard(category, user, Seq("1_2_3"))
        result.futureValue.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }
  }

  "CompareManager.upsert" should {
    "validate offer-id existence" in new mocks {
      forAll(ModelGenerators.PersonalUserRefGen) { user =>
        val rawOfferId = "favorite-1043045004-977b3"
        val category = CategorySelector.Cars
        when(compareClient.upsertCatalogCard(?, ?, ?, ?)(?))
          .thenReturn(Future.unit)
        when(catalogManager.subtree(?, ?, ?, ?, ?)(?))
          .thenReturnF(catalogResponse)
        when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException("offerNotFound"))
        val result = compareManager.upsertCatalogCard(category, user, Seq(rawOfferId))
        result.failed.futureValue shouldBe a[RuntimeException]
      }
    }
  }

}
