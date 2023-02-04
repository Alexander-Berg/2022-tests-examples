package ru.yandex.vertis.moderation.model.signal

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.{DetailedReason, SignalKey}
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class SignalSetSpec extends SpecBase {

  /**
    * Sometimes simple `SignalGen.next(n)` would generate
    * signals with same keys, which, converted to SignalSet, will collide.
    * This method fixes it by generating unique signal keys.
    *
    * Don't invoke it with large `n`
    *
    * @param n number of signals generated
    * @return signals with different keys
    */
  private def genSignals(n: Int): List[Signal] = {
    (1 to n)
      .foldLeft(Map.empty[SignalKey, Signal]) { case (map, _) =>
        val signal = SignalGen.suchThat(s => !map.contains(s.key)).next
        map + (signal.key -> signal)
      }
      .values
      .toList
  }

  "equals" should {

    "correctly works" in {
      val signal = BanSignalGen.next
      val left = SignalSet(signal)
      val right = SignalSet(signal)

      left should be(right)
    }

    "return false if argument is not SignalSet" in {
      val signals = genSignals(2)
      val signalSet = SignalSet(signals)

      signalSet should not be equal(signals)
    }

    "return false if different signals" in {
      val left = SignalSet(BanSignalGen.next)
      val right = SignalSet(UnbanSignalGen.next)

      left should not be equal(right)
    }

    "return false if different size" in {
      val signal = BanSignalGen.next
      val first = SignalSet(signal)
      val second = SignalSet(signal, UnbanSignalGen.next)

      first should not be equal(second)
      second should not be equal(first)
    }

    "return false if same signal but different value" in {
      val signal = BanSignalGen.next
      val left = SignalSet(signal.copy(detailedReason = DetailedReason.Sold))
      val right = SignalSet(signal.copy(detailedReason = DetailedReason.Commercial))

      left should not be equal(right)
    }

    "return false if same signal but different switch offs" in {
      val signal = BanSignalGen.next
      val switchOff = SignalSwitchOffGen.next
      val switchOffKey = StringGen.next
      SignalSet(signal).withSwitchOffs(Map(switchOffKey -> switchOff))
      val left = SignalSet(signal).withSwitchOffs(Map(switchOffKey -> switchOff))
      val right = SignalSet(signal)

      left should not be equal(right)
    }
  }

  "delete key (-)" should {

    "return same set if no such key" in {
      val nonexistentKey = UnbanSignalGen.next.key
      val signalSet = SignalSet(BanSignalGen.next)

      val actualResult = signalSet - nonexistentKey
      actualResult should be(signalSet)
    }

    "correctly delete signal by key" in {
      val signal = BanSignalGen.next
      val signalToDelete = UnbanSignalGen.next
      val signalSet = SignalSet(signal, signalToDelete)

      val actualResult = signalSet - signalToDelete.key
      val expectedResult = SignalSet(signal)

      actualResult should be(expectedResult)
    }
  }

  "subtract (--)" should {

    "delete all signals that contains in left (signals can be different by timestamp)" in {
      val signal = BanSignalGen.next
      val leftSignal = IndexErrorSignalGen.next
      val rightSignal = UnbanSignalGen.next

      val left = SignalSet(signal.copy(timestamp = DateTimeGen.next), leftSignal)
      val right = SignalSet(signal.copy(timestamp = DateTimeGen.next), rightSignal)

      val actualResult = left -- right
      val expectedResult = SignalSet(leftSignal)

      actualResult should be(expectedResult)
    }

    "correctly delete signals by keys" in {
      val signals = genSignals(5)
      val (signalsAList, signalsBList) = signals.splitAt(3)
      val signalsA = SignalSet(signalsAList)
      val signalsB = SignalSet(signalsBList)
      val signalSet = signalsA ++ signalsB

      (signalSet -- Set.empty[SignalKey]) shouldBe signalSet
      (signalSet -- signalsB.keySet) shouldBe signalsA
      (signalSet -- signalsA.keySet) shouldBe signalsB
    }

    "correctly delete signals" in {
      val signals = genSignals(5)
      val (signalsAList, signalsBList) = signals.splitAt(3)
      val signalsA = SignalSet(signalsAList)
      val signalsB = SignalSet(signalsBList)
      val signalSet = signalsA ++ signalsB

      (signalSet -- SignalSet.Empty) shouldBe signalSet
      (signalSet -- signalsB) shouldBe signalsA
      (signalSet -- signalsA) shouldBe signalsB
    }
  }

  "strict subtract" should {

    "delete all signals that contains in left" in {
      val signal = BanSignalGen.next
      val leftSignal = IndexErrorSignalGen.suchThat(_.key != signal.key).next
      val rightSignal = UnbanSignalGen.suchThat(s => s.key != signal.key && s.key != leftSignal.key).next

      val t1 = DateTimeGen.next
      val t2 = DateTimeGen.suchThat(_ != t1).next
      val leftBan = signal.copy(timestamp = t1, switchOff = signal.switchOff.map(_.copy(timestamp = t1)))
      val rightBan = signal.copy(timestamp = t2, switchOff = signal.switchOff.map(_.copy(timestamp = t2)))

      val left = SignalSet(leftBan, leftSignal)
      val right = SignalSet(rightBan, rightSignal)

      val actualResult = left.strictSubtract(right)
      val expectedResult = SignalSet(leftBan, leftSignal)

      actualResult should be(expectedResult)
    }

    "correctly delete signals" in {
      val signals = genSignals(5)
      val (signalsAList, signalsBList) = signals.splitAt(3)
      val signalsA = SignalSet(signalsAList)
      val signalsB = SignalSet(signalsBList)
      val signalSet = signalsA ++ signalsB

      (signalSet.strictSubtract(SignalSet.Empty)) shouldBe signalSet
      (signalSet.strictSubtract(signalsB)) shouldBe signalsA
      (signalSet.strictSubtract(signalsA)) shouldBe signalsB
    }
  }

  "replace (++)" should {

    "replace only if signals different not by timestamp" in {
      val signal = BanSignalGen.withoutSwitchOff.next
      val newSignal = UnbanSignalGen.withoutSwitchOff.next

      val signalToReplace =
        IndexErrorSignalGen.withoutSwitchOff.next.copy(detailedReasons = Set(DetailedReason.Another))
      val replacedSignal = signalToReplace.copy(detailedReasons = Set(DetailedReason.Commercial))

      val left = SignalSet(signal, signalToReplace)
      val right = SignalSet(signal.copy(timestamp = DateTimeGen.next), replacedSignal, newSignal)

      val actualResult = left ++ right
      val expectedResult = SignalSet(signal, replacedSignal, newSignal)

      actualResult should be(expectedResult)
    }

    "not replace if signals different only by timestamp" in {
      val oldSignal = BanSignalGen.withoutSwitchOff.next
      val newSignal = oldSignal.copy(timestamp = DateTimeGen.next)

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left ++ right).toSeq.head
      val expectedResult = oldSignal

      actualResult should be(expectedResult)
    }

    "add switch off if right operand contains switch off" in {
      val oldSignal = BanSignalGen.withoutSwitchOff.next
      val switchOff = SignalSwitchOffGen.next
      val newSignal =
        oldSignal
          .withSwitchOff(Some(switchOff))
          .copy(timestamp = oldSignal.timestamp.plusDays(1))

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left ++ right).toSeq.head
      val expectedResult = oldSignal.withSwitchOff(Some(switchOff))

      actualResult shouldBe expectedResult
    }

    "save switch off if left operand contains switch off" in {
      val switchOff = SignalSwitchOffGen.next
      val oldSignal = BanSignalGen.next.withSwitchOff(Some(switchOff))
      val newSignal =
        oldSignal
          .withSwitchOff(None)
          .withTimestamp(oldSignal.timestamp.plusDays(1))

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left ++ right).toSeq.head
      val expectedResult = oldSignal

      actualResult shouldBe expectedResult
    }

    "replace switch off" in {
      val oldSwitchOff = SignalSwitchOffGen.next
      val oldSignal = BanSignalGen.next.withSwitchOff(Some(oldSwitchOff))

      val newSwitchOff = SignalSwitchOffGen.next
      val newSignal =
        oldSignal
          .withSwitchOff(Some(newSwitchOff))
          .copy(timestamp = oldSignal.timestamp.plusDays(1))

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left ++ right).toSeq.head
      val expectedResult = oldSignal.withSwitchOff(Some(newSwitchOff))

      actualResult shouldBe expectedResult
    }
  }

  "replace (+++)" should {

    "replace signals if they different not by timestamp" in {
      val signal = BanSignalGen.withoutSwitchOff.next
      val newSignal = UnbanSignalGen.withoutSwitchOff.next

      val signalToReplace =
        IndexErrorSignalGen.withoutSwitchOff.next.copy(detailedReasons = Set(DetailedReason.Another))
      val replacedSignal = signalToReplace.copy(detailedReasons = Set(DetailedReason.Commercial))

      val left = SignalSet(signal, signalToReplace)
      val right = SignalSet(signal.copy(timestamp = DateTimeGen.next), replacedSignal, newSignal)

      val actualResult = left ++ right
      val expectedResult = SignalSet(signal, replacedSignal, newSignal)

      actualResult should be(expectedResult)
    }

    "replace if signals different only by timestamp" in {
      val oldSignal = BanSignalGen.withoutSwitchOff.next
      val newSignal = oldSignal.copy(timestamp = DateTimeGen.next)

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left +++ right).toSeq.head
      val expectedResult = newSignal

      actualResult should be(expectedResult)
    }

    "adds switch off if right operand contains switch off" in {
      val oldSignal = BanSignalGen.withoutSwitchOff.next
      val switchOff = SignalSwitchOffGen.next
      val newSignal = oldSignal.withSwitchOff(Some(switchOff))

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left +++ right).toSeq.head
      val expectedResult = oldSignal.withSwitchOff(Some(switchOff))

      actualResult shouldBe expectedResult
    }

    "saves switch off if left operand contains switch off" in {
      val switchOff = SignalSwitchOffGen.next
      val oldSignal = BanSignalGen.next.withSwitchOff(Some(switchOff))
      val newSignal = oldSignal.withSwitchOff(None)

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left +++ right).toSeq.head
      val expectedResult = oldSignal.withSwitchOff(Some(switchOff))

      actualResult shouldBe expectedResult
    }

    "replaces switch off" in {
      val oldSwitchOff = SignalSwitchOffGen.next
      val oldSignal = BanSignalGen.next.withSwitchOff(Some(oldSwitchOff))

      val newSwitchOff = SignalSwitchOffGen.next
      val newSignal =
        oldSignal
          .withSwitchOff(Some(newSwitchOff))
          .copy(timestamp = oldSignal.timestamp.plusDays(1))

      val left = SignalSet(oldSignal)
      val right = SignalSet(newSignal)

      val actualResult = (left +++ right).toSeq.head
      val expectedResult = newSignal

      actualResult shouldBe expectedResult
    }
  }

  "collectWithMaxTimestamp" should {

    "return correct result" in {
      val oldSignal =
        BanSignalGen.next.copy(timestamp = DateTimeUtil.now().minusDays(1), detailedReason = DetailedReason.IsAd)
      val newSignal = BanSignalGen.next.copy(timestamp = DateTimeUtil.now(), detailedReason = DetailedReason.AreaError)
      val signalSet = SignalSet(oldSignal, newSignal)

      val actualResult =
        signalSet.collectWithMaxTimestamp { case s @ (_: BanSignal) =>
          s
        }
      val expectedResult = Some(newSignal)

      actualResult should be(expectedResult)
    }

    "return None if no such signals" in {
      val actual =
        SignalSet.Empty.collectWithMaxTimestamp { case s @ (_: BanSignal) =>
          s
        }

      actual should be(None)
    }
  }

  "withoutInherited" should {

    def notInherited(gen: Gen[Signal]): Gen[Signal] = gen.suchThat(!_.isInherited)
    def inherited(gen: Gen[Signal]): Gen[Signal] = gen.suchThat(_.isInherited)

    "return signals without inherited" in {
      SignalSet.Empty.onlyNotInherited should be(SignalSet.Empty)
      SignalSet(
        notInherited(BanSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next
      ).onlyNotInherited.size should be(1)
      SignalSet(notInherited(WarnSignalGen).next).onlyNotInherited.size should be(1)
      SignalSet(notInherited(HoboSignalGen).next).onlyNotInherited.size should be(1)
      SignalSet(inherited(BanSignalGen).next).onlyNotInherited.size should be(0)
      SignalSet(
        inherited(BanSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next
      ).onlyNotInherited.size should be(0)
      SignalSet(inherited(WarnSignalGen).next).onlyNotInherited.size should be(0)
      SignalSet(inherited(HoboSignalGen).next).onlyNotInherited.size should be(0)
      SignalSet(notInherited(BanSignalGen).next, inherited(BanSignalGen).next).onlyNotInherited.size should be(1)
      SignalSet(notInherited(BanSignalGen).next, inherited(HoboSignalGen).next).onlyNotInherited.size should be(1)
      SignalSet(
        notInherited(BanSignalGen).next,
        inherited(HoboSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next,
        notInherited(WarnSignalGen).next
      ).onlyNotInherited.size should be(2)
    }

    "return signals with inherited" in {
      SignalSet.Empty.onlyInherited should be(SignalSet.Empty)
      SignalSet(
        inherited(BanSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next
      ).onlyInherited.size should be(1)
      SignalSet(inherited(WarnSignalGen).next).onlyInherited.size should be(1)
      SignalSet(inherited(HoboSignalGen).next).onlyInherited.size should be(1)
      SignalSet(notInherited(BanSignalGen).next).onlyInherited.size should be(0)
      SignalSet(
        notInherited(BanSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next
      ).onlyInherited.size should be(0)
      SignalSet(notInherited(WarnSignalGen).next).onlyInherited.size should be(0)
      SignalSet(notInherited(HoboSignalGen).next).onlyInherited.size should be(0)
      SignalSet(inherited(BanSignalGen).next, notInherited(BanSignalGen).next).onlyInherited.size should be(1)
      SignalSet(inherited(BanSignalGen).next, notInherited(HoboSignalGen).next).onlyInherited.size should be(1)
      SignalSet(
        inherited(BanSignalGen).next,
        notInherited(HoboSignalGen.map(_.withSwitchOff(Some(SignalSwitchOffGen.next)))).next,
        inherited(WarnSignalGen).next
      ).onlyInherited.size should be(2)
    }
  }

  "withSwitchOffs" should {
    "add switchOff for an absent signal" in {
      val signal = BanSignalGen.withoutSwitchOff.next
      val switchOffKey = SignalGen.map(_.key).suchThat(signal.key != _).next
      val switchOff = SignalSwitchOffGen.next

      val signalSet = SignalSet(signal)

      val actualResult = signalSet.withSwitchOffs(Map(switchOffKey -> switchOff))
      val expectedResult = SignalSet(signal).withSwitchOffs(Map(switchOffKey -> switchOff))

      actualResult shouldBe expectedResult
    }

    "overwrite signal switchOff" in {
      val signal = BanSignalGen.withoutSwitchOff.next
      val switchOff = SignalSwitchOffGen.next

      val signalSet = SignalSet(signal)

      val actualResult = signalSet.withSwitchOffs(Map(signal.key -> switchOff))
      val expectedResult = SignalSet(signal.withSwitchOff(Some(switchOff)))

      actualResult shouldBe expectedResult
    }
  }

  "apply" should {
    "drop all switch offs from signal objects" in {

      val signal =
        BanSignalGen.withoutSwitchOff.next
          .copy(switchOff = Some(SignalSwitchOffGen.next))

      val signalSet =
        SignalSet(Map(signal.key -> Right(signal)), Map.empty[SignalKey, Either[Tombstone, SignalSwitchOff]])

      val Right(internalSignal) = signalSet.signalMap(signal.key)

      internalSignal shouldBe signal.withSwitchOff(None)
      signalSet.switchOffMap shouldBe empty
    }

    "not mutate passed SignalSet" in {
      val signalSet = SignalSetGen.next

      SignalSet(signalSet) shouldBe signalSet
    }

    "move switch offs from signal objects to switch off map" in {
      val switchOff = SignalSwitchOffGen.next
      val signal =
        BanSignalGen.next
          .copy(switchOff = Some(switchOff))

      val signalSet = SignalSet(signal)

      val Right(storedSignal) = signalSet.signalMap(signal.key)
      val Right(storedSwitchOff) = signalSet.switchOffMap(signal.key)

      storedSignal shouldBe signal.withSwitchOff(None)
      storedSwitchOff shouldBe switchOff
    }
  }

  "updateTime" should {
    "collect max timestamp among update and delete timestamps" in {
      val List(first, second, third, fourth) = DateTimeGen.next(4).toList
      val max = List(first, second, third, fourth).maxBy(_.getMillis)

      val signalSet =
        SignalSet(
          Map(StringGen.next -> Right(WarnSignalGen.next.copy(timestamp = first))),
          Map(StringGen.next -> Right(SignalSwitchOffGen.next.copy(timestamp = second)))
        ).withSwitchOffTombstones(Map(StringGen.next -> Tombstone(third, None)))
          .withSignalTombstones(Map(StringGen.next -> Tombstone(fourth, None)))

      signalSet.updateTime shouldBe Some(max)
    }
  }

  "get" should {
    "attach switch off" in {
      val switchOff = SignalSwitchOffGen.next
      val signal = BanSignalGen.withoutSwitchOff.next
      val signalKey = StringGen.next

      val signalSet =
        SignalSet(
          Map(signalKey -> Right(signal)),
          Map(signalKey -> Right(switchOff))
        )

      val actual = signalSet.get(signalKey)
      val expected = Some(signal.withSwitchOff(Some(switchOff)))

      actual shouldBe expected
    }
  }

  "switchOffs" should {
    "return all switch offs" in {
      val signalSwitchOff = SignalSwitchOffGen.next
      val switchOffWithoutSignal = SignalSwitchOffGen.next
      val signal = BanSignalGen.next
      val switchOffKey = StringGen.suchThat(_ != signal.key).next

      val switchOffMap =
        Map(
          switchOffKey -> Right(switchOffWithoutSignal),
          signal.key -> Right(signalSwitchOff)
        )
      val signalSet = SignalSet(Map(signal.key -> Right(signal)), switchOffMap)

      val actual = signalSet.switchOffs
      val expected =
        Map(
          switchOffKey -> switchOffWithoutSignal,
          signal.key -> signalSwitchOff
        )

      actual shouldBe expected
    }
  }
}
