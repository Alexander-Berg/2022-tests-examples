package ru.auto.salesman.tasks

import org.joda.time.{DateTime, Days}
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.pushnoy.PushnoyClient
import ru.auto.salesman.dao.user.NotificationsDao.Filter.UserAndFireAtAfter
import ru.auto.salesman.dao.user.{NotificationsDao, UserPush}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Slave}
import ru.auto.salesman.service.user.GoodsService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.auto.salesman.test.TestException
import ru.auto.salesman.util.TimeUtils.Time
import ru.yandex.vertis.generators.DateTimeGenerators

class ResellerNeedMoneyOnCardPushTaskSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  val goodsService: GoodsService = mock[GoodsService]
  val pushnoyClient: PushnoyClient = mock[PushnoyClient]
  val notificationsDao: NotificationsDao = mock[NotificationsDao]
  val vosClient: VosClient = mock[VosClient]
  val time: Time = mock[Time]

  val task = new ResellerNeedMoneyOnCardPushTask(
    goodsService,
    pushnoyClient,
    notificationsDao,
    vosClient,
    Days.ONE,
    time
  )

  "ResellerNeedMoneyOnCard for one user" should {
    "fail if goodsService failed" in {
      forAll(DateTimeGenerators.dateTime()) { nowTime =>
        val testException = new TestException()

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .throwingZ(testException)

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .never

        (vosClient.getOffer _).expects(*, *).never

        (notificationsDao.insert(_: NotificationsDao.Request)).expects(*).never

        (pushnoyClient.pushToUser _).expects(*, *).never

        task.execute().failed.get shouldBe testException
      }
    }

    "user with one offer, go through all filters and call pushnoyClient" in {
      forAll(GoodsGen, ActiveOfferGen, DateTimeGenerators.dateTime()) {
        (goodGenerated, activeOffer, nowTime) =>
          val good =
            goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

          (time.now: () => DateTime).expects().returning(nowTime).once

          (goodsService.get _)
            .expects(*)
            .returningZ(List(good))

          (notificationsDao
            .get(_: NotificationsDao.Filter))
            .expects(*)
            .returningT(List.empty)
            .once()

          (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).once()

          (notificationsDao
            .insert(_: NotificationsDao.Request))
            .expects(*)
            .returningT(Unit)
            .once()

          (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

          task.execute().success
      }

    }

    "don't push for user if all his offers has deadline before [current time + 24 hours]" in {
      forAll(GoodsGen, DateTimeGenerators.dateTime()) { (goodGenerated, nowTime) =>
        val good =
          goodGenerated.copy(deadline = nowTime.plusDays(1).plusHours(1))

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .never

        (vosClient.getOffer _).expects(*, *).never

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .never

        (pushnoyClient.pushToUser _).expects(*, *).never

        task.execute().success
      }
    }

    "don't push for user with 2 offers if all his offers has deadline before [current time + 24 hours]" in {
      forAll(GoodsGen, GoodsGen, DateTimeGenerators.dateTime()) {
        (good1Generated, good2Generated, nowTime) =>
          val good1 =
            good1Generated.copy(deadline = nowTime.plusDays(1).plusHours(1))
          val good2 = good2Generated.copy(
            deadline = nowTime.plusDays(1).plusHours(1),
            user = good1.user
          )

          (time.now: () => DateTime).expects().returning(nowTime).once

          (goodsService.get _)
            .expects(*)
            .returningZ(List(good1, good2))

          (notificationsDao
            .get(_: NotificationsDao.Filter))
            .expects(*)
            .never

          (vosClient.getOffer _).expects(*, *).never

          (notificationsDao
            .insert(_: NotificationsDao.Request))
            .expects(*)
            .never

          (pushnoyClient.pushToUser _).expects(*, *).never
          task.execute().success
      }
    }

    "don't push for already pushed(have in table) user with one offer" in {
      forAll(GoodsGen, DateTimeGenerators.dateTime()) { (goodGenerated, nowTime) =>
        val good =
          goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good))

        val anyTime = DateTime.now()
        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List(UserPush(good.user, anyTime)))

        (vosClient.getOffer _).expects(*, *).never

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .never

        (pushnoyClient.pushToUser _).expects(*, *).never
        task.execute().success
      }

    }

    "don't push for already pushed(exception from dao) user with one offer" in {
      forAll(GoodsGen, DateTimeGenerators.dateTime()) { (goodGenerated, nowTime) =>
        val good =
          goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good))

        val testException = new TestException()
        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .throwingT(testException)

        (vosClient.getOffer _).expects(*, *).never

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .never

        (pushnoyClient.pushToUser _).expects(*, *).never
        task.execute().success
      }

    }

    "filter user with one not active offer" in {
      forAll(GoodsGen, NotActiveOfferGen, DateTimeGenerators.dateTime()) {
        (goodGenerated, notActiveOffer, nowTime) =>
          val good =
            goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

          (time.now: () => DateTime).expects().returning(nowTime).once

          (goodsService.get _)
            .expects(*)
            .returningZ(List(good))

          (notificationsDao
            .get(_: NotificationsDao.Filter))
            .expects(*)
            .returningT(List.empty)
            .once()

          (vosClient.getOffer _).expects(*, *).returningZ(notActiveOffer).once()

          (notificationsDao
            .insert(_: NotificationsDao.Request))
            .expects(*)
            .returningT(Unit)
            .never()

          (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).never()

          task.execute().success
      }

    }

    "filter user with one offer, when vosClient throws exception" in {
      forAll(GoodsGen, DateTimeGenerators.dateTime()) { (goodGenerated, nowTime) =>
        val good =
          goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .once()

        val testException = new TestException()
        (vosClient.getOffer _).expects(*, *).throwingZ(testException).once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .never()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).never()

        task.execute().success
      }

    }

    "don't push user if failed to insert into push_user table, for one user and one offer" in {
      forAll(GoodsGen, ActiveOfferGen, DateTimeGenerators.dateTime()) {
        (goodGenerated, activeOffer, nowTime) =>
          val good =
            goodGenerated.copy(deadline = nowTime.plusDays(1).minusHours(1))

          (time.now: () => DateTime).expects().returning(nowTime).once

          (goodsService.get _)
            .expects(*)
            .returningZ(List(good))

          (notificationsDao
            .get(_: NotificationsDao.Filter))
            .expects(*)
            .returningT(List.empty)
            .once()

          (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).once()

          (notificationsDao
            .insert(_: NotificationsDao.Request))
            .expects(*)
            .throwingT(new TestException())
            .once()

          (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).never()

          task.execute().success
      }

    }

    "should send one push, for two placements, among two corresponds offers one active and one not active" in {
      forAll(
        GoodsGen,
        GoodsGen,
        NotActiveOfferGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) {
        (
            good1Generated,
            good2Generated,
            notActiveOffer,
            activeOffer,
            nowTime
        ) =>
          val good1 =
            good1Generated.copy(deadline = nowTime.plusDays(1).minusHours(1))
          val good2 = good2Generated.copy(
            user = good1Generated.user,
            deadline = nowTime.plusDays(1).minusHours(1)
          )

          (time.now: () => DateTime).expects().returning(nowTime).once

          (goodsService.get _)
            .expects(*)
            .returningZ(List(good1, good2))

          (notificationsDao
            .get(_: NotificationsDao.Filter))
            .expects(*)
            .returningT(List.empty)
            .once()

          (vosClient.getOffer _)
            .expects(good1.offer, Slave)
            .returningZ(notActiveOffer)
            .once()

          (vosClient.getOffer _)
            .expects(good2.offer, Slave)
            .returningZ(activeOffer)
            .once()

          (notificationsDao
            .insert(_: NotificationsDao.Request))
            .expects(*)
            .returningT(Unit)
            .once()

          (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

          task.execute().success
      }

    }

    "send one push even if two placements for the same user" in {
      forAll(
        GoodsGen,
        GoodsGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (good1Generated, good2Generated, activeOffer, nowTime) =>
        //somehow fail if don't mock here
        val goodsService: GoodsService = mock[GoodsService]
        val pushnoyClient: PushnoyClient = mock[PushnoyClient]
        val notificationsDao: NotificationsDao = mock[NotificationsDao]
        val vosClient: VosClient = mock[VosClient]
        val time: Time = mock[Time]

        val task = new ResellerNeedMoneyOnCardPushTask(
          goodsService,
          pushnoyClient,
          notificationsDao,
          vosClient,
          Days.ONE,
          time
        )

        val good1 =
          good1Generated.copy(deadline = nowTime.plusDays(1).minusHours(1))
        val good2 = good2Generated.copy(
          user = good1Generated.user,
          deadline = nowTime.plusDays(1).minusHours(1)
        )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .once()

        (vosClient.getOffer _)
          .expects(*, *)
          .returningZ(activeOffer)
          .atLeastOnce()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }
    }
  }
  "ResellerNeedMoneyOnCard for two users" should {

    "all ok, should send two pushes" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .twice()

        (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).twice()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .twice()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).twice()

        task.execute().success
      }

    }

    "for one fails(already in notifications table) for other all ok, should send one push" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(where { a: NotificationsDao.Filter =>
            a match {
              case UserAndFireAtAfter(user, _) => user == good1.user
              case _ => false
            }
          })
          .returningT(List.empty)
          .once()

        val anyTime = DateTime.now()
        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(where { a: NotificationsDao.Filter =>
            a match {
              case UserAndFireAtAfter(user, _) => user == good2.user
              case _ => false
            }
          })
          .returningT(List(UserPush(good2.user, anyTime)))
          .once()

        (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }
    }

    "for one fails(failed to read for notifications table) for other all ok, should send one push" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(where { a: NotificationsDao.Filter =>
            a match {
              case UserAndFireAtAfter(user, _) => user == good1.user
              case _ => false
            }
          })
          .returningT(List.empty)
          .once()

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(where { filter: NotificationsDao.Filter =>
            filter match {
              case UserAndFireAtAfter(user, _) => user == good2.user
              case _ => false
            }
          })
          .throwingT(new TestException())
          .once()

        (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }

    }

    "for one fails(no active offer) for other all ok, should send one push" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        NotActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, notActiveOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .twice()

        (vosClient.getOffer _)
          .expects(good1.offer, Slave)
          .returningZ(activeOffer)
          .once()

        (vosClient.getOffer _)
          .expects(good2.offer, Slave)
          .returningZ(notActiveOffer)
          .once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }

    }

    "for one fail(vosClient get offer fails) for other all ok, should send one push" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .twice()

        (vosClient.getOffer _)
          .expects(good1.offer, Slave)
          .returningZ(activeOffer)
          .once()

        (vosClient.getOffer _)
          .expects(good2.offer, Slave)
          .throwingZ(new TestException())
          .once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects(*)
          .returningT(Unit)
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }

    }

    "for one fail(can't write into notifications table), for other all ok, should send one push" in {
      forAll(
        GoodsWithDifferentUserGen,
        ActiveOfferGen,
        DateTimeGenerators.dateTime()
      ) { (twoDifferentGoods, activeOffer, nowTime) =>
        val good1 =
          twoDifferentGoods._1.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )
        val good2 =
          twoDifferentGoods._2.copy(
            deadline = nowTime.plusDays(1).minusHours(1)
          )

        (time.now: () => DateTime).expects().returning(nowTime).once

        (goodsService.get _)
          .expects(*)
          .returningZ(List(good1, good2))

        (notificationsDao
          .get(_: NotificationsDao.Filter))
          .expects(*)
          .returningT(List.empty)
          .twice()

        (vosClient.getOffer _).expects(*, *).returningZ(activeOffer).twice()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects {
            where { req: NotificationsDao.Request =>
              req match {
                case NotificationsDao.Request(userId, _) =>
                  userId == good1.user
                case _ => false
              }
            }
          }
          .returningT(Unit)
          .once()

        (notificationsDao
          .insert(_: NotificationsDao.Request))
          .expects {
            where { req: NotificationsDao.Request =>
              req match {
                case NotificationsDao.Request(userId, _) =>
                  userId == good2.user
                case _ => false
              }
            }
          }
          .throwingT(new Exception())
          .once()

        (pushnoyClient.pushToUser _).expects(*, *).returningZ(1).once()

        task.execute().success
      }
    }
  }

  "ResellerNeedMoneyOnCard" should {
    "format date.month dateTime" in {
      val dt = DateTime.parse("2005-03-25")
      val r = ResellerNeedMoneyOnCardPushTask.formatDate(dt)
      r shouldBe "25.03"
    }

    "right text" in {
      import ResellerNeedMoneyOnCardPushTask._
      placementText(1) shouldBe "размещение"
      placementText(2) shouldBe "размещения"
      placementText(3) shouldBe "размещения"
      placementText(4) shouldBe "размещения"
      placementText(5) shouldBe "размещений"
      placementText(6) shouldBe "размещений"
      placementText(10) shouldBe "размещений"
      placementText(11) shouldBe "размещений"
      placementText(12) shouldBe "размещений"
      placementText(13) shouldBe "размещений"
      placementText(14) shouldBe "размещений"
      placementText(15) shouldBe "размещений"
      placementText(21) shouldBe "размещение"
      placementText(22) shouldBe "размещения"
    }

  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
