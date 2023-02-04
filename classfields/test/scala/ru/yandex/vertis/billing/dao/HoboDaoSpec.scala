package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.model_core.gens.{ModerationTaskGen, Producer}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.startOfToday

import scala.util.Success

/**
  * @author ruslansd
  */
trait HoboDaoSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with AsyncSpecBase {

  protected def hoboDao: HoboDao

  "Hobo dao" should {

    "store/get tasks" in {
      val task = ModerationTaskGen.next

      (hoboDao.add(Iterable(task)) should be).a(Symbol("Success"))

      val ts = hoboDao.get(HoboDao.ForId(task.id)).futureValue
      ts.head.value shouldBe task
    }

    "get by create time" in {
      val tasks = ModerationTaskGen.next(10).toList
      val taskSet = tasks.map(_.id).toSet
      (hoboDao.add(tasks) should be).a(Symbol("Success"))

      val ts = hoboDao.get(HoboDao.CreatedSince(startOfToday())).futureValue
      ts.filter(v => taskSet(v.value.id)).map(_.value) should contain theSameElementsAs tasks
    }

    "get nothing" in {
      val ts = hoboDao.get(HoboDao.CreatedSince(DateTimeInterval.currentDay.to)).futureValue
      ts.size shouldBe 0
    }

    "close tasks" in {
      val task = ModerationTaskGen.next

      (hoboDao.add(Iterable(task)) should be).a(Symbol("Success"))

      val res1 = hoboDao.get(HoboDao.ForId(task.id)).futureValue
      res1.head.value shouldBe task

      (hoboDao.close(task.id) should be).a(Symbol("Success"))
      val res2 = hoboDao.get(HoboDao.ForId(task.id)).futureValue
      res2.head.value shouldBe task
    }

  }

}
