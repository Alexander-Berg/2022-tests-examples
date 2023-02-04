package ru.auto.salesman.tasks

import org.scalatest.OneInstancePerTest
import ru.auto.salesman.dao.impl.jdbc.JdbcQuotaDao
import ru.auto.salesman.service.impl.QuotaServiceImpl
import ru.auto.salesman.service.quota_offers.QuotaOffersActualizer
import ru.auto.salesman.test.model.gens.expiredQuotaGen
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

class QuotaOffersActualizationTaskDbSpec
    extends BaseSpec
    with SalesmanJdbcSpecTemplate
    with IntegrationPropertyCheckConfig
    with OneInstancePerTest {

  private val quotaDao = new JdbcQuotaDao(database)
  private val quotaService = new QuotaServiceImpl(quotaDao)
  private val quotaOffersService = mock[QuotaOffersActualizer]
  private val epochService = AlwaysZeroEpochService
  private val deactivateMarker = Markers.QuotaCarsOffersDeactivateEpoch

  private val deactivateTask = new QuotaOffersDeactivateTask(
    quotaService,
    quotaOffersService,
    epochService,
    deactivateMarker
  )(_ => true)

  "Quota offers deactivate task" should {

    "not deactivate offers for one expired quota twice" in {
      val quotaDao = new JdbcQuotaDao(database)
      forAll(expiredQuotaGen) { quota =>
        quotaDao.add(quota).success
        (quotaOffersService.actualize _)
          .expects(quota.quotaType, quota.clientId)
          .returningT(())
          .once()
        deactivateTask.execute().success
        deactivateTask.execute().success
      }
    }

    "deactivate offers for two different clients" in {
      val quotaDao = new JdbcQuotaDao(database)
      forAll(expiredQuotaGen) { quota =>
        val anotherClientId = quota.clientId + 1
        val anotherQuota = quota.copy(clientId = anotherClientId)
        quotaDao.add(quota).success
        inSequence {
          (quotaOffersService.actualize _)
            .expects(quota.quotaType, quota.clientId)
            .returningT(())
            .once()
          (quotaOffersService.actualize _)
            .expects(quota.quotaType, anotherClientId)
            .returningT(())
            .once()
        }
        deactivateTask.execute().success
        quotaDao.add(anotherQuota).success
        deactivateTask.execute().success
      }
    }
  }
}
