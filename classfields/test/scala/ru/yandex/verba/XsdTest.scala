package ru.yandex.verba

import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.verba.core.util.VerbaUtils
import ru.yandex.verba.scheduler.tasks.export.ExportValidator

import scala.concurrent.duration.Duration

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 15.10.14
  */
@Ignore
class XsdTest extends AnyWordSpecLike with VerbaUtils with Matchers {

  implicit val tout = Duration("10 sec")

  "Xsd" should {
    "validate" in {
      val res = ExportValidator.validate(getClass.getResourceAsStream("/test-export.xml"))
      logger.info(s"Validation result: $res")
      res shouldEqual true
    }
  }
}
