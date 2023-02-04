package ru.yandex.vertis.parsing.auto.dao.parsedoffers

import java.sql.{Date, Timestamp}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers, OptionValues}
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.doppel.model.proto.{Cluster, Entity}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.CommonModel.{Source, Status}
import ru.yandex.vertis.parsing.auto.ParsingAutoModel
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.common.LicensePlateRecord
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.TParsedOffers
import ru.yandex.vertis.parsing.auto.dao.model.{ParsedRow, QueryParams}
import ru.yandex.vertis.parsing.auto.dao.watchers.{NotPublishedReasonsWatcher, PhonesWatcher}
import ru.yandex.vertis.parsing.auto.diffs.OfferFields
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.avito.AvitoTrucksParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.auto.util.dao.InitTestDbs
import ru.yandex.vertis.parsing.validators.FilterReason._
import ru.yandex.vertis.parsing.common.{AscDesc, Site}
import ru.yandex.vertis.parsing.dao.CommonParsedRow.ActiveOfferInfo
import ru.yandex.vertis.parsing.dao.SelectUpdateResult
import ru.yandex.vertis.parsing.dao.watchers.{IndexData, IndexDataUpdateResult}
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime
import ru.yandex.vertis.parsing.util.RandomUtil
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport
import ru.yandex.vertis.parsing.validators.ValidationResult
import ru.yandex.vertis.parsing.workers.importers.ImportResult

import scala.collection.JavaConverters._

/**
  * Created by andrey on 12/14/17.
  */
