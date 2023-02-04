package ru.auto.salesman.dao.user

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.BundleDao.Filter
import ru.auto.salesman.model.user.Bundle
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.model.gens.user.{UserDaoGenerators, UserModelGenerators}
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

trait BundleDaoSpec
    extends BaseSpec
    with UserModelGenerators
    with UserDaoGenerators
    with IntegrationPropertyCheckConfig {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  def newDao(data: Iterable[Bundle]): BundleDao

  "BundleDao" should {
    "filter vip bundles active activated after should get all" in {
      forAll(EpochInPastGen) { epoch =>
        val time = new DateTime(epoch)
        forAll(list(1, 10, VipBundleActiveGen)) { genBundles =>
          val timeInFuture = time.plusMinutes(2)
          val genBundlesForDao =
            genBundles.map(_.copy(activated = timeInFuture))
          val dao = newDao(genBundlesForDao)

          val bundles =
            dao.get(Filter.VipActiveActivatedAfter(time)).success.value
          bundles should have size genBundlesForDao.size
        }
      }
    }

    "filter vip bundles active activated after shouldn't get any in set where time in past" in {
      forAll(EpochInPastGen) { epoch =>
        val time = new DateTime(epoch)
        forAll(list(1, 10, VipBundleActiveGen)) { genBundles =>
          val timeInPast = time.minusMinutes(2)
          val genBundlesForDao = genBundles.map(_.copy(activated = timeInPast))
          val dao = newDao(genBundlesForDao)

          val bundles =
            dao.get(Filter.VipActiveActivatedAfter(time)).success.value
          bundles shouldBe empty
        }
      }
    }

    "filter vip bundles active activated after shouldn't get any in set without any vip" in {
      forAll(EpochInPastGen) { epoch =>
        val time = new DateTime(epoch)
        forAll(list(1, 10, NotVipBundleActiveGen)) { genBundles =>
          val dao = newDao(genBundles)
          val bundles =
            dao.get(Filter.VipActiveActivatedAfter(time)).success.value
          bundles shouldBe empty
        }
      }
    }

    "filter vip bundles active activated after shouldn't get any in set without active bundle" in {
      forAll(EpochInPastGen) { epoch =>
        val time = new DateTime(epoch)
        forAll(list(1, 10, NotActiveVipBundleGen)) { genBundles =>
          val dao = newDao(genBundles)
          val bundles =
            dao.get(Filter.VipActiveActivatedAfter(time)).success.value
          bundles shouldBe empty
        }
      }
    }

    "filter vip bundles VipDeactivatedAfter should get all" in {
      forAll(EpochInPastGen) { epochMillis =>
        forAll(list(1, 10, NotActiveVipBundleGen)) { genBundles =>
          val genBundlesForDao =
            genBundles.map(_.copy(epoch = epochMillis + 1000))
          val dao = newDao(genBundlesForDao)

          val bundles =
            dao
              .get(Filter.VipDeactivatedAfter(new DateTime(epochMillis)))
              .success
              .value
          bundles should have size genBundlesForDao.size
        }
      }
    }

    "filter vip bundles VipDeactivatedAfter shouldn't get any in set with time in past" in {
      forAll(EpochInPastGen) { epochMillis =>
        forAll(list(1, 10, NotActiveVipBundleGen)) { genBundles =>
          val genBundlesForDao =
            genBundles.map(_.copy(epoch = epochMillis - 1000))
          val dao = newDao(genBundlesForDao)

          val bundles =
            dao
              .get(Filter.VipDeactivatedAfter(new DateTime(epochMillis)))
              .success
              .value
          bundles shouldBe empty
        }
      }
    }

    "filter vip bundles VipDeactivatedAfter shouldn't get any in set with active bundles" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(list(1, 10, VipBundleActiveGen)) { genBundles =>
          val dao = newDao(genBundles)

          val bundles =
            dao
              .get(Filter.VipDeactivatedAfter(new DateTime(epoch)))
              .success
              .value
          bundles shouldBe empty
        }
      }
    }

    "filter vip bundles VipDeactivatedAfter shouldn't get any in set without any vip bundle" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(list(1, 10, NotVipBundleNotActiveGen)) { genBundles =>
          val dao = newDao(genBundles)

          val bundles =
            dao
              .get(Filter.VipDeactivatedAfter(new DateTime(epoch)))
              .success
              .value
          bundles shouldBe empty
        }
      }
    }

    "succeed on duplicated insertIfNotExists() invocation" in {
      forAll(bundleCreateRequestGen()) { request =>
        val dao = newDao(data = Nil)
        (dao.insertIfNotExists(request) *> dao.insertIfNotExists(
          request
        )).success
      }
    }
  }
}
