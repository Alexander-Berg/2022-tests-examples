package common.zio

import zio.Exit
import zio.test.Assertion
import zio.test.Assertion.{anything, dies, fails, isSubtype}

import scala.reflect.ClassTag

package object testkit {

  def failsWith[A: ClassTag]: Assertion[Exit[Any, Any]] =
    fails(isSubtype[A](anything))

  def diesWith[A: ClassTag]: Assertion[Exit[Any, Any]] =
    dies(isSubtype[A](anything))
}
