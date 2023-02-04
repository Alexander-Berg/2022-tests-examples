package ru.auto.salesman.tasks.schedule

import ru.auto.salesman.dao.impl.jvm.user.JvmProductScheduleDao
import ru.auto.salesman.dao.user.ProductScheduleDao.ScheduleFilter.IsDeleted
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.util.time.DateTimeUtil.now

class DeleteExpiredProductSchedulesTaskSpec
    extends BaseSpec
    with ProductScheduleModelGenerators {

  implicit override def domain: DeprecatedDomain = AutoRu

  "ExpireSchedulesTask" should {

    "delete only expired schedule" in {
      forAll(
        productScheduleGen(
          isDeletedGen = false,
          expireDateGen = Some(now().minusSeconds(5))
        ),
        productScheduleGen(
          isDeletedGen = false,
          expireDateGen = Some(now().plusSeconds(20))
        ),
        productScheduleGen(isDeletedGen = false, expireDateGen = None)
      ) { (expired, notExpiredYet, neverExpired) =>
        val dao =
          new JvmProductScheduleDao(List(expired, notExpiredYet, neverExpired))
        val task = new DeleteExpiredProductSchedulesTask(dao)
        task.execute().success
        dao
          .get(IsDeleted(false))
          .success
          .value should contain only (notExpiredYet, neverExpired)
      }
    }
  }
}
