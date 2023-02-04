package ru.yandex.vertis.general.search.logic.test

import general.common.seller_model.SellerId
import general.search.model.SearchSnippet
import ru.yandex.vertis.general.search.logic.Shuffles
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._

object ShufflesTest extends DefaultRunnableSpec {

  private def searchSnippet(offerId: String, seller: SellerId) =
    SearchSnippet(offerId = offerId, sellerId = Some(seller))

  private def isShuffled(window: Int)(snippets: Seq[SearchSnippet]) = {
    snippets
      .sliding(window, 1)
      .forall(window => window.flatMap(_.sellerId).distinct.size == window.size)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Shuffle")(
      test("UserDiversityShuffle best case") {
        val initial = (0 to 20).map { i =>
          searchSnippet(s"offer_$i", SellerId(SellerId.SellerId.UserId(10 + i / 2)))
        }
        val window = 5
        val shuffled = isShuffled(window)(Shuffles.userDiversity(initial))
        val notShuffled = !isShuffled(window)(initial)

        assert(shuffled)(isTrue) && assert(notShuffled)(isTrue)
      },
      test("UserDiversityShuffle worse case") {
        val initial = (1 to 10).map { i =>
          searchSnippet(s"offer_$i", SellerId(SellerId.SellerId.UserId(10 + i / 3)))
        }
        val window = 3
        val shuffled = isShuffled(window)(Shuffles.userDiversity(initial))
        val notShuffled = !isShuffled(window)(initial)

        assert(shuffled)(isTrue) && assert(notShuffled)(isTrue)
      },
      test("UserDiversityShuffle should be stable") {
        val initial = (1 to 10).map { i =>
          searchSnippet(s"offer_$i", SellerId(SellerId.SellerId.UserId(10 + i)))
        }
        val shuffled = Shuffles.userDiversity(initial)

        assert(shuffled)(equalTo(initial))
      }
    )
  }
}
