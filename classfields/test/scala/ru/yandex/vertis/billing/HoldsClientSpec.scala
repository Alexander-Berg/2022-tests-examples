package ru.yandex.vertis.billing

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.HoldsClientSpec._
import ru.yandex.vertis.billing.Model.HoldResponse.ResponseStatus
import ru.yandex.vertis.billing.microcore_model.Dsl
import Dsl._

import scala.util.{Failure, Random, Success}

/**
  * Tests for [[HoldsClient]]
  *
  * @author zvez
  */
trait HoldsClientSpec extends AnyWordSpec with Matchers {

  def client: HoldsClient

  implicit val requestContext = RequestContext("client-tests")

  val holdIdGen = for {
    l <- Gen.choose(7, 10)
    code <- Gen.listOfN(l, Gen.alphaNumChar).map(_.mkString)
  } yield s"test.$code"

  def nextHoldId = {
    holdIdGen.sample.get
  }

  def holdRequest(holdId: String = nextHoldId) = {
    Dsl.holdRequest(holdId, 1000L, HoldDuration)
  }

  def fullHoldRequest(orderId: Long = OrderId, holdId: String = nextHoldId) = {
    Dsl.fullHoldRequest(orderId, holdId, 1000L, HoldDuration)
  }

  val someHoldRequest = fullHoldRequest()

  val poorManHoldRequest = fullHoldRequest(PoorManOrderId, nextHoldId)

  val unknownOrderHoldRequest = fullHoldRequest(orderId = UnknownOrderId)

  "HoldsClient" should {

    "make one hold" in {
      client.hold(Seq(someHoldRequest)) match {
        case Success(Seq(r)) =>
          r.getHoldId shouldBe someHoldRequest.getHoldId
          r.getStatus shouldBe ResponseStatus.OK
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "return AlreadyExists when try to make hold with the same id" in {
      client.hold(Seq(someHoldRequest)) match {
        case Success(Seq(r)) =>
          r.getHoldId shouldBe someHoldRequest.getHoldId
          r.getStatus shouldBe ResponseStatus.ALREADY_EXISTS
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "return NoEnoughFunds when order doesn't have enough money" in {
      client.hold(Seq(poorManHoldRequest)) match {
        case Success(Seq(r)) =>
          r.getHoldId shouldBe poorManHoldRequest.getHoldId
          r.getStatus shouldBe ResponseStatus.NO_ENOUGH_FUNDS
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "return error on unknown order" in {
      client.hold(Seq(unknownOrderHoldRequest)) match {
        case Success(Seq(r)) =>
          r.getHoldId shouldBe unknownOrderHoldRequest.getHoldId
          r.hasStatus shouldBe false
          r.hasErrorMessage shouldBe true
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "make multiple holds" in {
      val requests = Seq.tabulate(100)(_ => fullHoldRequest())
      client.hold(requests) match {
        case Success(responses) =>
          responses.size shouldEqual requests.size
          for ((req, res) <- requests.zip(responses)) {
            res.getHoldId shouldEqual req.getHoldId
            res.getStatus shouldEqual ResponseStatus.OK
          }
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "work with different responses" in {
      val requests = Seq(
        someHoldRequest,
        poorManHoldRequest,
        unknownOrderHoldRequest,
        fullHoldRequest()
      )

      client.hold(requests) match {
        case Success(responses) =>
          responses.size shouldEqual requests.size
          val statuses = responses.map(r => if (r.hasStatus) Some(r.getStatus) else None)
          statuses shouldEqual Seq(
            Some(ResponseStatus.ALREADY_EXISTS),
            Some(ResponseStatus.NO_ENOUGH_FUNDS),
            None,
            Some(ResponseStatus.OK)
          )
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "make multiple holds for one order" in {
      val requests = Seq.tabulate(100)(_ => holdRequest())
      client.hold(OrderId, requests) match {
        case Success(responses) =>
          responses.size shouldEqual requests.size
          for ((req, res) <- requests.zip(responses)) {
            res.getHoldId shouldEqual req.getHoldId
            res.getStatus shouldEqual ResponseStatus.OK
          }
        case other =>
          fail(s"Unpredicted $other")
      }
    }

    "return error when order not found" in {
      val requests = Seq.tabulate(100)(_ => holdRequest())
      client.hold(UnknownOrderId, requests) match {
        case Failure(ex) if ex.isInstanceOf[IllegalArgumentException] => info("Done")
        case other => fail(s"Unpredicted $other")
      }
    }

    "keep order of responses" in {
      val requests = Random.shuffle(
        Seq.tabulate(7)(_ => fullHoldRequest(OrderId)) ++
          Seq.tabulate(7)(_ => fullHoldRequest(AnotherOrderId)) ++
          Seq.tabulate(7)(_ => fullHoldRequest(PoorManOrderId))
      )

      client.hold(requests) match {
        case Success(responses) =>
          responses.size shouldEqual requests.size
          for ((req, res) <- requests.zip(responses)) {
            res.getHoldId shouldEqual req.getHoldId
            req.getOrderId match {
              case `PoorManOrderId` => res.getStatus shouldEqual ResponseStatus.NO_ENOUGH_FUNDS
              case _ => res.getStatus shouldEqual ResponseStatus.OK
            }
          }
        case other =>
          fail(s"Unpredicted $other")
      }
    }

  }
}

object HoldsClientSpec {
  val UnknownOrderId = 999666
  val OrderId = 1000
  val PoorManOrderId = 1001
  val AnotherOrderId = 1002
  val HoldDuration = 60 * 1000
}
