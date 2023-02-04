package ru.yandex.util.cassandra.cache

/**
 *
 * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
 */

import org.slf4j.LoggerFactory

case class TestKey(id:String) extends CacheKey{

  val log = LoggerFactory.getLogger(classOf[TestKey])

  override def serializeId() = id
}