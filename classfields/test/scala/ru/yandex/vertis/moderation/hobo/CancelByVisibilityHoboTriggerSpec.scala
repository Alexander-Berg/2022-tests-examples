package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Visibility}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class CancelByVisibilityHoboTriggerSpec extends HoboTriggerSpecBase {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)

  private val checkType = HoboCheckTypeGen.next
  private val trigger = new CancelByVisibilityHoboTrigger(checkType)

  "toCreate" should {

    "return None for any input" in {
      forAll(Generators.HoboDeciderSourceGen) { source =>
        trigger.toCreate(source) shouldBe None
      }
    }
  }

  "toCancel" should {
    case class TestCase(description: String, source: HoboDecider.Source, expectedHasTombstone: Boolean)
    val visibilityTestCases =
      Visibility.values.map { visibility =>
        val description = s"correctly works for visibility=$visibility"
        val source =
          newSource(
            visibility = visibility,
            signalSet = signalSetWithUncompleteHoboSignal(checkType),
            diff = newMeaningfulDiff()
          )
        val expectedHasTombstone =
          visibility match {
            case Visibility.DELETED | Visibility.INACTIVE | Visibility.INVALID | Visibility.PAYMENT_REQUIRED |
                 Visibility.BLOCKED =>
              true
            case _ => false
          }
        TestCase(description, source, expectedHasTombstone)
      }
    val otherTestCases =
      Seq(
        TestCase(
          description = "return None if not meaningful diff",
          source =
            newSource(
              visibility = Visibility.DELETED,
              signalSet = signalSetWithUncompleteHoboSignal(checkType),
              diff = newEmptyDiff()
            ),
          expectedHasTombstone = false
        ),
        TestCase(
          description = "return None if hobo signal has wrong check type",
          source =
            newSource(
              visibility = Visibility.DELETED,
              signalSet = signalSetWithUncompleteHoboSignal(HoboCheckTypeGen.suchThat(_ != checkType).next),
              diff = newMeaningfulDiff()
            ),
          expectedHasTombstone = false
        )
      )
    val testCases = visibilityTestCases ++ otherTestCases

    testCases.foreach { case TestCase(description, source, expectedHasTombstone) =>
      s"$description" in {
        val actualHasTombstone = trigger.toCancel(source).isDefined
        actualHasTombstone shouldBe expectedHasTombstone
      }
    }
  }

  private def newMeaningfulDiff(): Diff =
    Diff.Realty(
      Set(
        Model.Diff.Realty.Value.SIGNALS,
        Model.Diff.Realty.Value.CONTEXT
      )
    )

  private def newEmptyDiff(): Diff = Diff.Realty.Empty

  private def signalSetWithUncompleteHoboSignal(checkType: HoboCheckType): SignalSet = {
    val hoboSignal =
      HoboSignalGen.withoutMarker.withoutSwitchOff.next
        .copy(`type` = checkType, result = HoboSignal.Result.Undefined, task = Some(HoboSignalTaskGen.next))
    SignalSet(hoboSignal)
  }

  private def newSource(visibility: Visibility, signalSet: SignalSet, diff: Diff): HoboDecider.Source = {
    val context = ContextGen.next.copy(visibility = visibility)
    val instance = InstanceGen.next.copy(context = context, signals = signalSet)
    Generators.HoboDeciderSourceGen.next.copy(instance = instance, diff = diff)
  }
}
