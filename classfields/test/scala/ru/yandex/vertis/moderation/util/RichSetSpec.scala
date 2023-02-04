package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.util.CollectionUtil.RichSet

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class RichSetSpec extends SpecBase {

  "containsOneOf" should {
    val params =
      Seq[(Set[Int], Set[Int], Boolean)](
        (Set(1), Set.empty, false),
        (Set.empty, Set(1), false),
        (Set.empty, Set.empty, false),
        (Set(1, 2), Set(3), false),
        (Set(1, 2), Set(2), true)
      )

    params.foreach { case (left, right, expectedResult) =>
      s"return $expectedResult for left=$left, right=$right" in {
        (left containsOneOf right) shouldBe expectedResult
      }
    }
  }

  "containsAllOf" should {
    val params =
      Seq[(Set[Int], Set[Int], Boolean)](
        (Set(1), Set.empty, true),
        (Set.empty, Set(1), false),
        (Set.empty, Set.empty, true),
        (Set(1, 2), Set(1, 2, 3), false),
        (Set(1, 2), Set(1, 2), true)
      )

    params.foreach { case (left, right, expectedResult) =>
      s"return $expectedResult for left=$left, right=$right" in {
        (left containsAllOf right) shouldBe expectedResult
      }
    }
  }
}
