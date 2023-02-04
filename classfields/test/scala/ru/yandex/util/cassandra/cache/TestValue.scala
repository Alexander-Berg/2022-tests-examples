package ru.yandex.util.cassandra.cache

/**
 *
 * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
 */

import org.slf4j.LoggerFactory

case class TestValue(result:String, data:String) extends CacheValue{

  val log = LoggerFactory.getLogger(classOf[TestValue])

}