package ru.yandex.vertis.promocoder.dao

/** @author alex-kovalenko
  */
trait FeatureInstanceArchiveDaoSpec extends FeatureInstanceBaseOpsBehavior {

  type D = FeatureInstanceArchiveDao

  "FeatureInstanceArchiveDao" should {
    behave.like(daoWithUpsert())
    behave.like(daoWithGet())
  }
}
