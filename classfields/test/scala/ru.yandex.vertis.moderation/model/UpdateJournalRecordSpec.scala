package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, UpdateJournalRecord}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * Specs on [[UpdateJournalRecord]]
  *
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class UpdateJournalRecordSpec extends SpecBase {

  "updated" should {

    "return correct result" in {
      val id = InstanceIdGen.next

      val leftDiff = Diff.Realty(Set(Model.Diff.Realty.Value.CONTEXT, Model.Diff.Realty.Value.OPINION))
      val rightDiff = Diff.Realty(Set(Model.Diff.Realty.Value.CONTEXT, Model.Diff.Realty.Value.SIGNALS))

      val now = DateTimeUtil.now()
      val leftDepth = IntGen.next
      val rightDepth = IntGen.next
      val leftInstance = InstanceGen.next.copy(id = id)
      val rightInstance = InstanceGen.next.copy(id = id)

      val leftRecord = UpdateJournalRecord(now.minus(1L), leftDepth, leftInstance, None, leftDiff)
      val rightRecord = UpdateJournalRecord(now, rightDepth, rightInstance, None, rightDiff)

      val expectedDiff =
        Diff.Realty(
          Set(
            Model.Diff.Realty.Value.CONTEXT,
            Model.Diff.Realty.Value.OPINION,
            Model.Diff.Realty.Value.SIGNALS
          )
        )
      val expectedResult = UpdateJournalRecord(now, leftDepth.max(rightDepth), rightInstance, None, expectedDiff)
      val actualResult = leftRecord.updated(rightRecord)

      actualResult shouldBe expectedResult
    }

    "throw error if diffs has different types" in {
      val id = InstanceIdGen.next

      val leftDiff = RealtyDiffGen.next
      val rightDiff = AutoruDiffGen.next

      val now = DateTimeUtil.now()
      val depth = 0
      val leftInstance = InstanceGen.next.copy(id = id)
      val rightInstance = InstanceGen.next.copy(id = id)

      val leftRecord = UpdateJournalRecord(now.minus(1L), depth, leftInstance, None, leftDiff)
      val rightRecord = UpdateJournalRecord(now, depth, rightInstance, None, rightDiff)

      assertThrows[IllegalArgumentException] {
        leftRecord.updated(rightRecord)
      }
    }

    "throw error if external ids are not equal" in {
      val leftId = InstanceIdGen.next
      val rightId = InstanceIdGen.next

      val diff = RealtyDiffGen.next

      val now = DateTimeUtil.now()
      val leftInstance = InstanceGen.next.copy(id = leftId)
      val rightInstance = InstanceGen.next.copy(id = rightId)

      val leftRecord = UpdateJournalRecord.withInitialDepth(now.minusHours(1), leftInstance, Some(leftInstance), diff)
      val rightRecord = UpdateJournalRecord.withInitialDepth(now, rightInstance, None, diff)

      assertThrows[IllegalArgumentException] {
        leftRecord.updated(rightRecord)
      }
    }
  }
}
