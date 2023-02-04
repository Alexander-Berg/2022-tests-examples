package ru.yandex.vertis.billing.integration.test.environment

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.billing.backend.{CallsRevenueStatistics, StatisticsContext}
import ru.yandex.vertis.billing.dao._
import ru.yandex.vertis.billing.dao.impl.jdbc._
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.event.call.TeleponyCallFactParser
import ru.yandex.vertis.billing.event.call.telepony.TeleponyCallProcessor
import ru.yandex.vertis.billing.integration.test.mocks.{TestBalance, TestCallPriceEstimateService, TestHoboClient}
import ru.yandex.vertis.billing.model_core.Division.{Components, Locales, Projects}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.reader.DivisionEventRecordsReader
import ru.yandex.vertis.billing.service._
import ru.yandex.vertis.billing.service.async._
import ru.yandex.vertis.billing.service.impl._
import ru.yandex.vertis.billing.service.logging._
import ru.yandex.vertis.billing.settings._
import ru.yandex.vertis.billing.task.{schedulerTask, NamedAsyncTask}
import ru.yandex.vertis.billing.tasks._
import ru.yandex.vertis.billing.util.Logging
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.scheduler.model.Payload.Sync
import ru.yandex.vertis.scheduler.model.Schedule.EveryMinutes
import ru.yandex.vertis.scheduler.model.{Payload => TaskPayload, Schedule, Task, TaskDescriptor}
import slick.jdbc

import scala.concurrent.ExecutionContext

