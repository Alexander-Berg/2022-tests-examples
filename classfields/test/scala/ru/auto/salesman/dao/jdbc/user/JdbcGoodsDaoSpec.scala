package ru.auto.salesman.dao.jdbc.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.salesman.dao.impl.jdbc.user.JdbcGoodsDao
import ru.auto.salesman.dao.user.{GoodsDao, GoodsDaoSpec}
import ru.auto.salesman.dao.user.GoodsDao.Condition.WithActiveOfferProduct
import ru.auto.salesman.dao.user.GoodsDao.Filter.{
  ActiveProlongablePlacementDeadlineInInterval,
  ForActiveProductOffers,
  ForGoodsId,
  ProlongIntervalIsNotEnded,
  UserProductDeadlineSince
}
import ru.auto.salesman.dao.user.GoodsDao.Patch.IncreaseDeadline
import ru.auto.salesman.model.{DeprecatedDomain, ProductStatuses}
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.{
  ExperimentInfo,
  PriceModifier,
  Prolongable,
  UserQuotaRemoved
}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.{Boost, Placement}
import ru.auto.salesman.test.IntegrationPropertyCheckConfig
import ru.auto.salesman.test.model.gens.AutoruOfferIdGen
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.util.CryptoFunctions._
import ru.yandex.vertis.generators.BasicGenerators

