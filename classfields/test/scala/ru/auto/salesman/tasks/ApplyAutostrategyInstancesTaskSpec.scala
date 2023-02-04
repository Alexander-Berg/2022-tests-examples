package ru.auto.salesman.tasks

import org.scalacheck.Gen
import org.scalacheck.Gen.const
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.salesman.client.PhpCabinetClient.PostServicesRequest
import ru.auto.salesman.client.SearcherClient.OfferPositionRequest
import ru.auto.salesman.client.{PhpCabinetClient, SearcherClient, VosClient}
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceFilter.ForStatuses
import ru.auto.salesman.dao.impl.jdbc.test.{
  JdbcAutostrategiesDaoForTests,
  JdbcScheduleInstanceDaoForTests
}
import ru.auto.salesman.environment._
import ru.auto.salesman.model.ProductId.Fresh
import ru.auto.salesman.model.ScheduleInstance.Statuses._
import ru.auto.salesman.model.autostrategies.AlwaysAtFirstPagePayload
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.searcher.OfferSorts.FreshDesc
import ru.auto.salesman.model.{AutoruDealer, ServicePlaces, Slave}
import ru.auto.salesman.service.ScheduleInstanceService.Source
import ru.auto.salesman.service.impl.AlwaysAtFirstPageApplyServiceImpl
import ru.auto.salesman.service.{
  AutostrategyInstanceGenImpl,
  AutostrategyInstanceService,
  EpochService
}
import ru.auto.salesman.tasks.Markers.ApplyAutostrategyInstancesEpoch
import ru.auto.salesman.tasks.schedule.ApplyScheduledInstancesTask
import ru.auto.salesman.test.model.gens.{AutostrategyGen, OfferModelGenerators}
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.RequestContext

import scala.concurrent.duration._

class ApplyAutostrategyInstancesTaskSpec
    extends BaseSpec
    with SalesmanJdbcSpecTemplate
    with BeforeAndAfter
    with OfferModelGenerators {

  private val autostrategiesDao = new JdbcAutostrategiesDaoForTests(database)

  private val instanceDao = new JdbcScheduleInstanceDaoForTests(
    database,
    "autostrategy_instance",
    "autostrategy_id"
  )

  private val epochService = mock[EpochService]

  private val instanceGen = new AutostrategyInstanceGenImpl

  private val vosClient = mock[VosClient]

  private val searcherClient = mock[SearcherClient]

  private val phpCabinetClient = mock[PhpCabinetClient]

  private val alwaysAtFirstPageApplyService =
    new AlwaysAtFirstPageApplyServiceImpl(searcherClient, phpCabinetClient)

  private val instanceService =
    new AutostrategyInstanceService(autostrategiesDao, instanceDao)

  private val applyPeriodInMinutes = 2

  private val settings = ApplyScheduledInstancesTask.Settings(
    window = (applyPeriodInMinutes * 3).minutes,
    maxExecutionLag = 2.hours
  )

  private val task = new ApplyAutostrategyInstancesTask(
    vosClient,
    alwaysAtFirstPageApplyService,
    instanceGen,
    instanceService,
    epochService,
    settings
  )

  before {
    autostrategiesDao.clean()
    instanceDao.clean()
  }

  private val marker = ApplyAutostrategyInstancesEpoch.toString

  "Apply autostrategy instances task" should {

    "apply instance" in {
      val autostrategy = AutostrategyGen.next.copy(
        payload = AlwaysAtFirstPagePayload(
          forMarkModelListing = true,
          forMarkModelGenerationListing = false
        )
      )
      autostrategiesDao.put(Seq(autostrategy)).success
      val stored = autostrategiesDao.all().success.value.headOption.value
      instanceDao
        .insert(Seq(Source(stored.id, fireTime = now(), timeAt(stored.epoch))))
        .success
      val pending = instanceDao.get()().success.value.headOption.value
      (epochService.getOptional _).expects(marker).returningT(None)
      val offer = offerGen(AutoruOfferId(autostrategy.offerId.value), CARS).next
      (vosClient.getOptOffer _)
        .expects(autostrategy.offerId, Slave)
        .returningZ(Some(offer))
      val notOnFirstPage = Gen.choose(38, 50000).next
      (searcherClient
        .offerPosition(_: OfferPositionRequest)(_: RequestContext))
        .expects(
          OfferPositionRequest(offer, withSuperGen = false, sort = FreshDesc),
          *
        )
        .returningT(Some(notOnFirstPage))
      (phpCabinetClient
        .postServices(_: PostServicesRequest)(_: RequestContext))
        .expects(
          PostServicesRequest(
            AutoruOfferId.apply(autostrategy.offerId.value).id,
            AutoruDealer(offer.getUserRef).id,
            CARS.toString,
            Fresh,
            ServicePlaces.AlwaysAtFirstPageAutostrategy
          ),
          *
        )
        .returningT(true)
      (epochService.set _).expects(marker, *).returningT(())
      task.execute().success
      val updated = instanceDao.get()().success.value
      updated should contain only pending.copy(
        status = Done,
        fireTime = updated.head.fireTime,
        epoch = updated.head.epoch
      )
    }

    "cancel instance if offer isn't active" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val stored = autostrategiesDao.all().success.value.headOption.value
      instanceDao
        .insert(Seq(Source(stored.id, fireTime = now(), timeAt(stored.epoch))))
        .success
      val pending = instanceDao.get()().success.value.headOption.value
      (epochService.getOptional _).expects(marker).returningT(None)
      val offer = NotActiveOfferGen.next
      (vosClient.getOptOffer _)
        .expects(autostrategy.offerId, Slave)
        .returningZ(Some(offer))
      (epochService.set _).expects(marker, *).returningT(())
      task.execute().success
      val cancelled = instanceDao.get(ForStatuses(Cancelled))().success.value
      cancelled should contain only pending.copy(
        status = Cancelled,
        fireTime = cancelled.head.fireTime,
        epoch = cancelled.head.epoch
      )
    }

    "cancel instance if offer not found" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val stored = autostrategiesDao.all().success.value.headOption.value
      instanceDao
        .insert(Seq(Source(stored.id, fireTime = now(), timeAt(stored.epoch))))
        .success
      val pending = instanceDao.get()().success.value.headOption.value
      (epochService.getOptional _).expects(marker).returningT(None)
      (vosClient.getOptOffer _)
        .expects(autostrategy.offerId, Slave)
        .returningZ(None)
      (epochService.set _).expects(marker, *).returningT(())
      task.execute().success
      val cancelled = instanceDao.get(ForStatuses(Cancelled))().success.value
      cancelled should contain only pending.copy(
        status = Cancelled,
        fireTime = cancelled.head.fireTime,
        epoch = cancelled.head.epoch
      )
    }

    "fail instance in case of vos exception" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val stored = autostrategiesDao.all().success.value.headOption.value
      instanceDao
        .insert(Seq(Source(stored.id, fireTime = now(), timeAt(stored.epoch))))
        .success
      val pending = instanceDao.get()().success.value.headOption.value
      (epochService.getOptional _).expects(marker).returningT(None)
      (vosClient.getOptOffer _)
        .expects(autostrategy.offerId, Slave)
        .throwingZ(new TestException)
      task.execute().failure.exception shouldBe an[TestException]
      val failed = instanceDao.get(ForStatuses(Failed))().success.value
      failed should contain only pending.copy(
        status = Failed,
        fireTime = failed.head.fireTime,
        epoch = failed.head.epoch
      )
    }
  }
}
