package ru.yandex.verba.core.preprocessor.validation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.preprocessor.validation.validator.{CountValidator, InseparablesValidator}
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 25.06.14
  */
class TransformersTest extends AnyFlatSpec with Matchers with VerbaUtils {
  val ref = new OnSaveTransformers()

  "Validators " should " create inseparables for string 'INSEPARABLES [one] [two]'" in {
    val InseparablesValidator(lst) = ref.getGroupAttrsTransformer("INSEPARABLES", Set("one", "two"))
    lst shouldEqual Set("one", "two")
  }
  "Validators " should " create count validator for string 'MAXCOUNT 2'" in {
    val CountValidator("a", 2) = ref.getSingleAttrTransformer("MAXCOUNT 2", "a", Path("/auto"))
  }
}
