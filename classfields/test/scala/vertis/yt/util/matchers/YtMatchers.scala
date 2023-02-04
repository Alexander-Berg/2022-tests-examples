package vertis.yt.util.matchers

import org.scalatest.matchers.{BeMatcher, MatchResult}
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping
import vertis.yt.YtTest

import scala.util.{Failure, Success, Try}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait YtMatchers extends YtTest {

  protected val closed = new TxClosedMatcher

  protected class TxClosedMatcher extends BeMatcher[GUID] {

    def apply(left: GUID): MatchResult =
      MatchResult(
        isClosed(left),
        left.toString + " is not closed",
        left.toString + " is closed"
      )
  }

  private def isClosed(txId: GUID): Boolean =
    Try {
      ytClient.transactions().getTransaction(txId)
    } match {
      case Failure(e: YtErrorMapping.ResolveError) if e.getCode == 500 => true
      case Failure(e) => throw e
      case Success(_) => false
    }
}
