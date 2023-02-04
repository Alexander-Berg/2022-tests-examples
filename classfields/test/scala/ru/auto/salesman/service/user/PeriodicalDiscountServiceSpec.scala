package ru.auto.salesman.service.user

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.{
  ApiOfferModel,
  OffersByParamsFilterOuterClass,
  RequestModel,
  ResponseModel
}
import ru.auto.salesman.Task
import ru.auto.salesman.client.{BunkerClient, VosClient}
import ru.auto.salesman.dao.PeriodicalDiscountDaoSpec
import ru.auto.salesman.dao.impl.jdbc.user.JdbcPeriodicalDiscountDao
import ru.auto.salesman.dao.impl.yt.user.YtUserExclusionsDao
import ru.auto.salesman.dao.slick.invariant
import ru.auto.salesman.dao.slick.invariant.StaticQuery._
import ru.auto.salesman.dao.user.{PeriodicalDiscountDao, UserExclusionsDao}
import ru.auto.salesman.dao.yt.user.YtUserExclusionsDaoSpec
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.periodical_discount_exclusion.Product.{
  InDiscount,
  NoActiveDiscount,
  UserExcludedFromDiscount
}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.{Turbo, Vip}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.{Placement, Top}
import ru.auto.salesman.model.user.product.Products.OfferProduct
import ru.auto.salesman.model.user.product.{AutoruProduct, ProductProvider}
import ru.auto.salesman.model.{
  AutoruUser,
  DbInstance,
  DeprecatedDomains,
  OfferTag,
  UserRef
}
import ru.auto.salesman.service.impl.ProductDescriptionServiceImpl
import ru.auto.salesman.service.impl.user.ModifyPriceServiceImplSpec.testBasePriceFor
import ru.auto.salesman.service.impl.user.PeriodicalDiscountServiceImpl
import ru.auto.salesman.service.user.ModifyPriceService.PatchedPrice
import ru.auto.salesman.service.user.PeriodicalDiscountService._
import ru.auto.salesman.service.user.PriceService.ZeroPrice
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.userExclusionsGen
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.test.template.{SalesmanUserJdbcSpecTemplate, YtSpecTemplate}
import ru.auto.salesman.util.{AutomatedContext, PriceRequestContextOffers, RequestContext}
import zio.ZIO
import zio.blocking.effectBlocking

import scala.util.Try
import scala.util.control.NoStackTrace

