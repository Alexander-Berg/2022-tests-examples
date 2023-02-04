package ru.yandex.vos2.config

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.apache.curator.framework.CuratorFramework
import ru.yandex.capa.CapaClient
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.application.ng.RuntimeProvider
import ru.yandex.realty.application.ng.s3.ExtendedS3Client
import ru.yandex.realty.application.ng.yt.{YtConfig, YtConfigSupplier}
import ru.yandex.realty.clients.balance.BalanceClient
import ru.yandex.realty.clients.moderation.ModerationClient
import ru.yandex.realty.clients.picapica.PicaPicaClientImpl
import ru.yandex.realty.clients.resource.ResourceServiceClient
import ru.yandex.realty.clients.statistics.RawStatisticsClient
import ru.yandex.realty.context.v2.NoRevokedOffersPartners
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.http.HttpClient
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.storage.{CampaignHeadersStorage, FreeJuridicalPlacementUidsStorage}
import ru.yandex.vertis.application.runtime.RuntimeConfig
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.pica.service.PicaServiceGrpc
import ru.yandex.vos2.callcenter.importers.OffersImporter
import ru.yandex.vos2.dao.utils.Database
import ru.yandex.realty.clients.billing.{BillingClient => CommonBillingClient}
import ru.yandex.realty.clients.seller.SellerClient
import ru.yandex.realty.ops.OperationalComponents
import ru.yandex.vos2.dao.call.{CallStatDao, ClientCallStatDao, UserCallStatDao}
import ru.yandex.vos2.dao.quota.QuotaDao
import ru.yandex.vos2.mirroring.dao.{MirroredOfferDao, MirroredUserDao}
import ru.yandex.vos2.moderation.{
  AgencyProfileModerationClient,
  UserModerationTransportDecider,
  UserRealtyModerationClient
}
import ru.yandex.vos2.realty.components.{RealtyCoreComponents, TestRealtyCoreComponents}
import ru.yandex.vos2.realty.dao.RealtyMySql
import ru.yandex.vos2.realty.dao.minibb.RealtyUserAuthCodeDao
import ru.yandex.vos2.realty.dao.offers.{OfferTransferQueueDao, RealtyOfferDao}
import ru.yandex.vos2.realty.dao.users.{PhoneUserRefsDao, RealtyUidDao, RealtyUserDao}
import ru.yandex.vos2.realty.services.billing.BillingClient
import ru.yandex.vos2.realty.services.moderation.AgencyProfileModerationTransportDecider
import ru.yandex.vos2.realty.services.pacement.PlacementService
import ru.yandex.vos2.realty.services.phone.ProbeSmsClient
import ru.yandex.vos2.realty.services.realty.RealtyApiClient
import ru.yandex.vos2.realty.services.spammer.{SpammerClient, SpammerClientConfig, SpammerClientSupplier}
import ru.yandex.vos2.realty.util.PhotoRemover
import ru.yandex.vos2.reasons.TestReasonService
import ru.yandex.vos2.services.holocron.comparable.HolocronComparableOfferSender
import ru.yandex.vos2.services.interfax.InterfaxClient
import ru.yandex.vos2.services.mds.{MdsBlockingClient, MdsClient}
import ru.yandex.vos2.services.pica.PicaPicaClient
import ru.yandex.vos2.services.statface.StatfaceClient
import ru.yandex.vos2.subscriptions.letters._
import ru.yandex.vos2.subscriptions.sms._
import ru.yandex.vos2.testing.meta.TestingMetaLoadingList
import ru.yandex.vos2.user.CapaUserDao
import ru.yandex.vos2.user.whitelist.AccessListService
import ru.yandex.vos2.watching.utils._

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
class TestRealtySchedulerComponents
  extends RealtySchedulerComponents
  with YtConfigSupplier
  with SpammerClientSupplier
  with RuntimeProvider
  with OperationalComponents {

  override def ytConfig: YtConfig = ???

  override def runtime: RuntimeConfig = ???

  override val ec: ExecutionContext =
    ExecutionContext.global

  override val coreComponents: RealtyCoreComponents = TestRealtyCoreComponents

  override val kafkaIdxClient: ProbeIdxClient =
    new ProbeIdxClient

  override lazy val mirror: Database = ???

  override lazy val mirroredUserDao: MirroredUserDao = ???

  override lazy val mirroredOfferDao: MirroredOfferDao = ???

  override lazy val actorSystem: ActorSystem = ???

  override lazy val picaPicaClient: PicaPicaClient = ???

  override lazy val picapicaClient: PicaPicaClientImpl = ???

  override lazy val photoRemover: PhotoRemover = ???

  override lazy val realtyCommercialBillingClient: CommonBillingClient = ???

  override lazy val realtyBillingClient: CommonBillingClient = ???

  override val reasonService = new TestReasonService

  override lazy val zkClient: CuratorFramework = ???

  override val smsClient = new ProbeSmsClient

  override lazy val rawStatisticsClient: RawStatisticsClient = ???

  override lazy val httpClient: HttpClient = ???

  override lazy val smsRenderer: SmsNotificationRenderer =
    new RealtySmsNotificationRenderer(reasonService)

  override lazy val emailRenderer: EmailNotificationRenderer =
    new RealtyEmailNotificationRenderer(reasonService)

  override lazy val commonCapaClient: CapaClient = ???

  override lazy val capaUserDao: CapaUserDao = ???

  override lazy val uidDao: RealtyUidDao = ???

  override lazy val usersModerationClient: UserRealtyModerationClient = ???

  override lazy val userModerationDecider: UserModerationTransportDecider = ???

  override lazy val moderationClient: ModerationClient = ???

  override lazy val realtyApiClient: RealtyApiClient = ???

  override val accessListService: AccessListService = new AccessListService {
    override def isAllowed(uid: Long): Boolean = true
  }

  override val billingClient: BillingClient = new BillingClient("host", 1) {
    override def isUserAgency(uid: String): Try[Boolean] = Success(false)
  }

  override lazy val balanceClient: BalanceClient = ???

  override lazy val mdsClient: MdsClient = ???

  override def operational: Option[OperationalSupport] = None

  override lazy val spammerClient: SpammerClient = ???

  override lazy val materializer: ActorMaterializer = ???

  override lazy val deactivatedOffersImporter: OffersImporter = ???

  override lazy val statfaceClient: StatfaceClient = ???

  override lazy val mdsBlockingClient: MdsBlockingClient = ???

  override def holocronComparableOfferSender: HolocronComparableOfferSender = ???

  override lazy val testingMetaLoadingUsersProvider: Provider[TestingMetaLoadingList] = ???

  override def campaignHeadersProvider: Provider[CampaignHeadersStorage] = ???

  override def disabledCampaignsProvider: Provider[CampaignHeadersStorage] = ???

  override def freeJuridicalPlacementUidsProvider: Provider[FreeJuridicalPlacementUidsStorage] = ???

  override def sitesService: SitesGroupingService = ???

  override def noRevokedOffersPartnersProvider: Provider[NoRevokedOffersPartners] = ???

  override def resourceServiceClient: ResourceServiceClient = ???

  override def regionGraphProvider: Provider[RegionGraph] = ???

  override lazy val agencyProfileModerationClient: AgencyProfileModerationClient = ???
  override lazy val agencyProfileModerationDecider: AgencyProfileModerationTransportDecider = ???

  override def s3Client: ExtendedS3Client = ???

  override def interfaxClient: InterfaxClient = ???

  override lazy val placementService: PlacementService = ???

  override lazy val pica2Client: PicaServiceGrpc.PicaServiceStub = ???

  override lazy val sellerClient: SellerClient = ???

  def typesafeConfig: Config = ???

  def spammerClientConfig: SpammerClientConfig = ???

  def ops: OperationalSupport = coreComponents.ops

  val userDao: RealtyUserDao = coreComponents.userDao

  def mySql: RealtyMySql = coreComponents.mySql

  def offerDao: RealtyOfferDao = coreComponents.offerDao

  def offerExportDao: RealtyOfferDao = coreComponents.offerExportDao

  def quotaDao: QuotaDao = coreComponents.quotaDao

  def userCallStatDao: CallStatDao[Long] = new UserCallStatDao(mySql)

  def clientCallStatDao: CallStatDao[Long] = new ClientCallStatDao(mySql)

  def phoneUserRefsDao: PhoneUserRefsDao = coreComponents.phoneUserRefsDao

  def minibbDao: RealtyUserAuthCodeDao = coreComponents.minibbDao

  def offerTransferQueueDao: OfferTransferQueueDao = coreComponents.offerTransferQueueDao
}
