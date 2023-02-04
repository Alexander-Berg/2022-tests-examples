package ru.yandex.vertis.clustering.services.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.services.{NodeDecider, NodeDeciderImpl}
import ru.yandex.vertis.clustering.utils.DateTimeUtils

import scala.util.{Failure, Try}

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class NodeDeciderSpec extends BaseSpec {

  import NodeDecider._

  val nodeDecider: NodeDecider = new NodeDeciderImpl

  val alice = "alice"
  val bob = "bob"
  val charlie = "charlie"

  val now = Some(DateTimeUtils.now)
  val late = Some(DateTimeUtils.now.minusMinutes(65))
  val updatedRecently = Some(DateTimeUtils.now.minusHours(1))
  val updatedLongAgo = Some(DateTimeUtils.now.minusHours(5))
  val updatedVeryLongAgo = Some(DateTimeUtils.now.minusHours(6))

  val aliceNodeNotAvailable = Node(alice, None, None)
  val aliceNodeReady = Node(alice, now, updatedLongAgo)
  val aliceNodeReadyUpdateRecently = Node(alice, now, updatedRecently)
  val aliceNodeLate = Node(alice, late, updatedRecently)
  val aliceNodeLateUpdateLongAgo = Node(alice, late, updatedLongAgo)

  val bobNodeNotAvailable = Node(bob, None, None)
  val bobNodeReady = Node(bob, now, updatedLongAgo)
  val bobNodeReadyUpdVeryLongAgo = Node(bob, now, updatedVeryLongAgo)
  val bobNodeLate = Node(bob, late, updatedRecently)

  val charlieNodeNotAvailable = Node(charlie, None, None)
  val charlieNodeReady = Node(charlie, now, updatedLongAgo)
  val charlieNodeLate = Node(charlie, late, updatedRecently)

  val aliceNotAvailableInput = Input(alice, Seq(aliceNodeNotAvailable, bobNodeReady, charlieNodeLate))

  val aliceReadyInputRotate = Input(alice, Seq(aliceNodeReady, bobNodeReady, charlieNodeLate))

  val aliceReadyInputNotRotate = Input(alice, Seq(aliceNodeReady, bobNodeLate, charlieNodeLate))

  val aliceLateInput = Input(alice, Seq(aliceNodeLate, bobNodeReady, charlieNodeLate))

  val aliceAndFriendsRefused = Input(alice, Seq(aliceNodeNotAvailable, bobNodeNotAvailable, charlieNodeNotAvailable))

  val aliceReadyInputBobRotate = Input(alice, Seq(aliceNodeReady, bobNodeReadyUpdVeryLongAgo, charlieNodeReady))

  val allNodesReadyButCharlie = Input(charlie, Seq(aliceNodeReady, bobNodeReady, charlieNodeReady))

  val aliceAloneReadyInput = Input(alice, Seq(aliceNodeReady))
  val aliceAloneLateInput = Input(alice, Seq(aliceNodeLate))
  val aliceAloneLateUpdateLongAgoInput = Input(alice, Seq(aliceNodeLateUpdateLongAgo))
  val aliceAloneNodeNotAvailableInput = Input(alice, Seq(aliceNodeNotAvailable))

  val allLates = Input(alice, Seq(aliceNodeLateUpdateLongAgo, charlieNodeLate))

  val test = Seq(
    aliceNotAvailableInput -> State(RedirectTo(bobNodeReady), RotateNow),
    aliceReadyInputRotate -> State(Ready, RotateNow),
    aliceReadyInputNotRotate -> State(Ready, TryLater),
    aliceLateInput -> State(RedirectTo(bobNodeReady), RotateNow),
    aliceAndFriendsRefused -> State(NotReady, RotateNow),
    aliceReadyInputBobRotate -> State(Ready, TryLater),
    aliceAloneReadyInput -> State(Ready, TryLater),
    aliceAloneLateInput -> State(Ready, RotateNow),
    aliceAloneLateUpdateLongAgoInput -> State(Ready, RotateNow),
    aliceAloneNodeNotAvailableInput -> State(NotReady, RotateNow),
    allNodesReadyButCharlie -> State(Ready, RotateNow),
    allLates -> State(Ready, RotateNow)
  )

  "decider must return true state" should {

    "failed with incorrect parameters" in {
      Try(nodeDecider(aliceReadyInputBobRotate, DateTimeUtils.now.minusYears(1))) shouldBe
        a[Failure[_]]
    }

    test.foreach {
      case (input, state) =>
        s"$input -> $state" in {
          nodeDecider(input) shouldBe state
        }
    }
  }

}