class PeriodicalDiscountServiceSpec
    extends BaseSpec
    with ServiceModelGenerators
    with PeriodicalDiscountDaoSpec.JdbcTemplate
    with SalesmanUserJdbcSpecTemplate
    with YtUserExclusionsDaoSpec.YtTemplate
    with YtSpecTemplate {

  import PeriodicalDiscountServiceSpec._

  implicit override def domain: DeprecatedDomains.Value =
    DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext(
    "PeriodicalDiscountServiceSpec"
  )

  private val periodicalDiscountDao = mock[PeriodicalDiscountDao]
  private val userExclusionsDao = mock[UserExclusionsDao]
  private val vosClient = mock[VosClient]
  private val priceService = mock[PriceService]
  private val bunkerClient = mock[BunkerClient]

  (bunkerClient.getDescriptions _)
    .expects()
    .returningZ(createDescriptionsMap(names))

  private val productDescriptionService = new ProductDescriptionServiceImpl(bunkerClient)

  private val periodicalDiscountService = new PeriodicalDiscountServiceImpl(
    periodicalDiscountDao,
    userExclusionsDao,
    vosClient,
    priceService,
    productDescriptionService
  )

  private val yt = client

  private val jdbcPeriodicalDiscountDao =
    new JdbcPeriodicalDiscountDao(transactor, transactor)

  private val periodicalDiscountServiceWithDbAndYt =
    new PeriodicalDiscountServiceImpl(
      jdbcPeriodicalDiscountDao,
      new YtUserExclusionsDao(yt, cfg),
      vosClient,
      priceService,
      productDescriptionService
    )

  "PeriodicalDiscountService.patchWith" should {
    "do not patch zero price with periodical discount" in {
      val discount = PeriodicalDiscountGen.next
      patchWith(InDiscount(discount))(
        PatchedPrice(
          testBasePriceFor(ZeroPrice),
          ZeroPrice,
          modifier = None,
          periodicalDiscountExclusion = None
        )
      ).effectivePrice shouldBe 0L
    }

    "patch price with periodical discount" in {
      val discountInstance = PeriodicalDiscountGen.next
      val price =
        PriceService.priceToFunds(100L)
      val expected =
        PriceService.priceWithDiscount(price, discountInstance.discount)

      patchWith(InDiscount(discountInstance))(
        PatchedPrice(
          testBasePriceFor(price),
          price,
          modifier = None,
          periodicalDiscountExclusion = None
        )
      ).effectivePrice shouldBe expected
    }

    "do not patch price with excluded discount" in {
      val discountInstance = PeriodicalDiscountGen.next
      val price = PriceService.priceToFunds(100L)
      val expected = PriceService.priceToFunds(100L)

      patchWith(UserExcludedFromDiscount(discountInstance))(
        PatchedPrice(
          testBasePriceFor(price),
          price,
          modifier = None,
          periodicalDiscountExclusion = None
        )
      ).effectivePrice shouldBe expected
    }

    "save periodical discount exclusion" in {
      val discountInstance = PeriodicalDiscountGen.next
      val price = PriceService.priceToFunds(100L)

      val startedWithPrice =
        PatchedPrice(
          testBasePriceFor(price),
          price,
          modifier = None,
          periodicalDiscountExclusion = None
        )

      val expected = PatchedPrice(
        testBasePriceFor(price),
        price,
        modifier = None,
        Some(Analytics.UserExcludedFromDiscount(discountInstance.discountId))
      )

      patchWith(UserExcludedFromDiscount(discountInstance))(
        startedWithPrice
      ) shouldBe expected
    }

    "do not patch price if there is no discount" in {
      val price = PriceService.priceToFunds(100L)
      val expected = PriceService.priceToFunds(100L)

      patchWith(NoActiveDiscount)(
        PatchedPrice(
          testBasePriceFor(price),
          price,
          modifier = None,
          periodicalDiscountExclusion = None
        )
      ).effectivePrice shouldBe expected
    }
  }

  "PeriodicalDiscountService.availableDiscount" should {
    "get available discount" in {
      val discount = PeriodicalDiscountGen.next
      (periodicalDiscountDao.getActive _)
        .expects(PeriodicalDiscountDao.ActiveFilter.ForUser(testUser))
        .returningZ(Some(discount))
        .noMoreThanOnce()
      (vosClient.countUserOffers _).expects(*).returningZ(5).noMoreThanOnce()
      (vosClient.getUserOffers _)
        .expects(*)
        .returningZ(OfferListingResponse.newBuilder().build())
        .noMoreThanOnce()
      val result = periodicalDiscountService
        .availableDiscount(testUser, Some(Category.CARS))
        .success
        .value
      result.getAvailableDiscount.getDiscount shouldBe discount.discount
      val expectedProducts =
        discount.context.flatMap(_.products.map(_.size)).getOrElse(0)
      result.getAvailableDiscount.getServicesList should have size expectedProducts
    }

    "discount service should fail on failed moisha response" in {
      val discount = PeriodicalDiscountGen.next
      val offer = offerGen().next
      (periodicalDiscountDao.getActive _)
        .expects(PeriodicalDiscountDao.ActiveFilter.ForUser(testUser))
        .returningZ(Some(discount))
        .noMoreThanOnce()
      (vosClient.countUserOffers _).expects(*).returningZ(5).noMoreThanOnce()
      (vosClient.getUserOffers _)
        .expects(*)
        .returningZ(OfferListingResponse.newBuilder().addOffers(offer).build())
        .noMoreThanOnce()
      (priceService
        .calculatePricesForMultipleOffers(
          _: List[AutoruProduct],
          _: PriceRequestContextOffers
        ))
        .expects(*, *)
        .throwingZ(new RuntimeException("TEST"))
      val fail = periodicalDiscountService
        .availableDiscount(testUser, Some(Category.CARS))
        .failure
      fail.cause.squash.getMessage shouldEqual "TEST"
    }

    @volatile
    var getUserOffersCalled: Boolean = false
    @volatile
    var countUserOffersCalled: Boolean = false

    val vosClientStub = new VosClient {
      def getOptOffer(
          offerId: OfferIdentity,
          dbInstance: DbInstance
      ): Task[Option[ApiOfferModel.Offer]] = ???

      def hasSameOffer(
          category: Category,
          offerId: OfferIdentity,
          userRef: String
      ): Task[Boolean] = ???

      def hasSameOfferAsDraft(
          category: Category,
          offerId: OfferIdentity,
          userRef: String
      ): Task[Boolean] = ???

      def countUserOffers(
          countUserOffersQuery: VosClient.CountUserOffersQuery
      ): Task[Int] =
        ZIO.succeed {
          countUserOffersCalled = true
          0
        }

      def addServices(
          offerId: OfferIdentity,
          offerCategory: Option[Category],
          userRef: String,
          services: RequestModel.AddServicesRequest
      ): Task[VosClient.AddServicesResult] =
        ???

      def getUserOffers(
          query: VosClient.GetUserOffersQuery
      ): Task[OfferListingResponse] =
        ZIO.succeed {
          getUserOffersCalled = true
          OfferListingResponse.newBuilder().build
        }

      def getMarkModels(
          query: VosClient.GetMarkModelsQuery
      ): Task[Option[ResponseModel.MarkModelsResponse]] =
        ???

      def setCountersStartDate(
          offerId: OfferIdentity,
          offerCategory: Option[Category],
          date: Option[DateTime]
      )(implicit rc: RequestContext): Try[Unit] = ???

      def putTags(
          offerId: OfferIdentity,
          offerCategory: Option[Category],
          tags: Set[OfferTag]
      ): Task[Unit] = ???

      def deleteTags(
          offerId: OfferIdentity,
          offerCategory: Option[Category],
          tags: Set[OfferTag]
      )(implicit rc: RequestContext): Try[Unit] = ???

      def getByParams(
          filter: OffersByParamsFilterOuterClass.OffersByParamsFilter
      ): Task[List[ApiOfferModel.Offer]] = ???

      override def checkOffersBelong(
          userRef: UserRef,
          offerIds: Seq[OfferIdentity]
      ): Task[Boolean] = ???
    }

    val periodicalDiscountServiceStubbed = new PeriodicalDiscountServiceImpl(
      periodicalDiscountDao,
      userExclusionsDao,
      vosClientStub,
      priceService,
      productDescriptionService
    )

    "don't call VOS if there is no available discount" in {
      (periodicalDiscountDao.getActive _)
        .expects(PeriodicalDiscountDao.ActiveFilter.ForUser(testUser))
        .returningZ(None)

      periodicalDiscountServiceStubbed
        .availableDiscount(testUser, Some(Category.CARS))
        .success

      getUserOffersCalled shouldBe false
      countUserOffersCalled shouldBe false
    }
  }

  "PeriodicalDiscountService.getBlacklistUsersByDiscount" should {
    "return empty list if dao returns empty list" in {
      val users = List.empty
      (periodicalDiscountDao.getExcludedUsersByDiscount _)
        .expects(testDiscountId)
        .returningZ(users)
        .once()

      periodicalDiscountService
        .getExcludedUsersByDiscount(testDiscountId)
        .success
        .value shouldEqual users

    }

    "return list if dao returns list" in {
      val users = List(1, 2).map(AutoruUser(_))
      (periodicalDiscountDao.getExcludedUsersByDiscount _)
        .expects(testDiscountId)
        .returningZ(users)
        .once()

      periodicalDiscountService
        .getExcludedUsersByDiscount(testDiscountId)
        .success
        .value shouldEqual users
    }

    "fails if dao fails" in {
      (periodicalDiscountDao.getExcludedUsersByDiscount _)
        .expects(testDiscountId)
        .throwingZ(TestException)
        .once()

      periodicalDiscountService
        .getExcludedUsersByDiscount(testDiscountId)
        .failure
        .exception shouldEqual TestException
    }
  }

  "PeriodicalDiscountService.saveExcludedUsers" should {
    "save excluded users" in {
      val exclusions = userExclusionsGen(allowedProducts = Set(Turbo)).next
      clean()
      val products = List(Turbo, Vip)
      val discount = createDiscount(products)
      database.withSession { implicit session =>
        insertDiscounts(List(discount), productsToJson(products))
      }

      val test =
        for {
          _ <- createData(exclusions, tomorrow, yt)
          _ <- periodicalDiscountServiceWithDbAndYt.saveExcludedUsers()
          excludedUsers <- periodicalDiscountServiceWithDbAndYt
            .getExcludedUsersByDiscount(discount.discountId)
        } yield
          excludedUsers should contain theSameElementsAs exclusions.map(
            _.userId
          )

      test
        .provideConstantClock(currentTime)
        .success
    }

    "don't save exclusion if there is no discounts" in {
      val exclusions = userExclusionsGen(allowedProducts = Set(Turbo)).next
      clean()

      val countQuery =
        "SELECT count(*) FROM periodical_discount_user_exclusion"
      val countExclusions = effectBlocking {
        database.withSession { implicit session =>
          invariant.StaticQuery.queryNA[Int](countQuery).first
        }
      }

      val test =
        for {
          _ <- createData(exclusions, tomorrow, yt)
          _ <- periodicalDiscountServiceWithDbAndYt.saveExcludedUsers()
          count <- countExclusions
        } yield count shouldBe 0

      test
        .provideConstantClock(currentTime)
        .success
    }

    "don't save exclusion if exclusions have been already saved" in {
      val exclusions = userExclusionsGen(allowedProducts = Set(Turbo)).next
      clean()
      val products = List(Turbo, Vip)
      val discount = createDiscount(products)
      database.withSession { implicit session =>
        insertDiscounts(List(discount), productsToJson(products))
      }

      val test =
        for {
          _ <- createData(exclusions, tomorrow, yt)
          _ <- jdbcPeriodicalDiscountDao.insertDiscountExclusions(
            discount.discountId,
            List(testUser)
          )
          _ <- periodicalDiscountServiceWithDbAndYt.saveExcludedUsers()
          excludedUsers <- periodicalDiscountServiceWithDbAndYt
            .getExcludedUsersByDiscount(discount.discountId)
        } yield excludedUsers should contain theSameElementsAs List(testUser)

      test
        .provideConstantClock(currentTime)
        .success
    }

    "don't save exclusion if there is no match by products" in {
      val exclusions = userExclusionsGen(allowedProducts = Set(Placement)).next
      clean()
      val products = List(Turbo, Vip)
      val discount = createDiscount(products)
      database.withSession { implicit session =>
        insertDiscounts(List(discount), productsToJson(products))
      }

      val test =
        for {
          _ <- createData(exclusions, tomorrow, yt)
          _ <- periodicalDiscountServiceWithDbAndYt.saveExcludedUsers()
          excludedUsers <- periodicalDiscountServiceWithDbAndYt
            .getExcludedUsersByDiscount(discount.discountId)
        } yield excludedUsers.isEmpty shouldBe true

      test
        .provideConstantClock(currentTime)
        .success
    }

    "save excluded users when there is a partial match" in {
      val exclusions =
        userExclusionsGen(allowedProducts = Set(Turbo, Placement, Top)).next

      clean()
      val products = List(Turbo, Placement, Vip)
      val discount = createDiscount(products)
      database.withSession { implicit session =>
        insertDiscounts(List(discount), productsToJson(products))
      }

      val (exclusionsToSave, exclusionsToSkip) =
        exclusions.partition(_.product != Top)

      val test =
        for {
          _ <- createData(exclusions, tomorrow, yt)
          _ <- periodicalDiscountServiceWithDbAndYt.saveExcludedUsers()
          excludedUsers <- periodicalDiscountServiceWithDbAndYt
            .getExcludedUsersByDiscount(discount.discountId)
        } yield {
          excludedUsers should contain theSameElementsAs exclusionsToSave
            .map(_.userId)
          excludedUsers shouldNot contain theSameElementsAs exclusionsToSkip
            .map(_.userId)
        }

      test
        .provideConstantClock(currentTime)
        .success
    }
  }
}

