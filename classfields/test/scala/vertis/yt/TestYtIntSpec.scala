package vertis.yt

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.inside.yt.kosher.cypress.YPath

/** Tests of yt container
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
class TestYtIntSpec extends AnyWordSpec with Matchers with YtTest {

  "yt" should {
    "work" in {
      ytClient.cypress().list(YPath.cypressRoot) should not be empty
    }
  }
}
