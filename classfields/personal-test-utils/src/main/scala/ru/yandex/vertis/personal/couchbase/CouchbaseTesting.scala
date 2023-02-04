package ru.yandex.vertis.personal.couchbase

import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import com.couchbase.client.java.bucket.BucketType
import com.couchbase.client.java.cluster.DefaultBucketSettings
import org.slf4j.LoggerFactory
import org.testcontainers.couchbase.CouchbaseContainer

/**
  * Couchbase testing stuff.
  *
  * @author tolmach
  */
object CouchbaseTesting {

  private val AdministratorName = "administrator"
  private val AdministratorPassword = "password must be at least six characters"
  private val TestBucketName = "unit-test"
  private val TestBucketPassword = "password must be at least six characters"

  private val log = LoggerFactory.getLogger(this.getClass)

  private val container: CouchbaseContainer = {
    val bucketSettings = DefaultBucketSettings
      .builder()
      .enableFlush(true)
      .name(TestBucketName)
      .password(TestBucketPassword)
      .quota(100)
      .`type`(BucketType.COUCHBASE)
      .build()

    val container = new CouchbaseContainer()
      .withClusterAdmin(AdministratorName, AdministratorPassword)
      .withNewBucket(bucketSettings)
      .withStartupAttempts(5)

    log.info("Couchbase container starting...")
    container.start()
    log.info("Couchbase container started")

    sys.addShutdownHook { () =>
      log.info("Couchbase container stopping...")
      container.stop()
      log.info("Couchbase container stopped")
    }

    container
  }

  /**
    * Couchbase bucket for unit tests running.
    */
  private val cluster: CouchbaseCluster = {
    container.initCluster()
    container.getCouchbaseCluster
  }

  val TestBucket: AsyncBucket = {
    cluster.openBucket(TestBucketName, TestBucketPassword).async()
  }

}
