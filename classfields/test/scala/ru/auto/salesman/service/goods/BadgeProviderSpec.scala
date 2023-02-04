package ru.auto.salesman.service.goods

import org.scalatest.concurrent.IntegrationPatience
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.impl.jdbc.{
  JdbcBadgeDao,
  JdbcCarsGoodsQueries,
  JdbcCategorizedGoodsQueries,
  JdbcGoodsDao
}
import ru.auto.salesman.model.AdsRequestTypes.CarsUsed
import ru.auto.salesman.model.ProductId.Badge
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.service.goods.domain.GoodsRequest
import ru.auto.salesman.service.goods.provider.BadgeProvider
import ru.auto.salesman.service.impl.GoodsDaoProviderImpl
import ru.auto.salesman.test.template.BadgeJdbcSpecTemplate
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.model._

class BadgeProviderSpec
    extends BaseSpec
    with IntegrationPatience
    with BadgeJdbcSpecTemplate {
  val badgeDao = new JdbcBadgeDao(database)

  val carsGoodsDao = new JdbcGoodsDao(JdbcCarsGoodsQueries, database)
  val categorizedGoodsDao = new JdbcGoodsDao(JdbcCategorizedGoodsQueries, database)

  val goodsDaoProvider =
    new GoodsDaoProviderImpl(carsDao = carsGoodsDao, categorizedDao = categorizedGoodsDao)

  val badgeProvider = new BadgeProvider(badgeDao, goodsDaoProvider, database)

  val TestClient = Client(
    clientId = 20101,
    agencyId = None,
    categorizedClientId = None,
    companyId = None,
    regionId = RegionId(1L),
    cityId = CityId(21735L),
    status = ClientStatuses.Active,
    singlePayment = Set(CarsUsed),
    firstModerated = true,
    name = Some("Test client"),
    paidCallsAvailable = false,
    priorityPlacement = false
  )

  val BadgerBadge = "badger"
  val MushroomBadge = "mushroom"
  val SnakeBadge = "snake"
  val NarwalBadge = "narwal"

  val TestRequest = GoodsRequest(
    offerId = AutoruOfferId(2, None),
    category = OfferCategories.Cars,
    section = Section.NEW,
    product = Badge,
    badges = Set(BadgerBadge, MushroomBadge, SnakeBadge)
  )
  // offer_ids|sale_ids can be found in sales.sql
  // ids are in range [2, 40]

  "BadgeProvider" when {
    "create badges for cars" in {
      // given
      val offerId = 3
      val category = OfferCategories.Cars
      val goodsRequest = TestRequest.copy(
        AutoruOfferId(offerId, None),
        category
      )

      // when
      val addingResults = badgeProvider
        .add(TestClient, goodsRequest)
        .success
        .value

      // expect
      addingResults.size shouldBe 3

      // there are three services now, connected with added badges
      val badgeServices =
        carsGoodsDao
          .get(
            GoodsDao.Filter
              .ForOfferProductStatus(
                offerId,
                ProductId.Badge,
                GoodStatuses.Active
              )
          )
          .get
      badgeServices.size shouldBe 3

      // and it's corresponding rows in badges table
      val insertedBadges = badgeServices.map { badgeService =>
        badgeDao
          .getBadge(
            goodsId = badgeService.primaryKeyId,
            offerId = offerId,
            category = category
          )
          .success
          .value
      }
      insertedBadges.size shouldBe 3

    }

    "create badges for trucks" in {
      // given
      val offerId = 4
      val category = OfferCategories.Trucks
      val goodsRequest = TestRequest.copy(
        AutoruOfferId(offerId, None),
        category
      )

      // when
      val addingResult = badgeProvider
        .add(TestClient, goodsRequest)
        .success
        .value

      // expect
      addingResult.size shouldBe 3

      // there are three services now, connected with added badges
      val badgeServices =
        categorizedGoodsDao
          .get(
            GoodsDao.Filter
              .ForOfferCategoryProduct(
                offerId,
                category,
                ProductId.Badge
              )
          )
          .get
      badgeServices.size shouldBe 3

      // and it's corresponding rows in badges table
      val insertedBadges = badgeServices.map { badgeService =>
        badgeDao
          .getBadge(
            goodsId = badgeService.primaryKeyId,
            offerId = offerId,
            category = category
          )
          .success
          .value
      }
      insertedBadges.size shouldBe 3
    }

    "replace badges and deactivate old services" in {
      val offerId = 5
      val category = OfferCategories.Cars
      val firstRequest = TestRequest.copy(
        AutoruOfferId(offerId, None),
        category
      )
      val secondRequest = firstRequest.copy(
        badges = Set(NarwalBadge, MushroomBadge)
      )
      val thirdRequest = secondRequest
      val fourthRequest = secondRequest.copy(
        badges = Set(NarwalBadge)
      )

      // add three new badges
      val firstAdd = badgeProvider.add(TestClient, firstRequest).success.value
      // remove two, add two, one is intersected
      val secondAdd = badgeProvider.add(TestClient, secondRequest).success.value
      // execute previous, should do nothing
      val thirdAdd = badgeProvider.add(TestClient, thirdRequest).success.value
      // adding one intersected - only rm's are expected
      val fourthAdd = badgeProvider.add(TestClient, fourthRequest).success.value

      firstAdd.size shouldBe 3
      secondAdd.size shouldBe 1
      thirdAdd.size shouldBe 0
      fourthAdd.size shouldBe 0

      val activeServices =
        carsGoodsDao
          .get(
            GoodsDao.Filter
              .ForOfferProductStatus(
                offerId,
                ProductId.Badge,
                GoodStatuses.Active
              )
          )
          .get

      val inactiveServices = carsGoodsDao
        .get(
          GoodsDao.Filter
            .ForOfferProductStatus(
              offerId,
              ProductId.Badge,
              GoodStatuses.Inactive
            )
        )
        .get

      val finalBadges = activeServices.map { badgeService =>
        badgeDao
          .getBadge(
            goodsId = badgeService.primaryKeyId,
            offerId = offerId,
            category = category
          )
          .success
          .value
      }
      inactiveServices.size shouldBe 3
      finalBadges.size shouldBe 1
      activeServices
        .map(_.primaryKeyId)
        .toSet
        .intersect(inactiveServices.map(_.primaryKeyId).toSet)
        .size shouldBe 0

    }
  }
}
