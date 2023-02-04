package ru.yandex.vertis.billing.service

import org.scalacheck.Gen
import ru.yandex.vertis.billing.SpecBase
import ru.yandex.vertis.billing.model_core.HoldResponse.{AlreadyExists, NoEnoughFunds, Ok}
import ru.yandex.vertis.billing.model_core.gens.{HoldRequestGen, Producer}
import ru.yandex.vertis.billing.model_core.{FullHoldRequest, OrderId}
import ru.yandex.vertis.billing.util.AutomatedContext

import scala.annotation.nowarn
import scala.util.{Failure, Random, Success}

/**
  * Tests for [[HoldOnlyOrderService]]
  *
  * @author zvez
  */
@nowarn("msg=discarded non-Unit value")
trait HoldOnlyOrderServiceSpec extends SpecBase {

  def service: HoldOnlyOrderService
  def orderWithoutMoney: OrderId
  def nonExistentOrder: OrderId

  implicit val requestContext = AutomatedContext("test")

  val fullRequestGen = for {
    hold <- HoldRequestGen
    orderId <- Gen.chooseNum(1, 3)
  } yield FullHoldRequest(orderId, hold)

  "HoldOnlyOrderService" should {
    "accept multiple requests" in {
      val requests = HoldRequestGen.next(10).toSeq
      service.hold(1, requests) match {
        case Success(responses) =>
          checkMatched(requests, responses) { (req, res) =>
            res should matchPattern { case Ok(`req`, Some(_)) =>
            }
          }
        case Failure(other) => fail(s"Unexpected $other: ${other.getStackTrace.mkString("\n")}")
      }
    }

    "accept multiple requests for different orders" in {
      val requests = fullRequestGen.next(50).toSeq
      val responses = service.hold(requests)
      checkMatched(requests, responses) { (req, res) =>
        res should matchPattern {
          case Success(Ok(r, Some(_))) if r == req.holdRequest =>
        }
      }
    }

    "allow different responses" in {
      val goodRequests = fullRequestGen.next(10).toSet
      val noFundRequests = fullRequestGen
        .next(10)
        .map(r => r.copy(orderId = orderWithoutMoney, holdRequest = r.holdRequest.copy(amount = 100)))
        .toSet
      var requests = Random.shuffle((goodRequests ++ noFundRequests).toSeq)
      var responses = service.hold(requests)
      checkMatched(requests, responses) { (req, res) =>
        if (goodRequests(req)) {
          res should matchPattern {
            case Success(Ok(r, Some(_))) if r == req.holdRequest =>
          }
        } else {
          res shouldBe Success(NoEnoughFunds(req.holdRequest))
        }
      }

      val moreRequests = fullRequestGen.next(5).toSet
      requests = Random.shuffle(requests ++ moreRequests)
      responses = service.hold(requests)
      checkMatched(requests, responses) { (req, res) =>
        if (goodRequests(req)) {
          res should matchPattern {
            case Success(AlreadyExists(r, Some(_))) if r == req.holdRequest =>
          }
        } else if (moreRequests(req)) {
          res should matchPattern {
            case Success(Ok(r, Some(_))) if r == req.holdRequest =>
          }
        } else {
          res shouldBe Success(NoEnoughFunds(req.holdRequest))
        }
      }
    }

    "can fail on some of the requests" in {
      val goodRequests = fullRequestGen.next(10).toSet
      val faultyRequests = fullRequestGen.next(5).map(_.copy(orderId = nonExistentOrder))
      val requests = Random.shuffle((goodRequests ++ faultyRequests).toSeq)
      val responses = service.hold(requests)
      checkMatched(requests, responses) { (req, res) =>
        if (goodRequests(req)) {
          res should matchPattern {
            case Success(Ok(r, Some(_))) if r == req.holdRequest =>
          }
        } else {
          res should matchPattern { case Failure(_) => }
        }
      }
    }
  }

  private def checkMatched[A, B](xs: Iterable[A], ys: Iterable[B])(f: (A, B) => Unit): Unit = {
    xs.size should be(ys.size)
    xs.zip(ys).foreach(f.tupled)
  }
}
