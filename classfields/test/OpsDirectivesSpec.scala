package common.akka.http.test

import common.akka.http.OpsDirectives
import zio.test._

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 31/01/2020
  */
object OpsDirectivesSpec extends DefaultRunnableSpec {

  def spec =
    suite("OpsDirectives")(
      test("simple") {
        OpsDirectives
        assertCompletes
      }
    )
}
