package ru.yandex.vertis.billing.service.checking

import java.util.concurrent.{CyclicBarrier, Executors}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalacheck.Gen
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.gens.CampaignSourceGen
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{
  JdbcBindingDao,
  JdbcCampaignCallDao,
  JdbcCampaignDao,
  JdbcCustomerDao,
  JdbcSpecTemplate
}
import ru.yandex.vertis.billing.model_core.{
  CampaignId,
  CustomerHeader,
  CustomerId,
  OrderProperties,
  PartnerRef,
  Product,
  Uid
}
import ru.yandex.vertis.billing.service.impl.CampaignServiceImpl
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Filter

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait ServiceParallelInputCheckerSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with ScalaFutures
  with OneInstancePerTest {

  private val DefaultPatienceConfig =
    PatienceConfig(Span(60, Seconds), Span(1, Seconds))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  implicit val operatorContext: OperatorContext = OperatorContext("test", Uid(0L))

  private val customerDao = new JdbcCustomerDao(billingDatabase)

  private val orderDao = new JdbcOrderDao(billingDualDatabase)

  private val campaignDao = new JdbcCampaignDao(billingDatabase)

  private val bindingDao = new JdbcBindingDao(billingDatabase)

  private val callDao = new JdbcCampaignCallDao(billingDatabase)

  private val DefaultCampaignGen = CampaignSourceGen.map(_.copy(offerIds = Iterable.empty))

  def defaultUserInputCheckerProvider: CampaignService => UserInputChecker

  private val checkedService =
    new CampaignServiceImpl(campaignDao, bindingDao, callDao, DuplicationPolicy.AllowDuplicates)
      with CheckedCampaignService {

      override def checker: UserInputChecker = defaultUserInputCheckerProvider(this)

    }

  def differentProductCountForCreate: Int = 10

  def campaignCountForCreate: Int = 100

  def campaignsCountForUpdate: Int = 10

  def updatersPerCampaign: Int = 5

  def countOfUpdateCycles: Int = 3

  def differentPatches: Int = 20

  def productMaker(id: Int): Product

  private val ServiceCampaignSourceGen = for {
    default <- DefaultCampaignGen
    product <- Gen.choose(1, differentProductCountForCreate).map(productMaker)
    campaign = default.copy(product = product)
  } yield campaign

  private val InitCampaigns: Iterable[CampaignService.Source] = {
    (1 to campaignsCountForUpdate).map { id =>
      DefaultCampaignGen.next.copy(product = productMaker(id))
    }
  }

  private val ServiceCampaignPatchGen: Gen[CampaignService.Patch] = for {
    id <- Gen.choose(1, differentPatches)
  } yield CampaignService.Patch(Some(productMaker(id)))

  private def makeWork[T](sources: Iterable[T], action: (CyclicBarrier, T) => Unit, suffix: String) = {
    val workersCount = sources.size

    implicit val ec = ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        workersCount,
        new ThreadFactoryBuilder()
          .setNameFormat(s"CheckedServiceCampaign$suffix-%d")
          .build()
      )
    )

    val barrier = new CyclicBarrier(workersCount)

    val futures = sources.map { source =>
      Future(action(barrier, source))
    }

    Future.sequence(futures).futureValue
  }

  "CheckedCampaignService" should {
    val resource = Seq(PartnerRef("1"), PartnerRef("2"))
    val clientId = 1
    val customer = CustomerHeader(CustomerId(clientId, None), resource)
    customerDao.create(customer)
    val order = orderDao.create(customer.id, OrderProperties("Text", Some("Description"))) match {
      case Success(created) =>
        created
      case other =>
        fail(s"Unable to create order: $other")
    }

    "be consistent when create in parallel" in {

      val campaigns = ServiceCampaignSourceGen
        .map(_.copy(orderId = order.id))
        .next(campaignCountForCreate)

      makeWork(
        campaigns,
        (barrier: CyclicBarrier, campaign: CampaignService.Source) => {
          barrier.await()
          checkedService.create(customer.id, campaign).get: @nowarn("msg=discarded non-Unit value")
        },
        "Creator"
      )

      val observed = campaignDao.get(Filter.ForCustomer(customer.id)).get.size
      val expected = campaigns.groupBy(_.product).keys.size
      observed shouldBe expected
    }

    "be consistent when update in parallel" in {

      val campaignIds = InitCampaigns
        .map { source =>
          checkedService.create(customer.id, source.copy(orderId = order.id)).get.id
        }
        .flatMap { id =>
          List.fill(updatersPerCampaign)(id)
        }

      makeWork(
        campaignIds,
        (barrier: CyclicBarrier, campaignId: CampaignId) => {
          for (_ <- 1 to countOfUpdateCycles) {
            barrier.await()
            checkedService.update(customer.id, campaignId, ServiceCampaignPatchGen.next)
          }
        },
        "Updater"
      )

      val observedCampaigns = campaignDao.get(Filter.ForCustomer(customer.id)).get

      val expectedProducts = InitCampaigns.size
      val observedSize = observedCampaigns.size
      observedSize shouldBe expectedProducts

      val observedProducts = observedCampaigns.map(_.product).toSet.size
      observedProducts shouldBe expectedProducts
    }
  }

}
