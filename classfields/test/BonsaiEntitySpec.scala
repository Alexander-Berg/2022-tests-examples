package ru.yandex.vertis.general.bonsai.model.test

import general.bonsai.category_model.Category
import ru.yandex.vertis.general.bonsai.model.BonsaiEntity._
import zio.test.Assertion._
import zio.test._

object BonsaiEntitySpec extends DefaultRunnableSpec {

  val category: Category = Category(id = "test_id", version = 123)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BonsaiEntity") {
      test("converts to json properly")(assert(category.json)(equalTo("""{"id":"test_id","version":"123"}""")))
    }
}
