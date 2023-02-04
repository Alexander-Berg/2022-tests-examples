package ru.auto.salesman.mocks

import ru.auto.salesman.test.BaseSpec

trait BaseMock[T] extends BaseSpec {

  protected def mocked: T
  final def getMock(): T = mocked

}
