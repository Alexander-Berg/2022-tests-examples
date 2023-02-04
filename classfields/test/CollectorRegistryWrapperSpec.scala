package common.ops.prometheus.test

import common.ops.prometheus.CollectorRegistryWrapper
import io.prometheus.client.{CollectorRegistry, Counter}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers._

class CollectorRegistryWrapperSpec extends AnyWordSpec {
  private val counter = Counter.build("test", "test").create()
  private val counterSame = Counter.build("test", "test").create()
  private def freshWrapper: CollectorRegistryWrapper = new CollectorRegistryWrapper(new CollectorRegistry())

  "CollectorRegistryWrapper" should {
    "register collector" in {
      val wrapper = freshWrapper
      wrapper.register(counter) shouldBe counter
    }

    "register same collector twice" in {
      val wrapper = freshWrapper
      wrapper.register(counter) shouldBe counter
      wrapper.register(counter) shouldBe counter
    }

    "deduplicate similar collectors" in {
      val wrapper = freshWrapper
      wrapper.register(counter) shouldBe counter
      wrapper.register(counterSame) shouldBe counter
    }

    "unregister collector" in {
      val wrapper = freshWrapper
      wrapper.register(counter) shouldBe counter
      wrapper.unregister(counter)
      wrapper.register(counterSame) shouldBe counterSame
      wrapper.register(counterSame) should not be counter
    }
  }
}
