package ru.yandex.auto.vin.decoder.scheduler.local.utils.options

import ru.yandex.auto.vin.decoder.scheduler.local.utils.options.MakeReportByUnlabeledOptionQueueDao._
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.IterableHasAsJava

class MakeReportByUnlabeledOptionQueueDao(yt: Yt)(implicit ec: ExecutionContext) {

  def uploadUnlabeledOptionForReportToYt(
      insertedInfo: Iterable[UnlabeledOptionForReport],
      tablePath: String): Future[Unit] =
    uploadToYT((getUnlabeledOptionForReportRecordsIterator(insertedInfo)), tablePath)

  def uploadLabeledOptionToYt(insertedInfo: Iterable[LabeledOption], tablePath: String): Future[Unit] =
    uploadToYT((getLabeledOptionRecordsIterator(insertedInfo)), tablePath)

  private def uploadToYT(ytIterator: Iterable[YTreeMapNode], tablePath: String): Future[Unit] = {
    val done = writeToYt(ytIterator, tablePath)

    done.onComplete(_ => {
      println(s"Inserted data to $tablePath")
    })
    done
  }

  private def writeToYt(records: Iterable[YTreeMapNode], outputPath: String): Future[Unit] =
    Future {
      println(s"Start write data to yt. path = $outputPath")
      yt.tables()
        .write(
          YPath.simple(outputPath).append(true),
          YTableEntryTypes.YSON,
          records.asJava
        )
      println(s"Finish write data to yt. path = $outputPath")
    }

  private def getUnlabeledOptionForReportRecordsIterator(
      insertedInfo: Iterable[UnlabeledOptionForReport]): Iterable[YTreeMapNode] = {
    insertedInfo.map(r => buildUnlabeledOptionForReportToYtRecord(r))
  }

  private def getLabeledOptionRecordsIterator(insertedInfo: Iterable[LabeledOption]): Iterable[YTreeMapNode] = {
    insertedInfo.map(r => buildLabeledOptionToYtRecord(r))
  }

  private def buildUnlabeledOptionForReportToYtRecord(raw: UnlabeledOptionForReport): YTreeMapNode = {
    YTree
      .mapBuilder()
      .key(VinColumn)
      .value(raw.vin)
      .key(MarkColumn)
      .value(raw.mark)
      .key(ModelColumn)
      .value(raw.model)
      .key(CodeColumn)
      .value(raw.code)
      .key(PartnerColumn)
      .value(raw.partner)
      .key(DescriptionColumn)
      .value(raw.description)
      .buildMap()
  }

  private def buildLabeledOptionToYtRecord(raw: LabeledOption): YTreeMapNode = {
    YTree
      .mapBuilder()
      .key(MarkColumn)
      .value(raw.mark)
      .key(CodeColumn)
      .value(raw.code)
      .buildMap()
  }

}

object MakeReportByUnlabeledOptionQueueDao {

  val VinColumn = "vin"
  val MarkColumn = "mark"
  val ModelColumn = "model"
  val CodeColumn = "code"
  val PartnerColumn = "source_id"
  val DescriptionColumn = "description"
}
