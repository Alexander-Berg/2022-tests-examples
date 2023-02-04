package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.files.GetDownloadUrlResponse
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.backend.inventory.InventoryPdfManager
import ru.yandex.realty.rent.proto.model.document.RentDocumentToken
import ru.yandex.realty.rent.proto.model.document.RentDocumentToken.RentContractToken
import ru.yandex.realty.rent.util.TokenManager.Config
import ru.yandex.realty.rent.util.DefaultTokenManager
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class TokenDocumentManagerSpec extends AsyncSpecBase with RequestAware with RentModelsGen {

  "DocumentService" should {
    "return rent contract document url by token" in new Data {
      (contractPdfManager
        .getContractUrl(_: String)(_: Traced))
        .expects(contractId, *)
        .returning(Future.successful(contractUrlResponse))

      val urlResponse = tokenDocumentManager.getDocumentUrlByToken(contractToken).futureValue

      urlResponse shouldBe contractUrlResponse
    }
  }

  trait Data {
    val contractId = "contractId"

    val bytes = RentDocumentToken
      .newBuilder()
      .setRentContract(RentContractToken.newBuilder().setId(contractId))
      .build()
      .toByteArray
    val contractToken = tokenManager.encrypt(bytes)
    val contractUrl = "contractUrl"

    val contractUrlResponse = GetDownloadUrlResponse
      .newBuilder()
      .setUrl(contractUrl)
      .build()
  }

  val contractPdfManager: ContractPdfManager = mock[ContractPdfManager]
  val inventoryPdfManger: InventoryPdfManager = mock[InventoryPdfManager]
  val keysHandoverManager: KeysHandoverManager = mock[KeysHandoverManager]

  val config = Config("keyWithLength16bkakfldtrmgldjtoa")
  val tokenManager = new DefaultTokenManager(config)

  val tokenDocumentManager =
    new DefaultTokenDocumentManager(contractPdfManager, inventoryPdfManger, keysHandoverManager, tokenManager)

}
