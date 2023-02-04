package ru.auto.api.util

import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions}
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.BaseSpec
import ru.auto.api.util.FutureSyntax._

import scala.concurrent.Future

class FutureSyntaxSpec extends BaseSpec with MockitoSugar {

  def runMe(callable: Callable) = Future {
    callable.callMeMaybe
    true
  }

  "future's `orElse` should not be evaluated unless condition is met" in {
    val inside = mock[Callable]
    assert(true.orElse(runMe(inside)).futureValue)
    verifyNoMoreInteractions(inside)
  }

  "future's `orElse` should be evaluated if condition is met" in {
    val inside = mock[Callable]
    assert(false.orElse(runMe(inside)).futureValue)
    verify(inside, times(1)).callMeMaybe
  }

  "future's `thenTry` should be evaluated if condition is met" in {
    val inside = mock[Callable]
    assert(true.thenTry(runMe(inside)).futureValue)
    verify(inside, times(1)).callMeMaybe
  }

  "future's `thenTry` should not be evaluated unless condition is met" in {
    val inside = mock[Callable]
    assert(!false.thenTry(runMe(inside)).futureValue)
    verifyNoMoreInteractions(inside)
  }

}

trait Callable {
  def callMeMaybe: Boolean
}
