package ru.yandex.vertis.subscriptions

import org.scalatest.{BeforeAndAfterAll, Suite}

import java.util.concurrent.{Executors, ThreadPoolExecutor}

import scala.concurrent.ExecutionContext

/**
  *
  * @author zvez
  */
trait TestExecutionContext extends BeforeAndAfterAll { this: Suite =>

  protected lazy val ecExecutor: ThreadPoolExecutor = Executors.newFixedThreadPool(4).asInstanceOf[ThreadPoolExecutor]
  implicit protected lazy val ec: ExecutionContext = ExecutionContext.fromExecutor(ecExecutor)

  override protected def afterAll(): Unit = {
    ecExecutor.shutdown()
    super.afterAll()
  }
}
