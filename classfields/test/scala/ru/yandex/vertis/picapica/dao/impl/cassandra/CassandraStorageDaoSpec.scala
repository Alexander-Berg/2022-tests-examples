package ru.yandex.vertis.picapica.dao.impl.cassandra

import com.codahale.metrics.MetricRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.monitoring.Metrics
import ru.yandex.vertis.picapica.dao.impl.StorageDaoSpec
import ru.yandex.vertis.picapica.metered.MeteredStorageDao
import ru.yandex.vertis.picapica.model.{Id, Realty}

/**
  * @author evans
  */
//todo migrage to testcontainers
//@RunWith(classOf[JUnitRunner])
class CassandraStorageDaoSpec extends StorageDaoSpec
    with CassandraSpecTemplate {

  lazy val service = Realty

  override def idToUrlPart(id: Id): String = service.toUrlPart(id)

  val dao = new CassandraStorageDao(session, service, initSchema = true)
      with MeteredCassandraStorageDao
      with MeteredStorageDao {
    override def metricRegistry: MetricRegistry = Metrics.defaultRegistry()
  }

  override protected def clear(): Unit = {
    session.execute("truncate table pica_pictures")
  }
}

