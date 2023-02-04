package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.application.DefaultYdbDaoSupplier
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{DataTransferEntityVersion, EntityState}
import ru.yandex.realty.rent.model.EntityProtoView._
import ru.yandex.realty.rent.proto.model.diffevent.FlatProtoView

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class EntityCacheDaoSpec extends AsyncSpecBase with TestYdbSupplier with DefaultYdbDaoSupplier with RentModelsGen {

  "EntityCacheDao.update" should {
    "not change value if Ignore is returned" in {
      val newEntity = flatProtoViewGen.next
      val newState = EntityState(newEntity, isDeleted = false, DataTransferEntityVersion(1, 2))
      entityCacheDao
        .update[FlatProtoView](newEntity.id) { oldState =>
          oldState shouldBe None
          Future.successful(EntityCacheDao.Ignore)
        }
        .futureValue

      entityCacheDao
        .update[FlatProtoView](newEntity.id) { oldState =>
          oldState shouldBe None
          Future.successful(EntityCacheDao.Update(newState))
        }
        .futureValue
    }

    "set new value in update" in {
      val newEntity = flatProtoViewGen.next
      val newState = EntityState(newEntity, isDeleted = false, DataTransferEntityVersion(3, 4))

      entityCacheDao
        .update[FlatProtoView](newEntity.id) { oldState =>
          oldState shouldBe None
          Future.successful(EntityCacheDao.Update(newState))
        }
        .futureValue

      val updatedState = newState.copy(
        newEntity.toBuilder.setAddress("tempAddress").build(),
        isDeleted = true,
        DataTransferEntityVersion(4, 5)
      )

      entityCacheDao
        .update[FlatProtoView](newEntity.id) { oldState =>
          oldState shouldBe Some(newState)
          Future.successful(EntityCacheDao.Update(updatedState))
        }
        .futureValue

      entityCacheDao
        .update[FlatProtoView](newEntity.id) { oldState =>
          oldState shouldBe Some(updatedState)
          Future.successful(EntityCacheDao.Ignore)
        }
        .futureValue
    }
  }

}
