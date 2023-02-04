package ru.yandex.vertis.parsing.auto.dao.holocron

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.{ParsedRow, QueryParams}
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.{ParsedOffersDao, ParsedOffersDaoWrapper}
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.{testAvitoCarsUrl, testAvitoTrucksUrl, testRow}
import ru.yandex.vertis.parsing.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.dao.DbResult
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.{THolocronSendData, TParsedOffers}
import ru.yandex.vertis.parsing.holocron.{HolocronConversionResult, HolocronConverter}
import ru.yandex.vertis.parsing.util.dao.{Shard, TestShard}
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.validation.model.MissingRequiredField

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HolocronSaveDaoTest extends FunSuite with MockitoSupport with Eventually {
  private val t: TParsedOffers = TParsedOffers.T_PARSED_OFFERS
  private val h: THolocronSendData = THolocronSendData.T_HOLOCRON_SEND_DATA

  private val mockedTimeService = mock[TimeService]

  private val mockedParsedOffersDao = mock[ParsedOffersDao]

  private val mockedHolocronConverter = mock[HolocronConverter[ParsedRow]]

  private val parsedOffersDao: ParsedOffersDao = new ParsedOffersDaoWrapper(mockedParsedOffersDao)
    with HolocronSaveDao
    with SameThreadExecutionContextSupport {
    override def timeService: TimeService = mockedTimeService

    override def parsingShard: Shard = TestShard

    override def holocronConverter: HolocronConverter[ParsedRow] = mockedHolocronConverter
  }

  implicit private val trace: Traced = TracedUtils.empty

  test("trucks") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoTrucksUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.TRUCKS)

    val hash = parsedRow.hash
    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Inserted)))
    parsedOffersDao.save(Seq(parsedRow))
    TestShard.master.verifyAllExpectedQueriesCalled()
    verify(mockedParsedOffersDao).save(eq(Seq(parsedRow)), eq(""), eq(false))(?)
    verifyNoMoreInteractions(mockedParsedOffersDao)
    verifyZeroInteractions(mockedHolocronConverter)
  }

  test("activate") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.ACTIVATE).build()

    val hash = parsedRow.hash
    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Inserted)))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041001')"
    )

    parsedOffersDao.save(Seq(parsedRow))
    verify(mockedParsedOffersDao).save(eq(Seq(parsedRow)), eq(""), eq(false))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("update") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.UPDATE).build()

    val hash = parsedRow.hash
    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Updated(Set.empty[String]))))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'UPDATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041002')"
    )

    parsedOffersDao.save(Seq(parsedRow))
    verify(mockedParsedOffersDao).save(eq(Seq(parsedRow)), eq(""), eq(false))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("set vins") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.UPDATE).build()

    val hash = parsedRow.hash

    val vin = "VIN1"

    when(mockedParsedOffersDao.setVins(?)(?)).thenReturn(Map(hash -> true))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'UPDATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041002')"
    )

    parsedOffersDao.setVins(Map(hash -> vin))
    verify(mockedParsedOffersDao).setVins(eq(Map(hash -> vin)))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("unable to convert") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val hash = parsedRow.hash
    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Updated(Set.empty[String]))))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.UnableToConvert(List("error"))))
    when(mockedHolocronConverter.calculateAction(?)).thenReturn(Action.ACTIVATE)
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `unable_to_convert`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, 1)"
    )

    parsedOffersDao.save(Seq(parsedRow))
    verify(mockedParsedOffersDao).save(eq(Seq(parsedRow)), eq(""), eq(false))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    verify(mockedHolocronConverter).calculateAction(eq(parsedRow))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("unable to validate") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val hash = parsedRow.hash
    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Updated(Set.empty[String]))))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.UnableToValidate(List(MissingRequiredField("fieldName")))))
    when(mockedHolocronConverter.calculateAction(?)).thenReturn(Action.ACTIVATE)
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `unable_to_convert`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, 2)"
    )

    parsedOffersDao.save(Seq(parsedRow))
    verify(mockedParsedOffersDao).save(eq(Seq(parsedRow)), eq(""), eq(false))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    verify(mockedHolocronConverter).calculateAction(eq(parsedRow))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("set deactivated") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.DEACTIVATE).build()

    val hash = parsedRow.hash

    when(mockedParsedOffersDao.setDeactivated(?, ?, ?, ?)(?)).thenReturn(Map(hash -> true))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'DEACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041003')"
    )

    parsedOffersDao.setDeactivated(Seq(hash))
    verify(mockedParsedOffersDao).setDeactivated(eq(Seq(hash)), eq(false), eq(false), eq(None))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("set deactivated unable to convert") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val hash = parsedRow.hash

    when(mockedParsedOffersDao.setDeactivated(?, ?, ?, ?)(?)).thenReturn(Map(hash -> true))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.UnableToConvert(List("error"))))
    when(mockedHolocronConverter.calculateAction(?)).thenReturn(Action.ACTIVATE)
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `unable_to_convert`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, 1)"
    )

    parsedOffersDao.setDeactivated(Seq(hash))
    verify(mockedParsedOffersDao).setDeactivated(eq(Seq(hash)), eq(false), eq(false), eq(None))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    verify(mockedHolocronConverter).calculateAction(eq(parsedRow))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("set deactivated unable to validate") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val hash = parsedRow.hash

    when(mockedParsedOffersDao.setDeactivated(?, ?, ?, ?)(?)).thenReturn(Map(hash -> true))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.UnableToValidate(List(MissingRequiredField("fieldName")))))
    when(mockedHolocronConverter.calculateAction(?)).thenReturn(Action.ACTIVATE)
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `unable_to_convert`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, 2)"
    )

    parsedOffersDao.setDeactivated(Seq(hash))
    verify(mockedParsedOffersDao).setDeactivated(eq(Seq(hash)), eq(false), eq(false), eq(None))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(false), eq(None))(?)
    verify(mockedHolocronConverter).calculateAction(eq(parsedRow))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("set deactivated unable to validate, but skip validation") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.DEACTIVATE).build()

    val hash = parsedRow.hash

    when(mockedParsedOffersDao.setDeactivated(?, ?, ?, ?)(?)).thenReturn(Map(hash -> true))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'DEACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041003')"
    )

    parsedOffersDao.setDeactivated(Seq(hash), skipValidation = true)
    verify(mockedParsedOffersDao).setDeactivated(eq(Seq(hash)), eq(true), eq(false), eq(None))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(true), eq(None))(?)
    verifyNoMoreInteractions(mockedHolocronConverter)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("save unable to validate, but skip validation") {
    TestShard.master.clear()
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    val url: String = testAvitoCarsUrl

    val parsedRow: ParsedRow = testRow(url, category = Category.CARS)

    val holoOffer = HoloOffer.newBuilder().setAction(Action.ACTIVATE).build()

    val hash = parsedRow.hash

    when(mockedParsedOffersDao.save(?, ?, ?)(?))
      .thenReturn(Map(hash -> (parsedRow.category, parsedRow.site, DbResult.Inserted)))
    when(mockedParsedOffersDao.getParsedOffersByParams(?, ?, ?, ?)(?)).thenReturn(Seq(parsedRow))
    when(mockedHolocronConverter.convert(?, ?, ?)(?))
      .thenReturn(Future.successful(HolocronConversionResult.Converted(holoOffer)))
    TestShard.master.expectQuery(
      "insert into `parsing`.`t_holocron_send_data` (`hash`, `action`, `create_date`, " +
        s"`update_date`, `work_date`, `holo_offer`) values ('$hash', 'ACTIVATE', " +
        "{ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, {ts '2019-01-30 00:00:00.0'}, X'02041001')"
    )

    parsedOffersDao.save(Seq(parsedRow), skipValidation = true)
    verify(mockedParsedOffersDao).save(?, ?, eq(true))(?)
    verify(mockedParsedOffersDao).getParsedOffersByParams(
      eq("for_holocron_queue"),
      eq(
        QueryParams(
          hash = Seq(hash),
          category = Seq(Category.CARS)
        )
      ),
      eq(true),
      eq(false)
    )(?)
    verify(mockedHolocronConverter).convert(eq(parsedRow), eq(true), eq(None))(?)
    verifyNoMoreInteractions(mockedHolocronConverter)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }
}
