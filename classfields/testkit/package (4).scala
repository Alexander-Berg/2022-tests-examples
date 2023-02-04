package ru.yandex.vertis.general.personal.storage

import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.ydb.zio.{Tx, TxRunner}
import zio.ZIO
import zio.clock.Clock

package object testkit {

  def runTx[R <: Clock with HasTxRunner, E, A](action: Tx[R, E, A]): ZIO[R, E, A] = {
    for {
      runner <- ZIO.service[TxRunner]
      result <- runner.runTx(action).flatMapError(e => e.get.orDie)
    } yield result
  }
}
