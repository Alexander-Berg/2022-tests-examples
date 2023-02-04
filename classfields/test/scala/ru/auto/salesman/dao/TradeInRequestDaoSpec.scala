package ru.auto.salesman.dao

import org.joda.time.{DateTime, LocalDate}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.dao.TradeInRequestDao.Filter.{
  CreatedBefore,
  CreatedSince,
  WithClients
}
import ru.auto.salesman.dao.TradeInRequestDao.TradeInRequestCoreData
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.{
  LimitOffset,
  Order,
  OrderBy
}
import ru.auto.salesman.model.TradeInRequest.Statuses
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{
  ClientInfo,
  ClientOfferInfo,
  ClientOfferType,
  OfferDescription,
  TradeInRequest,
  UserInfo,
  UserOfferInfo
}
import ru.auto.salesman.test.BaseSpec

trait TradeInRequestDaoSpec extends BaseSpec {

  def tradeInDao: TradeInRequestDao

  "TradeInDao" should {
    "insert new value with user car info" in {
      val clientInfo = ClientInfo(1)
      val clientOfferType = ClientOfferType(Section.NEW, Category.CARS)
      val clientOfferInfo = ClientOfferInfo(
        "1113-321",
        OfferDescription(
          Some("Kia"),
          Some("Rio"),
          Some(1991),
          Some(100501),
          Some(150001),
          Some("1234"),
          Some("1234")
        )
      )
      val userInfo = UserInfo(
        Some("Petya"),
        "+79183211233",
        Some(30154)
      )
      val suggestedOfferInfo = UserOfferInfo(
        "222",
        Category.CARS,
        OfferDescription(
          Some("Lada"),
          Some("Kalina"),
          Some(1999),
          Some(100500),
          Some(150000),
          Some("123"),
          Some("123")
        )
      )

      val tradeInRequest = new TradeInRequest(
        DateTime.now(),
        clientInfo,
        clientOfferType,
        Some(clientOfferInfo),
        userInfo,
        Some(suggestedOfferInfo)
      )

      tradeInDao.count(Nil).success.value shouldBe 5
      tradeInDao.insert(tradeInRequest).success
      tradeInDao.count(Nil).success.value shouldBe 6
    }

    "insert new value without client offer" in {
      val clientInfo = ClientInfo(1)
      val clientOfferType = ClientOfferType(Section.NEW, Category.CARS)
      val userInfo = UserInfo(
        Some("Petya"),
        "+79183211233",
        Some(30154)
      )
      val suggestedOfferInfo = UserOfferInfo(
        "222",
        Category.CARS,
        OfferDescription(
          Some("Lada"),
          Some("Kalina"),
          Some(1999),
          Some(100500),
          Some(150000),
          Some("123"),
          Some("123")
        )
      )

      val tradeInRequest = new TradeInRequest(
        DateTime.now(),
        clientInfo,
        clientOfferType,
        clientOfferInfo = None,
        userInfo,
        Some(suggestedOfferInfo)
      )

      tradeInDao.count(Nil).success.value shouldBe 5
      tradeInDao.insert(tradeInRequest).success
      tradeInDao.count(Nil).success.value shouldBe 6
    }

    "insert new value without user car info and user name" in {
      val clientInfo = ClientInfo(1)
      val clientOfferType = ClientOfferType(Section.NEW, Category.CARS)
      val clientOfferInfo = ClientOfferInfo(
        "1113-321",
        OfferDescription(
          Some("Lada"),
          Some("Kalina"),
          Some(1999),
          Some(100500),
          Some(150000),
          Some("123"),
          Some("123")
        )
      )
      val userInfo = UserInfo(
        userName = None,
        "+79183211233",
        userId = None
      )
      val tradeInRequest = new TradeInRequest(
        DateTime.now(),
        clientInfo,
        clientOfferType,
        Some(clientOfferInfo),
        userInfo,
        userOfferInfo = None
      )

      tradeInDao.count(Nil).success.value shouldBe 5
      tradeInDao.insert(tradeInRequest).success
      tradeInDao.count(Nil).success.value shouldBe 6
    }
  }

  "find records with all filters, ordered and limited" in {
    val expectedResponse = {
      val record = TradeInRequestCoreData(
        2,
        20101,
        Some(OfferIdentity("12345-6789")),
        Category.CARS,
        Section.NEW,
        Some(12345),
        Some(OfferIdentity("987654-3211")),
        Some(Category.CARS),
        "+79175576523",
        Some("Vasya"),
        Statuses.New,
        0,
        DateTime.parse("2019-01-02T20:00:00")
      )
      List(
        record,
        record.copy(id = 1, createDate = record.createDate.minusDays(1))
      )
    }
    val result = tradeInDao
      .find(
        List(
          WithClients(Iterable(20101)),
          CreatedSince(LocalDate.parse("2019-01-01")),
          CreatedBefore(LocalDate.parse("2019-01-03"))
        ),
        orderBy = Some(OrderBy("create_date", Order.Desc)),
        limitOffset = Some(LimitOffset(Some(2), Some(1)))
      )
      .success
      .value

    result.size shouldBe 2
    result shouldBe expectedResponse
  }

  "find records without filters" in {
    val expectedIds = for (i <- 1 to 5) yield i
    val result = tradeInDao
      .find(Nil)
      .success
      .value

    result.size shouldBe 5
    result.map(_.id) shouldBe expectedIds
  }

  "find records without filters with order" in {
    val expectedIds = for (i <- (1 to 5).reverse) yield i
    val result = tradeInDao
      .find(Nil, orderBy = Some(OrderBy("create_date", Order.Desc)))
      .success
      .value

    result.size shouldBe 5
    result.map(_.id) shouldBe expectedIds
  }

  "find records without filters with offset and limit" in {
    val expectedIds = for (i <- 2 to 3) yield i
    val result = tradeInDao
      .find(Nil, limitOffset = Some(LimitOffset(Some(2), Some(1))))
      .success
      .value

    result.size shouldBe 2
    result.map(_.id) shouldBe expectedIds
  }
}
