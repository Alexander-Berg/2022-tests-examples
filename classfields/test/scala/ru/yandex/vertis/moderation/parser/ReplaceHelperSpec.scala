package ru.yandex.vertis.moderation.parser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.parser.ReplaceHelper.{IndexMappingPolicy, Patch, ReplaceResult}
import ru.yandex.vertis.moderation.util.Range
import ru.yandex.vertis.moderation.util.CollectionUtil.RichIterable

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class ReplaceHelperSpec extends SpecBase {

  "ReplaceHelper" should {
    "replace correctly" in {
      val source = "some very long text"
      val patch = Patch(Range(5, 14), "short", IndexMappingPolicy.IllegalState)
      val result = ReplaceHelper.replace(source, patch)
      val newString = "some short text"
      result.value shouldBe newString
      verify(result, newIndex = 2, originalIndex = 2)
      verify(result, newIndex = 5, originalIndex = 5)
      verify(result, newIndex = 10, originalIndex = 14)
      verify(result, newIndex = newString.length, originalIndex = source.length)
      intercept[IllegalStateException] {
        result.toOriginalIndex(7)
      }
    }

    "use custom InnerIndexMappingPolicy as well" in {
      val source = "abc двадцать один qwerty"
      val replaced = "двадцать один"
      val start = source.indexOf(replaced)
      val position = Range(start, start + replaced.length)
      val innerIndexMappingPolicy = {
        val toNewMap: Map[Int, Int] =
          Seq(
            (0 to "двадцать".length) -> 0,
            ("двадцать".length + 1 until replaced.length) -> 1
          ).toFlattenMap

        val toOriginalMap: Map[Int, Int] =
          Map(
            0 -> 0,
            1 -> ("двадцать".length + 1)
          )

        IndexMappingPolicy.FromMap(toNewMap, toOriginalMap)
      }
      val patch = Patch(position, "21", innerIndexMappingPolicy)
      val result = ReplaceHelper.replace(source, patch)
      val newString = "abc 21 qwerty"
      result.value shouldBe newString
      verify(result, newIndex = 1, originalIndex = 1)
      verify(result, newIndex = newString.length, originalIndex = source.length)
      result.toNewIndex(source.indexOf("двадцать")) shouldBe newString.indexOf("2")
      result.toNewIndex(source.indexOf("двадцать") + "двадцать".length) shouldBe newString.indexOf("2")
      result.toNewIndex(source.indexOf("один")) shouldBe newString.indexOf("1")
      result.toNewIndex(source.indexOf("н")) shouldBe newString.indexOf("1")

      result.toOriginalIndex(newString.indexOf("1")) shouldBe source.indexOf("один")
      result.toOriginalIndex(newString.indexOf("2")) shouldBe source.indexOf("двадцать")
    }
  }

  private def verify(result: ReplaceResult, originalIndex: Int, newIndex: Int) = {
    result.toOriginalIndex(newIndex) shouldBe originalIndex
    result.toNewIndex(originalIndex) shouldBe newIndex
  }
}
