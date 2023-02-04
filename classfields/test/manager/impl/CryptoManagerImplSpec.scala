package ru.auto.iskra.core.test.manager.impl

import com.google.protobuf.struct.{Struct, Value}
import ru.auto.iskra.core.manager.impl.{CryptoConfig, CryptoManagerImpl}
import ru.auto.iskra.proto.palma_model.CarSubscriptionQuestionnaire.{Home, HomeOwnStatus, Work}
import ru.auto.iskra.proto.palma_model.{CarSubscriptionPersonalData, CarSubscriptionQuestionnaire}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._

object CryptoManagerImplSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CryptoManagerImpl") {
      test("Decrypts and encrypts personal data correctly") {
        val cryptoConfig = CryptoConfig(SecretKey)
        val cryptoManager = new CryptoManagerImpl(cryptoConfig)

        val results = PersonalDataTestInstances.map { personalData =>
          val encryptedData = cryptoManager.encryptPersonalData(personalData)
          val decryptedData = cryptoManager.decryptPersonalData(encryptedData)
          (personalData, decryptedData)
        }

        assert(results) {
          forall {
            assertion("Initial message equals to the result")() { case (initialMessage, resultMessage) =>
              initialMessage == resultMessage
            }
          }
        }
      }
    }

  private val SecretKey = "dTd4IUElRCpHLUthUGRTZ1ZrWXAzczV2OHkvQj9FKEg="

  private val PersonalDataTestInstances = {
    val withMainData = CarSubscriptionQuestionnaire()
      .withName("Name")
      .withEmail("some@yandex.ru")
      .withPhone("+79001234567")
    val withOptionalData = withMainData
      .withWork(Work("Work place", "Work address", 50))
      .withHome(Home("Home address", HomeOwnStatus.MORTGAGE))
      .withRelativesPhone("+79007654321")
      .withMonthlyIncomeInRubles(80000)
    val withAdditionalData = withOptionalData
      .withAdditional {
        val value = Value().withStringValue("Additional value")
        Struct().addFields(("additionalField", value))
      }

    List(withMainData, withOptionalData, withAdditionalData)
      .map(CarSubscriptionPersonalData().withQuestionnaire(_))
  }
}
