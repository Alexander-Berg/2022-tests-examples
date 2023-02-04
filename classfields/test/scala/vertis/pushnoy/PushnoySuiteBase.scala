package vertis.pushnoy

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.EitherValues
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

/** @author kusaeva
  */
trait PushnoySuiteBase
  extends AsyncFunSuite
  with ScalaFutures
  with EitherValues
  with Matchers
  with MockedCtx
  with TestEnvironment
