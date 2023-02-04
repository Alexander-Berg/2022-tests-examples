package ru.yandex.vertis.caching.couchbase

import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{AsyncCache, AsyncCacheSpec}

import scala.util.Random

/**
  * @author korvit
  */
class CouchbaseAsyncCacheIntSpec
  extends AsyncCacheSpec {

  override protected val asyncCache: AsyncCache[String, Int] =
    new CouchbaseAsyncCache(
      bucket = TestingCouchbase.asyncBucket,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.couchbase.async.${Random.nextInt()}"
      })
}
