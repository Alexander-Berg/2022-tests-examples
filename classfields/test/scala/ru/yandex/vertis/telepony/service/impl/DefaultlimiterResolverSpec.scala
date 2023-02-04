package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.slf4j.LoggerFactory
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, Block, Pass}

/**
  * @author @logab
  */
class DefaultlimiterResolverSpec extends SpecBase with ScalaCheckDrivenPropertyChecks {
  val log = LoggerFactory.getLogger(classOf[DefaultlimiterResolverSpec])
  val resolver = DefaultLimiterResolver

  val resolutionPairGen = for {
    option <- Gen.oneOf(AntiFraudOptions.values.toSeq)
    resolution <- Gen.oneOf(Block, Pass)
  } yield (option, resolution)

  val resolutionsGen = for {
    size <- Gen.choose(0, 20)
    resolutions <- Gen.listOfN(size, resolutionPairGen)
  } yield resolutions
  "DefaultLimiterResolver" should {
    "pass when contains whitelist pass signal" in {
      forAll(resolutionsGen) { resolutions =>
        resolver.resolve((AntiFraudOptions.WhiteList, Pass) :: resolutions) shouldEqual Pass
      }
    }
    "pass when no block signals" in {
      forAll(resolutionsGen) { resolutions =>
        resolver.resolve(resolutions.filterNot(_._2 == Block)) shouldEqual Pass
      }
    }
    "pass when no signals at all" in {
      resolver.resolve(Iterable.empty) shouldEqual Pass

    }
    "block when no whitelist signal and at least one block signal" in {
      forAll(resolutionsGen) { resolutions =>
        resolver.resolve(
          ((AntiFraudOptions.Blacklist, Block) :: resolutions)
            .filterNot(_ == ((AntiFraudOptions.WhiteList, Pass)))
        ) shouldEqual Block
      }
    }
  }

}
