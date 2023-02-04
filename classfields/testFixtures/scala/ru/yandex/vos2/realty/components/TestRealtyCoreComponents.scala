package ru.yandex.vos2.realty.components

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import ru.yandex.realty.application.extdata.providers.{ExtdataCurrencyComponents, ExtdataRegionGraphComponents}
import ru.yandex.realty.application.extdata.{ExtdataClientConfig, ExtdataComponents}
import ru.yandex.realty.application.ng.AppConfig
import ru.yandex.realty.clients.social.SocialClient
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.util.CryptoUtils
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vos2.TestEnvProvider
import ru.yandex.vos2.dao.MySqlConfig
import ru.yandex.vos2.features.SimpleFeatures
import ru.yandex.vos2.model.notifications.NotificationManager
import ru.yandex.vos2.realty.dao.RealtyMySql
import ru.yandex.vos2.realty.dao.minibb.{RealtyUserAuthCodeDao, RealtyUserAuthCodeDaoImpl}
import ru.yandex.vos2.realty.dao.offers.{
  OfferTransferQueueDao,
  OfferTransferQueueDaoImpl,
  RealtyOfferAccess,
  RealtyOfferDao,
  RealtyOfferDaoImpl,
  RealtyOfferExportAccess
}
import ru.yandex.vos2.realty.dao.users.{PhoneUserRefsDao, PhoneUserRefsDaoImpl, RealtyUserDao, RealtyUserDaoImpl}
import ru.yandex.vos2.realty.features.RealtyFeatures
import ru.yandex.vos2.realty.model.RealtyNotificationManager
import ru.yandex.vos2.realty.model.ids.{LocalRealtyOfferIdManager, RealtyOfferIdManager}
import ru.yandex.vos2.realty.services.sender.ProbeSenderClient
import ru.yandex.vos2.services.mds.{MdsPhotoUtils, RealtyMdsSettings}
import ru.yandex.vos2.util.ConfigUtils._
import ru.yandex.vos2.util.environment.Env

import scala.concurrent.duration._

/**
  * @author roose
  */
object TestRealtyCoreComponents
  extends ExtdataComponents(TestConfigProvider.testConfig)
  with ExtdataRegionGraphComponents
  with ExtdataCurrencyComponents
  with RealtyCoreComponents {

  override val env: Env = new Env(new TestEnvProvider)

  val mySqlConfig = new MySqlConfig(env.getConfig("vos2.mysql"))

  override lazy val mySql: RealtyMySql = new RealtyMySql(mySqlConfig, None)

  val offerIdManager: RealtyOfferIdManager = LocalRealtyOfferIdManager(mySqlConfig)

  override lazy val userDao: RealtyUserDao =
    new RealtyUserDaoImpl(mySql)

  override lazy val zkCommonClient: CuratorFramework = CuratorFrameworkFactory.newClient(
    env.getConfig("vos2.vertis-scheduler.zookeeper").string("conn-string"),
    new ExponentialBackoffRetry(1000, 3)
  )

  override val notificationManager: NotificationManager = RealtyNotificationManager

  override lazy val features: RealtyFeatures = new SimpleFeatures with RealtyFeatures

  override lazy val offerDao: RealtyOfferDao =
    new RealtyOfferDaoImpl(
      mySql,
      features,
      env.props,
      currencyProvider
    )

  override lazy val offerExportDao: RealtyOfferDao =
    new RealtyOfferDaoImpl(mySql, features, env.props, currencyProvider) {
      override val access: RealtyOfferAccess = new RealtyOfferExportAccess(defaultVisitDelay, features)
    }

  override val senderClient: ProbeSenderClient = new ProbeSenderClient

  override lazy val ops: OperationalSupport = ???

  override lazy val appConfig: AppConfig = ???

  override def crypto: CryptoUtils.Crypto = ???

  override lazy val socialClient: SocialClient = ???

  override def brokerClient: BrokerClient = ???

  override def phoneUserRefsDao: PhoneUserRefsDao = new PhoneUserRefsDaoImpl(mySql)

  override val minibbDao: RealtyUserAuthCodeDao = new RealtyUserAuthCodeDaoImpl(mySql)

  override val offerTransferQueueDao: OfferTransferQueueDao = new OfferTransferQueueDaoImpl(mySql)

  override def mdsPhotoUtils: MdsPhotoUtils =
    MdsPhotoUtils.fromConfig(env.getConfig("vos2.realty.avatars"), RealtyMdsSettings)

  override def mdsUrlBuilder: MdsUrlBuilder = new MdsUrlBuilder(
    env.props.getString("vos2.realty.avatars.read.url")
  )
}

object TestConfigProvider {

  val testConfig = ExtdataClientConfig(
    remoteUrl = "http://realty-resource-service-api.vrts-slb.test.vertis.yandex.net/",
    maxWeight = 40,
    maxConcurrentTasks = 10,
    extDataPath = "/tmp/extdata",
    replicatePeriod = 1.minute,
    awaitDuration = 600.seconds,
    replicationNumRetries = None
  )
}
