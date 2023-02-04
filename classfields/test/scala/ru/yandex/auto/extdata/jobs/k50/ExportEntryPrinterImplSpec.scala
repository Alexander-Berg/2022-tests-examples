package ru.yandex.auto.extdata.jobs.k50

import org.scalatest.{FlatSpecLike, Matchers}
import ru.yandex.auto.extdata.jobs.k50.model.ModelAuction
import ru.yandex.auto.extdata.jobs.k50.services.impl.{ExportEntryPrinterImpl, Formatter}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.io.Source

class ExportEntryPrinterImplSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val exportEntry = ModelAuction(2, "Moskva", 213, "bmw", "x5", "auto.ru", 0, 0, 0, "auto.ru/image")
  private val header = "ID;Region;Region id;Mark;Model;URL;min_bid;max_bid;n_dealers;img"
  private val formatter = mock[Formatter[ModelAuction]]
  when(formatter.header).answer { _ =>
    header
  }
  when(formatter.line(?)).answer { _ =>
    ModelAuction.unapply(exportEntry).map(_.productIterator.mkString(";"))
  }

  "ExportEntryPrinterImpl" should "print entries" in {
    val printer = new ExportEntryPrinterImpl(formatter)
    printer.print(exportEntry)
    printer.close()
    val source = Source.fromFile(printer.output)
    val lines = source.getLines().toArray
    lines(0) shouldBe header
    lines(1) shouldBe "2;Moskva;213;bmw;x5;auto.ru;0;0;0;auto.ru/image"
  }
}
