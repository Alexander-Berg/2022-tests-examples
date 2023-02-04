package ru.yandex.vertis.moisha.impl.autoru_users

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moisha.impl.autoru_users.gens.test.TestData._
import ru.yandex.vertis.moisha.impl.autoru_users.model._
import ru.yandex.vertis.moisha.test.BaseSpec

@RunWith(classOf[JUnitRunner])
class RootAutoRuUsersPolicySpec extends BaseSpec with TestPolicies {

  "RootAutoRuUsersPolicy" should {
    "get item with non-empty prolong price" in {

      val req = createReq(Products.Placement)

      val res = usersPolicy.estimateUserPrice(req).success.value
      res.points.head.product.id shouldBe "placement"
      res.points.head.product.goods.head.prolongPrice shouldBe Some(44400)

      res.points.head.product.total shouldBe 66600
      res.points.head.product.goods.head.price shouldBe 66600
    }

    "get item with non empty experiment" in {
      val req = {
        val generatedRequest = createReq(Products.TurboPackage)
        generatedRequest.copy(
          context = generatedRequest.context.copy(experiment = "EXP21")
        )
      }
      val res = usersPolicy.estimateUserPrice(req).success.value

      res.points.head.product.id shouldBe "turbo-package"

      res.points.head.product.total shouldBe 86545400
      res.points.head.experimentId.get shouldBe "EXP21"

    }

    "get item without prolong price" in {
      val req = createReq(Products.Boost)

      val res = usersPolicy.estimateUserPrice(req).success.value

      res.points.head.product.id shouldBe "boost"
      res.points.head.product.goods.head.prolongPrice shouldBe None

      res.points.head.product.total shouldBe 77700
      res.points.head.product.goods.head.price shouldBe 77700
    }

    "return default price if offer price equals Long.maxValue " in {
      val req = {
        val generatedRequest = createReq(Products.ShowInStories)
        generatedRequest.copy(offer = generatedRequest.offer.copy(price = Long.MaxValue))
      }
      val res = usersPolicy.estimateUserPrice(req).success.value

      res.points.head.product.id shouldBe "show-in-stories"
      res.points.head.product.goods.head.prolongPrice shouldBe None

      res.points.head.product.total shouldBe 179700
      res.points.head.product.goods.size shouldBe 1
      res.points.head.product.goods.head.price shouldBe 179700
    }

    "return price from matrix if price less 922337203685477500" in {
      val req = {
        val generatedRequest = createReq(Products.ShowInStories)
        generatedRequest.copy(offer = generatedRequest.offer.copy(price = 922337203685477500L))
      }

      val res = usersPolicy.estimateUserPrice(req).success.value

      res.points.head.product.id shouldBe "show-in-stories"
      res.points.head.product.goods.head.prolongPrice shouldBe None

      res.points.head.product.total shouldBe 7770000
      res.points.head.product.goods.size shouldBe 1
      res.points.head.product.goods.head.price shouldBe 7770000
    }

  }
}
