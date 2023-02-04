package ru.yandex.vertis.moderation.consumer

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, Instance}
import ru.yandex.vertis.moderation.photo.duplicates.{Duplicate, NotDuplicate, PhotoDuplicateDecider, Verdict, Warn}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author akhazhoyan 03/2019
  */
@RunWith(classOf[JUnitRunner])
class DuplicateByPhotoConsumerSpec extends SpecBase {

  case class TestCase(description: String,
                      decidersResults: Seq[Option[Verdict]],
                      expectedResult: Option[Verdict],
                      callsExpected: Int
                     )

  class NaiveDecider(result: Option[Verdict], var beenCalled: Boolean = false) extends PhotoDuplicateDecider {
    override def decide(instance: Instance): Future[Option[Verdict]] = {
      beenCalled = true
      Future.successful(result)
    }
    override def isDefinedAt(diff: Diff): Boolean = true
    override def name: String = "naiveDecider"
  }

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "return None for no deciders",
        decidersResults = Seq.empty,
        expectedResult = None,
        callsExpected = 0
      ),
      TestCase(
        description = "return None if all deciders returned None",
        decidersResults = Seq(None, None, None, None),
        expectedResult = None,
        callsExpected = 4
      ),
      TestCase(
        description = "return NotDuplicate if at least one decider returned NotDuplicate",
        decidersResults = Seq(None, Some(NotDuplicate), None, None),
        expectedResult = Some(NotDuplicate),
        callsExpected = 4
      ),
      TestCase(
        description = "return first Duplicates result and not attempt to call the rest deciders",
        decidersResults =
          Seq(
            Some(NotDuplicate),
            None,
            Some(Duplicate.fromDuplicates(Set.empty, info = "1", action = Warn, auxInfo = None, Nil)),
            None,
            None,
            Some(Duplicate.fromDuplicates(Set.empty, info = "2", action = Warn, auxInfo = None, Nil)),
            None,
            Some(NotDuplicate)
          ),
        expectedResult = Some(Duplicate.fromDuplicates(Set.empty, info = "1", action = Warn, auxInfo = None, Nil)),
        callsExpected = 3
      )
    )

  "findDuplicates" should {
    testCases.foreach { testCase =>
      import testCase._
      description in {
        val instance = InstanceGen.next
        val deciders = decidersResults.map(new NaiveDecider(_))
        val actualResult = DuplicateByPhotoConsumer.findDuplicates(instance, deciders, notDuplicate = false).futureValue
        val actualCallsNumber = deciders.count(_.beenCalled)
        actualResult shouldBe expectedResult
        actualCallsNumber shouldBe callsExpected
      }
    }
  }
}
