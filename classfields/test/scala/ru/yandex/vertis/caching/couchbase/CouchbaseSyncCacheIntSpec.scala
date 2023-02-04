package ru.yandex.vertis.caching.couchbase

import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{SyncCache, SyncCacheSpec}

import scala.util.Random


/**
  * @author korvit
  */
class CouchbaseSyncCacheIntSpec
  extends SyncCacheSpec {

  override protected val syncCache: SyncCache[String, Int] =
    new CouchbaseSyncCache(
      bucket = TestingCouchbase.bucket,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.couchbase.sync.${Random.nextInt()}"
      })
}
