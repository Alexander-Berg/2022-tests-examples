package ru.auto.salesman.tasks.impl

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.auto.salesman.dao.impl.jdbc.test.JdbcAutostrategiesDaoForTests
import ru.auto.salesman.model.autostrategies.AutostrategyStatus.{Active, Expired}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.tasks.ExpireAutostrategiesTask.TaskName
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

class ExpireAutostrategiesTaskImplSpec
    extends BaseSpec
    with SalesmanJdbcSpecTemplate
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with IntegrationPropertyCheckConfig {

  private val autostrategiesDao = new JdbcAutostrategiesDaoForTests(database)

  private val epochService = mock[EpochService]

  private val task =
    new ExpireAutostrategiesTaskImpl(autostrategiesDao, epochService)

  override protected def beforeAll(): Unit = autostrategiesDao.clean()

  "Expire autostrategies task" should {

    "expire only old autostrategies" in {
      forAll(AutostrategyListGen, OldAutostrategyListGen) { (notOld, old) =>
        autostrategiesDao.put(notOld).success
        autostrategiesDao.put(old).success
        (epochService.set _).expects(TaskName, *).returningT(())
        task.execute(handleAll = true).success
        val stored = autostrategiesDao.all().success.value.groupBy(_.status)
        val active = stored.getOrElse(Active, Nil).map(_.props)
        val expired = stored.getOrElse(Expired, Nil).map(_.props)
        active should contain theSameElementsAs notOld
        expired should contain theSameElementsAs old
      }
    }

    "expire only autostrategies created after last execution" in {
      forAll(OldAutostrategyListGen, OldAutostrategyListGen) { (tooOld, old) =>
        autostrategiesDao.put(tooOld).success
        val epoch = autostrategiesDao.all().success.value.maxBy(_.epoch).epoch
        autostrategiesDao.put(old).success
        (epochService.getOptional _).expects(TaskName).returningT(Some(epoch))
        (epochService.set _).expects(TaskName, *).returningT(())
        task.execute(handleAll = false).success
        val stored = autostrategiesDao.all().success.value.groupBy(_.status)
        val active = stored.getOrElse(Active, Nil).map(_.props)
        val expired = stored.getOrElse(Expired, Nil).map(_.props)
        active should contain theSameElementsAs tooOld
        expired should contain theSameElementsAs old
      }
    }
  }
}
