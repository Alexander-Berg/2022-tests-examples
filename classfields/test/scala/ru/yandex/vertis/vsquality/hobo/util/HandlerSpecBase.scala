package ru.yandex.vertis.vsquality.hobo.util

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.hobo.api.DirectivesBase.{`X-Yandex-Request-ID`, `X-Yandex-User`}
import ru.yandex.vertis.vsquality.hobo.api.{ApiHandlerImpl, DomainExceptionHandling}
import ru.yandex.vertis.vsquality.hobo.backend.{Backend, BackendImpl}
import ru.yandex.vertis.vsquality.hobo.dao.impl.StartSummaryPeriodDao
import ru.yandex.vertis.vsquality.hobo.dao.impl.mysql._
import ru.yandex.vertis.vsquality.hobo.dao.ydb.YdbTaskViewDaoImpl
import ru.yandex.vertis.vsquality.hobo.feature.{FeatureRegistryFactory, InMemoryFeatureRegistryConfig}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.{stringGen, UserIdGen}
import ru.yandex.vertis.vsquality.hobo.model.{QueueSettings, User}
import ru.yandex.vertis.vsquality.hobo.service._
import ru.yandex.vertis.vsquality.hobo.service.impl._
import ru.yandex.vertis.vsquality.hobo.service.impl.mysql.MySqlTaskService
import ru.yandex.vertis.vsquality.hobo.telephony.impl.StubTelephonyClient
import ru.yandex.vertis.vsquality.hobo.telepony.StubAnyDomainTeleponyClient
import ru.yandex.vertis.vsquality.utils.test_utils.ydb.YdbWrapperContainer
import ru.yandex.vertis.vsquality.utils.ydb_utils.WithTransaction

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author semkagtn
  */
trait HandlerSpecBase extends SpecBase with ScalatestRouteTest with YdbSpecBase with MySqlSpecBase {

  implicit val automatedContext: AutomatedContext = AutomatedContext("test")

  override def testConfig: Config = ConfigFactory.parseResources("application.test.conf")

  def basePath: String

  protected def url(remainPath: String): String = s"$basePath$remainPath"

  protected val OperatorContextGen: Gen[OperatorContext] =
    for {
      requestId <- stringGen(5, 5)
      user      <- UserIdGen
    } yield OperatorContext(requestId, user)

  protected def defaultHeaders(implicit operatorContext: OperatorContext): RequestTransformer =
    addHeaders(
      RawHeader(`X-Yandex-Request-ID`.name, operatorContext.requestId),
      RawHeader(`X-Yandex-User`.name, operatorContext.user.key)
    )

  private def seal(route: Route): Route =
    Route.seal(route)(
      routingSettings = implicitly[RoutingSettings],
      exceptionHandler = DomainExceptionHandling.domainExceptionHandler
    )

  protected def initialUsers: Iterable[User] = Iterable.empty

  protected lazy val backend: Backend = {
    val queueSettingsService =
      new CachedQueueSettingsService(
        new MySqlQueueSettingsKeyValueDao(database),
        updateEvery = 2.seconds,
        QueueSettings.defaultSettingsMap,
        implicitly[ExecutionContext]
      )
    val userService = new CachedUserService(new MySqlUserKeyValueDao(database), 2.seconds, initialUsers)
    val taskService =
      new MySqlTaskService(
        database,
        maxBatchSize = 1000,
        needSaveHistory = true,
        taskFactory = new TaskFactory(queueSettingsService),
        onCompleteActionsFactory = new OnCompleteActionsFactory(queueSettingsService, OnCompleteActionsRegistry.Stub),
        readUserSupport = userService
      )

    val ydbTaskViewDao = new YdbTaskViewDaoImpl[IO, WithTransaction[IO, *]](ydb)
    val viewService = new ViewServiceImpl(ydbTaskViewDao)
    val validateViewService = new CombinedViewService(viewService, taskService)
    val phoneCallService =
      new PhoneCallServiceImpl(
        userService,
        new MySqlPhoneCallDao(database),
        new StubTelephonyClient,
        new StubAnyDomainTeleponyClient,
        RedirectParams.Instantly,
        queueSettingsService,
        taskService
      )
    val summarySalaryStatisticsService =
      new SummarySalaryStatisticsServiceImpl(new MySqlSummarySalaryStatisticsKeyValueDao(database))
    val startSummaryPeriodService =
      new StartSummaryPeriodServiceImpl(new StartSummaryPeriodDao(new MySqlWaterlineKeyValueDao(database)))
    val featureService: FeatureService = new FeatureServiceImpl(FeatureRegistryFactory(InMemoryFeatureRegistryConfig))
    BackendImpl(
      taskService = spy(taskService),
      userService = spy(userService),
      phoneCallService = spy(phoneCallService),
      queueSettingsService = spy(queueSettingsService),
      summarySalaryStatisticsService = spy(summarySalaryStatisticsService),
      startSummaryPeriodService = spy(startSummaryPeriodService),
      featureService = spy(featureService),
      validateViewService = spy(validateViewService)
    )
  }

  protected lazy val route: Route = seal(new ApiHandlerImpl(backend).route)
}
