package ru.yandex.vertis.punisher.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.util.CollectionUtils._

@RunWith(classOf[JUnitRunner])
class CollectionUtilsSpec extends BaseSpec {

  private val Min: Int = 0
  private val Max: Int = 100

  private val nonEmptyList: List[Int] = (Min to Max).toList
  private val emptyList: List[Int] = List.empty

  "RichOrdering" should {
    "max with non empty list" in {
      nonEmptyList.maxOpt shouldBe Some(Max)
    }

    "max with empty list" in {
      emptyList.maxOpt shouldBe None
    }

    "min with non empty list" in {
      nonEmptyList.minOpt shouldBe Some(Min)
    }

    "min with empty list" in {
      emptyList.minOpt shouldBe None
    }
  }
}