class JdbcGoodsDaoSpec
    extends GoodsDaoSpec
    with BeforeAndAfter
    with SalesmanUserJdbcSpecTemplate
    with IntegrationPropertyCheckConfig
    with UserDaoGenerators
    with PushedToVosCheck
    with BasicGenerators {

  def newDao(): GoodsDao = new JdbcGoodsDao(database)
  implicit override def domain: DeprecatedDomain = AutoRu

  after {
    database.withSession { session =>
      session.conn.prepareStatement("DELETE FROM notifications").execute()
      session.conn.prepareStatement("DELETE FROM goods").execute()
    }
  }
  private val dao = newDao()

  "JdbcGoodsDao" should {

    "generate goodsId without UserQuotaChanged modifier" in {
      forAll(goodsCreateRequestGen()) { genRequest =>
        val request = genRequest.copy(
          user = "user:123",
          offer = AutoruOfferId("111-fff"),
          product = Placement,
          transactionId = "testTransaction",
          baseGoodsId = Some("prevGoodsId"),
          context = genRequest.context.copy(
            productPrice = genRequest.context.productPrice.copy(
              price = genRequest.context.productPrice.price.copy(
                modifier = genRequest.context.productPrice.price.modifier.map(
                  _.copy(userQuotaChanged = None)
                )
              )
            )
          )
        )

        request.goodsId shouldBe md5 {
          "user:123-111-fff-placement-testTransaction"
        }
      }
    }

    "generate goodsId without base goodsId" in {
      forAll(goodsCreateRequestGen()) { genRequest =>
        val request = genRequest.copy(
          user = "user:123",
          offer = AutoruOfferId("111-fff"),
          product = Placement,
          transactionId = "testTransaction",
          baseGoodsId = None,
          context = genRequest.context.copy(
            productPrice = genRequest.context.productPrice.copy(
              price = genRequest.context.productPrice.price.copy(
                modifier = Some {
                  genRequest.context.productPrice.price.modifier
                    .getOrElse(PriceModifier.empty)
                    .copy(userQuotaChanged = Some(UserQuotaRemoved(100L)))
                }
              )
            )
          )
        )

        request.goodsId shouldBe md5 {
          "user:123-111-fff-placement-testTransaction"
        }
      }
    }

    "generate goodsId with base goodsId" in {
      forAll(goodsCreateRequestGen()) { genRequest =>
        val request = genRequest.copy(
          user = "user:123",
          offer = AutoruOfferId("111-fff"),
          product = Placement,
          transactionId = "testTransaction",
          baseGoodsId = Some("prevGoodsId"),
          context = genRequest.context.copy(
            productPrice = genRequest.context.productPrice.copy(
              price = genRequest.context.productPrice.price.copy(
                modifier = Some {
                  genRequest.context.productPrice.price.modifier
                    .getOrElse(PriceModifier.empty)
                    .copy(userQuotaChanged = Some(UserQuotaRemoved(100L)))
                }
              )
            )
          )
        )

        request.goodsId shouldBe md5 {
          "user:123-111-fff-placement-testTransaction" + "prevGoodsId"
        }
      }
    }

    "insert new request for autoru offer" in {
      forAll(goodsCreateRequestGen()) { request =>
        val result = dao.insertIfNotExists(request).success.value
        result.id shouldBe request.goodsId
        result.offer shouldBe request.offer
        result.user shouldBe request.user
        result.product shouldBe request.product
        result.amount shouldBe request.amount
        result.status shouldBe request.status
        result.transactionId shouldBe request.transactionId
        result.context shouldBe request.context
        result.activated shouldBe request.activated
        result.deadline shouldBe request.deadline
        result.prolongable shouldBe request.prolongable
      }
    }

    "succeed on duplicated insertIfNotExists() invocation" in {
      forAll(goodsCreateRequestGen()) { request =>
        (dao.insertIfNotExists(request) *> dao.insertIfNotExists(
          request
        )).success
      }
    }

    "get some placement by filter ActiveProlongablePlacementDeadlineInInterval" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active,
          prolongable = Prolongable(true)
        )
        val d = request.deadline
        dao.insertIfNotExists(request).success
        dao
          .get(
            ActiveProlongablePlacementDeadlineInInterval(
              d.minusHours(1),
              d.plusHours(1)
            )
          )
          .success
          .value
          .size shouldBe 1
      }
    }

    "get nothing by filter ActiveProlongablePlacementDeadlineInInterval if deadline isn't in interval" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active,
          prolongable = Prolongable(true)
        )
        val d = request.deadline
        dao.insertIfNotExists(request)
        dao
          .get(
            ActiveProlongablePlacementDeadlineInInterval(
              d.plusHours(1),
              d.plusHours(3)
            )
          )
          .success
          .value
          .size shouldBe 0
      }
    }

    "get nothing by filter ActiveProlongablePlacementDeadlineInInterval if status isn't Active" in {
      forAll(
        goodsCreateRequestGen(),
        Gen.oneOf(ProductStatuses.Canceled, ProductStatuses.Inactive)
      ) { (goodsCreateRequestGenerated, notActiveStatus) =>
        val request = goodsCreateRequestGenerated.copy(
          product = Placement,
          status = notActiveStatus,
          prolongable = Prolongable(true)
        )
        val d = request.deadline
        dao.insertIfNotExists(request)
        dao
          .get(
            ActiveProlongablePlacementDeadlineInInterval(
              d.minusHours(1),
              d.plusHours(1)
            )
          )
          .success
          .value
          .size shouldBe 0
      }
    }

    "get nothing by filter ActiveProlongablePlacementDeadlineInInterval if isn't prolongable" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active,
          prolongable = Prolongable(false)
        )
        val d = request.deadline
        dao.insertIfNotExists(request)
        dao
          .get(
            ActiveProlongablePlacementDeadlineInInterval(
              d.minusHours(1),
              d.plusHours(1)
            )
          )
          .success
          .value
          .size shouldBe 0
      }
    }

    "update deadline by IncreaseDeadline patch" in {
      forAll(goodsCreateRequestGen(), Gen.posNum[Long], bool) {
        (goodsCreateRequestGenerated, offset, pushedToVos) =>
          val request = goodsCreateRequestGenerated.copy(
            product = Placement,
            status = ProductStatuses.Active
          )

          val d = request.deadline.plus(offset)

          val inserted = dao.insertIfNotExists(request).success.value
          setPushedToVos(inserted.id, pushedToVos)
          dao
            .update(
              WithActiveOfferProduct(request.user, request.offer, Placement),
              IncreaseDeadline(d)
            )
            .success

          val res = dao
            .get(ForActiveProductOffers(Placement, List(request.offer)))
            .success
            .value
          res.head.deadline shouldBe d
          getPushedToVos(inserted.id) shouldBe false
      }
    }

    "not update deadline by IncreaseDeadline patch" in {
      forAll(goodsCreateRequestGen(), Gen.posNum[Long], bool) {
        (goodsCreateRequestGenerated, offset, pushedToVos) =>
          val request = goodsCreateRequestGenerated.copy(
            product = Placement,
            status = ProductStatuses.Active
          )

          val d = request.deadline.minus(offset)

          val inserted = dao.insertIfNotExists(request).success.value
          setPushedToVos(inserted.id, pushedToVos)
          dao.update(
            WithActiveOfferProduct(request.user, request.offer, Placement),
            IncreaseDeadline(d)
          )

          val res = dao
            .get(ForActiveProductOffers(Placement, List(request.offer)))
            .success
            .value
          res.head.deadline shouldBe request.deadline
          getPushedToVos(inserted.id) shouldBe pushedToVos
      }
    }

    "get value for filter ProlongIntervalIsNotEnded" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active
        )
        val d = request.deadline
        dao.insertIfNotExists(request).success
        val res = dao
          .get(
            ProlongIntervalIsNotEnded(
              request.offer,
              Placement,
              d.minusHours(1)
            )
          )
          .success
          .value
        res.head.deadline shouldBe request.deadline
      }
    }

    "order values for filter ProlongIntervalIsNotEnded" in {
      val offerId = AutoruOfferIdGen.next
      forAll(
        goodsCreateRequestGen(Gen.const(offerId)),
        goodsCreateRequestGen(Gen.const(offerId))
      ) { (request1, request2) =>
        if (request1.goodsId != request2.goodsId) {
          val requestEarlierDeadline = request1.copy(
            product = Placement,
            status = ProductStatuses.Active
          )
          val d = requestEarlierDeadline.deadline
          val requestLaterDeadline = request2.copy(
            product = Placement,
            status = ProductStatuses.Active,
            deadline = d.plusMinutes(30)
          )
          dao.insertIfNotExists(requestEarlierDeadline).success
          dao.insertIfNotExists(requestLaterDeadline).success
          val res = dao
            .get(
              ProlongIntervalIsNotEnded(
                offerId,
                Placement,
                d.minusHours(1)
              )
            )
            .success
            .value
          res.head.deadline shouldBe requestLaterDeadline.deadline
        }
      }
    }

    "get value for filter ProlongIntervalIsNotEnded when deadline is after interval" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active
        )
        val d = request.deadline
        dao.insertIfNotExists(request).success
        val res = dao
          .get(
            ProlongIntervalIsNotEnded(
              request.offer,
              Placement,
              d.minusHours(2)
            )
          )
          .success
          .value
        res.size shouldBe 1
      }
    }

    "not get value for filter ProlongIntervalIsNotEnded for wrong offerId" in {
      forAll(goodsCreateRequestGen(), AutoruOfferIdGen) {
        (goodsCreateRequestGenerated, offerId) =>
          if (goodsCreateRequestGenerated.offer != offerId) {
            val request = goodsCreateRequestGenerated.copy(
              product = Placement,
              status = ProductStatuses.Active
            )
            val d = request.deadline
            dao.insertIfNotExists(request).success
            dao
              .get(
                ProlongIntervalIsNotEnded(
                  offerId,
                  Placement,
                  d.minusHours(1)
                )
              )
              .success
              .value shouldBe empty
          }
      }
    }

    "not get value for filter ProlongIntervalIsNotEnded for Canceled status" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Canceled
        )
        val d = request.deadline
        dao.insertIfNotExists(request).success
        dao
          .get(
            ProlongIntervalIsNotEnded(
              request.offer,
              Placement,
              d.minusHours(1)
            )
          )
          .success
          .value shouldBe empty
      }
    }

    "not get value for filter ProlongIntervalIsNotEnded for wrong product name" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active
        )
        val d = request.deadline
        dao.insertIfNotExists(request).success
        dao
          .get(
            ProlongIntervalIsNotEnded(
              request.offer,
              Boost,
              d.minusHours(1)
            )
          )
          .success
          .value shouldBe empty
      }
    }

    "not get value for filter UserProductDeadlineSince for Canceled status" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Canceled
        )
        dao.insertIfNotExists(request).success
        dao
          .get(
            UserProductDeadlineSince(
              request.deadline.minusHours(1),
              Placement,
              request.user
            )
          )
          .success
          .value shouldBe empty
      }
    }

    "get records for UserProductDeadlineSince with correct ordering" in {
      val requestE = goodsCreateRequestGen().next.copy(
        product = Placement,
        user = "user:123",
        offer = AutoruOfferId("111-fff"),
        status = ProductStatuses.Inactive,
        deadline = DateTime.parse("2020-06-08T00:00:00+03:00")
      )

      dao.insertIfNotExists(requestE).success

      val requestA = goodsCreateRequestGen().next.copy(
        product = Placement,
        user = "user:123",
        offer = AutoruOfferId("111-fff"),
        status = ProductStatuses.Inactive,
        deadline = DateTime.parse("2020-07-08T00:00:00+03:00")
      )

      dao.insertIfNotExists(requestA).success

      val requestB = goodsCreateRequestGen().next.copy(
        product = Placement,
        user = "user:123",
        offer = AutoruOfferId("222-ddd"),
        status = ProductStatuses.Active,
        deadline = DateTime.parse("2020-07-09T00:00:00+03:00")
      )

      dao.insertIfNotExists(requestB).success

      val requestC = goodsCreateRequestGen().next.copy(
        product = Placement,
        user = "user:123",
        offer = AutoruOfferId("111-fff"),
        status = ProductStatuses.Active,
        deadline = DateTime.parse("2020-07-08T00:00:00+03:00")
      )

      dao.insertIfNotExists(requestC).success

      val requestD = goodsCreateRequestGen().next.copy(
        product = Placement,
        user = "user:123",
        offer = AutoruOfferId("111-fff"),
        status = ProductStatuses.Inactive,
        deadline = DateTime.parse("2020-07-08T00:00:00+03:00")
      )

      dao.insertIfNotExists(requestD).success

      val expected = Iterable(requestC, requestB, requestD, requestA)

      val filter = UserProductDeadlineSince(
        DateTime.parse("2020-07-01T00:00:00+03:00"),
        Placement,
        "user:123"
      )

      val records = dao.get(filter).success.value
      records.map(_.id) shouldBe expected.map(_.goodsId)
    }

    "not get value for filter UserProductDeadlineSince for wrong product name" in {
      forAll(goodsCreateRequestGen()) { requestGenerated =>
        val request = requestGenerated.copy(
          product = Placement,
          status = ProductStatuses.Active
        )
        dao.insertIfNotExists(request).success
        dao
          .get(
            UserProductDeadlineSince(
              request.deadline.minusHours(1),
              Boost,
              request.user
            )
          )
          .success
          .value shouldBe empty
      }
    }

    "replace batch" in {
      forAll(Gen.listOfN(2, goodsCreateRequestGen())) { goodsRequests =>
        goodsRequests.foreach { goodsRequest =>
          val createRequest = goodsRequest.copy(
            product = Placement,
            status = ProductStatuses.Active
          )

          dao.insertIfNotExists(createRequest).success
        }

        val filter = ForActiveProductOffers(
          product = Placement,
          offers = goodsRequests.map(_.offer)
        )

        val insertedGoods = dao.get(filter).success.value

        insertedGoods.size shouldBe 2

        val testOfferId = AutoruOfferId("123-fff")
        val testUserId = "user:123"
        val testDeadline = DateTime.parse("2020-07-08T00:00:00+03:00")

        val batch = insertedGoods.map { goods =>
          val request = goodsCreateRequestGen().next
          goods -> request.copy(
            offer = testOfferId,
            user = testUserId,
            product = Placement,
            status = ProductStatuses.Active,
            deadline = testDeadline
          )
        }.toMap

        dao.replaceBatch(batch).success

        val newGoodsFilter = ForActiveProductOffers(
          product = Placement,
          offers = Iterable(testOfferId)
        )

        val newGoods = dao.get(newGoodsFilter).success.value

        newGoods.size shouldBe 2

        newGoods.foreach { goods =>
          goods.offer shouldBe testOfferId
          goods.user shouldBe testUserId
          goods.deadline.getMillis shouldBe testDeadline.getMillis
        }
      }
    }

    "insert price modifier correctly" in {
      val genGoodsRequest = goodsCreateRequestGen().next

      val testExperiment =
        Some(ExperimentInfo(Some("experiment-modifier"), "experiment-boxes"))

      val goodsRequest: GoodsDao.Request = genGoodsRequest.copy(
        product = Placement,
        context = genGoodsRequest.context.copy(
          productPrice = genGoodsRequest.context.productPrice.copy(
            price = genGoodsRequest.context.productPrice.price.copy(
              modifier = Some {
                PriceModifier.empty.copy(
                  experimentInfo = testExperiment,
                  userQuotaChanged = Some(UserQuotaRemoved(200L))
                )
              }
            )
          )
        )
      )

      dao.insertIfNotExists(goodsRequest).success

      val inserted =
        dao.get(ForGoodsId(goodsRequest.goodsId)).success.value.head

      inserted.product shouldBe Placement

      val modifier = inserted.context.productPrice.price.modifier
      modifier.flatMap(_.experimentInfo) shouldBe testExperiment
      modifier.flatMap(_.userQuotaChanged) shouldBe Some(UserQuotaRemoved(200L))
    }

    "insert applied experiment correctly" in {
      val genGoodsRequest = goodsCreateRequestGen().next

      val testExperiment =
        Some(ExperimentInfo(Some("experiment-modifier"), "experiment-boxes"))

      val testAppliedExperimentId = "experiment-applied"

      val goodsRequest: GoodsDao.Request = genGoodsRequest.copy(
        product = Placement,
        context = genGoodsRequest.context.copy(
          productPrice = genGoodsRequest.context.productPrice.copy(
            price = genGoodsRequest.context.productPrice.price.copy(
              modifier = Some {
                PriceModifier.empty.copy(
                  experimentInfo = testExperiment,
                  userQuotaChanged = Some(UserQuotaRemoved(200L)),
                  appliedExperimentId = Some(testAppliedExperimentId)
                )
              }
            )
          )
        )
      )

      dao.insertIfNotExists(goodsRequest).success

      val inserted =
        dao.get(ForGoodsId(goodsRequest.goodsId)).success.value.head

      inserted.product shouldBe Placement

      val modifier = inserted.context.productPrice.price.modifier
      modifier.flatMap(_.experimentInfo) shouldBe testExperiment
      modifier.flatMap(_.userQuotaChanged) shouldBe Some(UserQuotaRemoved(200L))
      modifier.flatMap(_.appliedExperimentId) shouldBe Some(testAppliedExperimentId)
    }

  }

  override protected def pushedToVosCheckTable: String = "goods"
}
