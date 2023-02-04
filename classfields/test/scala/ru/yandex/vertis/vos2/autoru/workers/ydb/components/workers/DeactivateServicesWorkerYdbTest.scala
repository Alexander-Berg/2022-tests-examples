package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, Inspectors}
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vos2.OfferModel.OfferFlag._
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.old.proxy.OldDbWriter
import ru.yandex.vos2.autoru.model.AutoruSale.{Badge, PaidService}
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

import java.util
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DeactivateServicesWorkerYdbTest extends AnyWordSpec with MockitoSupport with InitTestDbs with BeforeAndAfter {

  before {
    initOldSalesDbs()
  }

  val featuresRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featuresRegistry)

  implicit val traced: Traced = Traced.empty

  val tracingSupport: TracingSupport = {
    val endpointConfig = EndpointConfig("component", "localhost", 36240)
    LocalTracingSupport(endpointConfig)
  }

  val workersTokensDistribution = new AtomicReference[Option[WorkDistributionData]](None)

  abstract class Fixture {

    val worker = new DeactivateServicesWorkerYdb(
      components.oldDbWriter: OldDbWriter
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }
  import components._

  private val userSaleId = 1043270830 // объявление от частника с активными услугами

  private val dealerSaleId = 1044159039 // объявление клиента с активной услугой

  private lazy val userOffer = getOfferById(userSaleId) // expire_date = Thu Oct 20 14:37:02 MSK 2016

  private lazy val dealerOffer = getOfferById(dealerSaleId) // expire_date = Wed Sep 21 22:42:17 MSK 2016

  private val allFlags = OfferFlag.values().toSet

  private val inactiveFlags = Set(OF_DELETED, OF_DRAFT, OF_BANNED, OF_USER_BANNED, OF_EXPIRED, OF_INACTIVE)

  private val activeFlags = allFlags -- inactiveFlags

  for {
    flag <- activeFlags
  } (s"not handle dealer offer with $flag") in new Fixture {
    checkNotChanged(dealerOffer.toBuilder.clearFlag().putFlag(flag).build(), worker)
  }

  for {
    flag <- allFlags
  } (s"not handle user offer with $flag") in new Fixture {
    checkNotChanged(userOffer.toBuilder.clearFlag().putFlag(flag).build(), worker)
  }

  for {
    flag <- inactiveFlags
  } (s"handle dealer offer with $flag") in new Fixture {
    addBadgeToOld(dealerOffer.getOfferIRef, "test-badge")
    val servicesBefore = getServicesFromOld(dealerOffer.getOfferIRef)
    val offer = dealerOffer.toBuilder.clearFlag().putFlag(flag).build()
    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(result.getOfferAutoru.getServicesList.asScala.forall(!_.getIsActive))
    val asInit = result.toBuilder
    asInit.getOfferAutoruBuilder.getServicesBuilderList.asScala.foreach(_.setIsActive(true))
    assert(asInit.build() == offer)
    val servicesAfter = getServicesFromOld(dealerOffer.getOfferIRef)
    assertServicesEquals(servicesAfter, servicesBefore, flipActivated = true)
    assert(autoruSalesDao.getOffer(dealerOffer.getOfferIRef).value.badges.isEmpty)
  }

  (s"should not handle offer without active services") in new Fixture {
    val builder = dealerOffer.toBuilder
    builder.getOfferAutoruBuilder.getServicesBuilderList.asScala.foreach(_.setIsActive(false))
    val offer = builder.build()
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  private def checkNotChanged(offer: Offer, worker: DeactivateServicesWorkerYdb): Unit = {
    if (worker.shouldProcess(offer, None).shouldProcess) {
      val res = worker.process(offer, None)
      assert(res.updateOfferFunc.isEmpty)
    }
  }

  private def getServicesFromOld(saleId: Long) =
    autoruSalesDao.getOffer(saleId).value.services.value

  private def addBadgeToOld(saleId: Long, badge: String) = {
    autoruSalesDao.saveOffer(
      autoruSalesDao
        .getOffer(saleId)
        .value
        .copy(badges = Some(List(Badge(id = 0, saleId, 15, DateTime.now(), isActivated = true, badge)))),
      isNew = false
    )
  }

  private def assertServicesEquals(servicesBefore: List[PaidService],
                                   servicesAfter: List[PaidService],
                                   flipActivated: Boolean = false): Unit = {
    assert(servicesBefore.nonEmpty)
    assert(servicesBefore.size == servicesAfter.size)
    Inspectors.forEvery(servicesBefore.zip(servicesAfter)) {
      case (service1, service2) =>
        util.Arrays.equals(service1.offerBilling.toArray.flatten, service2.offerBilling.toArray.flatten)
        assert(
          service1.copy(
            offerBilling = service2.offerBilling,
            isActivated = service1.isActivated ^ flipActivated
          ) == service2
        )
    }
  }
}
