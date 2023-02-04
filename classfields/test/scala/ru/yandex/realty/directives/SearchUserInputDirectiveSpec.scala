package ru.yandex.realty.directives

import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.message.ExtDataSchema.Tour3dMessage.Tour3dType
import ru.yandex.realty.search.common.util.HttpQueryParameters

@RunWith(classOf[JUnitRunner])
class SearchUserInputDirectiveSpec extends SpecBase with PrivateMethodTester {

  "SearchUserInputDirective " should {
    "get 3d tours from request " in {
      val map = Map(UserInputDictionary.Tour3dType -> Seq("TOUR_3D", "OVERVIEW_360").toList)
      val parameters = HttpQueryParameters(map)
      val parseTour3dType: PrivateMethod[Iterable[Tour3dType]] = PrivateMethod[Iterable[Tour3dType]]('parseTour3dType)

      val values = SearchUserInputDirective invokePrivate parseTour3dType(parameters)

      values.size shouldBe 2
      values.toSet shouldBe Set(Tour3dType.TOUR_3D, Tour3dType.OVERVIEW_360)
    }
    "throw IllegalArgumentException for unknown type " in {
      val map = Map(UserInputDictionary.Tour3dType -> Seq("TOUR_3D", "OVERVIEW_360", "SOMETHING").toList)
      val parameters = HttpQueryParameters(map)
      val parseTour3dType: PrivateMethod[Iterable[Tour3dType]] = PrivateMethod[Iterable[Tour3dType]]('parseTour3dType)

      assertThrows[IllegalArgumentException] {
        SearchUserInputDirective invokePrivate parseTour3dType(parameters)
      }
    }
  }

}
