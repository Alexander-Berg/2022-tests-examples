package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners.customs

import ru.yandex.auto.vin.decoder.raw.customs.{CustomsRawModel, CustomsRawModelManager}
import ru.yandex.auto.vin.decoder.report.processors.entities.CustomsEntity

import java.io.{File, FileInputStream}

object CustomsChecker extends App {

  val manager = new CustomsRawModelManager

  // check(new File("/Users/sievmi/Desktop/tamojnya/output"))
  // check(new File("/Users/sievmi/Desktop/tamojnya/data-20190401-structure-20161230.json"))

  private val username = "yourname"

  check(new File(s"/Users/$username/Downloads/customs/json/"))

  def check(dirOrFile: File): Unit = {
    val it = {
      if (dirOrFile.isDirectory) {
        readFromDir(dirOrFile)
      } else {
        readFromFile(dirOrFile)
      }
    }

    val allRecords = it.map(_.record).toList

    println(s"Total size - ${allRecords.size}")
    println(s"With empty vin - ${allRecords.count(_.getVin.isEmpty)}")
    println(s"Distinct vins - ${allRecords.flatMap(_.getVin).toSet.size}")
    println(
      s"Distinct vins + countryId - ${allRecords.flatMap(r => r.getVin.map(v => v -> r.countryId)).toSet.size}"
    )
    println(
      s"Distinct vins + countryId + timestamp - ${allRecords.flatMap(r => r.getVin.map(v => (v, r.countryId, r.timestamp))).toSet.size}"
    )

    println(s"Distinct country ids - ${allRecords.flatMap(_.countryId).toSet.size}")
    println(s"Distinct country names - ${allRecords.flatMap(_.countryName).toSet.size}")

    val unknownCountryIds = allRecords.flatMap(_.countryId).distinct.filterNot(CustomsEntity.countryCode2Name.contains)
    println(s"Unknown country ids count - ${unknownCountryIds.size} ids = $unknownCountryIds")

    require(unknownCountryIds.isEmpty)

    println(
      s"Min date - ${CustomsCsvToJsonConverter.rawCustomsDateFormatter.print(allRecords.map(_.timestamp).min)}"
    )
    println(
      s"Max date - ${CustomsCsvToJsonConverter.rawCustomsDateFormatter.print(allRecords.map(_.timestamp).max)}"
    )

    println(s"Empty country code - ${allRecords.count(_.countryId.isEmpty)}")
    println(s"Empty country name - ${allRecords.count(_.countryName.isEmpty)}")
  }

  private def readFromDir(dir: File): Iterator[CustomsRawModel] = {
    dir
      .listFiles()
      .filter(_.getName.startsWith("data-"))
      .iterator
      .flatMap(file => {
        println(s"Process - ${file.getName}")
        readFromFile(file)
      })
  }

  private def readFromFile(file: File): Iterator[CustomsRawModel] = {
    manager.parseFile(new FileInputStream(file), file.getName).collect { case Right(r) =>
      r
    }
  }

}
