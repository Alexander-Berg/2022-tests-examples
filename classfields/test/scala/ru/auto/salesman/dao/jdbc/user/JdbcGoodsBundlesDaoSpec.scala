package ru.auto.salesman.dao.jdbc.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.impl.jdbc.user.{
  JdbcBundleDao,
  JdbcGoodsBundlesDao,
  JdbcGoodsDao
}
import ru.auto.salesman.dao.user.{
  BundleDao,
  GoodsBundlesDao,
  GoodsBundlesDaoSpec,
  GoodsDao
}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.salesman.dao.user.GoodsDao.Filter.{ForActiveProductUserOffer, ForGoodsId}
import ru.auto.salesman.model.{DeprecatedDomain, ProductStatuses, UserId}
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.ProductStatuses.Active
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Vip
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.yandex.vertis.generators.BasicGenerators

class JdbcGoodsBundlesDaoSpec
    extends GoodsBundlesDaoSpec
    with BeforeAndAfter
    with SalesmanUserJdbcSpecTemplate
    with IntegrationPatience
    with UserDaoGenerators
    with PushedToVosCheck
    with BasicGenerators {
  def newDao(): GoodsBundlesDao = new JdbcGoodsBundlesDao(database)
  def goodDao = new JdbcGoodsDao(database)
  def bundleDao = new JdbcBundleDao(database)

  implicit override def domain: DeprecatedDomain = AutoRu

  after {
    database.withSession { session =>
      session.conn.prepareStatement("DELETE FROM goods").execute()
      session.conn.prepareStatement("DELETE FROM bundle").execute()
    }
  }

  private val dao = newDao()

  "JdbcGoodsBundlesDao" should {

    "update placement by vip" in {
      val offerId = OfferIdentityGen.next
      val timeInterval = dateTimeIntervalGen.next
      val userId = AutoruUserIdGen.next
      val placement = placementGen(offerId, userId, timeInterval.from).next
      val vip = vipGen(offerId, timeInterval.to, timeInterval.to).next
      val inserted = goodDao.insertIfNotExists(placement).success.value
      setPushedToVos(inserted.id, pushedToVos = true)
      bundleDao.insertIfNotExists(vip).success
      dao.updatePlacementByVip(timeInterval.from.minus(50000)).success
      val res = goodDao
        .get(ForActiveProductUserOffer(Placement, userId, offerId))
        .success
        .value
      val deadline = res.head.deadline

      deadline shouldBe timeInterval.to
      getPushedToVos(inserted.id) shouldBe false
    }

    "not update placement by vip because vip activated_at is after release date" in {
      val offerId = OfferIdentityGen.next
      val timeInterval = dateTimeIntervalGen.next
      val userId = AutoruUserIdGen.next
      val placement = placementGen(offerId, userId, timeInterval.from).next
      val vip = vipGen(offerId, timeInterval.to, timeInterval.to).next
      val inserted = goodDao.insertIfNotExists(placement).success.value
      bundleDao.insertIfNotExists(vip).success
      val pushedToVos = bool.next
      setPushedToVos(inserted.id, pushedToVos)
      dao.updatePlacementByVip(timeInterval.to.plus(50000))
      val res = goodDao
        .get(ForActiveProductUserOffer(Placement, userId, offerId))
        .success
        .value
      val deadline = res.head.deadline

      deadline shouldBe timeInterval.from
      getPushedToVos(inserted.id) shouldBe pushedToVos
    }

    "not update placement by vip because different offer_id's" in {
      val offerId1 = OfferIdentityGen.next
      val offerId2 = OfferIdentityGen.next
      if (offerId1 != offerId2) {
        val timeInterval = dateTimeIntervalGen.next
        val userId = AutoruUserIdGen.next
        val placement = placementGen(offerId1, userId, timeInterval.from).next

        val vip = vipGen(offerId2, timeInterval.to, timeInterval.to).next
        val inserted = goodDao.insertIfNotExists(placement).success.value
        bundleDao.insertIfNotExists(vip).success
        val pushedToVos = bool.next
        setPushedToVos(inserted.id, pushedToVos)
        dao.updatePlacementByVip(timeInterval.from.minus(50000))
        val res = goodDao
          .get(ForActiveProductUserOffer(Placement, userId, offerId1))
          .success
          .value
        val deadline = res.head.deadline

        deadline shouldBe timeInterval.from
        getPushedToVos(inserted.id) shouldBe pushedToVos
      }
    }

    "not update inactive placement" in {
      val offerId = OfferIdentityGen.next
      val timeInterval = dateTimeIntervalGen.next
      val userId = AutoruUserIdGen.next
      val placement = placementGen(offerId, userId, timeInterval.from)
        .map(_.copy(status = ProductStatuses.Inactive))
        .next
      val vip = vipGen(offerId, timeInterval.to, timeInterval.to).next
      for {
        inserted <- goodDao.insertIfNotExists(placement)
        _ <- bundleDao.insertIfNotExists(vip)
        _ <- dao.updatePlacementByVip(timeInterval.from.minus(50000))
        pushedToVos = bool.next
        _ = setPushedToVos(inserted.id, pushedToVos)
        res = goodDao
          .get(ForGoodsId(inserted.id))
          .success
          .value
      } yield {
        res.head.deadline shouldBe timeInterval.from
        getPushedToVos(inserted.id) shouldBe pushedToVos
      }
    }.success

    "not update placement by inactive vip" in {
      val offerId = OfferIdentityGen.next
      val timeInterval = dateTimeIntervalGen.next
      val userId = AutoruUserIdGen.next
      val placement = placementGen(offerId, userId, timeInterval.from).next
      val vip = vipGen(offerId, timeInterval.to, timeInterval.to)
        .map(_.copy(status = ProductStatuses.Inactive))
        .next

      for {
        inserted <- goodDao.insertIfNotExists(placement)
        _ <- bundleDao.insertIfNotExists(vip)
        _ <- dao.updatePlacementByVip(timeInterval.from.minus(50000))
        pushedToVos = bool.next
        _ = setPushedToVos(inserted.id, pushedToVos)
        res = goodDao
          .get(ForGoodsId(inserted.id))
          .success
          .value
      } yield {
        res.head.deadline shouldBe timeInterval.from
        getPushedToVos(inserted.id) shouldBe pushedToVos
      }

    }.success

  }

  private def placementGen(
      offerId: OfferIdentity,
      userId: UserId,
      deadline: DateTime
  ): Gen[GoodsDao.Request] =
    goodsCreateRequestGen(
      offerId = offerId,
      userId = userId,
      product = Placement,
      status = Active,
      deadline = deadline
    )

  private def vipGen(
      offerId: OfferIdentity,
      activated: DateTime,
      deadline: DateTime
  ): Gen[BundleDao.Request] =
    bundleCreateRequestGen(
      offerId = offerId,
      product = Vip,
      status = Active,
      activated = activated,
      deadline = deadline
    )

  override protected def pushedToVosCheckTable: String = "goods"
}
