package auto.carfax.common.clients.transfer_manager.test

import auto.carfax.common.clients.transfer_manager.model.TransferStatus
import auto.carfax.common.clients.transfer_manager.model.TransferStatus.{CopyMethod, State}
import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.enablers.Emptiness.emptinessOfOption
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class TransferStatusSpec extends AnyWordSpecLike with Matchers {

  val commonResponse: String = ResourceUtils.getStringFromResources("/common_response.json")
  val errorResponse: String = ResourceUtils.getStringFromResources("/error_response.json")
  val notFoundResponse: String = ResourceUtils.getStringFromResources("/not_found_response.json")

  "TransferStatus" should {

    "be correctly parsed from transfer managers response" in {

      val res = TransferStatus(200, commonResponse)

      res.state shouldBe State.Running
      res.copyMethod shouldBe CopyMethod.Native
      res.error shouldBe empty
    }

    "be correctly parsed from transfer managers response when process was failed" in {

      val res = TransferStatus(200, errorResponse)

      println(res)
      res.state shouldBe State.Failed
      res.copyMethod shouldBe CopyMethod.Native
      res.error.get.message shouldBe "Received HTTP response with error"
      res.error.get.innerErrors.head.message shouldBe "Operation has failed to start"
      res.error.get.innerErrors.head.innerErrors.head.message shouldBe "User \"zomb-auto-import\" has been denied access to pool /physical/verticals"
      res.error.get.innerErrors.head.innerErrors.head.innerErrors.head.message shouldBe "Access denied for user \"zomb-auto-import\": \"use\" permission is not allowed by any matching ACE"
    }

    "be correctly parsed from transfer managers response when transfer id was not found" in {

      an[RuntimeException] should be thrownBy TransferStatus(404, notFoundResponse)
    }
  }
}
