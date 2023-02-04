package ru.yandex.vertis.general.wizard.core.utils

import zio.test._
import zio.test.Assertion._
import SeqUtils._

object SeqWithPercentileSpec extends DefaultRunnableSpec {

  private val seqGen = Gen.int(1, 100).flatMap(size => Gen.listOfN(size)(Gen.int(0, 10000)))
  private val percentileGen = Gen.double(0, 100)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SeqWithPercentile") {
      testM("Should correctly get percentile value") {
        checkM(seqGen, percentileGen) { case (seq, percentile) =>
          for {
            percentileValue <- seq.percentile(percentile)
          } yield assert(seq.count(_ <= percentileValue).toDouble)(isGreaterThanEqualTo(seq.length * percentile / 100))
        }
      }
    }
}
