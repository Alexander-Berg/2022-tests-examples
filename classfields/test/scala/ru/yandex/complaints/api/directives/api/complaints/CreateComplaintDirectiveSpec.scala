package ru.yandex.complaints.api.directives.api.complaints

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.api.directives.DirectiveSpec
import ru.yandex.complaints.api.handlers.api.complaint.model.{RequestInfo, UpdateComplaintRequest}
import ru.yandex.complaints.api.handlers.api.user.model.UserInfo

/**
  * Created by s-reznick on 15.03.17.
  */
@RunWith(classOf[JUnitRunner])
class CreateComplaintDirectiveSpec
  extends WordSpec
  with Matchers
  with DirectiveSpec {
  val GoodIpAddresses = Seq("127.0.0.1", "::1", "2a5b::cdef")

  val WrongIpAddresses = Seq("", "1224142315321")

  val GoodAuthorIds = Seq(
    "yandex_uid_123", "partner_2"
  )

  val WrongAuthorIds = Seq(
    "_123", "partner", "12345"
  )

  val GoodRequests = GoodIpAddresses.map(ip => {
      UpdateComplaintRequest(
        user = UserInfo(domain = "domain", id = "id_1234", None),
        requestInfo = RequestInfo(
          ip = Some(ip),
          userAgent = Some("Yandex"),
          proxyIp = Some(ip),
          flashCookie = Some("flashcookie"),
          yandexuid = Some("1234567890")
        ),
        text = Some("some description"),
        reason = "WRONG_PRICE",
        source = None
      )
    })

  def withIp(req: UpdateComplaintRequest, v: String): UpdateComplaintRequest = {
    req.copy(requestInfo = req.requestInfo.copy(ip = Some(v)))
  }
  def withProxyIp(req: UpdateComplaintRequest, v: String): UpdateComplaintRequest = {
    req.copy(requestInfo = req.requestInfo.copy(proxyIp = Some(v)))
  }

  def check(req: UpdateComplaintRequest, author: String): CheckStatus = {
    check(CreateComplaintDirective.prepareRequest(req, author))
  }

  "CreateComplaintDirective" should {
    "accept correct values" in {
      for (author <- GoodAuthorIds) {
        for (req <- GoodRequests) {
          val res = check(req, author)
          assert(res.isAccepted)
          assert(res.rejectReasons.isEmpty)
        }
      }
    }

    "reject ill-formatted author id" in {
      for (author <- WrongAuthorIds) {
        for (req <- GoodRequests) {
          val res = check(req, author)
          assert(!res.isAccepted)
          assert(res.rejectReasons.size == 1)
        }
      }
    }

    "reject ill-formatted ip" in {
      for (author <- GoodAuthorIds) {
        for (req <- GoodRequests) {
          for (ip <- WrongIpAddresses) {
            val res = check(withIp(req, ip), author)
            assert(!res.isAccepted)
            assert(res.rejectReasons.size == 1)
          }
        }
      }
    }

    "reject ill-formatted proxy ip" in {
      for (author <- GoodAuthorIds) {
        for (req <- GoodRequests) {
          for (ip <- WrongIpAddresses) {
            val res = check(withProxyIp(req, ip), author)
            assert(!res.isAccepted)
            assert(res.rejectReasons.size == 1)
          }
        }
      }
    }
  }

}