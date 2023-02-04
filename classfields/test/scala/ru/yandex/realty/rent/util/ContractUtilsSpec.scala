package ru.yandex.realty.rent.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.rent.model.ContractUtils.generateContractNumber

@RunWith(classOf[JUnitRunner])
class ContractUtilsSpec extends SpecBase {
  import ContractUtils.extractMoveOutLeadId
  "ContractUtils" when {
    "extractMoveOutLeadId" should {
      "parse valid link" in {
        extractMoveOutLeadId("https://yandexarenda.amocrm.ru/leads/detail/20409919") should be(
          Some(20409919L)
        )
        extractMoveOutLeadId("http://amocrm.ru/leads/detail/20408819") should be(Some(20408819L))
      }
      "parse invalid link" in {
        extractMoveOutLeadId("https://yandexarenda.amocrm.ru/leads/20409919") should be(None)
        extractMoveOutLeadId("gfldsjbiybjvneribg") should be(None)
        extractMoveOutLeadId("") should be(None)
      }
      "generate contract number" in {
        assert(generateContractNumber().matches("\\d{2}-\\d{2}-\\d{7}"))
      }
    }
  }
}
