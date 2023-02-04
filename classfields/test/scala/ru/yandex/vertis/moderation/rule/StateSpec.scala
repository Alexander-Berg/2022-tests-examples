package ru.yandex.vertis.moderation.rule

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.rule.Generators.ModerationRuleStateGen

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class StateSpec extends SpecBase {

  "ModerationRuleStateSerializer" should {
    "work correctly" in {
      check(forAll(ModerationRuleStateGen)(state => state == State.withId(state.id)))
    }

    "be compatibly with isActive flag" in {
      State.withId(0) shouldBe State(onMatch = RuleApplyingPolicy.Undo, onMismatch = RuleApplyingPolicy.Undo)
      State.withId(1) shouldBe State(onMatch = RuleApplyingPolicy.Do, onMismatch = RuleApplyingPolicy.Undo)
    }

    "get different ids for different states" in {
      check(forAll(ModerationRuleStateGen, ModerationRuleStateGen) { (state1, state2) =>
        state1 == state2 && state1.id == state2.id ||
        state1 != state2 && state1.id != state2.id
      })
    }
  }
}
