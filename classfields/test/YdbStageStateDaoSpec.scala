package ru.yandex.vertis.general.gost.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.gost.model.sheduler.Stage.StageId
import ru.yandex.vertis.general.gost.model.testkit.{OfferGen, StateGen}
import ru.yandex.vertis.general.gost.storage.StageStateDao
import ru.yandex.vertis.general.gost.storage.ydb.scheduler.YdbStageStateDao
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object YdbStageStateDaoSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("YdbStageStateDao")(
      testM("Запись стейта и получение стейта") {
        (checkNM(1): CheckVariants.CheckNM)(OfferGen.anyOfferId.noShrink, Gen.listOfN(2)(StateGen.any).noShrink) {
          (offerId, states) =>
            val stageId = StageId("first_test")
            for {
              initialState <- runTx(StageStateDao.get(stageId, offerId))
              _ <- runTx(StageStateDao.upsert(stageId, offerId, states.head))
              stateAfterWrite <- runTx(StageStateDao.get(stageId, offerId))
              _ <- runTx(StageStateDao.upsert(stageId, offerId, states.last))
              stateAfterRewrite <- runTx(StageStateDao.get(stageId, offerId))
            } yield assert(initialState)(isNone) &&
              assert(stateAfterWrite)(isSome(equalTo(states.head))) &&
              assert(stateAfterRewrite)(isSome(equalTo(states.last)))
        }
      },
      testM("Чтение нескольких стейтов") {
        (checkNM(1): CheckVariants.CheckNM)(OfferGen.anyOfferId.noShrink, Gen.listOfN(2)(StateGen.any).noShrink) {
          (offerId, states) =>
            val stages = List(StageId("a"), StageId("b"), StageId("c"))
            for {
              _ <- runTx(StageStateDao.upsert(stages.head, offerId, states.head))
              _ <- runTx(StageStateDao.upsert(stages(1), offerId, states(1)))
              stateMap <- runTx(StageStateDao.get(stages, offerId))
            } yield assert(stateMap)(equalTo(Map(stages.head -> states.head, stages(1) -> states(1))))
        }
      }
    ) @@ shrinks(1) @@ sequential
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbStageStateDao.live ++ Ydb.txRunner) ++ Clock.live
  }
}
