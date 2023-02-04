package ru.yandex.vos2.realty.processing.updates

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.cadastr.proto.event.CadastrEvent.OfferReportStatusChanged
import ru.yandex.realty.cadastr.proto.event.ExcerptReportStatus.ReportReceived
import ru.yandex.realty.cadastr.proto.event.{CadastrEvent, ExcerptReportStatus}
import ru.yandex.realty.proto.PersonFullName
import ru.yandex.realty.util.CryptoUtils
import ru.yandex.realty.util.CryptoUtils.Crypto
import ru.yandex.vos2.realty.components.TestRealtyCoreComponents
import ru.yandex.vos2.realty.dao.offers.RealtyOfferDao
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class OfferUpdatesServiceSpec extends WordSpec with MockFactory with ScalaFutures with Matchers {

  val mockDao = mock[RealtyOfferDao]
  lazy val mockMySql = TestRealtyCoreComponents.mySql
  val mockFeatures = TestRealtyCoreComponents.features
  private val randomAes256KeyBase64 = "Qj9FKEgrTWJRZVRoV21acTR0N3cheiVDKkYpSkBOY1I="
  val crypto: Crypto = CryptoUtils.Crypto.create(randomAes256KeyBase64)
  val service = new OfferUpdatesService(mockDao, mockMySql, mockFeatures, crypto)

  "updateCadastrInfo" should {
    "get unique persons only" in {
      val offer = RealtyOfferGenerator.offerGen().sample.get
      val event = CadastrEvent
        .newBuilder()
        .setOfferReportStatusChanged(
          OfferReportStatusChanged
            .newBuilder()
            .setReportStatus(
              ExcerptReportStatus
                .newBuilder()
                .setReportReceived(
                  ReportReceived
                    .newBuilder()
                    .addAllOwnerPersonsEncrypted(
                      Seq(getPerson(), getPerson("ДругоеИмя"), getPerson("ДругоеИмя"), getPerson()).asJava
                    )
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
      val offerUpdate = service.updateCadastrInfo(offer, event)
      offerUpdate.getUpdate.get.getOfferRealty.getCadastrInfo.getOwnerPersonsEncryptedCount shouldBe 2
      val list =
        offerUpdate.getUpdate.get.getOfferRealty.getCadastrInfo.getOwnerPersonsEncryptedList.asScala
          .map { x =>
            crypto.decryptToStr(x.getName)
          }
      list should contain("Имя")
      list should contain("ДругоеИмя")
    }
  }

  private def getPerson(name: String = "Имя") =
    PersonFullName
      .newBuilder()
      .setName(crypto.encrypt(name))
      .setSurname(crypto.encrypt("Фамилия"))
      .setPatronymic(crypto.encrypt("Отчество"))
      .build()
}
