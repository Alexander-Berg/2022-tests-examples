package ru.yandex.vertis.billing.dao

import java.util.concurrent.{CyclicBarrier, Executors}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.{Assertion, TryValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy._
import ru.yandex.vertis.billing.dao.CampaignDao._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Filter.{ForCampaignCustomer, ForCampaigns, ForCustomer}
import ru.yandex.vertis.billing.service.CampaignService.{EnabledCampaignsFilter, Filter, Patch}
import ru.yandex.vertis.billing.util.{DateTimeUtils, Page}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
  * Specs on [[CampaignDao]]
  */
trait CampaignDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase with TryValues {

  protected def customerDao: CustomerDao

  protected def orderDao: OrderDao

  protected def campaignDao: CampaignDao

  "CampaignDao" should {
    val resource1 = PartnerRef("1")
    val resource2 = PartnerRef("2")
    val resource = Seq(resource1, resource2)
    val clientId = 1
    val agencyId = 2
    val clientId2 = 3
    val directCustomer = CustomerHeader(CustomerId(clientId, None), resource)
    val agencyCustomer = CustomerHeader(CustomerId(clientId, Some(agencyId)), resource)
    val offlineBizCustomer = CustomerHeader(CustomerId(clientId2, None), Seq(OfflineBizRef))
    var directOrder: Order = null
    var agencyOrder: Order = null
    var offlineBizOrder: Order = null
    var directCampaign: CampaignHeader = null
    var agencyCampaign: CampaignHeader = null
    var offlineBizCampaign: CampaignHeader = null

    def createCustomerAndOrder(customer: CustomerHeader): Order = {
      customerDao.create(customer)
      orderDao.create(customer.id, OrderProperties("Text", Some("Description"), actText = Some("actText"))) match {
        case Success(created) =>
          created
        case other =>
          fail(s"Unable to create order: $other")
      }
    }

    def createCampaign(customer: CustomerHeader, source: CampaignService.Source): CampaignHeader = {
      campaignDao.create(customer.id, source, DuplicationPolicy.AllowDuplicates) match {
        case Success(created) =>
          created.customer should be(customer)
          created.name should be(source.name)
          created.order.id should be(source.orderId)
          created.product should be(source.product)
          created
        case other =>
          fail(s"Error while create campaign: $other")
      }
    }

    "prepare dependencies" in {
      directOrder = createCustomerAndOrder(directCustomer)
      agencyOrder = createCustomerAndOrder(agencyCustomer)
      offlineBizOrder = createCustomerAndOrder(offlineBizCustomer)
    }

    "create named non-empty campaign and access it" in {
      val product = ProductGen.next
      val offerIds = OfferIdGen.next(10)

      val directSource = CampaignService.Source(
        Some("Name"),
        directOrder.id,
        product,
        CampaignSettings.Default,
        None,
        offerIds
      )

      val agencySource = CampaignService.Source(
        Some("Name"),
        agencyOrder.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable.empty
      )

      directCampaign = createCampaign(directCustomer, directSource)
      agencyCampaign = createCampaign(agencyCustomer, agencySource)

      campaignDao
        .list(
          Filter.ForCustomer(directCampaign.customer.id),
          Page(0, 10)
        )
        .map(_.toSet) should be(Success(Set(directCampaign)))

      campaignDao
        .list(
          Filter.ForCustomer(agencyCampaign.customer.id),
          Page(0, 10)
        )
        .map(_.toSet) should be(Success(Set(agencyCampaign)))
    }

    "list campaigns with filter All" in {
      val filter = Filter.All
      campaignDao.list(filter, Page(0, 10)).get.toSet should be(Set(agencyCampaign, directCampaign))
    }

    "list campaigns filtered by client" in {
      val filter = Filter.ForClient(clientId)
      campaignDao.list(filter, Page(0, 10)).get.toSet should be(Set(agencyCampaign, directCampaign))
    }

    "list campaigns filtered by direct customer" in {
      val filter = Filter.ForCustomer(directCustomer.id)
      campaignDao.list(filter, Page(0, 10)).get.toSet should be(Set(directCampaign))
    }

    "list campaigns filtered by agency customer" in {
      val filter = Filter.ForCustomer(agencyCustomer.id)
      campaignDao.list(filter, Page(0, 10)).get.toSet should be(Set(agencyCampaign))
    }

    "update campaign product" in {
      val newProduct = ProductGen.next
      val update = CampaignService.Patch(Some(newProduct))
      checkUpdate(directCampaign, update) { updated =>
        updated.product should be(newProduct)
      }
    }

    "successfully get campaigns for empty set of campaign ids" in {
      campaignDao.get(Filter.ForCampaigns(Iterable.empty)) should be(Success(Iterable.empty))
    }

    "update campaign order" in {
      val newOrder = orderDao
        .create(
          directCampaign.customer.id,
          OrderProperties("Another order", Some("Yet another purchase order"), actText = Some("Yet another act text"))
        )
        .get

      val update = CampaignService.Patch(order = Some(newOrder.id))
      checkUpdate(directCampaign, update) { updated =>
        updated.order.id should be(newOrder.id)
      }
    }

    "update campaign name" in {
      val newName = Some("Renamed campaign")
      val update = CampaignService.Patch(name = newName)
      checkUpdate(directCampaign, update) { updated =>
        updated.name should be(newName)
      }
    }

    "update campaign isEnabled to false" in {
      val update = CampaignService.Patch(isEnabled = Some(false))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.isEnabled should be(false)
      }
    }

    "update campaign isEnabled to true" in {
      val update = CampaignService.Patch(isEnabled = Some(true))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.isEnabled should be(true)
      }
    }

    "update campaign settings by empty call settings" in {
      val update = CampaignService.Patch(callSettings = None)
      checkUpdate(directCampaign, update) { updated =>
        updated.settings should
          be(CampaignSettings.Default)
      }
    }

    "update campaign settings by valuable call settings" in {
      val callSettings = CallSettingsGen.sample
      val update = CampaignService.Patch(callSettings = callSettings)
      checkUpdate(directCampaign, update) { updated =>
        updated.settings should be(CampaignSettings.Default.copy(callSettings = callSettings))
      }
    }

    "update campaign platforms" in {
      val platforms = EnabledPlatformsGen.sample
      val update = CampaignService.Patch(platforms = Some(Update(platforms)))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.platforms should (not be empty and be(platforms))
      }
    }

    "update campaign deposit" in {
      val deposit = DepositGen.next
      val update = CampaignService.Patch(deposit = Some(Update(Some(deposit))))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.deposit shouldBe Some(deposit)
      }
    }

    "unset campaign deposit" in {
      val update = CampaignService.Patch(deposit = Some(Update(None)))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.deposit shouldBe None
      }
    }

    "reset campaign platforms" in {
      val update = CampaignService.Patch(platforms = Some(Update(None)))
      checkUpdate(directCampaign, update) { updated =>
        updated.settings.platforms should be(None)
      }
    }

    "set campaign inactive_since" in {
      val campaign = campaignDao.get(ForCampaignCustomer(directCustomer.id, directCampaign.id)).get.head
      val update = CampaignService.Patch(inactiveChange = Some(Update(Some(DateTimeUtils.now()))))
      checkUpdate(campaign, update) { updated =>
        updated.inactiveSince.isDefined should be(true)
        updated.epoch shouldEqual campaign.epoch
      }
    }

    "clean campaign inactive_since" in {
      val campaign = campaignDao.get(ForCampaignCustomer(directCustomer.id, directCampaign.id)).get.head
      val update = CampaignService.Patch(inactiveChange = Some(Update(None)))
      checkUpdate(campaign, update) { updated =>
        updated.inactiveSince.isDefined should be(false)
        updated.epoch shouldEqual campaign.epoch
      }
    }

    "get active campaigns" in {
      campaignDao.get(EnabledCampaignsFilter.All) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
    }

    "get active campaigns with filters" in {
      campaignDao.get(EnabledCampaignsFilter.ForResourceRefType(ResourceRefTypes.Partner)) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
      campaignDao.get(EnabledCampaignsFilter.ForResourceRefType(ResourceRefTypes.OfflineBiz)) match {
        case Success(campaings) if campaings.isEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }

      val product = ProductGen.next
      val offlineBizSource = CampaignService.Source(
        Some("Name"),
        offlineBizOrder.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable()
      )
      offlineBizCampaign = createCampaign(offlineBizCustomer, offlineBizSource)

      campaignDao.get(EnabledCampaignsFilter.ForResourceRefType(ResourceRefTypes.OfflineBiz)) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "get campaigns with filters" in {
      campaignDao.get(Filter.All) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
      campaignDao.get(Filter.ForCustomer(directCustomer.id)) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
      campaignDao.get(Filter.ForCustomer(agencyCustomer.id)) match {
        case Success(campaings) if campaings.nonEmpty =>
          info("Done")
        case other => fail(s"Unexpected $other")
      }
      campaignDao.get(Filter.ForCampaignCustomer(directCustomer.id, directCampaign.id)) match {
        case Success(campaigns) =>
          campaigns should have size 1
          campaigns.head.id shouldBe directCampaign.id
          campaigns.head.order.owner shouldBe directCustomer.id
        case other => fail(s"Unexpected $other")
      }
      campaignDao.get(Filter.ForCampaignCustomer(agencyCustomer.id, agencyCampaign.id)) match {
        case Success(campaigns) =>
          campaigns should have size 1
          campaigns.head.id shouldBe agencyCampaign.id
          campaigns.head.order.owner shouldBe agencyCustomer.id
        case other => fail(s"Unexpected $other")
      }
    }

    "create campaign with target phone" in {
      val product = ProductGen.next
      val phone = PhoneGen.next
      val source = CampaignService.Source(
        Some("Name"),
        directOrder.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable.empty,
        Some(phone)
      )
      val result = campaignDao.create(directCustomer.id, source, DuplicationPolicy.AllowDuplicates)
      result.success.value

      val campaign = result.get
      campaignDao.get(ForCampaigns(campaign.id)).map(_.toList) match {
        case Success(cm :: Nil) =>
          cm.settings.callSettings shouldBe Some(CallSettings(Some(phone), None))
        case other =>
          fail(s"Unexpected $other")
      }

      val newPhone = PhoneGen.next
      val patch = CampaignService.Patch(targetPhone = Some(Update(Some(newPhone))))
      campaignDao.update(directCustomer.id, campaign.id, patch, DuplicationPolicy.AllowDuplicates) match {
        case Success(cm) =>
          cm.settings.callSettings shouldBe Some(CallSettings(Some(newPhone), None))
        case other =>
          fail(s"Unexpected $other")
      }
      val doNotChangePhone = CampaignService.Patch(name = Some("test"))
      campaignDao.update(directCustomer.id, campaign.id, doNotChangePhone, DuplicationPolicy.AllowDuplicates) match {
        case Success(cm) =>
          cm.settings.callSettings shouldBe Some(CallSettings(Some(newPhone), None))
        case other =>
          fail(s"Unexpected $other")
      }

      val deletePhone = CampaignService.Patch(targetPhone = Some(Update(None)))
      campaignDao.update(directCustomer.id, campaign.id, deletePhone, DuplicationPolicy.AllowDuplicates) match {
        case Success(cm) =>
          cm.settings.callSettings shouldBe None
        case other =>
          fail(s"Unexpected $other")
      }

    }

    "allow create duplicates if policy allow" in {
      val customer = CustomerHeaderGen.next
      val order = createCustomerAndOrder(customer)

      val product = ProductGen.next
      val source = CampaignService.Source(
        Some("Name"),
        order.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable.empty
      )
      (campaignDao.create(customer.id, source, DuplicationPolicy.AllowDuplicates) should be).a(Symbol("Success"))
      (campaignDao.create(customer.id, source, DuplicationPolicy.AllowDuplicates) should be).a(Symbol("Success"))

      campaignDao.get(ForCustomer(customer.id)).map(_.size) shouldBe Success(2)
    }

    "disallow create duplicates if policy disallow" in {
      val customer = CustomerHeaderGen.next
      val order = createCustomerAndOrder(customer)

      val product = ProductGen.next
      val source = CampaignService.Source(
        Some("Name"),
        order.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable.empty
      )
      (campaignDao.create(customer.id, source, DisallowDuplicates) should be).a(Symbol("Success"))
      intercept[CampaignWithDuplicateProductException] {
        campaignDao.create(customer.id, source, DisallowDuplicates).get
      }

      campaignDao.get(ForCustomer(customer.id)).map(_.size) shouldBe Success(1)
    }

    "disallow create duplicates in concurrent case" in {
      val customer = CustomerHeaderGen.next
      val order = createCustomerAndOrder(customer)

      val product = ProductGen.next
      val source = CampaignService.Source(
        Some("Name"),
        order.id,
        product,
        CampaignSettings.Default,
        None,
        Iterable.empty
      )
      val parties = 25
      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(
          parties,
          new ThreadFactoryBuilder()
            .setNameFormat("OrderDao4Spec-%d")
            .build()
        )
      )

      val barrier = new CyclicBarrier(parties)

      val future = Future.sequence((0 until parties).map { _ =>
        Future {
          barrier.await()
          campaignDao.create(customer.id, source, DisallowDuplicates).get
        }
      })

      intercept[CampaignWithDuplicateProductException] {
        future.await
      }
      val campaigns = campaignDao.get(ForCustomer(customer.id)).get

      campaigns.size shouldBe 1

    }
  }

  private def checkUpdate(
      campaign: CampaignHeader,
      update: CampaignService.Patch
    )(check: CampaignHeader => Assertion): Assertion =
    campaignDao.update(campaign.customer.id, campaign.id, update, DuplicationPolicy.AllowDuplicates) match {
      case Success(updated) =>
        check(updated)
      case other =>
        fail(s"Error while update campaign: $other")
    }
}
