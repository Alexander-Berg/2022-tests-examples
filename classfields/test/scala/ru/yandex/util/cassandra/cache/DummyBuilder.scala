package ru.yandex.util.cassandra.cache

/**
 *
 * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
 */

import java.util

import scala.collection.JavaConverters._

object DummyBuilder extends Builder[TestKey,TestValue]{

  override def build(keys: util.Set[TestKey]) = {
    keys.asScala.map(o=>o->new TestValue(o.id, o.id+"result")).toMap.asJava
  }
}