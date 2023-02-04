package ru.yandex.vertis.billing.service

import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{CompactBindingRequestGen, CustomerIdGen, Producer}
import ru.yandex.vertis.billing.model_core.{Binding, BindingFilter, CampaignHeader, CustomerId}

import scala.collection.mutable

/**
  * Specs on [[BindingService]]
  *
  * @author alex-kovalenko
  */
trait BindingServiceSpec extends AnyWordSpec with Matchers with TryValues {

  def campaignFor(customerId: CustomerId): CampaignHeader

  def service: BindingService

  "BindingService" should {
    "scan bindings" when {
      "got filter ForCustomer" in {
        val customerId :: otherCustomerId :: Nil = CustomerIdGen.next(2).toList

        val campaign = campaignFor(customerId)
        val otherCampaign = campaignFor(otherCustomerId)

        val request = CompactBindingRequestGen.next
          .copy(campaignId = campaign.id, isDeleted = false)
        val otherRequest = CompactBindingRequestGen.next
          .copy(campaignId = otherCampaign.id, isDeleted = false)

        service.update(request).success.value
        service.update(otherRequest).success.value

        val bindings: mutable.ArrayBuffer[Binding] = mutable.ArrayBuffer()
        service.scan(BindingFilter.ForCustomer(customerId))(bindings += _)

        bindings.map(_.point.offerId).toSet shouldBe request.offerIds.toSet

        val otherBindings: mutable.ArrayBuffer[Binding] = mutable.ArrayBuffer()
        service.scan(BindingFilter.ForCustomer(otherCustomerId))(otherBindings += _)

        otherBindings.map(_.point.offerId).toSet shouldBe otherRequest.offerIds.toSet
      }

      "got filter ForCustomer with includeDeleted = true" in {
        val customerId = CustomerIdGen.next
        val campaign = campaignFor(customerId)

        val request = CompactBindingRequestGen.next
          .copy(campaignId = campaign.id, isDeleted = false)
        val deletedRequest = CompactBindingRequestGen.next
          .copy(campaignId = campaign.id, isDeleted = true)

        (service.update(request) should be).a(Symbol("Success"))
        (service.update(deletedRequest) should be).a(Symbol("Success"))

        val notDeleted: mutable.ArrayBuffer[Binding] = mutable.ArrayBuffer()
        service.scan(BindingFilter.ForCustomer(customerId, includeDeleted = false))(notDeleted += _)
        notDeleted.map(_.point.offerId).toSet shouldBe request.offerIds.toSet

        val all: mutable.ArrayBuffer[Binding] = mutable.ArrayBuffer()
        service.scan(BindingFilter.ForCustomer(customerId, includeDeleted = true))(all += _)
        all.map(_.point.offerId).toSet shouldBe (request.offerIds ++ deletedRequest.offerIds).toSet
      }
    }
  }
}
