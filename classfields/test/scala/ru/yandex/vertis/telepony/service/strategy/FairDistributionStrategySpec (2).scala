package ru.yandex.vertis.telepony.service.strategy

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.util.collection.RichMap

import scala.util.Random

/**
  *
  * @author zvez
  */
class FairDistributionStrategySpec extends SpecBase with ScalaCheckPropertyChecks {

  val PosNumGen = Gen.chooseNum(1, 10)

  "weightedShuffle" should {
    "keep original elements" in {
      forAll(Gen.listOf(PosNumGen)) { xs =>
        val shuffled = FairDistributionStrategy.weightedShuffle(xs)(_.toDouble)
        shuffled should contain theSameElementsAs xs
      }
    }
    "actually shuffles" in {
      forAll(Gen.listOfN(10, PosNumGen).suchThat(xs => xs.distinct.size > 2)) { xs =>
        val tries = (1 to 10).map { _ =>
          val shuffled = FairDistributionStrategy.weightedShuffle(xs)(_.toDouble)
          shuffled != xs
        }
        tries should contain(true)
      }
    }
    "use weights" in {
      val xs = Random.shuffle(Seq(1, 3, 6))
      val heads = (1 to 1000).map { _ =>
        FairDistributionStrategy.weightedShuffle(xs)(_.toDouble).head
      }
      val counts = heads.groupBy(identity).mapValuesStrict(_.size)
      counts(6) should be > counts(3)
      counts(3) should be > counts(1)
    }
    "move zero-weighted elements to the end" in {
      forAll(Gen.listOf(PosNumGen), Gen.choose(1, 10)) { (xs, zeroesCount) =>
        val someZeroes = Seq.fill(zeroesCount)(0)
        val prepared = Random.shuffle(xs ++ someZeroes)
        val shuffled = FairDistributionStrategy.weightedShuffle(prepared)(_.toDouble)
        shuffled should contain theSameElementsAs prepared
        shuffled.takeRight(zeroesCount) should contain theSameElementsAs someZeroes
      }
    }
  }

}
