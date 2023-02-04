package ru.auto.salesman.dao

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.dao.VinHistoryDao.{Filter, Source}
import ru.auto.salesman.test.BaseSpec

trait VinHistoryDaoSpec extends BaseSpec {

  def vinHistoryDao: VinHistoryDao

  "VinHistoryDao" should {
    "get records with filter" in {
      val user = AutoruUser(123L)
      val anotherUser = AutoruUser(321L)

      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = None
        )
      }

      vinHistoryDao.put {
        Source(
          anotherUser,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = None
        )
      }

      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = None
        )
      }

      val results =
        vinHistoryDao
          .get(
            NonEmptyList.of(Filter.ForUser(user)),
            order = None,
            limitOffset = None
          )
          .success
          .value

      results.size shouldBe 2

      results.foreach { result =>
        result.userRef shouldBe user
      }
    }

    "correctly save/read garage_id" in {
      val user = AutoruUser(123L)
      val garageId = "123"
      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now,
          garageId = None
        )
      }
      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now,
          garageId = Some(garageId)
        )
      }
      val results = vinHistoryDao
        .get(
          NonEmptyList.of(Filter.ForUser(user)),
          order = None,
          limitOffset = None
        )
        .success
        .value

      results.size shouldBe 2
      results.flatMap(_.garageId).toList shouldBe List(garageId)
    }

    "count records" in {
      val user = AutoruUser(123L)
      val anotherUser = AutoruUser(321L)

      vinHistoryDao
        .count {
          NonEmptyList.of(Filter.ForUser(user))
        }
        .success
        .value shouldBe 0

      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = Some("123")
        )
      }

      vinHistoryDao.put {
        Source(
          anotherUser,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = None
        )
      }

      vinHistoryDao.put {
        Source(
          user,
          "VIN123",
          offerId = None,
          holdId = None,
          DateTime.now(),
          garageId = None
        )
      }

      vinHistoryDao
        .count {
          NonEmptyList.of(Filter.ForUser(user))
        }
        .success
        .value shouldBe 2
    }
  }
}
