package ru.yandex.auto.extdata.jobs.k50

import org.scalatest.{FlatSpecLike, Matchers}
import ru.yandex.auto.extdata.jobs.k50.model.{ExportEntry, ModelAuction}
import ru.yandex.auto.extdata.jobs.k50.services.{ExportEntryPrinter, PrintWriterFactory}
import ru.yandex.auto.extdata.jobs.k50.services.impl.{ExportEntries2FilesExporterImpl, ExportEntryDispatcherImpl}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.vertis.mockito.MockitoSupport

import java.io.File

class ExportEntries2FilesExporterImplSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val file1 = new File("/1")
  private val file2 = new File("/2")

  private val exportEntryPrinter1 = mock[ExportEntryPrinter[ExportEntry, File]]
  when(exportEntryPrinter1.print(?)).answer { _ =>
    ()
  }
  when(exportEntryPrinter1.output).answer { _ =>
    file1
  }
  when(exportEntryPrinter1.close()).answer { _ =>
    ()
  }

  private val exportEntryPrinter2 = mock[ExportEntryPrinter[ExportEntry, File]]
  when(exportEntryPrinter2.print(?)).answer { _ =>
    ()
  }
  when(exportEntryPrinter2.output).answer { _ =>
    file2
  }
  when(exportEntryPrinter2.close()).answer { _ =>
    ()
  }

  private val printWriterFactory = mock[PrintWriterFactory[ExportEntry, File]]

  when(printWriterFactory.apply()).answer { _ =>
    exportEntryPrinter1
  }

  when(printWriterFactory.apply()).answer { _ =>
    exportEntryPrinter2
  }

  private val exportEntry2FilesExporterImpl =
    new ExportEntries2FilesExporterImpl(ExportEntryDispatcherImpl, printWriterFactory)

  private val exportEntry1 = ModelAuction(1, "", 1, "", "", "", 0, 0, 0, "")
  private val exportEntry2 = ModelAuction(2, "", 1, "", "", "", 0, 0, 0, "")

  "ExportEntry2FilesExporterImpl" should "exports to correct shards" in {
    exportEntry2FilesExporterImpl.export(Seq(exportEntry1, exportEntry2).iterator).keys.toSeq shouldBe Seq(1, 0)
  }

}
