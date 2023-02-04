package ru.yandex.vertis.general.darkroom.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.darkroom.model.testkit.{ComputationResultGen, DarkroomResultGen, FunctionArgumentsGen}
import ru.yandex.vertis.general.darkroom.storage.ComputationResultDao
import ru.yandex.vertis.general.darkroom.storage.ydb.YdbComputationResultDao
import ru.yandex.vertis.ydb.zio.{Tx, TxRunner}
import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object YdbComputationResultDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("YdbComputationResultDao")(
      testM("save result") {
        val genArgs = FunctionArgumentsGen.ratio.noShrink
        val results = DarkroomResultGen.ofType(ComputationResultGen.ratio).noShrink
        checkNM(1)(genArgs, results) { (args, result) =>
          for {
            dao <- ZIO.service[ComputationResultDao.Service]
            _ <- dao.put(args, result, 1.day)
          } yield assertCompletes
        }
      },
      testM("get result") {
        checkNM(1)(
          Gen.listOfN(20)(FunctionArgumentsGen.mainColor).noShrink,
          Gen.listOfN(20)(DarkroomResultGen.ofType(ComputationResultGen.mainColor)).noShrink
        ) { (args, results) =>
          for {
            dao <- ZIO.service[ComputationResultDao.Service]
            _ <- ZIO.foreach_(args.zip(results)) { case (k, v) =>
              dao.put(k, v, 2.days)
            }
            got <- dao.get(args)
          } yield assert(args.flatMap(got.get))(hasSameElements(results))
        }
      }
    ) @@ sequential)
      .provideCustomLayerShared {
        (Clock.live ++ TestYdb.ydb) >>> YdbComputationResultDao.live
      }
  }
}