object PeriodicalDiscountServiceSpec {
  private val currentTime = DateTime.parse("2020-10-14T10:00:00.000")
  private val tomorrow = currentTime.plusDays(1)

  private val testDiscountId = "1"
  private val testUser = AutoruUser(123)

  private val topName = "Топ"
  private val boostName = "Буст"
  private val turboName = "Турбо"

  private val names =
    Map[OfferProduct, String](
      ProductProvider.AutoruGoods.Top -> topName,
      ProductProvider.AutoruGoods.Boost -> boostName,
      ProductProvider.AutoruBundles.Turbo -> turboName
    )

  private def createDiscount(products: List[AutoruProduct]) =
    PeriodicalDiscount(
      discountId = testDiscountId,
      start = tomorrow.withTimeAtStartOfDay(),
      deadline = tomorrow.plusDays(1),
      discount = 30,
      context = Some(PeriodicalDiscountContext(Some(products.map(_.name))))
    )

  private def createDescriptionsMap(
      names: Map[OfferProduct, String]
  ): ProductsDescriptions = {
    def toMap(name: String) = {
      val productDescription =
        ProductDescription(name = Some(name))

      val descriptionByType =
        Map[EndUserType, ProductDescription](
          EndUserType.Default -> productDescription
        )

      Map(
        ApiOfferModel.Category.CARS -> descriptionByType,
        ApiOfferModel.Category.MOTO -> descriptionByType,
        ApiOfferModel.Category.TRUCKS -> descriptionByType
      )
    }

    ProductsDescriptions(
      AutoruProductsDescriptions(names.mapValues(toMap), Map.empty)
    )
  }

  private def productsToJson(products: List[AutoruProduct]): String =
    s"""{"products": [${products.map(p => s""""$p"""").mkString(", ")}]}"""

  case object TestException extends Exception with NoStackTrace
}
