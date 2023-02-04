package ru.yandex.vertis.parsing.auto.diffs

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.components.TestCatalogsAndFeaturesComponents
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.parsers.av100.cars.avito.Av100AvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.validators.FilterReason._
import ru.yandex.vertis.parsing.diffs.{DiffAnalyzer, DiffAnalyzerContext, DiffAnalyzerFactory, DiffAnalyzerFactoryImpl}
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRowUtils._
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class AutoDiffAnalyzerFactoryImplTest extends FunSuite with OptionValues {

  private val diffAnalyzerFactory: DiffAnalyzerFactory[ParsedRow] = new DiffAnalyzerFactoryImpl[ParsedRow] {

    override def newDiffAnalyzer(newRow: ParsedRow,
                                 existingRow: ParsedRow,
                                 comment: String): DiffAnalyzer[ParsedRow] = {
      val diffs = new AutoDiffCalculator(newRow, existingRow).generateDiff
      new AutoDiffAnalyzer(newRow, existingRow, diffs, comment)(TestCatalogsAndFeaturesComponents.regionsAware)
    }
  }

  implicit private val trace: Traced = TracedUtils.empty

  test("checkHasIsDealer") {
    pending // отключил проверку: мб с переходом на sh она не актуальна
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getIsDealerBuilder.setValue(false)
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.isEmpty)
      assert(ctx.diffs.length == 2)
      assert(ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getName == OfferFields.IsDealer)
      assert(ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getIgnored.getValue)
    }
  }

  test("checkHasIsDealer: not ignored") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getIsDealerBuilder.setValue(false)
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(ctx.diffs.length == 2)
      assert(ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getName == OfferFields.IsDealer)
      assert(!ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getIgnored.getValue)
    }
  }

  test("checkHasAddress") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getOfferBuilder.setDescription("new description")
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setAddress("existing address")
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(res.value.data.getOffer.getDescription == "new description")
      assert(res.value.data.getOffer.getSeller.getLocation.getAddress == "existing address")
      assert(ctx.diffs.length == 3)
      assert(ctx.diffs.find(_.getName == OfferFields.Address).value.getIgnored.getValue)
    }
  }

  test("new address not in russia and no existing address") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getOfferBuilder.setDescription("new description")
    newOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(11514)
    newOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setAddress("new address")
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(res.value.data.getOffer.getDescription == "new description")
      assert(res.value.data.getOffer.getSeller.getLocation.getGeobaseId == 0)
      assert(res.value.data.getOffer.getSeller.getLocation.getAddress.isEmpty)
      assert(ctx.diffs.length == 4)
      assert(ctx.diffs.find(_.getName == OfferFields.Address).value.getIgnored.getValue)
      assert(ctx.diffs.find(_.getName == OfferFields.GeobaseId).value.getIgnored.getValue)
    }
  }

  test("checkAddressInRussia") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getOfferBuilder.setDescription("new description")
    newOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(11514)
    newOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setAddress("new address")
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(1)
    existingOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setAddress("existing address")
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(res.value.data.getOffer.getDescription == "new description")
      assert(res.value.data.getOffer.getSeller.getLocation.getGeobaseId == 1)
      assert(res.value.data.getOffer.getSeller.getLocation.getAddress == "existing address")
      assert(ctx.diffs.length == 4)
      assert(ctx.diffs.find(_.getName == OfferFields.Address).value.getIgnored.getValue)
      assert(ctx.diffs.find(_.getName == OfferFields.GeobaseId).value.getIgnored.getValue)
    }
  }

  test("checkGeobaseIdInRussia") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getOfferBuilder.setDescription("new description")
    newOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(11514)
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(1)
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(res.value.data.getOffer.getDescription == "new description")
      assert(res.value.data.getOffer.getSeller.getLocation.getGeobaseId == 1)
      assert(ctx.diffs.length == 3)
      assert(ctx.diffs.find(_.getName == OfferFields.GeobaseId).value.getIgnored.getValue)
    }
  }

  test("checkHasGeobaseId") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getOfferBuilder.setDescription("new description")
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(1)
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.nonEmpty)
      assert(res.value.data.getOffer.getDescription == "new description")
      assert(res.value.data.getOffer.getSeller.getLocation.getGeobaseId == 1)
      assert(ctx.diffs.length == 3)
      assert(ctx.diffs.find(_.getName == OfferFields.GeobaseId).value.getIgnored.getValue)
    }
  }

  test("checkHasSection") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    existingOffer.getOfferBuilder.setSection(Section.USED)
    val existingRow = testRow(url, existingOffer)
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    withClue(ctx.diffs) {
      assert(res.isEmpty)
      assert(ctx.diffs.length == 2)
      assert(ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getName == OfferFields.Section)
      assert(ctx.diffs.filter(_.getName != OfferFields.ParseDate).head.getIgnored.getValue)
    }
  }

  test("updateRequired: isInactive") {
    val url = testAvitoTrucksUrl
    val newOffer = ParsedOffer.newBuilder()
    newOffer.getParseDateBuilder.setSeconds(1)
    val newRow = testRow(url, newOffer)
    val existingOffer = ParsedOffer.newBuilder()
    val existingRow = testRow(url, existingOffer).copy(deactivateDate = Some(DateTime.now().withMillisOfDay(0)))
    implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
    val res = diffAnalyzerFactory.analyze(newRow, existingRow)
    assert(res.nonEmpty)
  }

  {
    for {
      reason <- Seq(Older5Days, Older20Days, TodaySent, RecentlySent30Days, TodaySentPhpByPhone, RecentlySentPhpByPhone)
    } test(s"updateRequired: $reason") {
      val url = testAvitoTrucksUrl
      val newOffer = ParsedOffer.newBuilder()
      newOffer.getParseDateBuilder.setSeconds(1)
      val newRow = testRow(url, newOffer)
      val existingOffer = ParsedOffer.newBuilder()
      existingOffer.addStatusHistoryBuilder().setStatus(CommonModel.Status.FILTERED)
      existingOffer.addFilterReason(reason)
      val existingRow = testRow(url, existingOffer).copy(status = CommonModel.Status.FILTERED)
      implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
      val res = diffAnalyzerFactory.analyze(newRow, existingRow)
      assert(res.nonEmpty)
    }

    test("isImportPossible: sent: allow to import, if other conditions ok") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser
      for {
        status <- Seq(
          CommonModel.Status.SENT,
          CommonModel.Status.OPENED,
          CommonModel.Status.PUBLISHED,
          CommonModel.Status.NOT_PUBLISHED
        )
      } {
        val existingRow = testRow(url).copy(status = status)
        assert(diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
      }
    }

    test("isImportPossible: no existing row") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj())
      val source = CommonModel.Source.HTTP
      val parser = AvitoCarsParser
      assert(diffAnalyzerFactory.isImportPossible(newRow, None, parser, source))
    }

    test("isImportPossible: no existing parse date") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser
      val existingRow = testRow(url).copy(status = CommonModel.Status.FILTERED)
      assert(diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: no existing parse date for same source") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.AV100,
        status = CommonModel.Status.FILTERED
      )

      existingOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)

      assert(diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: no existing parse date for same source 2") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.HTTP,
        status = CommonModel.Status.FILTERED
      )

      assert(diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: no existing parse date for same source 3") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.AV100,
        status = CommonModel.Status.FILTERED
      )

      existingOffer
        .addStatusHistoryBuilder()
        .setSource(CommonModel.Source.AV100)
        .addDiffBuilder()
        .setName(OfferFields.ParseDate)
        .setNewValue(DateUtils.jodaFormat(DateTime.now()))

      assert(diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: existing parse date is newer") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().minusHours(1).getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.HTTP,
        status = CommonModel.Status.FILTERED
      )

      existingOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)

      assert(!diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: existing parse date is equal") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.HTTP,
        status = CommonModel.Status.FILTERED
      )

      existingOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)

      assert(!diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("analyze: existing parse date is equal") {
      val url = testAvitoTrucksUrl
      val newOffer = ParsedOffer.newBuilder()
      newOffer.getOfferBuilder.getDocumentsBuilder.setYear(1996)
      val newRow = testRow(url, newOffer)
      val existingOffer = ParsedOffer.newBuilder()
      val existingRow = testRow(url, existingOffer)
      implicit val ctx: DiffAnalyzerContext = new DiffAnalyzerContext
      val res = diffAnalyzerFactory.analyze(newRow, existingRow)
      withClue(ctx.diffs) {
        assert(res.nonEmpty)
        assert(ctx.diffs.length == 1)
        assert(ctx.diffs.head.getName == OfferFields.Year)
      }
    }

    test("isImportPossible: existing parse date is newer 2") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().minusHours(1).getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.AV100,
        status = CommonModel.Status.FILTERED
      )

      existingOffer
        .addStatusHistoryBuilder()
        .setSource(CommonModel.Source.HTTP)
        .addDiffBuilder()
        .setName(OfferFields.ParseDate)
        .setNewValue(DateUtils.jodaFormat(DateTime.now()))

      assert(!diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: HTTP and IMPORT are the same sources") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().minusHours(1).getMillis / 1000))
      val source = CommonModel.Source.HTTP
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow: ParsedRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.IMPORT,
        status = CommonModel.Status.FILTERED
      )

      existingOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)

      assert(!diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }

    test("isImportPossible: HTTP and IMPORT are the same sources 2") {
      val url = testAvitoCarsUrl
      val newRow = ImportRow(url, Json.obj("OFFER_CREDATETS" -> DateTime.now().minusHours(1).getMillis / 1000))
      val source = CommonModel.Source.IMPORT
      val parser = Av100AvitoCarsParser

      val existingOffer = ParsedOffer.newBuilder()

      def existingRow = testRow(url, existingOffer).copy(
        source = CommonModel.Source.HTTP,
        status = CommonModel.Status.FILTERED
      )

      existingOffer.getParseDateBuilder.setSeconds(DateTime.now().getMillis / 1000)

      assert(!diffAnalyzerFactory.isImportPossible(newRow, Some(existingRow), parser, source))
    }
  }
}