case class TestTmsEnvironment(
    database: jdbc.JdbcBackend.DatabaseDef,
    eventDatabase: jdbc.JdbcBackend.DatabaseDef)
  extends Logging {

  val service = ServiceSettings(
    "autoru",
    ConfigFactory.parseString("""
      service.autoru {
        balance {
          service-id: 1
          act-text: someActText
          product {
            default = 507211
          }
        }
        telepony.whitelist = ["+74996488506"]
        yocto.call-requests.base-path="/tmp/yocto"
        call-modify {
          modify.period = 21d
          complain.period = 7d
          moderation.period = 7d
        }
      }
    """.stripMargin)
  )

  val system = ActorSystem()
  private val materializer = Materializer(system)

  private val tasksServiceComponents = TasksServiceComponents(service.name)

  private val slaveDatabase = database;
  private val masterDatabase = database;
  private val dualDatabase = DualDatabase(masterDatabase, slaveDatabase)

  val keyValueDao = new JdbcKeyValueDao(masterDatabase)
  val customerDao = new JdbcCustomerDao(masterDatabase)

  private val asyncKeyValueService =
    new AsyncKeyValueServiceImpl(keyValueDao)(system.dispatcher)

  private val balance = new TestBalance

  val orderDao = new JdbcOrderDao(dualDatabase)

  private val bindingDao = new JdbcBindingDao(slaveDatabase)

  private val campaignDao = new JdbcCampaignDao(masterDatabase)

  private val campaignEventDao = new JdbcCampaignEventDao(masterDatabase)

  private val balanceSettings = service.BalanceConfig

  private val limitDao = new JdbcLimitDao(masterDatabase)

  private val limitService = new LimitServiceImpl(limitDao, orderDao)

  private val paidOffersDao = new JdbcPaidOffersDao(dualDatabase)

  private val campaignCallDao = new JdbcCampaignCallDao(masterDatabase)

  val campaignService =
    new CampaignServiceImpl(campaignDao, bindingDao, campaignCallDao, CampaignDao.DuplicationPolicy.DisallowDuplicates)
      with LoggedCampaignService
      with CampaignServiceWithStatusAndLimits
      with CampaignServiceWithHold
      with HistoryCampaignService {

      override def serviceName: String = "CampaignService"

      override def historyService: Option[CampaignHistoryService] = Some(campaignHistoryService)

      override def enricher: CampaignWithStatusAndLimitsEnricher =
        new CampaignWithStatusAndLimitsEnricher(limitService, orderDao)

      override def limitService: LimitService = TestTmsEnvironment.this.limitService

      override def holdingOrderService: HoldingOrderService = TestTmsEnvironment.this.orderService
    }

  private val hoboDao = new JdbcHoboDao(masterDatabase)

  private val hoboService = new HoboServiceImpl(hoboDao)

  private val overdraftReporter = new LogOverdraftReporter

  private val (holdService, optHoldCleanerTask) = {
    val effectiveHoldService =
      new JdbcHoldDao(masterDatabase)
    val cleaner =
      Task(TaskDescriptor("holds-cleaner", Schedule.EveryMinutes(5)), TaskPayload(effectiveHoldService.newCleaner()))
    (effectiveHoldService, Some(cleaner))
  }

  private val orderService =
    new OrderServiceImpl(orderDao, balance, balanceSettings)
      with LimitedOrderService
      with HoldingOrderService
      with DetailedLoggedOrderService {

      override def serviceName: String = "OrderService"
      def overdraftReporter: OverdraftReporter = TestTmsEnvironment.this.overdraftReporter
      def limitService: LimitService = TestTmsEnvironment.this.limitService
      def holdService: HoldService = TestTmsEnvironment.this.holdService
    }

  private val epochService = new EpochServiceImpl(keyValueDao)

  private val typedKeyValueService = new TypedKeyValueServiceImpl(keyValueDao)

  val campaignHistoryDao = new JdbcCampaignHistoryDao(masterDatabase)

  private val campaignHistoryService = new CampaignHistoryServiceImpl(campaignHistoryDao)

  val eventDualDatabase = DualDatabase(eventDatabase, eventDatabase)

  val phoneShowEventsDivisionDao = {
    new JdbcEventDivisionDao(
      eventDualDatabase,
      Division(Projects.withName(service.name), Locales.Ru, Components.PhoneShow).identity
    )
  }

  val indexingEventsDivisionDao = {
    new JdbcEventDivisionDao(
      eventDualDatabase,
      Division(Projects.withName(service.name), Locales.Ru, Components.Indexing).identity
    )
  }

  private val indexingEventsReader = new DivisionEventRecordsReader(indexingEventsDivisionDao)

  private val WithdrawFormTask =
    new WithdrawFormTask(
      campaignEventDao,
      orderService,
      epochService,
      keyValueDao,
      tasksServiceComponents
    )

  val withdrawForm = Task(
    TaskDescriptor("withdraw-form", Schedule.EveryMinutes(10)),
    Sync(() => WithdrawFormTask.run().get)
  )

  val callFactDao = new JdbcCallFactDao(dualDatabase)

  val priceService =
    new TestCallPriceEstimateService

  private val StatisticsContext = new StatisticsContext(
    service,
    tasksServiceComponents,
    campaignEventDao,
    paidOffersDao,
    campaignCallDao,
    campaignDao,
    indexingEventsReader,
    phoneShowEventsDivisionDao,
    indexingEventsDivisionDao,
    callFactDao,
    epochService,
    campaignHistoryService,
    Some(priceService)
  )

  val CallsRevenue = CallsRevenueStatistics(
    StatisticsContext,
    hoboService
  )(materializer, system.dispatcher)

  val teleponyCallProcessor = new TeleponyCallProcessor(
    callFactDao,
    new TeleponyCallFactParser(),
    tasksServiceComponents.callFilter,
    service.callModifySettings
  )

  val hoboClient = new TestHoboClient

  val hoboPushTask = {
    import system.dispatcher

    val responseUrl = "response-url"
    val commonCallQueue = QueueId.AUTO_RU_CALL_AUCTION.name
    val callCenterCallQueue = Some(QueueId.AUTO_RU_CALL_CENTER.name)
    val callCarUsedQueue = Some(QueueId.AUTO_RU_USED_CALL_AUCTION.name)
    val task = new HoboPushTask(
      hoboDao,
      hoboClient,
      epochService,
      responseUrl,
      commonCallQueue,
      callCenterCallQueue,
      callCarUsedQueue,
      tasksServiceComponents.callTagParser
    ) with NamedAsyncTask {
      override def taskName: String = HoboPushTask.Name
    }

    schedulerTask(task, EveryMinutes(30))
  }

}
