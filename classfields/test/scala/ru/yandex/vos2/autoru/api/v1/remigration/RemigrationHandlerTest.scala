package ru.yandex.vos2.autoru.api.v1.remigration

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.old.{AutoruSalesDaoCommon, MigrationSalesCommon}
import ru.yandex.vos2.autoru.model.AutoruOfferID
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.dao.utils.SimpleRowMapper
import ru.yandex.vos2.model.ModelUtils.RichOffer
import ru.yandex.vos2.util.RandomUtil

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 7/6/17.
  */
@RunWith(classOf[JUnitRunner])
class RemigrationHandlerTest extends AnyFunSuite with InitTestDbs with Vos2ApiSuite with BeforeAndAfterAll {

  initDbs()
  initNewOffersDbs()
  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"remigration test (category = $category)") {
    clearChangeLong(category)
    val ids = validIds(category).sortBy(_.id)
    assume(ids.nonEmpty)
    val validId1 = RandomUtil.choose(ids)
    val validId2 = RandomUtil.choose(ids)
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/$category/${validId1.toPlain},${validId2.toPlain}"))
    checkId(validId1, category, added = true)
    checkId(validId2, category, added = true)
    val invalidId = ids.last.copy(id = ids.last.id + 1)
    checkErrorRequest(
      Post(s"/api/v1/remigration/$category/${invalidId.toPlain}"),
      StatusCodes.NotFound,
      unknownOfferError
    )
    checkId(invalidId, category, added = false)
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/$category/clean"))
    checkLogEmpty(category, needEmpty = true)
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/$category/show,hidden,expired"))
    checkLogEmpty(category, needEmpty = false)
    val count = (checkSuccessJsonValueRequest(Get(s"/api/v1/remigration/$category")) \ "count").as[Int]
    assert(count > 0)
    // 'all' operations
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/all/clean"))
    checkLogEmpty(category, needEmpty = true)
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/all/${validId1.toPlain},${validId2.toPlain}"))
    checkSimpleSuccessRequest(Post(s"/api/v1/remigration/all/show,hidden,expired"))
    checkLogEmpty(category, needEmpty = false)
    val allCount = (checkSuccessJsonValueRequest(Get(s"/api/v1/remigration/all")) \ "count").as[Int]
    assert(allCount > 0)
  }

  private def clearChangeLong(category: String): Unit = {
    def clear(dao: MigrationSalesCommon[_]): Unit = {
      val table = dao.changeLogTable
      dao.salesDao.shard.master.jdbc.update(s"delete from $table")
    }

    category match {
      case "cars" =>
        clear(components.migrationSalesDao)
      case "trucks" =>
        clear(components.migrationTruckSalesDao)
      case "moto" =>
        clear(components.migrationMotoSalesDao)
    }
  }

  private def makeOfferCopiesInDb(offer: Offer): Seq[Offer] = {
    // создадим две копии, созданные на день раньше и на два дня раньше, и одну, созданную на 61 день раньше
    val user = offer.getUser
    def newOffer(day: Int): Offer = {
      offer.toBuilder.setTimestampCreate(new DateTime(offer.getTimestampCreate).minusDays(day).getMillis).build()
    }
    val offer1 = components.getOfferDao().create(user, newOffer(1))(Traced.empty)
    val offer2 = components.getOfferDao().create(user, newOffer(2))(Traced.empty)
    val offer3 = components.getOfferDao().create(user, newOffer(61))(Traced.empty)
    Seq(offer1, offer2, offer3)
  }

  private def validIds(category: String): Seq[AutoruOfferID] = {
    def getIds(dao: AutoruSalesDaoCommon[_], table: String): Seq[AutoruOfferID] = {
      val offer1 = getOfferById(1042409964L).toBuilder.setTimestampCreate(System.currentTimeMillis()).build()
      val offers = makeOfferCopiesInDb(offer1)

      assert(offers.nonEmpty)
      offers.map(offer => AutoruOfferID.parse(offer.getOfferID))
    }
    category match {
      case "cars" =>
        getIds(components.autoruSalesDao, "all7.sales")
      case "trucks" =>
        getIds(components.autoruTrucksDao, "all.sale3")
      case "moto" =>
        getIds(components.autoruMotoDao, "all.sale5")
    }
  }

  private def checkId(id: AutoruOfferID, category: String, added: Boolean): Unit = {
    def checkId(dao: MigrationSalesCommon[_]): Boolean = {

      val offer =
        components.getOfferDao().findById(id.toPlain, includeRemoved = true, operateOnMaster = true)(Traced.empty)
      offer.exists(_.hasFlag(OfferFlag.OF_NEED_REMIGRATION))
    }
    category match {
      case "cars" =>
        assert(checkId(components.migrationSalesDao) == added)
      case "trucks" =>
        assert(checkId(components.migrationTruckSalesDao) == added)
      case "moto" =>
        assert(checkId(components.migrationMotoSalesDao) == added)
    }
  }

  private def checkLogEmpty(category: String, needEmpty: Boolean): Unit = {
    def checkEmpty(dao: MigrationSalesCommon[_]): Unit = {
      val table = dao.changeLogTable
      val count = dao.salesDao.shard.master.jdbc
        .query(s"select count(*) from $table", SimpleRowMapper(rs => Int.box(rs.getInt(1))))
        .asScala
        .head
        .toInt
      val isEmpty: Boolean = count == 0
      assert(isEmpty == needEmpty)
    }

    category match {
      case "cars" =>
        checkEmpty(components.migrationSalesDao)
      case "trucks" =>
        checkEmpty(components.migrationTruckSalesDao)
      case "moto" =>
        checkEmpty(components.migrationMotoSalesDao)
    }
  }
}
