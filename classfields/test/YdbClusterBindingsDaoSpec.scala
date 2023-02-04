package ru.yandex.vertis.general.aglomerat.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.aglomerat.model.{AglomeratError, ClusterType, OfferId}
import ru.yandex.vertis.general.aglomerat.storage.ClusterBindingsDao
import zio.test.DefaultRunnableSpec
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.aglomerat.storage.ydb.YdbClusterBindingsDao
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import zio.test._
import zio.test.Assertion._

object YdbClusterBindingsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("YdbClusterBindingsDaoSpec")(
      testM("save and get binding") {
        val to = OfferId("to")
        for {
          _ <- runTx(ClusterBindingsDao.bindOffer(ClusterType.FullDuplicates, OfferId("from"), to))
          boundTo <- runTx(ClusterBindingsDao.getBinding(ClusterType.FullDuplicates, OfferId("from")))
        } yield assertTrue {
          boundTo.contains(to)
        }
      },
      testM("delete binding") {
        val from = OfferId("to_bind")
        val to = OfferId("bind_to")
        for {
          _ <- runTx(ClusterBindingsDao.bindOffer(ClusterType.FullDuplicates, from, to))
          bound <- runTx(ClusterBindingsDao.getBinding(ClusterType.FullDuplicates, from))
          _ <- runTx(ClusterBindingsDao.unbindOffer(ClusterType.FullDuplicates, from))
          unbound <- runTx(ClusterBindingsDao.getBinding(ClusterType.FullDuplicates, from))
        } yield assertTrue {
          bound.contains(to) && unbound.isEmpty
        }
      },
      testM("forbid binding to bound offers") {
        val zeroth = OfferId("zeroth")
        val first = OfferId("first")
        val second = OfferId("second")
        for {
          _ <- runTx(ClusterBindingsDao.bindOffer(ClusterType.FullDuplicates, zeroth, first))
          failure <- runTx(ClusterBindingsDao.bindOffer(ClusterType.FullDuplicates, second, zeroth)).run
        } yield assert(failure)(
          fails(isSubtype[AglomeratError.AlreadyBound](equalTo(AglomeratError.AlreadyBound(zeroth, first))))
        )
      },
      testM("get bound offers") {
        val bound = OfferId("bound")
        val bindTo = OfferId("bind_to_2")
        for {
          _ <- runTx(ClusterBindingsDao.bindOffer(ClusterType.FullDuplicates, bound, bindTo))
          boundOffers <- runTx(ClusterBindingsDao.getBoundOffers(ClusterType.FullDuplicates, bindTo, LimitOffset(1, 0)))
        } yield assertTrue {
          boundOffers.contains(bound)
        }
      }
    )
  }.provideCustomLayerShared {
    TestYdb.ydb >>> (YdbClusterBindingsDao.live ++ Ydb.txRunner)
  }
}
