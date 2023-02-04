package vertis.pushnoy.util

import vertis.pushnoy.model.MetricaTokenType
import vertis.pushnoy.model.MetricaTokenType.MetricaTokenType
import vertis.pushnoy.model.request.enums.ClientOS.ClientOS
import vertis.pushnoy.model.request.enums.Platform.Platform
import vertis.pushnoy.model.request.enums.{ClientOS, Platform}

import scala.util.Random

trait TestUtils {
  def randomString(n: Int = 32): String = Random.alphanumeric.take(n).mkString

  def randomStringSeq(n: Int = 64): Seq[String] = for (_ <- 1 to n) yield randomString()

  def randomPlatform: Platform = Random.shuffle(Platform.values.slice(0, 2).toList).head

  def randomClientOS: ClientOS = Random.shuffle(ClientOS.values.toList).head

  def randomMetricaTokenType: MetricaTokenType = Random.shuffle(MetricaTokenType.values.toList).head
}
