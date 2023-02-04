package ru.auto.chatbot.utils

import org.scalatest.FunSuite
import ru.auto.chatbot.exception.NotAutoruLinkExpetion
import ru.auto.chatbot.utils.validation.ValidationUtils

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-04.
  */
class ValidationUtilsTest extends FunSuite {

  test("url parsing") {
    val url1 = "https://test.avto.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/"
    val url2 = "https://auto.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/"
    val url3 = "https://avito.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/"
    val url4 = "https://test.avto.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec"
    val url5 = "https://test.avto.ru/cars/used/sale/toyota/rav_4/1085453866-4f4e839d/"
    val url6 = "https://m.auto.ru/cars/new/sale/1085453866-4f4e839d/"
    val url7 = "https://test.avto.ru/cars/used/sale/lifan/murman/1085868358-65ea5404/"

    assert(ValidationUtils.getOfferIdFromTextWithUrl(url1).contains("1076660054-57b48aec"))
    assert(ValidationUtils.getOfferIdFromTextWithUrl(url2).contains("1076660054-57b48aec"))
    assertThrows[NotAutoruLinkExpetion](ValidationUtils.getOfferIdFromTextWithUrl(url3).isEmpty)
    assert(ValidationUtils.getOfferIdFromTextWithUrl(url4).contains("1076660054-57b48aec"))
    assert(ValidationUtils.getOfferIdFromTextWithUrl(url5).contains("1085453866-4f4e839d"))
    assert(ValidationUtils.getOfferIdFromTextWithUrl(url6).contains("1085453866-4f4e839d"))
    assert(ValidationUtils.getOfferIdFromTextWithUrl(url7).contains("1085868358-65ea5404"))
  }

  test("url in text parsing") {
    val text1 = "Привет! Я собираюсь на осмотр этого автомобиля — " +
      "https://test.avto.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/"
    val text2 = "Привет! Я собираюсь на осмотр этого автомобиля — " +
      "https://test.avto.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/ а потом буду смотреть " +
      "https://test.avto.ru/cars/used/sale/mercedes/gl_klasse/123-abc/"

    val text3 = "смотрю это https://avito.ru/cars/used/sale/mercedes/gl_klasse/1076660054-57b48aec/"

    assertThrows[NotAutoruLinkExpetion](ValidationUtils.getOfferIdFromTextWithUrl(text3))

    assert(ValidationUtils.getOfferIdFromTextWithUrl(text1).contains("1076660054-57b48aec"))
    assert(ValidationUtils.getOfferIdFromTextWithUrl(text2).contains("1076660054-57b48aec"))
  }

  test("contains cyrillic") {
    val text1 = "What is your name?"
    val text2 = "My name is Александр"

    assert(!ValidationUtils.containsCyrillic(text1))
    assert(ValidationUtils.containsCyrillic(text2))
  }

  test("is it url") {
    val text1 = "cho delat ya nichego ne ponimau"
    val text2 = "http://yandex.ru/"

    assert(!ValidationUtils.isUrl(text1))
    assert(ValidationUtils.isUrl(text2))
  }

  test("is it auto.ru url") {
    val text1 = "http://yandex.ru/"
    val text2 = "https://auto.ru/lcv/used/sale/mercedes/sprinter_classic/15854216-0461d909/"

    assert(!ValidationUtils.isAutoruUrl(text1))
    assert(ValidationUtils.isAutoruUrl(text2))
  }

}
