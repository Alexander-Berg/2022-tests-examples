package ru.auto.api.extdata

import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 02.03.17
  */
class ProxyUtilsSpec extends BaseSpec {

  case class Target(private val value: String) {
    def getValue: String = value
  }

  "ProxyUtils" should {
    "create proxy" in {
      val target = Target("val1")
      val (proxy, _) = ProxyUtils.createProxy(target, lazyLoad = false)
      proxy.getValue shouldBe "val1"
    }
    "create replacer method for proxy".which {
      "should replace target" in {
        val target = Target("val1")
        val (proxy, replacer) = ProxyUtils.createProxy(target, lazyLoad = false)
        proxy.getValue shouldBe "val1"

        replacer(Target("val2"))

        proxy.getValue shouldBe "val2"
      }

      "return old value" in {
        val target = Target("val1")
        val (proxy, replacer) = ProxyUtils.createProxy(target, lazyLoad = false)
        proxy.getValue shouldBe "val1"

        val oldValue = replacer(Target("val2"))
        oldValue shouldBe target
      }
    }
    "create non lazy proxy" in {
      var created = false
      ProxyUtils.createProxy({
        created = true
        Target("val1")
      }, lazyLoad = false)

      created shouldBe true
    }
    "create lazy proxy" in {
      var created = false
      val (proxy, _) = ProxyUtils.createProxy({
        created = true
        Target("val1")
      }, lazyLoad = true)

      created shouldBe false
      proxy.getValue
      created shouldBe true
    }
  }
}
