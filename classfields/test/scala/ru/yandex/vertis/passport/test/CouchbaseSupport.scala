package ru.yandex.vertis.passport.test

import com.couchbase.client.java.Bucket
import ru.yandex.vertis.passport.dao.config.CouchbaseClusterConfig
import ru.yandex.vertis.passport.util.couchbase.CouchbaseClusterFactory

/**
  * todo
  *
  * @author zvez
  */
trait CouchbaseSupport {

  val testBucket: Bucket = CouchbaseSupport.couchTestBucket

}

object CouchbaseSupport {

  lazy val couchCluster =
    CouchbaseClusterFactory.build(CouchbaseClusterConfig(nodes = Set("couchbase-01-sas.test.vertis.yandex.net")))

  lazy val couchTestBucket = CouchbaseSupport.couchCluster.openBucket("passport_unit_test", "passport_unit_test")
}
