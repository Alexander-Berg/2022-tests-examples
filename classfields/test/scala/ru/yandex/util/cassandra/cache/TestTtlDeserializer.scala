package ru.yandex.util.cassandra.cache

/**
 *
 * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
 */

object TestTtlDeserializer extends Deserializer[TestKey, TestValue] {


  override def toValue(id:String, data: Array[Byte]) = {
    TestValue(id, new String(data))
  }

  override def toKey(id:String, data: Array[Byte]) = {
    TestKey(id)
  }


  override def tableName = "cassandra_ttl_cache_test"

  override def serializeBuilt(key: TestKey, value: TestValue) = value.data.getBytes

  override def serializeNotFound(key: TestKey) = Array.empty
}