@RunWith(classOf[JUnitRunner])
class ParsedOffersDaoImplTest
  extends FunSuite
  with InitTestDbs
  with OptionValues
  with MockitoSupport
  with EmptyTraceSupport
  with Matchers {
  initDb()

  private val shard = components.parsingShard

  private def db = shard.master.jooq

  private val parsedOffersDao: ParsedOffersDao = components.parsedOffersDao

  private val o = TParsedOffers.T_PARSED_OFFERS

  private val dayStart = DateTime.now().withMillisOfDay(0)

  test("index tables insert delete select") {
    val url: String = testAvitoTrucksUrl
    val hash: String = AvitoTrucksParser.hash(url)
    val row: ParsedRow = testRow(url)

    assertSaveResult(row, inserted = true)

    val watcher = PhonesWatcher
    val data1: IndexData = IndexData(hash, "123456")
    val insertResult = db.insert("")(dsl => watcher.insertQuery(dsl, data1))
    assert(insertResult == 1)
    val result = db.query("")(dsl => watcher.selectQuery(dsl, Seq(hash))).map(watcher.mapRow)
    assert(result.length == 1)
    assert(result.head == data1)
    val deleteResult = db.delete("")(dsl => watcher.deleteQuery(dsl, data1))
    assert(deleteResult == 1)
    val result2 = db.query("")(dsl => watcher.selectQuery(dsl, Seq(hash))).map(watcher.mapRow)
    assert(result2.isEmpty)
  }

  test("index update for phones with leading zeros") {
    val parsedOffer: ParsedOffer.Builder = ParsingAutoModel.ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("09002200").setOriginal("09002200")
    val url: String = testAvitoTrucksUrl
    val hash: String = AvitoTrucksParser.hash(url)
    val row: ParsedRow = testRow(url, parsedOffer)

    assertSaveResult(row, inserted = true)

    val IndexDataUpdateResult(toInsert, toDelete) = parsedOffersDao
      .asInstanceOf[ParsedOffersDaoImpl]
      .updateIndexFields(Seq(hash), forceUpdate = false)
    assert(toInsert.isEmpty)
    assert(toDelete.isEmpty)
  }

  test("less parse date") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date меньше, то не обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(2).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)
  }

  test("parse date - the only change") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    val hash: String = DromTrucksParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date единственное изменение, то не обновляем
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)

    // однако если объявление отфильтровано по причине older_20_days - обновляем, мб причина пропадет
    assertFailedFiltration(hash)(Older20Days, NoPhones)

    assertSaveResult(newRow, updated = true)
  }

  test("same parse date") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date не изменился, то обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)
  }

  test("set comment on update") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если parse_date не изменился, то обновляем
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResultWithComment(newRow, comment = "test", updated = true, dbComment = "test")
  }

  test("unable to set comment on insert") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResultWithComment(existingRow, comment = "test", inserted = true, dbComment = "received first time")
  }

  test("ignored diffs") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    val phone: String = "79297771122"
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone).setOriginal(phone)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если все изменения либо ignored, либо parse_date - не обновляем
    parsedOffer.getOfferBuilder.getSellerBuilder.clearPhones()
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)

    // если есть что-то еще - обновляем, ignored дифы в истории также остаются
    parsedOffer.getOfferBuilder.setDescription("test1")
    val newRow2 = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow2, updated = true)

    val row = parsedOffersDao.getParsedOffers(Seq(newRow.hash)).head
    assert(row.data.getStatusHistoryCount == 2)
    assert(row.data.getStatusHistory(1).getDiffCount == 3)
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.Phones && d.getIgnored.getValue && d.getOldValue == phone
        })
    )
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.Description
        })
    )
    assert(
      row.data
        .getStatusHistory(1)
        .getDiffList
        .asScala
        .exists(d => {
          d.getName == OfferFields.ParseDate
        })
    )
    assert(row.data.getOffer.getSeller.getPhones(0).getPhone == phone)

    // если объявление отфильтровано по причине older_20_days - обновляем

    assertFailedFiltration(existingRow.hash)(Older20Days, NoPhones)

    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(2).getMillis / 1000)
    val newRow3 = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow3, updated = true)
  }

  test("ignored isDealer") {
    pending // отключил проверку: мб с переходом на sh она не актуальна
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    parsedOffer.getIsDealerBuilder.setValue(true)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если признак isDealer был и пропал - игнорируем, не даем ему пропасть
    parsedOffer.getIsDealerBuilder.setValue(false)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow)

    val row = parsedOffersDao.getParsedOffers(Seq(newRow.hash)).head
    assert(row.data.getIsDealer.getValue)
  }

  test("isDealer: not ignored") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    parsedOffer.getIsDealerBuilder.setValue(true)
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // если признак isDealer был и пропал - снимаем его
    parsedOffer.getIsDealerBuilder.setValue(false)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)

    val row = parsedOffersDao.getParsedOffers(Seq(newRow.hash)).head
    assert(!row.data.getIsDealer.getValue)
  }

  test("save: no phones") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79293334455").setOriginal("79293334455")
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // в апдейте пропали телефоны - сохраняем, но телефоны оставляем
    parsedOffer.getOfferBuilder.getSellerBuilder.clearPhones()
    parsedOffer.getOfferBuilder.setDescription("test1")
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)

    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row.data.getOffer.getDescription == "test1")
    assert(row.data.getOffer.getSeller.getPhonesCount == 1)
    assert(row.data.getOffer.getSeller.getPhones(0).getPhone == "79293334455")
    assert(row.data.getStatusHistoryCount == 2)
    assert(row.data.getStatusHistory(1).getDiff(0).getName == "Phones")
    assert(row.data.getStatusHistory(1).getDiff(0).getIgnored.getValue)
  }

  test("save: fake phones") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).getMillis / 1000)
    parsedOffer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79293334455").setOriginal("79293334455")
    val existingRow = testRow(url, parsedOffer)
    assertSaveResult(existingRow, inserted = true)

    // в апдейте фейковые телефоны - игнорим
    parsedOffer.getOfferBuilder.setDescription("test2")
    parsedOffer.getOfferBuilder.getSellerBuilder
      .clearPhones()
      .addPhonesBuilder()
      .setPhone("79295556677")
      .setOriginal("79295556677")
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
    parsedOffer.getParseDateBuilder.setSeconds(DateTime.now().minusDays(1).plusHours(1).getMillis / 1000)
    val newRow = existingRow.copy(data = parsedOffer.build())
    assertSaveResult(newRow, updated = true)
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row.data.getOffer.getDescription == "test2")
    assert(row.data.getOffer.getSeller.getPhonesCount == 1)
    assert(row.data.getOffer.getSeller.getPhones(0).getPhone == "79293334455")
    assert(row.data.getStatusHistoryCount == 2)
    assert(row.data.getStatusHistory(1).getDiff(0).getName == "Phones")
    assert(row.data.getStatusHistory(1).getDiff(0).getIgnored.getValue)
  }

  test("setSent") {
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    val callCenter = "te_ex"
    val row = testRow(url)

    setMinutes(1)
    assertSaveResult(row, inserted = true)

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)

    setMinutes(4)
    val result4 = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 100)
    assert(result4.length == 1)
    assert(result4.head.hash == hash)
    assert(result4.head.sentDate.isEmpty)

    setMinutes(5)
    assertSuccessSent(hash)

    val row3 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row3.sentDate.value == dayStart.plusMinutes(5))
  }

  test("setOpened") {
    val callCenter = "te_ex"
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    val row = testRow(url)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assert(!parsedOffersDao.setOpened(hash).value)

    setMinutes(2)
    assertSuccessFiltration(hash)
    assert(!parsedOffersDao.setOpened(hash).value)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)

    setMinutes(4)
    val result4 = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 100)
    assert(result4.length == 1)
    assert(result4.head.hash == hash)
    assert(result4.head.sentDate.isEmpty)
    assert(result4.head.openDate.isEmpty)
    assert(!parsedOffersDao.setOpened(hash).value)

    setMinutes(5)
    assertSuccessSent(hash)

    setMinutes(6)
    assert(parsedOffersDao.setOpened(hash).value)
    val row3: ParsedRow =
      assertRow(hash, Status.OPENED, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(6))
    assert(row3.openDate.value == dayStart.plusMinutes(6))

    setMinutes(7)
    assert(parsedOffersDao.setNotPublished(hash, "reason").value)
    assert(!parsedOffersDao.setOpened(hash).value)
  }

  test("innerBatchUpdateRows") {
    val url1: String = testDromTrucksUrl
    val hash1 = DromTrucksParser.hash(url1)
    val row1 = testRow(url1)
    val url2: String = testDromTrucksUrl
    val hash2 = DromTrucksParser.hash(url2)
    val row2 = testRow(url2)

    assertSaveResults(row1, row2)(inserted = 2)

    // делаем первый ready
    assertSuccessFiltration(hash1)

    // теперь пробуем сделать оба ready
    val result3 = parsedOffersDao.filter(Seq(row1, row2)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(!result3(hash1))
    assert(result3(hash2))

    val result4 = parsedOffersDao.filter(Seq(row1, row2)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(!result4(hash1))
    assert(!result4(hash2))
  }

  test("filter skipped") {
    val url1: String = testDromTrucksUrl
    val hash1 = DromTrucksParser.hash(url1)
    val row1 = testRow(url1)

    assertSaveResults(row1)(inserted = 1)

    assertSkippedFiltration(hash1)
  }

  test("refilter") {
    val callCenter = "te_ex"
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    val row = testRow(url)

    setMinutes(1)
    assertSaveResult(row, inserted = true)

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)
    val dbRow1 =
      assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(3), dayStart.plusMinutes(2))
    assert(dbRow1.callCenter.value == "te_ex")
    assert(parsedOffersDao.count(QueryParams(hash = Seq(row.hash))) == 1)

    setMinutes(4)
    val result5 = parsedOffersDao.refilter(QueryParams(hash = Seq(row.hash)))
    assert(result5 == SelectUpdateResult(1, 1))
    val dbRow2 = assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(4), dayStart.plusMinutes(4))
    assert(dbRow2.callCenter.isEmpty)
  }

  test("notPublishedReasonsWatcher") {
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    val row = testRow(url, parsedOffer)
    assertSaveResult(row, inserted = true)

    assertSuccessFiltration(hash)

    val row2: ParsedRow = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.status == Status.READY)

    assertSuccessCallCenterSet(hash, "te_ex")

    val result4 = parsedOffersDao.getOrSetSendingForCallCenter("te_ex", Category.TRUCKS, 0, 100)
    assert(result4.length == 1)
    assert(result4.head.hash == hash)
    assert(result4.head.sentDate.isEmpty)

    // объявление не в статусе "послано" - не обновляем
    assert(!parsedOffersDao.setNotPublished(hash, "Я не спешу с продажей").value)

    assertSuccessSent(hash)

    // теперь обновляем
    assert(parsedOffersDao.setNotPublished(hash, "Я не спешу с продажей").value)

    val watcher = NotPublishedReasonsWatcher
    val result = db.query("")(dsl => watcher.selectQuery(dsl, Seq(hash))).map(watcher.mapRow)
    assert(result.length == 1)
    assert(result.head == IndexData(hash, "Я не спешу с продажей"))
  }

  test("updateHashes") {
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val remoteId = AvitoTrucksParser.remoteId(url)
    val oldRemoteId: String = "avito|trucks|pewpew"
    val oldHash: String = "pewpew"
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.setHash(oldHash)
    parsedOffer.getOfferBuilder.getAdditionalInfoBuilder.setRemoteId(oldRemoteId)
    val row = testRow(url, parsedOffer).copy(hash = oldHash)
    val result1 = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert(result1.insertedSimple == 1)
    assert(result1.updatedSimple == 0)

    val row2 = parsedOffersDao.getParsedOffers(Seq(oldHash)).head
    assert(row2.url == url)
    assert(row2.hash == oldHash)
    assert(row2.data.getHash == oldHash)
    assert(row2.data.getOffer.getAdditionalInfo.getRemoteId == oldRemoteId)

    val result2 = parsedOffersDao.updateHashes(QueryParams(hash = Seq(oldHash)), allowDeletion = true)
    assert(result2.updated == 1)
    assert(parsedOffersDao.getParsedOffers(Seq(oldHash)).isEmpty)

    val row3 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row3.url == url)
    assert(row3.data.getHash == hash)
    assert(row3.data.getOffer.getAdditionalInfo.getRemoteId == remoteId)
  }

  test("setPublished, setNotPublished, setOpened for unknown hash") {
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    assert(parsedOffersDao.setPublished(hash, "100500-hash", Seq.empty).isEmpty)
    assert(parsedOffersDao.setNotPublished(hash, "100500-hash").isEmpty)
    assert(parsedOffersDao.setOpened(hash).isEmpty)
  }

  test("getOrSetSendingForCallCenter: no exclude sites") {
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url)
    assertSaveResult(row, inserted = true)

    assertSuccessFiltration(hash)

    val row2: ParsedRow = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.status == Status.READY)

    assertSuccessCallCenterSet(hash, "te_ex")

    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10, Seq.empty)
    assert(rows.length == 1)
    assert(rows.head.hash == hash)

    val result4 = parsedOffersDao.setSent(Seq(hash))
    assert(result4(hash))
    val result5 = parsedOffersDao.setNotPublished(hash, "reason")
    assert(result5.value)
  }

  test("getOrSetSendingForCallCenter: exclude sites") {
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url)
    val result1 = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert(result1.insertedSimple == 1)
    assert(result1.updatedSimple == 0)

    val result2 = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result2(hash))

    val row2: ParsedRow = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.status == Status.READY)

    assertSuccessCallCenterSet(hash, "te_ex")

    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10, Seq(Site.Avito))
    assert(rows.isEmpty)

    parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10, Seq())
    parsedOffersDao.setSent(Seq(hash))
    parsedOffersDao.setNotPublished(hash, "reason")
  }

  test("setPublished: add published phones") {
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url)
    val result1 = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert(result1.insertedSimple == 1)
    assert(result1.updatedSimple == 0)

    val result2 = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result2(hash))

    val row2: ParsedRow = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.status == Status.READY)

    assertSuccessCallCenterSet(hash, "te_ex")

    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10)
    assert(rows.nonEmpty)

    parsedOffersDao.setSent(Seq(hash))

    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)

    // второй раз не даст опубликовать
    val wrongOfferId: String = "100502-hash2"
    val wrongPhone: String = "792944445566"
    assertFailedPublished(hash, wrongOfferId, wrongPhone)

    val row4 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row4.status == Status.PUBLISHED)
    assert(row4.offerId.contains(offerId))
    assert(row4.data.getPublishedPhoneList.asScala == Seq(phone))
  }

  test("processParsedOffersByParams") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val description = "description"
    parsedOffer.getOfferBuilder.setDescription(description)
    val row = testRow(url, parsedOffer = parsedOffer)

    val result1 = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert(result1.insertedSimple == 1)
    assert(result1.updatedSimple == 0)

    val allChecksPassed = parsedOffersDao.processParsedOffersByParams(
      "test",
      QueryParams(hash = Seq(hash)),
      it => {
        val row1 = it.next
        assert(it.isEmpty)
        assert(row1.hash == hash)
        assert(row1.site == Site.Avito)
        assert(row1.url == url)
        assert(row1.status == Status.NEW)
        assert(row1.source == Source.HTTP)
        assert(row1.category == Category.TRUCKS)
        assert(row1.createDate == dayStart)
        assert(row1.data.getOffer.getDescription == description)
        true
      }
    )
    assert(allChecksPassed)
  }

  test("receive updates for existing sent rows") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(1), dayStart.plusMinutes(1))

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(3), dayStart.plusMinutes(2))

    setMinutes(4)
    assertNonEmptySendingRows(callCenter)
    assertRow(hash, Status.SENDING, dayStart.plusMinutes(1), dayStart.plusMinutes(4), dayStart.plusMinutes(4))

    setMinutes(5)
    assertSuccessSent(hash)
    assertRow(hash, Status.SENT, dayStart.plusMinutes(1), dayStart.plusMinutes(5), dayStart.plusMinutes(5))

    setMinutes(6)
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(6))

    setMinutes(7)
    parsedOffer.getOfferBuilder.setDescription("description")
    parsedOffer.getParseDateBuilder.setSeconds(10)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(7), dayStart.plusMinutes(6))
    assert(parsedOffersDao.getParsedOffers(Seq(hash)).head.data.getOffer.getDescription == "description")
  }

  test("restore sent field") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(1), dayStart.plusMinutes(1))

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(3), dayStart.plusMinutes(2))

    setMinutes(4)
    assertNonEmptySendingRows(callCenter)
    assertRow(hash, Status.SENDING, dayStart.plusMinutes(1), dayStart.plusMinutes(4), dayStart.plusMinutes(4))

    setMinutes(5)
    assertSuccessSent(hash)
    assertRow(hash, Status.SENT, dayStart.plusMinutes(1), dayStart.plusMinutes(5), dayStart.plusMinutes(5))

    setMinutes(6)
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(6))

    components.parsingShard.master.jooq.update("upd") { dsl =>
      dsl.update(o).set(o.SENT_DATE, null.asInstanceOf[Timestamp]).where(o.HASH.equal(hash))
    }

    val row2 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.sentDate.isEmpty)

    setMinutes(7)
    parsedOffer.getOfferBuilder.setDescription("description")
    parsedOffer.getParseDateBuilder.setSeconds(10)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    val row3 =
      assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(7), dayStart.plusMinutes(6))
    assert(row3.data.getOffer.getDescription == "description")
    assert(row3.sentDate.value == dayStart.plusMinutes(5))

    parsedOffersDao.resetStatusUpdateDate(QueryParams(hash = Seq(hash)))
    val row4 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row4.statusUpdateDate == new DateTime(1970, 1, 1, 0, 0, 0, 0))

    setMinutes(8)
    parsedOffersDao.asInstanceOf[ParsedOffersDaoImpl].batchUpdate("add new statusHistory", Seq(hash)) { row =>
      Some(
        row.update("add new statusHistory")(
          data = builder =>
            builder
              .addStatusHistoryBuilder()
              .setStatus(Status.NEW)
              .getUpdateDateBuilder
              .setSeconds(components.timeService.getNow.getMillis / 1000)
        )
      )
    }

    setMinutes(9)
    parsedOffer.getOfferBuilder.setDescription("description2")
    parsedOffer.getParseDateBuilder.setSeconds(20)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    val row5 =
      assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(9), dayStart.plusMinutes(6))
    assert(row5.data.getOffer.getDescription == "description2")
  }

  test("setActive") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(1), dayStart.plusMinutes(1))

    val updated = components.parsingShard.master.jooq.update("set-fake-deactivate-date") { dsl =>
      dsl.update(o).set(o.DEACTIVATE_DATE, new Date(0)).where(o.HASH.equal(hash))
    }
    assert(updated == 1)

    val row2 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.deactivateDate.contains(new DateTime(1970, 1, 1, 0, 0, 0, 0)))

    val setActiveResult = parsedOffersDao.setActive(Seq(hash))
    assert(setActiveResult.size == 1)
    assert(setActiveResult(hash))

    val row3 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row3.deactivateDate.isEmpty)

    val setActiveResult2 = parsedOffersDao.setActive(Seq(hash))
    assert(setActiveResult2.size == 1)
    assert(!setActiveResult2(hash))

    val setDeactivatedResult = parsedOffersDao.setDeactivated(Seq(hash))
    assert(setDeactivatedResult.size == 1)
    assert(setDeactivatedResult(hash))

    val row4 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row4.deactivateDate.contains(dayStart))

    val setActiveResult3 = parsedOffersDao.setActive(Seq(hash))
    assert(setActiveResult3.size == 1)
    assert(!setActiveResult3(hash))
  }

  test("setDeactivated: skip already deactivated check") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer)

    setMinutes(1)
    assertSaveResult(row, inserted = true)

    setMinutes(2)
    assertSuccessFiltration(hash)

    setMinutes(3)
    assertSuccessDeactivation(hash)
    assertRow(
      hash,
      Status.NEW,
      expectedCreateDate = dayStart.plusMinutes(1),
      expectedUpdateDate = dayStart.plusMinutes(3),
      expectedStatusUpdateDate = dayStart.plusMinutes(3),
      expectedDeactivateDate = Some(dayStart)
    )

    setMinutes(4)
    assertFailedDeactivation(hash)

    setMinutes(5)
    assertSuccessDeactivation(hash, skipCheck = true)
    assertRow(
      hash,
      Status.NEW,
      expectedCreateDate = dayStart.plusMinutes(1),
      expectedUpdateDate = dayStart.plusMinutes(5),
      expectedStatusUpdateDate = dayStart.plusMinutes(3),
      expectedDeactivateDate = Some(dayStart)
    )
  }

  test("setDeactivated") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(1), dayStart.plusMinutes(1))

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessDeactivation(hash)
    assertRow(
      hash,
      Status.NEW,
      dayStart.plusMinutes(1),
      dayStart.plusMinutes(3),
      dayStart.plusMinutes(3),
      expectedDeactivateDate = Some(dayStart)
    )
    assertFailedDeactivation(hash)

    setMinutes(4)
    parsedOffer.getOfferBuilder.setDescription("description")
    parsedOffer.getParseDateBuilder.setSeconds(10)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    assertRow(
      hash,
      Status.NEW,
      dayStart.plusMinutes(1),
      dayStart.plusMinutes(4),
      dayStart.plusMinutes(3),
      expectedDeactivateDate = None
    )

    setMinutes(5)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(5), dayStart.plusMinutes(5))

    setMinutes(6)
    assertSuccessCallCenterSet(hash, callCenter)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(5))

    setMinutes(7)
    assertNonEmptySendingRows(callCenter)
    assertRow(hash, Status.SENDING, dayStart.plusMinutes(1), dayStart.plusMinutes(7), dayStart.plusMinutes(7))

    setMinutes(8)
    assertSuccessSent(hash)
    assertRow(hash, Status.SENT, dayStart.plusMinutes(1), dayStart.plusMinutes(8), dayStart.plusMinutes(8))

    setMinutes(9)
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(9), dayStart.plusMinutes(9))

    setMinutes(10)
    assertSuccessDeactivation(hash)
    assertRow(
      hash,
      Status.PUBLISHED,
      dayStart.plusMinutes(1),
      dayStart.plusMinutes(10),
      dayStart.plusMinutes(9),
      expectedDeactivateDate = Some(dayStart)
    )

    setMinutes(11)
    assertFailedDeactivation(hash)

    setMinutes(12)
    parsedOffer.getOfferBuilder.setDescription("description2")
    parsedOffer.getParseDateBuilder.setSeconds(11)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    assertRow(
      hash,
      Status.PUBLISHED,
      dayStart.plusMinutes(1),
      dayStart.plusMinutes(12),
      dayStart.plusMinutes(9),
      expectedDeactivateDate = None
    )
  }

  test("getCurrentDaySentOrReadyWithCallCenters") {
    setTime(dayStart)
    val parsedOffer = ParsedOffer.newBuilder()
    val callCenter = "unique_cc_" + RandomUtil.nextHexString(5)
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url, parsedOffer = parsedOffer, geobaseId = 11)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(1), dayStart.plusMinutes(1))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .getOrElse((Site.Avito, callCenter, 10776), 0) == 0
    )

    setMinutes(2)
    assertFailedFiltration(row.hash)(Older20Days, NoPhones)
    assertRow(hash, Status.FILTERED, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .getOrElse((Site.Avito, callCenter, 10776), 0) == 0
    )

    setMinutes(3)
    parsedOffer.getOfferBuilder.setDescription("description")
    parsedOffer.getParseDateBuilder.setSeconds(11)
    assertSaveResult(testRow(url, parsedOffer = parsedOffer), updated = true)
    assertRow(hash, Status.NEW, dayStart.plusMinutes(1), dayStart.plusMinutes(3), dayStart.plusMinutes(3))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .getOrElse((Site.Avito, callCenter, 10776), 0) == 0
    )

    setMinutes(4)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(4), dayStart.plusMinutes(4))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .getOrElse((Site.Avito, callCenter, 10776), 0) == 0
    )

    setMinutes(5)
    assertSuccessCallCenterSet(hash, callCenter)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(5), dayStart.plusMinutes(4))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .apply((Site.Avito, callCenter, 10776)) == 1
    )

    setMinutes(6)
    assertNonEmptySendingRows(callCenter)
    assertRow(hash, Status.SENDING, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(6))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .apply((Site.Avito, callCenter, 10776)) == 1
    )

    setMinutes(7)
    assertSuccessSent(hash)
    assertRow(hash, Status.SENT, dayStart.plusMinutes(1), dayStart.plusMinutes(7), dayStart.plusMinutes(7))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .apply((Site.Avito, callCenter, 10776)) == 1
    )

    setMinutes(8)
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(8), dayStart.plusMinutes(8))
    assert(
      parsedOffersDao
        .getCurrentDaySentOrReadyWithCallCenters(Category.TRUCKS)
        .apply((Site.Avito, callCenter, 10776)) == 1
    )
  }

  test("update call center") {
    val url: String = testDromTrucksUrl
    val hash = DromTrucksParser.hash(url)
    val callCenter1 = "cc1"
    val callCenter2 = "cc2"
    val row = testRow(url)

    setMinutes(1)
    assertSaveResult(row, inserted = true)
    assertFailedCallCenterSet(hash, callCenter1)
    assertNoCallCenter(hash)

    setMinutes(2)
    assertSuccessFiltration(hash)
    assertRow(hash, Status.READY, dayStart.plusMinutes(1), dayStart.plusMinutes(2), dayStart.plusMinutes(2))

    setMinutes(3)
    assertSuccessCallCenterSet(hash, callCenter1)
    assertCallCenter(hash, callCenter1)
    assertSuccessCallCenterSet(hash, callCenter2)
    assertCallCenter(hash, callCenter2)

    setMinutes(4)
    assertNonEmptySendingRows(callCenter2)
    assertRow(hash, Status.SENDING, dayStart.plusMinutes(1), dayStart.plusMinutes(4), dayStart.plusMinutes(4))
    assertFailedCallCenterSet(hash, callCenter1)
    assertCallCenter(hash, callCenter2)

    setMinutes(5)
    assertSuccessSent(hash)
    assertRow(hash, Status.SENT, dayStart.plusMinutes(1), dayStart.plusMinutes(5), dayStart.plusMinutes(5))
    assertFailedCallCenterSet(hash, callCenter1)
    assertCallCenter(hash, callCenter2)

    setMinutes(6)
    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)
    assertRow(hash, Status.PUBLISHED, dayStart.plusMinutes(1), dayStart.plusMinutes(6), dayStart.plusMinutes(6))
    assertFailedCallCenterSet(hash, callCenter1)
    assertCallCenter(hash, callCenter2)
  }

  test("setVins") {
    val url1 = testAvitoTrucksUrl
    val hash1 = CommonAutoParser.hash(url1)
    val row1 = testRow(url1)

    setMinutes(1)
    assertSaveResult(row1, inserted = true)

    parsedOffersDao.setVins(Map(hash1 -> "VIN1"))
    val row = parsedOffersDao.getParsedOffers(Seq(hash1)).head
    assert(row.data.getOffer.getDocuments.getVin == "VIN1")
  }

  test("updatePublishedPhotos: not published") {
    val url1 = testAvitoTrucksUrl
    val hash1 = CommonAutoParser.hash(url1)
    val row1 = testRow(url1)

    setMinutes(1)
    assertSaveResult(row1, inserted = true)

    val row0 = parsedOffersDao.getParsedOffers(Seq(hash1)).head
    assert(row0.data.getPublishedPhotoCount == 0)

    val res = parsedOffersDao.updatedPublishedPhotos(hash1, Seq("photo1", "photo2"))
    assert(!res.value)
    val row = parsedOffersDao.getParsedOffers(Seq(hash1)).head
    assert(row.data.getPublishedPhotoCount == 0)
  }

  test("updatePublishedPhotos: not found") {
    val url1 = testAvitoTrucksUrl
    val hash1 = CommonAutoParser.hash(url1)

    val res = parsedOffersDao.updatedPublishedPhotos(hash1, Seq("photo1", "photo2"))
    assert(res.isEmpty)
  }

  test("updatePublishedPhotos: success and no changes") {
    val callCenter = "te_ex"
    val url: String = testAvitoTrucksUrl
    val hash = AvitoTrucksParser.hash(url)
    val row = testRow(url)
    val result1 = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert(result1.insertedSimple == 1)
    assert(result1.updatedSimple == 0)

    val result2 = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result2(hash))

    val row2: ParsedRow = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.status == Status.READY)

    assertSuccessCallCenterSet(hash, "te_ex")

    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10)
    assert(rows.nonEmpty)

    parsedOffersDao.setSent(Seq(hash))

    val offerId: String = "100500-hash"
    val phone: String = "79291112233"
    assertSuccessPublished(hash, offerId, phone)

    assert(parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo1", "photo2")).value)
    assert(!parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo1", "photo2")).value)
    assert(parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo2")).value)
    assert(!parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo2")).value)
    assert(parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo2", "photo3")).value)
    assert(parsedOffersDao.updatedPublishedPhotos(hash, Seq("photo4", "photo5")).value)
    val row3 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row3.data.getPublishedPhotoCount == 2)
    assert(row3.data.getPublishedPhoto(0) == "photo4")
    assert(row3.data.getPublishedPhoto(1) == "photo5")
  }

  test("getActiveHashes") {
    val url1 = testAvitoTrucksUrl
    val hash1 = CommonAutoParser.hash(url1)
    val row1 = testRow(url1)

    val url2 = testAvitoTrucksUrl
    val hash2 = CommonAutoParser.hash(url2)
    val row2 = testRow(url2)

    val url3 = testAvitoTrucksUrl
    val hash3 = CommonAutoParser.hash(url3)
    val row3 = testRow(url3)

    setMinutes(0)
    parsedOffersDao.setDeactivated(
      parsedOffersDao
        .getParsedOffersByParams(
          "active_hashes",
          QueryParams(
            site = Seq(Site.Avito),
            category = Seq(Category.TRUCKS),
            idGte = Some(0),
            isActive = Some(true),
            orderById = Some(AscDesc.Asc),
            limit = Some(Int.MaxValue)
          )
        )
        .map(_.hash)
    )

    parsedOffersDao.setDeactivated(
      parsedOffersDao
        .getParsedOffersByParams(
          "active_hashes",
          QueryParams(
            site = Seq(Site.Drom),
            category = Seq(Category.TRUCKS),
            idGte = Some(0),
            isActive = Some(true),
            orderById = Some(AscDesc.Asc),
            limit = Some(Int.MaxValue)
          )
        )
        .map(_.hash)
    )

    setMinutes(1)
    assertSaveResult(row1, inserted = true)
    val id1 = parsedOffersDao.getParsedOffers(Seq(hash1)).headOption.value.id
    assertSaveResult(row2, inserted = true)
    val id2 = parsedOffersDao.getParsedOffers(Seq(hash2)).headOption.value.id
    assertSaveResult(row3, inserted = true)
    val id3 = parsedOffersDao.getParsedOffers(Seq(hash3)).headOption.value.id

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = 0,
      limit = 10,
      Seq(
        (id1, url1, hash1),
        (id2, url2, hash2),
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = 0,
      limit = 3,
      Seq(
        (id1, url1, hash1),
        (id2, url2, hash2),
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = 0,
      limit = 2,
      Seq(
        (id1, url1, hash1),
        (id2, url2, hash2)
      )
    )

    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = 0, limit = 1, Seq((id1, url1, hash1)))
    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = 0, limit = 0, Seq())

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = id2,
      limit = 10,
      Seq(
        (id2, url2, hash2),
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = id2,
      limit = 3,
      Seq(
        (id2, url2, hash2),
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = id2,
      limit = 2,
      Seq(
        (id2, url2, hash2),
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id2, limit = 1, Seq((id2, url2, hash2)))
    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id2, limit = 0, Seq())

    expectActiveHashes(
      site = Site.Avito,
      category = Category.TRUCKS,
      idGte = id3,
      limit = 10,
      Seq(
        (id3, url3, hash3)
      )
    )

    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id3, limit = 3, Seq((id3, url3, hash3)))
    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id3, limit = 2, Seq((id3, url3, hash3)))
    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id3, limit = 1, Seq((id3, url3, hash3)))
    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id3, limit = 0, Seq())

    expectActiveHashes(site = Site.Avito, category = Category.TRUCKS, idGte = id3 + 1, limit = 10, Seq())

    expectActiveHashes(site = Site.Drom, category = Category.TRUCKS, idGte = 0, limit = 10, Seq())
    expectActiveHashes(site = Site.Avito, category = Category.CARS, idGte = 0, limit = 10, Seq())
    expectActiveHashes(site = Site.Drom, category = Category.CARS, idGte = 0, limit = 10, Seq())
  }

  test("add 1 doppel cluster in empty clusters") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    val cluster = initDoppelClusterWithCreatedAt(1577448353) // newest
    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster))
    rowWithDoppel.data.getDoppelClusterList should contain theSameElementsAs Seq(cluster)
  }

  test("set to new after add doppel cluster") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    assertSuccessFiltration(row.hash)
    val cluster = initDoppelClusterWithCreatedAt(1577448353) // newest
    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster))
    rowWithDoppel.data.getDoppelClusterList should contain theSameElementsAs Seq(cluster)
    rowWithDoppel.status shouldBe Status.NEW
  }

  test("add 3 doppel cluster in empty clusters when 2 allow") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    val maxClusterCount = 2
    val cluster1 = initDoppelClusterWithCreatedAt(1577448353) // newest
    val cluster2 = initDoppelClusterWithCreatedAt(1577448355)
    val cluster3 = initDoppelClusterWithCreatedAt(1577448356) // latest
    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster1, cluster3, cluster2), maxClusterCount)
    (rowWithDoppel.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster2, cluster3))
  }

  test("add 1 doppel cluster when 2 exists and 2 allow") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    val maxClusterCount = 2
    val cluster1 = initDoppelClusterWithCreatedAt(1577448353, "1", "2") // newest
    val cluster2 = initDoppelClusterWithCreatedAt(1577448355, "2", "3")
    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster2, cluster1), maxClusterCount)
    (rowWithDoppel.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster1, cluster2))

    // add latest cluster
    val cluster3 = initDoppelClusterWithCreatedAt(1577448356, "3", "4") // latest
    val updatedRow1WithLatestDoppel = updateRowWithDoppel(row, Seq(cluster3), maxClusterCount)
    updatedRow1WithLatestDoppel.data.getDoppelClusterList should have size maxClusterCount
    (updatedRow1WithLatestDoppel.data.getDoppelClusterList should contain)
      .theSameElementsInOrderAs(Seq(cluster2, cluster3))
  }

  test("add the same 1 doppel cluster that already exists inside") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    val cluster1 = initDoppelClusterWithCreatedAt(1577448353) // newest
    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster1), 10)
    (rowWithDoppel.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster1))

    // add the same cluster
    val rowWithDoppelUpdated = updateRowWithDoppel(row, Seq(cluster1), 10)
    (rowWithDoppelUpdated.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster1))
  }

  test("add the same doppel cluster but with reorded matched offers") {
    val row: ParsedRow = testRow(testDromCarsUrl, doppelClusterSeq = Some(Seq()))
    parsedOffersDao.save(Seq(row))
    val cluster1 = initDoppelClusterWithCreatedAt(1577448353, "1", "2", "3")

    val rowWithDoppel = updateRowWithDoppel(row, Seq(cluster1), 10)
    (rowWithDoppel.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster1))

    val cluster2 = initDoppelClusterWithCreatedAt(1577448355, "3", "2", "1")
    val cluster3 = initDoppelClusterWithCreatedAt(1577448357, "4", "5", "6")

    val rowWithDoppel2 = updateRowWithDoppel(row, Seq(cluster2, cluster3), 10)
    (rowWithDoppel2.data.getDoppelClusterList should contain).theSameElementsInOrderAs(Seq(cluster1, cluster3))
  }

  test("updateLicensePlates: empty map") {
    val result = parsedOffersDao.updateLicensePlates(Map())
    result should have size 0
  }

  test("updateLicensePlates: empty seq") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate = randomLicensePlate
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    val result = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq())
    )
    result should have size 1
    result.get(row.hash).value shouldBe false
  }

  test("updateLicensePlates: add new license plate") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate = randomLicensePlate
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    val result = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate, confidence)))
    )
    result should have size 1
    result.get(row.hash).value shouldBe true

    val updated = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
    updated.data.getRecognizedLicensePlateCount shouldBe 1
    updated.data.getRecognizedLicensePlate(0).getLicensePlate shouldBe licensePlate
    updated.data.getRecognizedLicensePlate(0).getPhoto shouldBe photoUrl
    updated.data.getRecognizedLicensePlate(0).getConfidence shouldBe confidence
  }

  test("updateLicensePlates: empty license plate - don't add anything") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate = ""
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    val result = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate, confidence)))
    )
    result should have size 1
    result.get(row.hash).value shouldBe false

    val updated = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
    updated.data.getRecognizedLicensePlateCount shouldBe 0
  }

  test("updateLicensePlates: empty license plate - remove existing") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate = randomLicensePlate
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate, confidence)))
    )

    val result = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, "", confidence)))
    )

    result should have size 1
    result.get(row.hash).value shouldBe true

    val updated = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
    updated.data.getRecognizedLicensePlateCount shouldBe 0
  }

  test("updateLicensePlates: update existing") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate1 = randomLicensePlate
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate1, confidence)))
    )

    val updated1 = parsedOffersDao.getParsedOffers(Seq(row.hash)).head

    updated1.data.getRecognizedLicensePlateCount shouldBe 1
    updated1.data.getRecognizedLicensePlate(0).getLicensePlate shouldBe licensePlate1

    val licensePlate2 = randomLicensePlate

    val result2 = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate2, confidence)))
    )

    result2 should have size 1
    result2.get(row.hash).value shouldBe true

    val updated2 = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
    updated2.data.getRecognizedLicensePlateCount shouldBe 1
    updated2.data.getRecognizedLicensePlate(0).getLicensePlate shouldBe licensePlate2
  }

  test("updateLicensePlates: same licensePlate: don't update existing") {
    val photoUrl = testAvitoPhotoUrl
    val licensePlate1 = randomLicensePlate
    val confidence = RandomUtil.nextDouble(0, 1)
    val row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
    parsedOffersDao.save(Seq(row))

    parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate1, confidence)))
    )

    val updated1 = parsedOffersDao.getParsedOffers(Seq(row.hash)).head

    updated1.data.getRecognizedLicensePlateCount shouldBe 1
    updated1.data.getRecognizedLicensePlate(0).getLicensePlate shouldBe licensePlate1

    val result2 = parsedOffersDao.updateLicensePlates(
      Map(row.hash -> Seq(LicensePlateRecord(row.remoteId, photoUrl, licensePlate1, confidence)))
    )

    result2 should have size 1
    result2.get(row.hash).value shouldBe false

    val updated2 = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
    updated2.data.getRecognizedLicensePlateCount shouldBe 1
    updated2.data.getRecognizedLicensePlate(0).getLicensePlate shouldBe licensePlate1
  }

  def updateRowWithDoppel(row: ParsedRow, clusters: Seq[Cluster], maxClusterCount: Int = 2): ParsedRow = {
    val mapForUpdateAgain: Map[String, Seq[Cluster]] = Map(row.hash -> clusters)
    parsedOffersDao.addDoppelClusters(mapForUpdateAgain, maxClusterCount)
    parsedOffersDao.getParsedOffers(Seq(row.hash)).last
  }

  private def initDoppelClusterWithCreatedAt(createdAt: Long, matched: String*): Cluster = {
    Cluster
      .newBuilder()
      .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(createdAt))
      .addAllMatched(matched.map(m => Entity.newBuilder().setId(m).build()).asJava)
      .build()
  }

  private def expectActiveHashes(site: Site,
                                 category: Category,
                                 idGte: Long,
                                 limit: Int,
                                 expected: Seq[(Long, String, String)]): Unit = {
    val active = parsedOffersDao
      .getParsedOffersByParams(
        "active_hashes",
        QueryParams(
          site = Seq(site),
          category = Seq(category),
          idGte = Some(idGte),
          isActive = Some(true),
          orderById = Some(AscDesc.Asc),
          limit = Some(limit)
        )
      )
      .map(_.asActiveOfferInfo)
    assert(active.length == expected.length)
    active.zipWithIndex.zip(expected).foreach {
      case ((ActiveOfferInfo(actualId, actualUrl, actualHash, _), idx), (expectedId, expectedUrl, expectedHash)) =>
        assert(actualId == expectedId, s"row $idx")
        assert(actualUrl == expectedUrl, s"row $idx")
        assert(actualHash == expectedHash, s"row $idx")
    }
  }

  private def setSecond(second: Int) = {
    when(components.timeService.getNow).thenReturn(dayStart.withSecondOfMinute(second))
  }

  private def ensureInsert(parsedRow: ParsedRow, expectedHoloQueueSize: Int = 1): Unit = {
    val saveResult2 = ImportResult(parsedOffersDao.save(Seq(parsedRow)))(_.name())
    assert(saveResult2.insertedSimple == 1)
    assert(saveResult2.updatedSimple == 0)
  }

  private def assertSuccessPublished(hash: String, offerId: String, phone: String) = {
    val result = parsedOffersDao.setPublished(hash, offerId, Seq(phone))
    assert(result.value)
  }

  private def assertFailedPublished(hash: String, offerId: String, phone: String) = {
    val result = parsedOffersDao.setPublished(hash, offerId, Seq(phone))
    assert(!result.value)
  }

  private def assertSaveResult(row: ParsedRow, inserted: Boolean = false, updated: Boolean = false): Unit = {
    val result = ImportResult(parsedOffersDao.save(Seq(row)))(_.name())
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
  }

  private def assertSaveResultWithComment(row: ParsedRow,
                                          comment: String = "",
                                          inserted: Boolean = false,
                                          updated: Boolean = false,
                                          dbComment: String = ""): Unit = {
    val result = ImportResult(parsedOffersDao.save(Seq(row), comment))(_.name())
    assert((result.insertedSimple == 1) == inserted)
    assert((result.updatedSimple == 1) == updated)
    if (inserted) {
      val dbRow = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
      assert(dbRow.data.getStatusHistoryList.asScala.last.getComment == dbComment)
    } else if (updated) {
      val dbRow = parsedOffersDao.getParsedOffers(Seq(row.hash)).head
      assert(dbRow.data.getStatusHistoryList.asScala.last.getComment == dbComment)
    }
  }

  private def assertSaveResults(rows: ParsedRow*)(inserted: Int = 0, updated: Int = 0): Unit = {
    val result = ImportResult(parsedOffersDao.save(rows))(_.name())
    assert(result.insertedSimple == inserted)
    assert(result.updatedSimple == updated)
  }

  private def assertFailedFiltration(hash: String)(errors: String*): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Invalid(errors)).toMap
    }
    assert(result(hash))
    val row2 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.data.getFilterReasonList.asScala.sorted == errors.sorted)
    assert(row2.data.getStatusHistoryList.asScala.last.getComment.split(",").sorted.toSeq == errors.sorted)
    errors.foreach(error => {
      parsedOffersDao.getHashesByParams(QueryParams(filterReasons = Seq(error), limit = Some(1))).contains(hash)
    })
  }

  private def assertSuccessFiltration(hash: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Valid).toMap
    }
    assert(result(hash))
  }

  private def assertSkippedFiltration(hash: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    val result = parsedOffersDao.filter(Seq(row)) { rows =>
      rows.map(row => row -> ValidationResult.Skipped).toMap
    }
    assert(!result(hash))
    val row2 = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row2.version == row.version)
    assert(row2.status == row.status)
    assert(row2.status == CommonModel.Status.NEW)
    assert(row2.statusUpdateDate == row.statusUpdateDate)
    assert(row2.updateDate == row.updateDate)
  }

  private def assertFailedCallCenterSet(hash: String, callCenter: String): Unit = {
    val result = parsedOffersDao.setCallCenter(Seq(hash), callCenter)
    assert(!result(hash))
  }

  private def assertSuccessCallCenterSet(hash: String, callCenter: String): Unit = {
    val result = parsedOffersDao.setCallCenter(Seq(hash), callCenter)
    assert(result(hash))
  }

  private def assertNoCallCenter(hash: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row.callCenter.isEmpty)
  }

  private def assertCallCenter(hash: String, callCenter: String): Unit = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row.callCenter.contains(callCenter))
  }

  private def assertNonEmptySendingRows(callCenter: String): Unit = {
    val rows = parsedOffersDao.getOrSetSendingForCallCenter(callCenter, Category.TRUCKS, 0, 10)
    assert(rows.nonEmpty)
  }

  private def assertSuccessSent(hash: String): Unit = {
    val result = parsedOffersDao.setSent(Seq(hash))
    assert(result(hash))
  }

  private def assertSuccessDeactivation(hash: String, skipCheck: Boolean = false): Unit = {
    val result = parsedOffersDao.setDeactivated(Seq(hash), skipDeactivatedCheck = skipCheck)
    assert(result(hash))
  }

  private def assertFailedDeactivation(hash: String): Unit = {
    val result = parsedOffersDao.setDeactivated(Seq(hash))
    assert(!result(hash))
  }

  private def assertRow(hash: String,
                        expectedStatus: Status,
                        expectedCreateDate: DateTime,
                        expectedUpdateDate: DateTime,
                        expectedStatusUpdateDate: DateTime,
                        expectedDeactivateDate: Option[DateTime] = None): ParsedRow = {
    val row = parsedOffersDao.getParsedOffers(Seq(hash)).head
    assert(row.status == expectedStatus)
    assert(row.data.getStatusHistoryList.asScala.last.getStatus == expectedStatus)
    assert(row.createDate == expectedCreateDate)
    assert(row.data.getStatusHistoryList.asScala.head.getUpdateDate.getSeconds == expectedCreateDate.getMillis / 1000)
    assert(row.data.getStatusHistoryList.asScala.last.getUpdateDate.getSeconds == expectedUpdateDate.getMillis / 1000)

    val statusMap = getStatusMap(row)

    val lastStatusChangeMomentSeconds = getStatusHistoryMoments(row).head._2
    assert(lastStatusChangeMomentSeconds == expectedStatusUpdateDate.getMillis / 1000)
    assert(row.statusUpdateDate == expectedStatusUpdateDate)

    if (row.sentDate.nonEmpty || statusMap.contains(Status.SENT)) {
      assert(row.sentDate.nonEmpty)
      assert(statusMap.contains(Status.SENT))
      assert(statusMap(Status.SENT) == row.sentDate.value.getMillis / 1000)
    }

    if (row.openDate.nonEmpty || statusMap.contains(Status.OPENED)) {
      assert(row.openDate.nonEmpty)
      assert(statusMap.contains(Status.OPENED))
      assert(statusMap(Status.OPENED) == row.openDate.value.getMillis / 1000)
    }

    if (row.status == Status.SENDING || row.status == Status.SENT || row.status == Status.OPENED ||
        row.status == Status.PUBLISHED) {
      assert(row.callCenter.nonEmpty)
    }

    if (row.status == Status.PUBLISHED) {
      assert(row.offerId.nonEmpty)
    }

    assert(row.deactivateDate == expectedDeactivateDate)

    row
  }

  private def getStatusMap(row: ParsedRow): Map[Status, Long] = {
    getStatusHistoryMoments(row)
      .groupBy(h => h._1)
      .mapValues(h => h.map(_._2).min)
  }

  private def getStatusHistoryMoments(row: ParsedRow): List[(Status, Long)] = {
    val l = row.data.getStatusHistoryList.asScala.map(h => (h.getStatus, h.getUpdateDate.getSeconds)).sortBy(_._2)
    l.foldLeft[List[(Status, Long)]](Nil) {
      case (res, e) =>
        if (res.isEmpty) List(e)
        else {
          val prevStatus = res.head._1
          val newStatus = e._1
          if (prevStatus == newStatus) res
          else {
            if (newStatus == Status.NEW &&
                (prevStatus == Status.SENT ||
                prevStatus == Status.OPENED ||
                prevStatus == Status.PUBLISHED ||
                prevStatus == Status.NOT_PUBLISHED)) {
              res
            } else e :: res
          }
        }
    }
  }

  private def setMinutes(minutes: Int) = {
    setTime(dayStart.plusMinutes(minutes))
  }

  private def setTime(date: DateTime) = {
    when(components.timeService.getNow).thenReturn(date)
  }

  private def getSeconds: Long = components.timeService.getNow.getSeconds
}
