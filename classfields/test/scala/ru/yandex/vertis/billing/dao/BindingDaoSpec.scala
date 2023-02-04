package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.BindingFilter
import ru.yandex.vertis.billing.model_core.gens.{BindingRequestGen, Producer}

/**
  * Specs on [[BindingDao]]
  *
  * @author dimas
  */
trait BindingDaoSpec extends AnyWordSpec with Matchers {

  protected def bindingDao: BindingDao

  "BindingDao" should {
    val request = BindingRequestGen.next
    val campaignId = request.points.head.campaignId

    "bind offers with campaign" in {
      bindingDao.update(request).get
      val filter = BindingFilter.ForCampaign(campaignId)
      val bindings = bindingDao.get(filter).get
      bindings.foreach { binding =>
        binding.point.campaignId should be(campaignId)
      }
    }

    "mark bindings as deleted" in {
      val toDelete = request.points.map(_.offerId)
      val source = request.points.head.source
      bindingDao.delete(toDelete, source).get
      val filter = BindingFilter.ForOffers(toDelete)
      val bindings = bindingDao.get(filter).get
      bindings.foreach { binding =>
        if (binding.point.source == source)
          binding.point.isDeleted should be(true)
      }
    }

    "remove bindings" in {
      val filter = BindingFilter.ForCampaign(campaignId)

      val exists = bindingDao.get(filter).get
      exists should not be empty

      val request2 = BindingRequestGen.next
      bindingDao.update(request2).get

      val offers = exists.map(_.point.offerId)
      bindingDao.remove(BindingFilter.ForOffers(offers)).get

      bindingDao.get(filter).get should be(empty)

      val campaignId2 = request2.points.head.campaignId
      val anotherFilter = BindingFilter.ForCampaign(campaignId2)
      bindingDao.get(anotherFilter).get should not be empty

      bindingDao.remove(anotherFilter).get

      bindingDao.get(anotherFilter).get shouldBe empty
    }
  }

}
