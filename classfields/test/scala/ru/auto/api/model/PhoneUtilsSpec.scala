package ru.auto.api.model

import ru.auto.api.BaseSpec

/**
  * Created by mcsim-gr on 19.10.17.
  */
class PhoneUtilsSpec extends BaseSpec {
  "PhoneUtils" should {
    "testPhoneNumberFormatting" in {
      // проверяем парсинг валидного номера
      testNumberParsing("+79161793608", "+7 916 179-36-08")
      testNumberParsing("88002000600", "+7 800 200-06-00")
    }

    "testPhoneNumberFormattingBelarus" in {
      testNumberParsing("375299209920", "+375 29 920-99-20")
    }

    "testPhoneNumberFormattingUkraine" in {
      testNumberParsing("380667419930", "+380 66 741 9930")
    }

    "test phone number formatting with suffix" in {
      testNumberParsing("749522352225070", "+7 495 223-52-22 ext. 5070", mask = "1:3:7:4")
    }

    "normalize" in {
      // проверим, что телефон нормализуется правильно, удаляется все, кроме цифр
      assert(PhoneUtils.normalize("+79028717926пыщпыщь") == "79028717926")
      assert(PhoneUtils.normalize("9261234545") == "79261234545") // если без кода, то ставим семерку в начале
      assert(PhoneUtils.normalize("89261234545") == "79261234545") // восьмерку в начале меняем на семерку
      assert(PhoneUtils.normalize("79261234545") == "79261234545")
      assert(PhoneUtils.normalize("129261234545") == "129261234545") // если больше 11 символов, оставляем как есть
    }
  }

  private def testNumberParsing(number: String, mustBe: String, mask: String = ""): Unit = {
    assert(PhoneUtils.format(number, mask) == mustBe)
  }
}
