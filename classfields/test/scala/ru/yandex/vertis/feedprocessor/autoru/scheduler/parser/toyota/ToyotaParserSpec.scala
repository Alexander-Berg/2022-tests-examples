package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.toyota

import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.TaskContext

import java.time.YearMonth
import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class ToyotaParserSpec extends WordSpecBase {
  implicit val taskContext = TaskContext(newTasksGen.next)

  private def checkFirstOffer(offer: RowOffer): Unit = {
    offer.mark shouldEqual "Toyota"
    offer.model shouldEqual "ALPHARD"
    offer.fullKatashiki shouldEqual "GGH30L-PFZVK"
    offer.suffix shouldEqual "1C"
    offer.color shouldEqual "070/21"
    offer.vin shouldEqual "JTNGZ3DH008035575"
    offer.urn shouldEqual "J183395638"
    offer.purpose shouldEqual "Retail"
    offer.productionDate shouldEqual Some(YearMonth.of(2018, 3))
    offer.complectationName shouldEqual ""
    offer.engineNumber shouldEqual "2GR K531488"
    offer.pts shouldEqual "78УХ467138"
    offer.price shouldEqual ""
    offer.comment shouldEqual ""
    offer.taskContext shouldEqual taskContext
  }

  "ToyotaParser" should {
    "parse XLS files" in {
      val parser = new ToyotaParser(getClass.getResourceAsStream("/toyota/xlsxCBDD_mini.xls"), taskContext)
      val offers = parser.toList
      offers should have size (10)
      val offer = offers.head.right.get
      checkFirstOffer(offer)
    }

    "parse XLSX files" in {
      val parser = new ToyotaParser(getClass.getResourceAsStream("/toyota/xlsxCBDD_mini.xlsx"), taskContext)
      val offers = parser.toList
      offers should have size (10)
      val offer = offers.head.right.get
      checkFirstOffer(offer)
    }

    "parse CSV files" in {
      pending
    }

    "handle any headers order" in {
      val parser = new ToyotaParser(getClass.getResourceAsStream("/toyota/xlsxCBDD_mini_columnorder.xls"), taskContext)
      val offers = parser.toList
      offers should have size (10)
      val offer = offers.head.right.get
      checkFirstOffer(offer)
    }

    "handle header names case insensitively" in {
      val parser = new ToyotaParser(getClass.getResourceAsStream("/toyota/xlsxCBDD_mini_caseinsen.xls"), taskContext)
      val offers = parser.toList
      offers should have size (10)
      val offer = offers.head.right.get
      checkFirstOffer(offer)
    }

    "handle TMR_EPReserveExportReport22011932480.xlsx file" in {
      val parser =
        new ToyotaParser(getClass.getResourceAsStream("/toyota/TMR_EPReserveExportReport22011932480.xlsx"), taskContext)
      val offers = parser.toList
      offers should have size (129)
      val offer = offers.head.right.get
      offer.mark shouldEqual "Toyota"
      offer.model shouldEqual "CAMRY"
      offer.fullKatashiki shouldEqual "ASV70L-RETNKX"
      offer.suffix shouldEqual "B3"
      offer.color shouldEqual "040/20"
      offer.vin shouldEqual "XW7BF4HK60S125800"
      offer.urn shouldEqual "RU21732200"
      offer.productionDate shouldEqual Some(YearMonth.of(2018, 12))
      offer.engineNumber shouldEqual "2AR J239199"
      offer.pts shouldEqual "78РВ206469"
      offer.price shouldEqual "1834000"
    }
  }
}
