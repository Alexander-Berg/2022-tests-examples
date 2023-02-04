package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.{CleanableJvmFeatureInstanceDao, FeatureInstanceArchiveDaoSpec}

/** @author alex-kovalenko
  */
class JvmFeatureInstanceArchiveDaoSpec extends FeatureInstanceArchiveDaoSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val dao = new JvmFeatureInstanceArchiveDao with CleanableJvmFeatureInstanceDao
}
