package vertistraf.notification_center.events_broker.main.test.utils

import ru.vertistraf.notification_center.events_broker.utils.SerializationUtils._
import zio.test.environment.TestEnvironment
import zio.test.{assert, Assertion, DefaultRunnableSpec, ZSpec}

object SerializationUtilsSpec extends DefaultRunnableSpec {
  import Assertion._

  override def spec: ZSpec[TestEnvironment, Any] = suite("SerializationUtilsSpec")(
    suite("cleanQuotes")(
      test("Should clean quoters")(
        assert("123 \"\'\'abc \"".cleanQuotes)(equalTo("123 abc "))
      )
    )
  )

}
