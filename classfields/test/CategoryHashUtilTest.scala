package general.bonsai.utils.test

import ru.yandex.vertis.general.bonsai.utils.CategoryHashUtil
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object CategoryHashUtilTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("CategoryHashUtilTest")(testM("Category hash") {
      for {
        category <- ZIO("mobilnie-telefoni_OobNbL")
        hash = CategoryHashUtil.getCategoryIdHash(category)
      } yield assert(hash)(equalTo(-473260091014025696L))
    })
  }
}
