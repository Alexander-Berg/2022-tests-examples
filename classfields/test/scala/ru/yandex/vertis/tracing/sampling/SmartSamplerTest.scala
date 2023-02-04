package ru.yandex.vertis.tracing.sampling

import java.util.concurrent.ThreadLocalRandom

import org.scalatest.WordSpec
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import org.scalatest.Matchers._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 07.03.17
  */
class SmartSamplerTest extends WordSpec with TableDrivenPropertyChecks {

  private val r = ThreadLocalRandom.current()

  "SmartSampler" should {
    "ensure minimal samples per second" in {
      val sampler = SmartSampler.create(100, 0.0f)
      Thread.sleep(1000) // warm up builtin RateLimiter
      for (i ← 1 to 100) {
        sampler.isSampled(r.nextLong()) should beSampled(i)
      }
    }

    "always sample if sampleRate == 1.0" in {
      val sampler = SmartSampler.create(0, 1.0f)
      for (i ← 1 to 10000) {
        sampler.isSampled(r.nextLong()) should beSampled(i)
      }
    }
    "never sample if sampleRate == 0.0 and minTracesPerSecond == 0" in {
      val sampler = SmartSampler.create(0, 0.0f)
      for (i ← 1 to 10000) {
        sampler.isSampled(r.nextLong()) shouldNot beSampled(i)
      }
    }
    "sample part of traces" in {
      val sampler = SmartSampler.create(1, 0.5f)
      var sampled = 0
      for (_ ← 1 to 10000) {
        if (sampler.isSampled(r.nextLong())) sampled += 1
      }
      sampled shouldBe 5000 +- 500
    }
    "trow exception on incorrect parameters" in {
      val data: TableFor2[Int, Float] = Table(
        ("minTracesPerSecond", "sampleRate"),
        (-1, 0.5f),
        (0, 0.5f),
        (1, -1f),
        (1, 10f)
      )
      forAll(data) { (minTracesPerSecond, sampleRate) ⇒
        intercept[IllegalArgumentException] {
          SmartSampler.create(minTracesPerSecond, sampleRate)
        }
      }
    }
  }

  private def beSampled(i: Long) = Matcher[Boolean](sampled ⇒ {
    MatchResult(
      sampled,
      s"Trace not sampled on $i-th execution",
      s"Trace sampled on $i-th execution"
    )
  })
}
