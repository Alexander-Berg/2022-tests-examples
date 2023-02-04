package ru.yandex.vertis.ydb.skypper

import java.util.concurrent.ForkJoinPool

import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait InitTestYdb extends Suite with BeforeAndAfterAll {

  abstract override protected def afterAll(): Unit = {
    super.afterAll()
    ydb.close()
  }

  implicit protected val ec: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(ForkJoinPool.commonPool())

  protected val ydb: YdbWrapper = getYdb

  private def getYdb(implicit ec: ExecutionContext): YdbWrapper = {
    TestDockerConfigBuilder.checkStartOrCreate("ydb_test", "/schema.yql")
  }
}
