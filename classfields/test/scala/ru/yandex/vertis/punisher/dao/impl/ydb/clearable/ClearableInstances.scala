package ru.yandex.vertis.punisher.dao.impl.ydb.clearable

import ru.yandex.vertis.punisher.dao.impl.ydb.{YdbAutoruOffersStateDaoImpl, YdbAutoruUserLastYandexUidTsDao}
import ru.yandex.vertis.quality.cats_utils.Awaitable
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.DefaultYdbWrapper

object ClearableInstances {
  implicit def ydbAutoruUserLastYandexUidTsDao[F[_]: Awaitable, A[_], B[_]](
      implicit ydb: DefaultYdbWrapper[F]
  ): Clearable[YdbAutoruUserLastYandexUidTsDao[A, B]] =
    () => ydb.runTx(ydb.execute(s"DELETE FROM ${YdbAutoruUserLastYandexUidTsDao.TableName}")).await

  implicit def ydbAutoruOffersStateDaoImpl[F[_]: Awaitable, A[+_], B[_]](
      implicit ydb: DefaultYdbWrapper[F]
  ): Clearable[YdbAutoruOffersStateDaoImpl[A, B]] =
    () => ydb.runTx(ydb.execute(s"DELETE FROM ${YdbAutoruOffersStateDaoImpl.TableName}")).await
}
