package ru.auto.salesman.test

import scala.concurrent.ExecutionContext

trait TestExecutionContext {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
}
