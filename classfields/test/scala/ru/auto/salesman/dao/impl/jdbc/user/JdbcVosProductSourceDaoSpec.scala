package ru.auto.salesman.dao.impl.jdbc.user

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import ru.auto.salesman.dao.slick.invariant.StaticQuery._
import ru.auto.salesman.dao.user.BundleDao.Condition.WithBundleIds
import ru.auto.salesman.dao.user.BundleDao.Filter.ForBundleId
import ru.auto.salesman.dao.user.GoodsDao.Condition.WithGoodsIds
import ru.auto.salesman.dao.user.GoodsDao.Filter.ForGoodsId
import ru.auto.salesman.dao.user._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.ProductStatuses._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods._
import ru.auto.salesman.model.user.{Bundle, Goods, PaidOfferProduct, VosProductSource}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

class JdbcVosProductSourceDaoSpec
    extends BaseSpec
    with BeforeAndAfter
    with SalesmanUserJdbcSpecTemplate
    with IntegrationPropertyCheckConfig
    with UserDaoGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val goodsDao = new JdbcGoodsDao(database)
  private val bundleDao = new JdbcBundleDao(database)

  private val dao = new JdbcVosProductSourceDao(database)

  import database.withTransaction

  after {
    withTransaction { implicit session =>
      updateNA("DELETE FROM goods").execute
      updateNA("DELETE FROM bundle").execute
    }
  }

  "JdbcVosProductSourceDao" should {

    "return empty list if goods and bundle tables are empty" in {
      getWaitingForPush shouldBe empty
    }

    "return good if it's not pushed yet" in {
      forAll(goodsCreateRequestGen()) { good =>
        val inserted = insert(good)
        val expected = VosProductSource(inserted)
        getWaitingForPush should contain only expected
      }
    }

    "return bundle if it's not pushed yet" in {
      forAll(bundleCreateRequestGen()) { bundle =>
        val inserted = insert(bundle)
        val expected = VosProductSource(inserted)
        getWaitingForPush should contain only expected
      }
    }

    "return both good and bundle if they're not pushed yet" in {
      forAll(
        goodsCreateRequestGen(),
        bundleCreateRequestGen()
      ) { (good, bundle) =>
        val insertedGood = insert(good)
        val insertedBundle = insert(bundle)
        val expectedGood = VosProductSource(insertedGood)
        val expectedBundle = VosProductSource(insertedBundle)
        getWaitingForPush should contain only (expectedGood, expectedBundle)
      }
    }

    "not return good if it's already been pushed" in {
      forAll(goodsCreateRequestGen()) { good =>
        val inserted = insert(good)
        markPushed(inserted)
        getWaitingForPush shouldBe empty
      }
    }

    "not return bundle if it's already been pushed" in {
      forAll(bundleCreateRequestGen()) { bundle =>
        val inserted = insert(bundle)
        markPushed(inserted)
        getWaitingForPush shouldBe empty
      }
    }

    "return only non-pushed good if pushed good exists too" in {
      // Если offerId и product у двух продуктов совпадают, уже запушенный
      // активный продукт отдаём в getWaitingForPush вместе с незапушенным.
      // Поэтому в этом тесте генерим разные product.
      forAll(
        goodsCreateRequestGen(product = Placement),
        goodsCreateRequestGen(product = Boost)
      ) { (toPush, notToPush) =>
        val pushed = insert(toPush)
        val notPushed = insert(notToPush)
        markPushed(pushed)
        val expected = VosProductSource(notPushed)
        getWaitingForPush should contain only expected
      }
    }

    "return only non-pushed bundle if pushed bundle exists too" in {
      // Если offerId и product у двух продуктов совпадают, уже запушенный
      // активный продукт отдаём в getWaitingForPush вместе с незапушенным.
      // Поэтому в этом тесте генерим разные product.
      forAll(
        bundleCreateRequestGen(product = Vip),
        bundleCreateRequestGen(product = Turbo)
      ) { (toPush, baseNotToPush) =>
        val pushed = insert(toPush)
        val notPushed = insert(baseNotToPush)
        markPushed(pushed)
        val expected = VosProductSource(notPushed)
        getWaitingForPush should contain only expected
      }
    }

    /** См. документацию в [[VosProductSourceDao]], почему селектим активную
      * услугу с теми же offer_id + product
      */
    "return already pushed active good if there is non-pushed good with the same offer + product" in {
      forAll(
        goodsCreateRequestGen(status = Active),
        goodsCreateRequestGen()
      ) { (toPush, baseNotToPush) =>
        val notToPush =
          baseNotToPush.copy(offer = toPush.offer, product = toPush.product)
        val pushed = insert(toPush)
        insert(notToPush)
        markPushed(pushed)
        val expectedPushed = VosProductSource(pushed)
        getWaitingForPush should contain(expectedPushed)
      }
    }

    /** См. документацию в [[VosProductSourceDao]], почему селектим активную
      * услугу с теми же offer_id + product
      */
    "return already pushed active bundle if there is non-pushed bundle with the same offer + product" in {
      forAll(
        bundleCreateRequestGen(status = Active),
        bundleCreateRequestGen()
      ) { (toPush, baseNotToPush) =>
        val notToPush =
          baseNotToPush.copy(offer = toPush.offer, product = toPush.product)
        val pushed = insert(toPush)
        insert(notToPush)
        markPushed(pushed)
        val expectedPushed = VosProductSource(pushed)
        getWaitingForPush should contain(expectedPushed)
      }
    }

    "not mark good pushed if it's status changed" in {
      forAll(goodsCreateRequestGen(status = Active)) { good =>
        val outdated = insert(good)
        deactivate(outdated)
        val updated = getGood(outdated.id)
        markPushed(outdated)
        val expectedUpdated = VosProductSource(updated)
        getWaitingForPush should contain(expectedUpdated)
      }
    }

    "not mark bundle pushed if it's status changed" in {
      forAll(bundleCreateRequestGen(status = Active)) { bundle =>
        val outdated = insert(bundle)
        deactivate(outdated)
        val updated = getBundle(outdated.id)
        markPushed(outdated)
        val expectedUpdated = VosProductSource(updated)
        getWaitingForPush should contain(expectedUpdated)
      }
    }

    // Нет аналогичного теста для bundle по двум причинам:
    // 1. В BundleDao нет запросов, обновляющих только deadline
    // 2. Код для goods и bundle одинаковый
    "not mark good pushed if it's deadline changed" in {
      forAll(goodsCreateRequestGen(status = Active)) { good =>
        val outdated = insert(good)
        increaseDeadline(outdated, outdated.deadline.plusWeeks(1))
        val updated = getGood(outdated.id)
        markPushed(outdated)
        val expectedUpdated = VosProductSource(updated)
        getWaitingForPush should contain(expectedUpdated)
      }
    }
  }

  private def insert(good: GoodsDao.Request) =
    goodsDao.insertIfNotExists(good).success.value

  private def insert(bundle: BundleDao.Request) =
    bundleDao.insertIfNotExists(bundle).success.value

  private def deactivate(good: Goods) =
    goodsDao
      .update(WithGoodsIds(good.id), GoodsDao.Patch.Deactivate(Inactive))
      .success

  private def deactivate(bundle: Bundle) =
    bundleDao
      .update(
        WithBundleIds(bundle.id),
        BundleDao.Patch.Deactivate(Inactive, deadline = now())
      )
      .success

  private def increaseDeadline(good: Goods, deadline: DateTime) =
    goodsDao
      .update(WithGoodsIds(good.id), GoodsDao.Patch.IncreaseDeadline(deadline))
      .success

  private def getGood(id: String) =
    goodsDao.get(ForGoodsId(id)).success.value.head

  private def getBundle(id: String) =
    bundleDao.get(ForBundleId(id)).success.value.head

  private def markPushed(product: PaidOfferProduct): Unit =
    dao.markPushed(List(VosProductSource(product))).success.value

  private def getWaitingForPush =
    dao.getWaitingForPush.success.value
}
