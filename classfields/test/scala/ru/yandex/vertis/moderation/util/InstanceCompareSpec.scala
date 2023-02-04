package ru.yandex.vertis.moderation.util

import org.joda.time.DateTime
import org.mockito.Mockito.when
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Instance}

class InstanceCompareSpec extends SpecBase {

  "InstanceCompare.compareOnConditionWithFallback" should {
    val now = DateTimeUtil.now()
    val afterNow = now.plusMinutes(1)

    val ordering =
      new Ordering[Instance] {
        def compare(a: Instance, b: Instance): Int =
          InstanceCompare.compareOnConditionWithFallback(
            a = a,
            b = b,
            compareFn = InstanceCompare.compareOnIsFromCallCenter,
            fallback = InstanceCompare.compareOnCreateTime
          )
      }

    def setMock(time: DateTime, isCallCenter: Boolean): Instance = {
      val instance = mock[Instance]
      val essentials = mock[AutoruEssentials]
      when(instance.essentials).thenReturn(essentials)
      when(essentials.timestampCreate).thenReturn(Some(time))
      when(essentials.isCallCenter).thenReturn(isCallCenter)
      when(essentials.getIsFromCallCenter).thenReturn(Some(isCallCenter))
      instance
    }

    "prefer offers that are not from call center" in {
      val a = setMock(time = now, isCallCenter = true)
      val b = setMock(time = now, isCallCenter = false)

      ordering.max(a, b) shouldBe b
      ordering.max(b, a) shouldBe b
    }

    "prefer offers that are older" in {
      val a = setMock(time = now, isCallCenter = false)
      val b = setMock(time = afterNow, isCallCenter = false)
      val c = setMock(time = now, isCallCenter = true)
      val d = setMock(time = afterNow, isCallCenter = true)

      ordering.max(a, b) shouldBe a
      ordering.max(b, a) shouldBe a
      ordering.max(c, d) shouldBe c
      ordering.max(d, c) shouldBe c
    }
  }
}
