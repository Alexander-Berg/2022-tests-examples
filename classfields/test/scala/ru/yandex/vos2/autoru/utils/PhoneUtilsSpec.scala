package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.util.PhoneUtils

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 26.09.16
  */
@RunWith(classOf[JUnitRunner])
class PhoneUtilsSpec extends AnyFunSuite with OptionValues {

  test("testPhoneNumberFormatting") {
    // проверяем парсинг валидного номера
    testNumberParsing("79161793608", "+7 916 179-36-08")
    testNumberParsing("88002000600", "+7 800 200-06-00")
  }

  test("testPhoneNumberFormattingBelarus") {
    testNumberParsing("375299209920", "+375 29 920-99-20")
  }
  test("testPhoneNumberFormattingUkraine") {
    testNumberParsing("380667419930", "+380 66 741 9930")
  }
  test("test phone number formatting with suffix") {
    testNumberParsing("749522352225070", "+7 495 223-52-22 ext. 5070", mask = "1:3:7:4")
  }

  test("normalize") {
    // проверим, что телефон нормализуется правильно, удаляется все, кроме цифр
    assert(PhoneUtils.normalize("+79028717926пыщпыщь") == "79028717926")
    assert(PhoneUtils.normalize("9261234545") == "79261234545") // если без кода, то ставим семерку в начале
    assert(PhoneUtils.normalize("89261234545") == "79261234545") // восьмерку в начале меняем на семерку
    assert(PhoneUtils.normalize("79261234545") == "79261234545")
    assert(PhoneUtils.normalize("129261234545") == "129261234545") // если больше 11 символов, оставляем как есть
  }

  private def testNumberParsing(number: String, mustBe: String, mask: String = ""): Unit = {
    val phone = AutoruOffer.Phone.newBuilder().setNumber(number).setNumberMask(mask).build()
    if (mask.isEmpty) {
      assert(PhoneUtils.formatPhone(number) == mustBe)
    }
    assert(
      PhoneUtils.formatPhone(phone) == mustBe,
      s": Phone [$number] with mask [$mask] should be formatted to [$mustBe]"
    )
  }
}
