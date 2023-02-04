package ru.yandex.vertis.moderation

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.api.common.functions.util.ListCollector

import scala.collection.JavaConverters._

/**
  * @author potseluev
  */
object TestUtils {

  implicit def asScalaFlatMap[IN, OUT](function: FlatMapFunction[IN, OUT])(input: IN): Iterable[OUT] = {
    val buffer = new java.util.ArrayList[OUT]()
    val collector = new ListCollector[OUT](buffer)
    function.flatMap(input, collector)
    buffer.asScala
  }
}
