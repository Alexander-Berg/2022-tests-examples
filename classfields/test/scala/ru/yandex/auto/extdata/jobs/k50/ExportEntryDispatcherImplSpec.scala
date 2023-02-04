package ru.yandex.auto.extdata.jobs.k50

import org.scalatest.{FlatSpecLike, Matchers}
import ru.yandex.auto.extdata.jobs.k50.model.{ExportEntry, ModelAuction}
import ru.yandex.auto.extdata.jobs.k50.services.impl.ExportEntryDispatcherImpl
import ru.yandex.vertis.mockito.MockitoSupport

class ExportEntryDispatcherImplSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val exportEntry1 = ModelAuction(123, "", 1, "", "", "", 0, 0, 0, "")
  private val exportEntry2 = ModelAuction(10928, "", 1, "", "", "", 0, 0, 0, "")

  "ExportEntryDispatcherImpl" should "dispatch exportEntries by id" in {
    ExportEntryDispatcherImpl.apply(exportEntry1) shouldBe 1
    ExportEntryDispatcherImpl.apply(exportEntry2) shouldBe 0
  }

}
