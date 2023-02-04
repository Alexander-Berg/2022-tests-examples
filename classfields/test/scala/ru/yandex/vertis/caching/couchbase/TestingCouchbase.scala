package ru.yandex.vertis.caching.couchbase

import com.couchbase.client.java.bucket.BucketType
import com.couchbase.client.java.cluster.DefaultBucketSettings
import com.couchbase.client.java.{AsyncBucket, Bucket, CouchbaseCluster}
import org.testcontainers.couchbase.CouchbaseContainer

/**
  * @author korvit
  */
object TestingCouchbase {
  val adminName = "Administrator"
  val adminPass = "password"
  val bucketName = "unit_test"
  val password = "unit_test"

  private val couchBaseContainer = {
    val container = new CouchbaseContainer()
      .withClusterAdmin(adminName, adminPass)
      .withNewBucket(
        DefaultBucketSettings
          .builder()
          .enableFlush(true)
          .name(bucketName)
          .password(password)
          .quota(256)
          .`type`(BucketType.COUCHBASE)
          .build()
      )

    container.start()

    sys.addShutdownHook { () =>
      container.stop()
    }

    container
  }


  val cluster: CouchbaseCluster = {
    couchBaseContainer.initCluster()
    couchBaseContainer.getCouchbaseCluster
  }

  val bucket: Bucket = cluster.openBucket(bucketName, password)
  val asyncBucket: AsyncBucket =  bucket.async()
}
