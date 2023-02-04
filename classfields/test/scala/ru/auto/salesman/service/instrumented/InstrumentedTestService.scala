package ru.auto.salesman.service.instrumented

import sourcecode.{File, Name}

object InstrumentedTestService {

  def testMethod: String =
    SpanName(implicitly[File], implicitly[Name])
}
