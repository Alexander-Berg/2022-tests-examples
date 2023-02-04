package ru.auto.api.managers.product

import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Multiposting.Classified
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel.{Multiposting, OfferStatus}
import ru.auto.api.BaseSpec
import ru.auto.api.RequestModel.ApplyAutoruProductsRequest
import ru.auto.api.auth.Application
import ru.auto.api.exceptions._
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.offers.OffersManager
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.CategorySelector.{All, Cars}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.gen.DeprecatedBillingModelGenerators.SalesmanTicketIdGen
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.model.salesman.{Prolongable, SaleCategory, TransactionId}
import ru.auto.api.model.{AutoruProduct, RequestParams}
import ru.auto.api.services.billing.AutoruProductActions.{Activate, Deactivate}
import ru.auto.api.services.billing.CabinetClient
import ru.auto.api.services.billing.util.SalesmanTicketId
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.multiposting.MultipostingClient
import ru.auto.api.services.salesman.SalesmanClient.{Good, GoodsRequest}
import ru.auto.api.services.salesman.{SalesmanClient, SalesmanUserClient}
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{ManagerUtils, Request, RequestImpl}
import ru.auto.salesman.SalesmanModel
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class ProductManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {

  private val vosClient = mock[VosClient]
  private val multipostingClient = mock[MultipostingClient]
  // по дефолту везде включено добавление/удаление продукта через мультипостинг, кроме когда в тесте есть префикс [VOS]
  private val featureManager = mock[FeatureManager]
  private val allowProxyAddDeleteProductThroughMultiposting = mock[Feature[Boolean]]
  private val regionsWithDisabledTurboFeature = mock[Feature[List[Long]]]
  private val manageGoodsInSalesmanFeature = mock[Feature[Boolean]]
  private val cabinetClient = mock[CabinetClient]
  private val cabinetApiClient = mock[CabinetApiClient]
  private val salesmanClient = mock[SalesmanClient]
  private val salesmanUserClient = mock[SalesmanUserClient]
  private val offersManager = mock[OffersManager]
  private val tree = mock[Tree]

  private val regionIdWithDisabledTurbo = 3228L
  private val regionWithDisabledTurboActivation = RegionGen(idGen = regionIdWithDisabledTurbo).next
  private val regionWithEnabledTurboActivation = RegionGen.suchThat(_.id != regionIdWithDisabledTurbo).next

  before {
    // чтобы не писать when(salesmanClient.getGoods) во всех тестах внутри forAll(), ресетим его отдельно
    reset(salesmanClient)
    // Аналогично причине выше, но для tree.unsafeFederalSubject
    reset(tree)
    resetAll()
    when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(Nil)
    when(tree.unsafeFederalSubject(?))
      .thenReturn(regionWithEnabledTurboActivation)
    when(allowProxyAddDeleteProductThroughMultiposting.value).thenReturn(true)
    when(manageGoodsInSalesmanFeature.value).thenReturn(false)
    when(featureManager.allowProxyAddDeleteProductThroughMultiposting)
      .thenReturn(allowProxyAddDeleteProductThroughMultiposting)
    when(regionsWithDisabledTurboFeature.value)
      .thenReturn(List(regionIdWithDisabledTurbo))
    when(featureManager.regionsWithDealerDisabledTurbo)
      .thenReturn(regionsWithDisabledTurboFeature)
    when(featureManager.manageGoodsInSalesman)
      .thenReturn(manageGoodsInSalesmanFeature)
  }

  private val productManager =
    new ProductManager(
      vosClient,
      cabinetClient,
      cabinetApiClient,
      salesmanClient,
      salesmanUserClient,
      multipostingClient,
      offersManager,
      featureManager,
      tree
    )

  private def verifyNoMoreInteractionsAndReset(): Unit = {
    verifyNoMoreInteractions(vosClient, cabinetClient, salesmanUserClient, offersManager)
    resetAll()
  }

  private def resetAll(): Unit = {
    reset[AnyRef](vosClient, cabinetClient, salesmanUserClient, offersManager)
  }

  implicit private val trace: Traced = Traced.empty

  implicit private val r: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.desktop)
    r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
    r
  }

  private val deletableProductGen = Gen.oneOf(Premium, SpecialOffer, Badge)
  private val deletableProductExcludingBadgeGen = Gen.oneOf(Premium, SpecialOffer)

  "ProductManager.applyProducts()" should {

    "return success on empty request" in {
      forAll(SelectorGen, RegisteredUserRefGen, OfferIDGen) { (category, user, offerId) =>
        val request = ApplyAutoruProductsRequest.getDefaultInstance
        val response = productManager.applyProducts(category, user, offerId, request, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success on request with one product and disabled multiposting" in {
      forAll(StrictCategoryGen, DealerUserRefGen, OfferGen, ApplyOneNonBadgeAutoruProductRequestGen) {
        (category, user, offer, request) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
            .thenReturnF(offer)
          val response =
            productManager.applyProducts(category, user, offer.id, request, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offer.id, category.enum, request.getProducts(0), Activate)
          if (request.getProducts(0).getCode == PackageTurbo.salesName && category == Cars) {
            verify(vosClient).getUserOffer(
              category,
              user,
              offer.id,
              includeRemoved = false,
              forceTeleponyInfo = false,
              executeOnMaster = false
            )
          }

          verifyNoMoreInteractionsAndReset()
      }
    }

    "return error on request with Turbo and disabled multiposting in region with disabled turbo for CARS USED" in {
      val productGen = applyAutoruProductsRequestGen(autoruProductGen(PackageTurbo).map(List(_)))
      reset(tree)
      when(tree.unsafeFederalSubject(?))
        .thenReturn(regionWithDisabledTurboActivation)
      forAll(
        DealerUserRefGen,
        DealerCarsUsedOfferGen,
        productGen
      ) { (user, offer, request) =>
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
          .thenReturnF(offer)
        val response =
          productManager.applyProducts(Cars, user, offer.id, request, ClassifiedName.AUTORU).failed.futureValue
        response shouldBe a[BadRequestDetailedException]
        response.getMessage shouldBe s"Tubro package is disabled for region region $regionIdWithDisabledTurbo"
        verify(vosClient).getUserOffer(
          Cars,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return error on request with one product and enabled multiposting but disabled autoru classified" in {
      forAll(StrictCategoryGen, DealerUserRefGen, OfferGen, ApplyOneNonBadgeAutoruProductRequestGen) {
        (category, user, offer, request) =>
          offer.toBuilder.getMultipostingBuilder.getClassifiedsBuilderList.asScala.collect {
            case c if c.getName == ClassifiedName.AUTORU => c.setEnabled(false).build()
          }
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
          val response =
            productManager.applyProducts(category, user, offer.id, request, ClassifiedName.AUTORU).failed.futureValue
          response shouldBe a[BadRequestDetailedException]
          verify(vosClient).getUserOffer(
            category,
            user,
            offer.id,
            includeRemoved = false,
            forceTeleponyInfo = false,
            executeOnMaster = false
          )
          verifyNoMoreInteractionsAndReset()
      }
    }

    "MULTIPOSTING return success on request with one product and enabled multiposting and enabled autoru classified" in {
      forAll(
        StrictCategoryGen,
        DealerUserRefGen,
        OfferGen.suchThat(_.isDealer),
        ApplyOneNonBadgeAutoruProductRequestGen
      ) { (category, user, offer, request) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(ClassifiedName.AUTORU).setEnabled(true).build())
          )
          .build()
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(vosClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.applyProducts(category, user, offer.id, request, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offer.id, category.enum, request.getProducts(0), Activate)
        verify(multipostingClient).addMultipostingServices(
          category,
          user,
          offer.id,
          withClassified.getMultiposting.getClassifiedsList.asScala.head,
          Set(request.getProducts(0).getCode)
        )
        verify(vosClient).getUserOffer(
          category,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "[VOS] return success on request with one product and enabled multiposting and enabled autoru classified" in {
      forAll(
        StrictCategoryGen,
        DealerUserRefGen,
        OfferGen.suchThat(_.isDealer),
        ApplyOneNonBadgeAutoruProductRequestGen
      ) { (category, user, offer, request) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(ClassifiedName.AUTORU).setEnabled(true).build())
          )
          .build()
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(vosClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(allowProxyAddDeleteProductThroughMultiposting.value).thenReturn(false)
        when(featureManager.allowProxyAddDeleteProductThroughMultiposting)
          .thenReturn(allowProxyAddDeleteProductThroughMultiposting)
        val response =
          productManager.applyProducts(category, user, offer.id, request, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offer.id, category.enum, request.getProducts(0), Activate)
        verify(vosClient).addMultipostingServices(
          category,
          user,
          offer.id,
          ClassifiedName.AUTORU,
          Set(request.getProducts(0).getCode)
        )
        verify(vosClient).getUserOffer(
          category,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success on request with one product and enabled multiposting and enabled autoru classified for TURBO" +
      "in region with disabled turbo for CARS NEW" in {
      val productGen = applyAutoruProductsRequestGen(Gen.listOfN(1, autoruProductGen(PackageTurbo)))
      reset(tree)
      when(tree.unsafeFederalSubject(?))
        .thenReturn(regionWithDisabledTurboActivation)
      forAll(
        DealerUserRefGen,
        DealerCarsNewOfferGen,
        productGen
      ) { (user, offer, request) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(ClassifiedName.AUTORU).setEnabled(true).build())
          )
          .build()
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(vosClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(allowProxyAddDeleteProductThroughMultiposting.value).thenReturn(false)
        when(featureManager.allowProxyAddDeleteProductThroughMultiposting)
          .thenReturn(allowProxyAddDeleteProductThroughMultiposting)
        val response =
          productManager.applyProducts(Cars, user, offer.id, request, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offer.id, Cars.enum, request.getProducts(0), Activate)
        verify(vosClient).addMultipostingServices(
          Cars,
          user,
          offer.id,
          ClassifiedName.AUTORU,
          Set(request.getProducts(0).getCode)
        )
        verify(vosClient).getUserOffer(
          Cars,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return error on request with one product and enabled multiposting and enabled autoru classified " +
      "for region with disabled TURBO for CARS USED" in {
      val productGen = applyAutoruProductsRequestGen(Gen.listOfN(1, autoruProductGen(PackageTurbo)))
      reset(tree)
      when(tree.unsafeFederalSubject(?))
        .thenReturn(regionWithDisabledTurboActivation)
      forAll(
        DealerUserRefGen,
        DealerCarsUsedOfferGen,
        productGen
      ) { (user, offer, request) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(ClassifiedName.AUTORU).setEnabled(true).build())
          )
          .build()
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(allowProxyAddDeleteProductThroughMultiposting.value).thenReturn(false)
        when(featureManager.allowProxyAddDeleteProductThroughMultiposting)
          .thenReturn(allowProxyAddDeleteProductThroughMultiposting)
        val response =
          productManager.applyProducts(Cars, user, offer.id, request, ClassifiedName.AUTORU).failed.futureValue
        response shouldBe a[BadRequestDetailedException]
        response.getMessage shouldBe s"Tubro package is disabled for region region $regionIdWithDisabledTurbo"

        verify(vosClient).getUserOffer(
          Cars,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )

        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on request with multiple products" in {
      forAll(SelectorGen, RegisteredUserRefGen, OfferIDGen, ApplyMultipleAutoruProductsRequestGen) {
        (category, user, offerId, request) =>
          val ex =
            productManager.applyProducts(category, user, offerId, request, ClassifiedName.AUTORU).failed.futureValue
          ex shouldBe an[IllegalArgumentException]
          verifyNoMoreInteractionsAndReset()
      }
    }
  }

  "ProductManager.applyProduct()" should {

    "return success if product application succeed on category=all" in {
      forAll(DealerUserRefGen, OfferGen, AutoruNonBadgeProductGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        val response = productManager.applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(cabinetClient).postProduct(offer.id, offer.getCategory, product, Activate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if product application succeed on strict category" in {
      forAll(DealerUserRefGen, OfferGen, StrictCategoryGen, AutoruNonBadgeProductGen) {
        (user, offer, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
            .thenReturnF(offer)
          val response =
            productManager.applyProduct(category, user, offer.id, product, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offer.id, category.enum, product, Activate)
          if (product.getCode == PackageTurbo.salesName && category == Cars) {
            verify(vosClient).getUserOffer(
              category,
              user,
              offer.id,
              includeRemoved = false,
              forceTeleponyInfo = false,
              executeOnMaster = false
            )
          }
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success and don't request for existing goods for non-related to turbo or fresh product" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, AutoruBadgeProductGen) {
        (user, offerId, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
          val response =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offerId, category.enum, product, Activate)
          verifyNoMoreInteractions(salesmanClient)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if requested premium, special or turbo application and no active turbo exist on category=all" in {
      val productGen = autoruProductGen(Gen.oneOf(Premium, SpecialOffer, PackageTurbo))
      forAll(DealerUserRefGen, OfferGen, productGen) { (user, offer, product) =>
        reset(salesmanClient)
        when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(Placement)))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        val response = productManager.applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(cabinetClient).postProduct(offer.id, offer.getCategory, product, Activate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    //noinspection ScalaStyle
    "return success if requested premium, special or turbo application and no active turbo exist on strict category" in {
      reset(salesmanClient)
      when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(Placement)))
      val productGen = autoruProductGen(Gen.oneOf(Premium, SpecialOffer, PackageTurbo))
      forAll(DealerUserRefGen, OfferGen, StrictCategoryGen, productGen) { (user, offer, category, product) =>
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
          .thenReturnF(offer)
        val response = productManager.applyProduct(category, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offer.id, category.enum, product, Activate)
        if (product.getCode == PackageTurbo.salesName && category == Cars) {
          verify(vosClient).getUserOffer(
            category,
            user,
            offer.id,
            includeRemoved = false,
            forceTeleponyInfo = false,
            executeOnMaster = false
          )
        }
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if requested premium, special or turbo application and active turbo exists" in {
      reset(salesmanClient)
      when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(PackageTurbo)))
      val productGen = autoruProductGen(Gen.oneOf(Premium, SpecialOffer, PackageTurbo))
      forAll(DealerUserRefGen, OfferGen, StrictCategoryGen, productGen) { (user, offer, category, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
          .thenReturnF(offer)
        val e = productManager.applyProduct(category, user, offer.id, product, ClassifiedName.AUTORU).failed.futureValue
        e shouldBe a[StatusConflict]
        if (product.getCode == PackageTurbo.salesName && category == Cars) {
          verify(vosClient).getUserOffer(
            category,
            user,
            offer.id,
            includeRemoved = false,
            forceTeleponyInfo = false,
            executeOnMaster = false
          )
        }
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if requested fresh activation" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, autoruProductGen(Boost)) {
        (user, offerId, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          val response =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offerId, category.enum, product, Activate)
          verifyNoMoreInteractions(salesmanClient)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if requested reset activation" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, autoruProductGen(Reset)) {
        (user, offerId, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          val response =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offerId, category.enum, product, Activate)
          verifyNoMoreInteractions(salesmanClient)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if badge application succeed on category=all" in {
      forAll(DealerUserRefGen, OfferGen, AutoruBadgeProductGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        val response = productManager.applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(cabinetClient).postProduct(offer.id, offer.getCategory, product, Activate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if badge application succeed on strict category" in {
      forAll(DealerUserRefGen, StrictCategoryGen, OfferIDGen, AutoruBadgeProductGen) {
        (user, category, offerId, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
          val response =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offerId, category.enum, product, Activate)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if product activated on non-autoru classifieds" in {
      forAll(
        DealerUserRefGen,
        OfferGen.suchThat(_.isDealer),
        AutoruNonBadgeProductGen,
        Gen.asciiPrintableStr,
        NonAutoruClassifiedNameGen
      ) { (user, offer, productTemplate, productCode, classified) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(classified).setEnabled(true).build())
          )
          .build()
        val product = productTemplate.toBuilder.setCode(productCode).build()
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(vosClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.addMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        val response = productManager.applyProduct(All, user, offer.id, product, classified).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(multipostingClient).addMultipostingServices(
          All,
          user,
          offer.id,
          withClassified.getMultiposting.getClassifiedsList.asScala.head,
          Set(product.getCode)
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "don't activate product on disabled non-autoru classified" in {
      forAll(
        DealerUserRefGen,
        OfferGen,
        AutoruNonBadgeProductGen,
        Gen.asciiPrintableStr,
        NonAutoruClassifiedNameGen
      ) { (user, offer, productTemplate, productCode, classified) =>
        val withClassified = offer.toBuilder
          .setMultiposting(
            Multiposting
              .newBuilder()
              .setStatus(OfferStatus.ACTIVE)
              .addClassifieds(Classified.newBuilder().setName(classified).setEnabled(false).build())
          )
          .build()
        val product = productTemplate.toBuilder.setCode(productCode).build()
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(withClassified)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        val response = productManager.applyProduct(All, user, offer.id, product, classified).failed.futureValue
        response shouldBe a[BadRequestDetailedException]
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "don't activate product on non-autoru classified when multiposting is disabled" in {
      forAll(
        DealerUserRefGen,
        OfferGen,
        AutoruNonBadgeProductGen,
        Gen.asciiPrintableStr,
        NonAutoruClassifiedNameGen
      ) { (user, offer, productTemplate, productCode, classified) =>
        val product = productTemplate.toBuilder.setCode(productCode).build()
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        val response = productManager.applyProduct(All, user, offer.id, product, classified).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if search for offer failed" in {
      forAll(DealerUserRefGen, OfferIDGen, AutoruProductGen) { (user, offerId, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
        val ex = productManager.applyProduct(All, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[RuntimeException]
        verify(vosClient).getUserOffer(
          All,
          user,
          offerId,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if product application failed on category=all" in {
      forAll(DealerUserRefGen, OfferGen, AutoruNonBadgeProductGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
        productManager
          .applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU)
          .failed
          .futureValue shouldBe a[RuntimeException]
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(cabinetClient).postProduct(offer.id, offer.category.enum, product, Activate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if product application failed on strict category" in {
      forAll(DealerUserRefGen, OfferGen, StrictCategoryGen, AutoruNonBadgeProductGen) {
        (user, offer, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
          when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?))
            .thenReturnF(offer)
          productManager
            .applyProduct(category, user, offer.id, product, ClassifiedName.AUTORU)
            .failed
            .futureValue shouldBe a[RuntimeException]
          verify(cabinetClient).postProduct(offer.id, category.enum, product, Activate)
          if (product.getCode == PackageTurbo.salesName && category == Cars) {
            verify(vosClient).getUserOffer(
              category,
              user,
              offer.id,
              includeRemoved = false,
              forceTeleponyInfo = false,
              executeOnMaster = false
            )
          }
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply product to user's offer" in {
      forAll(SelectorGen, PrivateUserRefGen, OfferIDGen, AutoruProductGen) { (category, user, offerId, product) =>
        val ex = productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[NeedAuthentication]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply invalid product" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, InvalidProductGen) { (category, user, offerId, product) =>
        val ex = productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply non-badge product with badge" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, InvalidAutoruProductWithBadgeGen) {
        (category, user, offerId, product) =>
          val ex =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
          ex shouldBe a[IllegalArgumentException]
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply badge product without badge" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen) { (category, user, offerId) =>
        val product = SalesmanModel.AutoruProduct.newBuilder().setCode("all_sale_badge").build()
        val ex = productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply badge product with empty badge" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, AutoruBadgeProductGen) { (category, user, offerId, product) =>
        val withEmptyBadge = product.toBuilder.setBadges(0, "").build()
        val ex =
          productManager.applyProduct(category, user, offerId, withEmptyBadge, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply badge product with space badge" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, AutoruBadgeProductGen) { (category, user, offerId, product) =>
        val withSpaceBadge = product.toBuilder.setBadges(0, " ").build()
        val ex =
          productManager.applyProduct(category, user, offerId, withSpaceBadge, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to apply badge product with more than 3 badges" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, AutoruTooManyBadgesProductGen) {
        (category, user, offerId, product) =>
          val ex =
            productManager.applyProduct(category, user, offerId, product, ClassifiedName.AUTORU).failed.futureValue
          ex shouldBe a[IllegalArgumentException]
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if badge application succeeded on salesman" in {
      forAll(DealerUserRefGen, AnyCategoryOfferGen, AutoruBadgeProductGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        when(salesmanClient.createGoods(?, ?, ?)(?)).thenReturnF(())
        when(featureManager.manageGoodsInSalesman).thenReturn(new Feature[Boolean] {
          override def name: String = ""

          override def value: Boolean = true
        })
        val response = productManager.applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "check that badges are in request" in {
      forAll(DealerUserRefGen, CarsOfferGen, AutoruBadgeProductGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(false)
        when(salesmanClient.createGoods(?, ?, ?)(?)).thenReturnF(())
        when(featureManager.manageGoodsInSalesman).thenReturn(new Feature[Boolean] {
          override def name: String = ""

          override def value: Boolean = true
        })
        val response = productManager.applyProduct(All, user, offer.id, product, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(salesmanClient).createGoods(
          user.asDealer.clientId,
          GoodsRequest(
            offerId = offer.getId,
            category = SaleCategory.Cars,
            section = offer.getSection,
            product = AutoruProduct.Badge,
            badges = Some(product.getBadgesList.asScala.toSeq)
          ),
          withMoneyCheck = true
        )

        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }
  }

  "ProductManager.deleteProduct()" should {

    "return success if product deletion succeed on category=all" in {
      val user = DealerUserRefGen.next
      val offer = OfferGen.next
      val product = deletableProductExcludingBadgeGen.next
      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
      when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
      when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
      when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
      val response =
        productManager.deleteProduct(All, user, offer.id, product.salesName, ClassifiedName.AUTORU).futureValue
      response shouldBe ManagerUtils.SuccessResponse
      verify(vosClient).getUserOffer(
        All,
        user,
        offer.id,
        includeRemoved = false,
        forceTeleponyInfo = false,
        executeOnMaster = false
      )
      verify(multipostingClient).removeMultipostingServices(
        All,
        user,
        offer.id,
        ClassifiedName.AUTORU,
        Set(product.name)
      )
      verify(cabinetClient).postProduct(offer.id, offer.getCategory, toProto(product), Deactivate)
      verifyNoMoreInteractionsAndReset()
    }

    "[VOS] return success if product deletion succeed on category=all" in {
      val user = DealerUserRefGen.next
      val offer = OfferGen.next
      val product = deletableProductExcludingBadgeGen.next
      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
      when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
      when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
      when(allowProxyAddDeleteProductThroughMultiposting.value).thenReturn(false)
      when(featureManager.allowProxyAddDeleteProductThroughMultiposting)
        .thenReturn(allowProxyAddDeleteProductThroughMultiposting)
      val response =
        productManager.deleteProduct(All, user, offer.id, product.salesName, ClassifiedName.AUTORU).futureValue
      response shouldBe ManagerUtils.SuccessResponse
      verify(vosClient).getUserOffer(
        All,
        user,
        offer.id,
        includeRemoved = false,
        forceTeleponyInfo = false,
        executeOnMaster = false
      )
      verify(vosClient).removeMultipostingServices(
        All,
        user,
        offer.id,
        ClassifiedName.AUTORU,
        Set(product.name)
      )
      verify(cabinetClient).postProduct(offer.id, offer.getCategory, toProto(product), Deactivate)
      verifyNoMoreInteractionsAndReset()
    }

    "return success if product deletion succeed on strict category" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, deletableProductExcludingBadgeGen) {
        (user, offerId, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
          when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
          when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
          when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
          val response =
            productManager.deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU).futureValue
          response shouldBe ManagerUtils.SuccessResponse
          verify(cabinetClient).postProduct(offerId, category.enum, toProto(product), Deactivate)
          verify(multipostingClient).removeMultipostingServices(
            category,
            user,
            offerId,
            ClassifiedName.AUTORU,
            Set(product.name)
          )
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if badge deletion succeed on category=all" in {
      forAll(DealerUserRefGen, OfferGen) { (user, offer) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(All, user, offer.id, Badge.salesName, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(multipostingClient).removeMultipostingServices(
          All,
          user,
          offer.id,
          ClassifiedName.AUTORU,
          Set(Badge.name)
        )
        verify(cabinetClient).postProduct(offer.id, offer.getCategory, toProto(Badge), Deactivate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if badge deletion succeed on strict category" in {
      forAll(DealerUserRefGen, StrictCategoryGen, OfferIDGen) { (user, category, offerId) =>
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(category, user, offerId, Badge.salesName, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(multipostingClient).removeMultipostingServices(
          category,
          user,
          offerId,
          ClassifiedName.AUTORU,
          Set(Badge.name)
        )
        verify(cabinetClient).postProduct(offerId, category.enum, toProto(Badge), Deactivate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success and don't request for existing goods for non-related to turbo or fresh product" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen) { (user, offerId, category) =>
        val product = Badge
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offerId, category.enum, toProto(product), Deactivate)
        verify(multipostingClient).removeMultipostingServices(
          category,
          user,
          offerId,
          ClassifiedName.AUTORU,
          Set(product.name)
        )
        verifyNoMoreInteractions(salesmanClient)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if requested premium, special or turbo deactivation and no turbo activated on category=all" in {
      val productGen = Gen.oneOf(Premium, SpecialOffer, PackageTurbo)
      forAll(DealerUserRefGen, OfferGen, productGen) { (user, offer, product) =>
        reset(salesmanClient)
        when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(Placement)))
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(All, user, offer.id, product.salesName, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(multipostingClient).removeMultipostingServices(
          All,
          user,
          offer.id,
          ClassifiedName.AUTORU,
          Set(product.name)
        )
        verify(cabinetClient).postProduct(offer.id, offer.getCategory, toProto(product), Deactivate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if requested premium, special or turbo deactivation and no turbo activated on strict category" in {
      reset(salesmanClient)
      when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(Placement)))
      val productGen = Gen.oneOf(Premium, SpecialOffer, PackageTurbo)
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, productGen) { (user, offerId, category, product) =>
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(cabinetApiClient.isMultipostingEnabled(?)(?)).thenReturnF(true)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(cabinetClient).postProduct(offerId, category.enum, toProto(product), Deactivate)
        verify(multipostingClient).removeMultipostingServices(
          category,
          user,
          offerId,
          ClassifiedName.AUTORU,
          Set(product.name)
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return success if product deletion succeed for non-autoru classified" in {
      forAll(
        DealerUserRefGen,
        OfferIDGen,
        StrictCategoryGen,
        Gen.asciiPrintableStr,
        NonAutoruClassifiedNameGen
      ) { (user, offerId, category, product, classified) =>
        when(vosClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        when(multipostingClient.removeMultipostingServices(?, ?, ?, ?, ?)(?)).thenReturnF(())
        val response =
          productManager.deleteProduct(category, user, offerId, product, classified).futureValue
        response shouldBe ManagerUtils.SuccessResponse
        verify(multipostingClient).removeMultipostingServices(
          category,
          user,
          offerId,
          classified,
          Set(product)
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if requested premium, special or turbo deactivation and turbo activated" in {
      reset(salesmanClient)
      when(salesmanClient.getGoods(?, ?)(?)).thenReturnF(List(Good(PackageTurbo)))
      val productGen = Gen.oneOf(Premium, SpecialOffer, PackageTurbo)
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, productGen) { (user, offerId, category, product) =>
        val e =
          productManager
            .deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU)
            .failed
            .futureValue
        e shouldBe a[StatusConflict]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if search for offer failed" in {
      forAll(DealerUserRefGen, OfferIDGen, deletableProductGen) { (user, offerId, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
        val ex =
          productManager.deleteProduct(All, user, offerId, product.salesName, ClassifiedName.AUTORU).failed.futureValue
        ex shouldBe a[RuntimeException]
        verify(vosClient).getUserOffer(
          All,
          user,
          offerId,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if product deletion failed on category=all" in {
      forAll(DealerUserRefGen, OfferGen, deletableProductExcludingBadgeGen) { (user, offer, product) =>
        when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
        when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
        productManager
          .deleteProduct(All, user, offer.id, product.salesName, ClassifiedName.AUTORU)
          .failed
          .futureValue shouldBe a[RuntimeException]
        verify(vosClient).getUserOffer(
          All,
          user,
          offer.id,
          includeRemoved = false,
          forceTeleponyInfo = false,
          executeOnMaster = false
        )
        verify(cabinetClient).postProduct(offer.id, offer.category.enum, toProto(product), Deactivate)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if product application failed on strict category" in {
      forAll(DealerUserRefGen, OfferIDGen, StrictCategoryGen, deletableProductExcludingBadgeGen) {
        (user, offerId, category, product) =>
          when(cabinetClient.postProduct(?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
          productManager
            .deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU)
            .failed
            .futureValue shouldBe a[RuntimeException]
          verify(cabinetClient).postProduct(offerId, category.enum, toProto(product), Deactivate)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to delete product from user's offer" in {
      forAll(SelectorGen, PrivateUserRefGen, OfferIDGen, deletableProductGen) { (category, user, offerId, product) =>
        val ex =
          productManager
            .deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU)
            .failed
            .futureValue
        ex shouldBe a[NeedAuthentication]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to delete non-autoru product" in {
      forAll(SelectorGen, DealerUserRefGen, OfferIDGen, NonAutoruVasProductGen) { (category, user, offerId, product) =>
        val ex =
          productManager
            .deleteProduct(category, user, offerId, product.salesName, ClassifiedName.AUTORU)
            .failed
            .futureValue
        ex shouldBe a[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on attempt to delete boost" in {
      forAll(DealerUserRefGen, OfferIDGen, SelectorGen) { (user, offerId, category) =>
        val ex =
          productManager
            .deleteProduct(category, user, offerId, Boost.salesName, ClassifiedName.AUTORU)
            .failed
            .futureValue
        ex shouldBe an[IllegalArgumentException]
        verifyNoMoreInteractionsAndReset()
      }
    }
  }

  "ProductManager.setProlongable(product)" should {

    "return success when put prolongable" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, Prolongable(true), privateRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          when(offersManager.checkOwnership(?)(?)).thenReturnF(())
          when(salesmanUserClient.putProlongable(?, ?, ?)(?)).thenReturnF(())
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).futureValue
          result shouldBe ManagerUtils.SuccessResponse
          verify(offersManager).checkOwnership(List(offerId))(request)
          verify(salesmanUserClient).putProlongable(domain, offerId, product)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success when delete prolongable" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, Prolongable(false), privateRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          when(offersManager.checkOwnership(?)(?)).thenReturnF(())
          when(salesmanUserClient.deleteProlongable(?, ?, ?)(?)).thenReturnF(())
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).futureValue
          result shouldBe ManagerUtils.SuccessResponse
          verify(offersManager).checkOwnership(List(offerId))(request)
          verify(salesmanUserClient).deleteProlongable(domain, offerId, product)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure for dealer" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, ProlongableGen, dealerRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).failed.futureValue
          result shouldBe a[NeedAuthentication]
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if current user doesn't own offer" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, ProlongableGen, privateRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          when(offersManager.checkOwnership(?)(?)).thenThrowF(new OfferNotFoundException)
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).failed.futureValue
          result shouldBe an[OfferNotFoundException]
          verify(offersManager).checkOwnership(List(offerId))(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if salesman fails while putting prolongable" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, Prolongable(true), privateRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          when(offersManager.checkOwnership(?)(?)).thenReturnF(())
          when(salesmanUserClient.putProlongable(?, ?, ?)(?)).thenThrowF(new Exception)
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).failed.futureValue
          result shouldBe an[Exception]
          verify(offersManager).checkOwnership(List(offerId))(request)
          verify(salesmanUserClient).putProlongable(domain, offerId, product)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if salesman fails while deleting prolongable" in {
      forAll(SalesmanDomainGen, OfferIDGen, ProductGen, Prolongable(false), privateRequestGen) {
        (domain, offerId, product, prolongable, request) =>
          when(offersManager.checkOwnership(?)(?)).thenReturnF(())
          when(salesmanUserClient.deleteProlongable(?, ?, ?)(?)).thenThrowF(new Exception)
          val result = productManager.setProlongable(domain, offerId, product, prolongable)(request).failed.futureValue
          result shouldBe an[Exception]
          verify(offersManager).checkOwnership(List(offerId))(request)
          verify(salesmanUserClient).deleteProlongable(domain, offerId, product)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }
  }

  "ProductManager.setProlongable(transaction)" should {

    "return success for Prolongable(true)" in {
      forAll(SalesmanDomainGen, TransactionIdGen, privateRequestGen, TransactionGen) {
        (domain, transactionId, request, transaction) =>
          val user = request.user.privateRef
          val userTransaction = transaction.toBuilder.setUser(user.toPlain).build()
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(userTransaction)
          when(salesmanUserClient.putProlongable(?, TransactionId(any()))(?)).thenReturnF(())
          val result = productManager.setProlongable(domain, transactionId, Prolongable(true))(request).futureValue
          result shouldBe ManagerUtils.SuccessResponse
          val ticketId = SalesmanTicketId(transactionId.value, domain)
          verify(salesmanUserClient).getTransaction(user, ticketId)(request)
          verify(salesmanUserClient).putProlongable(domain, transactionId)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return success for Prolongable(false)" in {
      forAll(SalesmanDomainGen, TransactionIdGen, privateRequestGen, TransactionGen) {
        (domain, transactionId, request, transaction) =>
          val user = request.user.privateRef
          val userTransaction = transaction.toBuilder.setUser(user.toPlain).build()
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(userTransaction)
          when(salesmanUserClient.deleteProlongable(?, TransactionId(any()))(?)).thenReturnF(())
          val result = productManager.setProlongable(domain, transactionId, Prolongable(false))(request).futureValue
          result shouldBe ManagerUtils.SuccessResponse
          val ticketId = SalesmanTicketId(transactionId.value, domain)
          verify(salesmanUserClient).getTransaction(user, ticketId)(request)
          verify(salesmanUserClient).deleteProlongable(domain, transactionId)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure for dealer" in {
      forAll(SalesmanDomainGen, TransactionIdGen, dealerRequestGen, ProlongableGen) {
        (domain, transactionId, request, prolongable) =>
          val result = productManager.setProlongable(domain, transactionId, prolongable)(request).failed.futureValue
          result shouldBe a[NeedAuthentication]
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on check ownership error" in {
      forAll(SalesmanDomainGen, TransactionIdGen, privateRequestGen, TransactionGen, ProlongableGen) {
        (domain, transactionId, request, transaction, prolongable) =>
          val user = request.user.privateRef
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(transaction)
          val result = productManager.setProlongable(domain, transactionId, prolongable)(request).failed.futureValue
          result shouldBe an[TransactionNotFound]
          val ticketId = SalesmanTicketId(transactionId.value, domain)
          verify(salesmanUserClient).getTransaction(user, ticketId)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on salesman putProlongable() error" in {
      forAll(SalesmanDomainGen, TransactionIdGen, privateRequestGen, TransactionGen) {
        (domain, transactionId, request, transaction) =>
          val user = request.user.privateRef
          val userTransaction = transaction.toBuilder.setUser(user.toPlain).build()
          when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(userTransaction)
          when(salesmanUserClient.putProlongable(?, TransactionId(any()))(?)).thenThrowF(new Exception)
          val result =
            productManager.setProlongable(domain, transactionId, Prolongable(true))(request).failed.futureValue
          result shouldBe an[Exception]
          val ticketId = SalesmanTicketId(transactionId.value, domain)
          verify(salesmanUserClient).getTransaction(user, ticketId)(request)
          verify(salesmanUserClient).putProlongable(domain, transactionId)(request)
          verifyNoMoreInteractionsAndReset()
      }
    }
  }

  "ProductManager.checkOwnership(transaction)" should {

    "return success" in {
      forAll(SalesmanTicketIdGen, privateRequestGen, TransactionGen) { (ticketId, request, transaction) =>
        val userTransaction = transaction.toBuilder
          .setUser(request.user.privateRef.toPlain)
          .build()
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(userTransaction)
        productManager.checkOwnership(ticketId)(request).futureValue shouldBe (())
        verify(salesmanUserClient).getTransaction(request.user.privateRef, ticketId)(request)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure if user doesn't match" in {
      forAll(SalesmanTicketIdGen, privateRequestGen, TransactionGen) { (ticketId, request, transaction) =>
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenReturnF(transaction)
        productManager.checkOwnership(ticketId)(request).failed.futureValue shouldBe a[TransactionNotFound]
        verify(salesmanUserClient).getTransaction(request.user.privateRef, ticketId)(request)
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure for dealer" in {
      forAll(SalesmanTicketIdGen, dealerRequestGen) { (ticketId, request) =>
        productManager.checkOwnership(ticketId)(request).failed.futureValue shouldBe a[NeedAuthentication]
        verifyNoMoreInteractionsAndReset()
      }
    }

    "return failure on salesman error" in {
      forAll(SalesmanTicketIdGen, privateRequestGen) { (ticketId, request) =>
        when(salesmanUserClient.getTransaction(?, ?)(?)).thenThrowF(new Exception)
        productManager.checkOwnership(ticketId)(request).failed.futureValue shouldBe an[Exception]
        verify(salesmanUserClient).getTransaction(request.user.privateRef, ticketId)(request)
        verifyNoMoreInteractionsAndReset()
      }
    }
  }

  private def toProto(product: AutoruProduct) = {
    SalesmanModel.AutoruProduct.newBuilder().setCode(product.salesName).build()
  }
}
