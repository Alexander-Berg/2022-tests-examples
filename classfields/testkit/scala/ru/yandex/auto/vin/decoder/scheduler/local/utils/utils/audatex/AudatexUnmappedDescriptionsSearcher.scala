package ru.yandex.auto.vin.decoder.scheduler.local.utils.audatex

import auto.carfax.common.utils.config.Environment
import ru.yandex.auto.vin.decoder.extdata.ApiExtDataClient
import ru.yandex.auto.vin.decoder.partners.audatex.AudatexTextCleaner
import ru.yandex.auto.vin.decoder.scheduler.local.utils.LocalScript
import ru.yandex.auto.vin.decoder.yql.{YQLJdbc, YqlConfig}
import ru.yandex.vertis.feature.model.Feature

import java.io.{File, FileWriter}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AudatexUnmappedDescriptionsSearcher extends LocalScript {

  private val output: File = new File("/home/maratvin/Downloads/audatex_new.csv")

  ApiExtDataClient.start()

  private val yt = createYqlJdbc
  private val autdatexExtractor = createAudatexTextCleaner

  private val audatexUnmappedSearchRequest =
    """
      |PRAGMA yt.InferSchema = '1';
      |
      |$audatex = (
      |    SELECT Yson::ParseJson(prepared_data) as data
      |    from hahn.`//home/verticals/carfax/raw_storage/sources/AUDATEX`
      |);
      |
      |$extract_description = ($work) -> {
      |    return if(Yson::Contains($work, "description"), Yson::ConvertToString($work.description) , "")
      |};
      |
      |$descriptions = (
      |    SELECT ListMap(Yson::ConvertToList(ListHead(Yson::ConvertToList(data.adaperioAudatex)).report.works), $extract_description) as descriptions
      |    from $audatex
      |    where Yson::Contains(data, "adaperioAudatex")
      |    and Yson::Contains(ListHead(Yson::ConvertToList(data.adaperioAudatex)), 'report')
      |    and Yson::Contains(ListHead(Yson::ConvertToList(data.adaperioAudatex)).report, 'works')
      |);
      |
      |SELECT description, count(*) as records_count
      |from (
      |    SELECT descriptions as description
      |    from $descriptions flatten by descriptions
      |)
      |group by description
      |order by records_count desc;
      |""".stripMargin

  override def action: Future[Any] = {
    progressBar.start(1800000)
    val existing = ApiExtDataClient.Providers.audatexDictionaryProvider.get().descriptions
    val audatexRecords = yt
      .execute(
        audatexUnmappedSearchRequest,
        rs => {
          progressBar.inc(1)
          val description = rs.getString("description")
          val count = rs.getString("records_count").toLong
          if (description.trim.nonEmpty) {
            val (clean, _) = autdatexExtractor.cleanByRegexAndLocationDictionary(description)
            if (existing.contains(clean)) {
              None
            } else {
              Some(AudatexRecord(description, count))
            }
          } else {
            None
          }

        }
      )
      .flatten
      .toList
    outputToCsv(output, audatexRecords)
    Future.unit
  }

  private case class AudatexRecord(
      description: String,
      recordsCount: Long)

  private def outputToCsv(f: File, records: List[AudatexRecord]): Unit = {
    val writer = new FileWriter(f)
    writer.write("cleared_description,records_count\n") // csv header
    records.foreach { r =>
      writer.write(s""""${r.description}",${r.recordsCount}\n""") // csv lines
    }
    writer.flush()
    writer.close()
  }

  private def createYqlJdbc: YQLJdbc = {
    val yqlConfig: YqlConfig = YqlConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.yql.config"))
    new YQLJdbc(yqlConfig)
  }

  private def createAudatexTextCleaner: AudatexTextCleaner = {
    val provider = ApiExtDataClient.Providers.audatexDictionaryProvider
    val feature = new Feature[Boolean] {
      override def name: String = ""
      override def value: Boolean = true
    }
    new AudatexTextCleaner(provider, feature)
  }
}
