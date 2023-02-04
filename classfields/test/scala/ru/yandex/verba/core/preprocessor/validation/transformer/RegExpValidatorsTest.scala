package ru.yandex.verba.core.preprocessor.validation.transformer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.TestUtils._
import ru.yandex.verba.core.attributes.Str
import ru.yandex.verba.core.preprocessor.validation.validator.RegExpValidators
import ru.yandex.verba.core.util.VerbaUtils

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 02.08.14
  */
class RegExpValidatorsTest extends AnyFlatSpec with Matchers with VerbaUtils {
  "Email validator " should "  validate" in {
    val validator = RegExpValidators.getValidator("EMAIL", "a").get
    validator.transform(asTerm(Str(""))).shouldProblems()
    validator.transform(asTerm(Str("X@mail.co.uk"))).shouldProblems() // strange requirement for lowercase

    validator.transform(asTerm(Str("xxx@mail.ru"))).shouldValid()
    validator.transform(asTerm(Str("1xxx-1-1-1-@mail.co.uk"))).shouldValid()
    validator.transform(asTerm(Str("kashirskoe.subaru@uservice.ru"))).shouldValid()
  }
  "Phone validator " should "  validate" in {
    val validator = RegExpValidators.getValidator("PHONE", "a").get
    validator.transform(asTerm(Str(""))).shouldProblems()
    validator.transform(asTerm(Str("8 (9311) 2-00-12"))).shouldProblems()
    validator.transform(asTerm(Str("6 (931) 2-00-12"))).shouldProblems()

    validator.transform(asTerm(Str("+7 (931) 232-00-32"))).shouldValid()
    validator.transform(asTerm(Str("8 (9312) 23-00-12"))).shouldValid()
    validator.transform(asTerm(Str("8 (93121) 2-00-12"))).shouldValid()
  }
  "Work days validator " should "  validate" in {
    val validator = RegExpValidators.getValidator("WORK_DAYS", "a").get
    validator.transform(asTerm(Str(""))).shouldProblems()

    validator.transform(asTerm(Str("пн-ср 09:00-20:00"))).shouldValid()
    validator.transform(asTerm(Str("пн-ср 09:00-20:00, чт-пт 09:00-16:00"))).shouldValid()
    validator.transform(asTerm(Str("ежедн. 09:00-20:00"))).shouldValid()
    validator.transform(asTerm(Str("ежедн. 8:30-20:30"))).shouldValid()
    validator.transform(asTerm(Str("круглосуточно"))).shouldValid()
  }
  "Url " should "  validate" in {
    val validator = RegExpValidators.getValidator("URL", "a").get
    validator.transform(asTerm(Str(""))).shouldProblems()

    validator.transform(asTerm(Str("http://ya.ru"))).shouldValid()
    validator.transform(asTerm(Str("ya.ru"))).shouldValid()
    validator.transform(asTerm(Str("http://www.ya.ru"))).shouldValid()
    validator.transform(asTerm(Str("www.ya.ru/"))).shouldValid()
    validator.transform(asTerm(Str("www.ya.ru"))).shouldValid()
    validator.transform(asTerm(Str("www.ya.ru.ru"))).shouldValid()
    validator.transform(asTerm(Str("www.яндекс.рф"))).shouldValid()
    validator.transform(asTerm(Str("http://dacar.su/Hyundai/"))).shouldValid()
  }

}
