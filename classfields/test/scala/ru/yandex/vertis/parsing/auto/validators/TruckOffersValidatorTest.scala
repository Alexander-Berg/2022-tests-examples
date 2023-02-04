package ru.yandex.vertis.parsing.auto.validators

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.phones.PhonesDao
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.extdata.geo.Tree

import scala.collection.mutable.ListBuffer

/**
  * Created by andrey on 2/11/18.
  */
@RunWith(classOf[JUnitRunner])
class TruckOffersValidatorTest extends FunSuite with MockitoSupport {
  private val phonesDao = mock[PhonesDao]
  private val regionTree = mock[Tree]
  when(regionTree.region(any())).thenReturn(None)
  private val offersValidator = new TruckOffersValidator(phonesDao, regionTree)

  test("phones not fake") {
    val parsedOffer = ParsedOffer.newBuilder()
    parsedOffer.setJson(
      """[{
        |"owner":["{
        |  \"name\":[\"\\n\\t\\t\\t\\t\\t\\t\\t\"],
        |  \"id\":[\"10084690\"],
        |  \"login\":[\"10084690\"]}"],
        |"address":["Уфа"],
        |"year":["2017"],
        |"phone":["+7 924 307 40-75,+7 908 596 24-93,+7 924 940 64-68,+7 902 539 71-49,+7 906 756 89-92"],
        |"price":["4 800 000"],
        |"fn":["Камаз 6520"],
        |"photo":[
        |  "https://static.baza.farpost.ru/v/1518158394262_bulletin",
        |  "https://static.baza.farpost.ru/v/1518158785586_bulletin",
        |  "https://static.baza.farpost.ru/v/1518158785695_bulletin",
        |  "https://static.baza.farpost.ru/v/1518158785984_bulletin"],
        |"description":["На всю технику предоставляется заводская гарантия. Звоните в любое время суток! • мы официальные дилеры. • оформление 30 минут. • возможность доставки. • различные варианты оплаты. • работаем в лизинг."],
        |"offer_id":["60873378"],
        |"info":["{
        |  \"transmission\":[\"Механическая\"],
        |  \"engine\":[\"11 760 куб. см.\"],
        |  \"wheel-drive\":[\"6x4\"],
        |  \"mileage_in_russia\":[\"Без пробега\"],
        |  \"documents\":[\"Есть ПТС\"],
        |  \"wheel\":[\"Левый\"],
        |  \"fuel\":[\"Дизель\"],
        |  \"state\":[\"Новое\"],
        |  \"type\":[\"Самосвал\"],
        |  \"category\":[\"Грузовики и спецтехника\"],
        |  \"capacity\":[\"22 000 кг.\"]}"],
        |"parse_date":["2018-02-10T01:10:46.034+03:00"]}]""".stripMargin.replace("\n", "")
    )
    val url: String = "https://spec.drom.ru/ufa/truck/kamaz-6520-ljux-60873378.html"
    val hash = DromTrucksParser.hash(url)
    val parsedRow = ParsedRow(
      id = 0,
      hash = hash,
      category = ApiOfferModel.Category.TRUCKS,
      status = CommonModel.Status.READY,
      site = Site.Drom,
      url = url,
      data = parsedOffer.build(),
      createDate = DateTime.now(),
      updateDate = DateTime.now(),
      statusUpdateDate = DateTime.now(),
      sentDate = None,
      openDate = None,
      deactivateDate = None,
      source = CommonModel.Source.HTTP,
      callCenter = None,
      offerId = None,
      version = 1
    )

    val errors = ListBuffer[String]()
    offersValidator.validatePhoneNotFake(parsedRow)(errors)
    assert(errors.length == 1)
    assert(errors.head == "fake_phones")
  }
}
