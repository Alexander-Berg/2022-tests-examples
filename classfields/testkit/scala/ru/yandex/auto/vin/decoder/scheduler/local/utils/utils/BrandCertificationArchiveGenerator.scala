package ru.yandex.auto.vin.decoder.scheduler.local.utils

import play.api.libs.json.{JsArray, Json}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.{BrandCertification, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.autoru.programs.model.{AutoruFormatProgramModel, ProgramInfo}
import ru.yandex.vertis.protobuf.ProtobufUtils

import java.io.{File, FileOutputStream, PrintWriter}
import scala.concurrent.duration._
import scala.io.Source
import scala.jdk.CollectionConverters.ListHasAsScala

object BrandCertificationArchiveGenerator extends App {

  // 1. распарсить файл
  // 2. для каждого вина - сгруппировать по имени программы, выбрать самую последнюю по дате обновления
  // 3. если дата не слишком старая - то active, иначе falseъ
  // 4. в качестве event_date - create timestamp
  // 5. дальше - сгруппировоать списки program name -> моделька всех сертификаций
  // 6. дальше разбить по source_id и файлы нагенерить

  // 7. начать можно с какого-то одного

  val archiveFile = new File("/Users/sievmi/Desktop/cert/all_certifications.json")

  val input: Seq[(VinCode, Seq[BrandCertification])] = Source
    .fromFile(archiveFile)
    .getLines()
    .map(Json.parse)
    .map(json => {
      val proto = ProtobufUtils
        .fromJson(VinInfoHistory.getDefaultInstance, (json \ "data").as[String])
        .getCertificationList
        .asScala
      val vin = VinCode((json \ "vin").as[String])
      vin -> proto.toList
    })
    .toList

  val vinProgramsLastData = input.map { case (vin, certifications) =>
    val data = certifications.groupBy(_.getProgramName).map { case (group, list) =>
      group -> list.maxBy(_.getLastUpdateTimestamp)
    }
    vin -> data
  }

  val Pegetut1 = "BentleyCertified"
  val Pegetut2 = "BentleyCertified"

  val peugeutPrograms = vinProgramsLastData
    .flatMap { case (vin, programs) =>
      programs.get(Pegetut1).orElse(programs.get(Pegetut2)).map(p => vin -> p)
    }
    .map { case (vin, proto) =>
      val isActive = (System.currentTimeMillis() - proto.getLastUpdateTimestamp) <= 5.days.toMillis
      AutoruFormatProgramModel(
        proto.getProgramName,
        vin,
        102,
        Some(
          ProgramInfo(proto.getCreateTimestamp, proto.getProgramName, None, isActive, None, None)
        )
      )
    }

  val json = JsArray(peugeutPrograms.map(m => Json.toJson(m)))
  val fileJ = new File("/Users/sievmi/Desktop/cert/BentleyCertified.json")
  val writer = new PrintWriter(new FileOutputStream(fileJ))
  writer.println(Json.prettyPrint(json))
  writer.close()

  val y = 2
  println(input.size)

}
