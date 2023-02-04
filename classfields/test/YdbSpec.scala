package common.zio.ydb.test

import common.zio.ydb.Ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object YdbSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("Ydb") {
      testM("Check that it starts properly") {
        for {
          ydb <- ZIO.access[Ydb](_.get)
          res <- ydb.runTx {
            ydb.execute("select 1")
          }
        } yield assert(res.resultSets)(hasSize(equalTo(1))) && assert(res.resultSets.head.getRowCount)(equalTo(1))
      }
    }.provideCustomLayerShared(TestYdb.ydb)
  }
}
