package ru.yandex.vertis.tvm.impl

import org.scalatest.Ignore
import ru.yandex.vertis.billing.banker.util.DateTimeUtils
import ru.yandex.vertis.tvm.TvmSpecBase

import scala.util.{Failure, Success}

/**
  * To run this spec you need a binary libticket_parser2 in test's classpath.
  * It is provided in deb-packages https://wiki.yandex-team.ru/passport/tvm2/library/#paketyvlinux-repozitorijax
  * Building for mac: https://wiki.yandex-team.ru/passport/tvm2/library/#isxodnikiarkadija
  *
  * @author alex-kovalenko
  */
@Ignore
class RequestSignerImplSpec extends TvmSpecBase {

  lazy val signer = new RequestSignerImpl(SelfClientId, ClientSecret)

  val dstIds = List(1, 2)

  "RequestSigner" should {
    "fail if got empty dstClientIds" in {
      signer.sign(DateTimeUtils.now(), Nil) should matchPattern { case Failure(_: IllegalArgumentException) =>
      }
    }
    "sign request" in {
      val time = DateTimeUtils.now()
      signer.sign(time, List(2000423)) match {
        case Success(signature) =>
          info(s"Done: $signature")
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }
}
