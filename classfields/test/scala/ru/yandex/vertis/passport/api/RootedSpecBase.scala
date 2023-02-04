package ru.yandex.vertis.passport.api

import com.codahale.metrics.MetricRegistry
import org.scalatest.Suite
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.prometheus.CompositeCollector
import ru.yandex.vertis.passport.Domains
import ru.yandex.vertis.passport.backend.ServiceBackend
import ru.yandex.vertis.passport.dao.KeyValueDao
import ru.yandex.vertis.passport.integration.UserProvider
import ru.yandex.vertis.passport.service.ban.{BanService, ModerationService}
import ru.yandex.vertis.passport.service.communication.UserCommunicationService
import ru.yandex.vertis.passport.service.events.EventsService
import ru.yandex.vertis.passport.service.identity.DeviceUidService
import ru.yandex.vertis.passport.service.promocoder.PromoCodeService
import ru.yandex.vertis.passport.service.session.SessionsFacade
import ru.yandex.vertis.passport.service.sms.SmsLogService
import ru.yandex.vertis.passport.service.tokens.ApiTokenService
import ru.yandex.vertis.passport.service.tvm.UserTvmService
import ru.yandex.vertis.passport.service.user.UserService
import ru.yandex.vertis.passport.service.user.auth.AuthenticationService
import ru.yandex.vertis.passport.service.user.client.ClientService
import ru.yandex.vertis.passport.service.user.social.SocialUserService
import ru.yandex.vertis.passport.service.user.tokens.UserAuthTokenService
import ru.yandex.vertis.passport.service.user.userpic.UserpicService
import ru.yandex.vertis.passport.service.user.yandex.YandexService
import ru.yandex.vertis.passport.service.visits.VisitsService
import ru.yandex.vertis.passport.service.vox.VoxEncryptors
import ru.yandex.vertis.passport.util.InstrumentationDirectives
import ru.yandex.vertis.passport.util.curator.DynamicPropertiesService
import ru.yandex.vertis.passport.util.img.MdsImageResolver
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport}

import scala.concurrent.ExecutionContext
import ru.yandex.tvm.TvmTicketChecker
import ru.yandex.vertis.passport.integration.features.FeatureManager
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.passport.tvm.TvmAuthorizationSupport
import ru.yandex.vertis.passport.tvm.MeteredTvmAuthorizationSupport

/**
  * Base for handler tests that use full uri, starting from root
  *
  * @author zvez
  */
trait RootedSpecBase extends HandlerSpecBase { this: Suite with ServiceBackendProvider =>

  lazy val route = InstrumentationDirectives.enrichRequestContext {
    val tracing = LocalTracingSupport(EndpointConfig.Empty)
    val tvmAuthorizationSupport =
      new TvmAuthorizationSupport(featureManager, tvmTicketChecker) with MeteredTvmAuthorizationSupport {
        override def prometheusRegistry = ops.prometheusRegistry
      }
    val builder =
      new ApiBuilder(ops, tracing, VertisRuntime, tvmAuthorizationSupport)
    builder.register(AutoruServiceHandlerBuilder.build(serviceBackend, Environments.Local, ops))
    seal(builder.build().innerRoute)
  }

}

trait ApiDomainContextProvider {

  lazy val ops = new OperationalSupport {
    override def codahaleRegistry = new MetricRegistry
    override def prometheusRegistry = new CompositeCollector
    override def healthChecks = new CompoundHealthCheckRegistry()
  }

  implicit val imageResolver =
    new MdsImageResolver("localhost", "autoru-users", Seq("24x24", "48x48", "430x600"))

  implicit lazy val apiCtx =
    new AutoruApiContext(imageResolver, Environments.Local, ops)(ExecutionContext.global)
}

trait ServiceBackendProvider extends ApiDomainContextProvider {

  def sessionFacade: SessionsFacade
  def userService: UserService
  def authService: AuthenticationService
  def clientService: ClientService
  def banService: BanService
  def eventsService: EventsService
  def userProvider: UserProvider
  def deviceUidService: DeviceUidService
  def socialUserService: SocialUserService
  def moderationService: ModerationService
  def communicationService: UserCommunicationService
  def userpicService: UserpicService
  def dynamicPropertiesService: DynamicPropertiesService
  def keyValueDao: KeyValueDao
  def userAuthTokenService: UserAuthTokenService
  def apiTokenService: ApiTokenService
  def visitsService: VisitsService
  def yandexService: YandexService
  def userTvmService: UserTvmService
  def voxEncryptors: VoxEncryptors
  def smsLogService: SmsLogService
  def promoCodeService: PromoCodeService
  def featureRegistry: FeatureRegistry
  def featureManager: FeatureManager
  def tvmTicketChecker: TvmTicketChecker

  lazy val serviceBackend = ServiceBackend(
    domain = Domains.Auto,
    sessionFacade = sessionFacade,
    userService = userService,
    authService = authService,
    clientService = clientService,
    banService = banService,
    eventsService = eventsService,
    userpicService = userpicService,
    userProvider = userProvider,
    imageResolver = imageResolver,
    deviceUidService = deviceUidService,
    socialUserService = socialUserService,
    communicationService = communicationService,
    moderationService = moderationService,
    dynamicPropertiesService = dynamicPropertiesService,
    keyValueDao = keyValueDao,
    userTokensService = userAuthTokenService,
    apiTokenService = apiTokenService,
    visitsService = visitsService,
    yandexService = yandexService,
    userTvmService = userTvmService,
    voxEncryptors = voxEncryptors,
    nonBlockingExecContext = ExecutionContext.global,
    smsLogService = smsLogService,
    promoCodeService = promoCodeService,
    featureRegistry = featureRegistry,
    featureManager = featureManager,
    tvmTicketChecker = tvmTicketChecker
  )
}

trait MockedBackend extends ServiceBackendProvider with MockitoSupport {

  val sessionFacade = mock[SessionsFacade]
  val userService = mock[UserService]
  val clientService = mock[ClientService]
  val userProvider = mock[UserProvider]
  val deviceUidService = mock[DeviceUidService]
  val socialUserService = mock[SocialUserService]
  val moderationService = mock[ModerationService]
  val userpicService = mock[UserpicService]
  val authService = mock[AuthenticationService]
  val banService = mock[BanService]
  val eventsService = mock[EventsService]
  val dynamicPropertiesService = mock[DynamicPropertiesService]
  val keyValueDao = mock[KeyValueDao]
  val communicationService = mock[UserCommunicationService]
  val userAuthTokenService = mock[UserAuthTokenService]
  val yandexService = mock[YandexService]
  val userTvmService = mock[UserTvmService]
  val voxEncryptors = mock[VoxEncryptors]
  val smsLogService = mock[SmsLogService]
  val promoCodeService = mock[PromoCodeService]

  val apiTokenService = mock[ApiTokenService]
  val visitsService = mock[VisitsService]
  val featureRegistry = mock[FeatureRegistry]
  val featureManager = mock[FeatureManager]
  val tvmTicketChecker = mock[TvmTicketChecker]
}

trait NoTvmAuthorization { this: MockedBackend =>
  private val featureOff: Feature[Boolean] = mock[Feature[Boolean]]
  when(featureOff.value).thenReturn(false)

  when(featureManager.TvmAuthorization).thenReturn(featureOff)
}
