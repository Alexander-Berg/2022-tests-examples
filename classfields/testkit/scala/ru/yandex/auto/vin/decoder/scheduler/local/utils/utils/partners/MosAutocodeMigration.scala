package ru.yandex.auto.vin.decoder.scheduler.local.utils.partners

import auto.carfax.common.storages.yt.YtConfig
import auto.carfax.common.utils.config.Environment
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.autocode.MosAutocodeRawModel
import ru.yandex.bolts.collection.IteratorF
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.{TableReaderOptions, YTableEntryTypes}
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode

import java.util.Optional
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.jdk.CollectionConverters._

object MosAutocodeMigration extends App {

  // 2020-05-24T10:31:14Z

  val dateFormat: DateTimeFormatter =
    DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  private val ytConfig: YtConfig = YtConfig.fromConfig(Environment.config.getConfig("auto-vin-decoder.yt.hahn"))

  private val yt: Yt = YtUtils.http(
    ytConfig.url,
    ytConfig.token
  )

  val resultsIterator = yt
    .tables()
    .read(
      Optional.empty[GUID](),
      false,
      YPath.simple("//home/verticals/carfax/CARFAX-888/all_raw_data"),
      YTableEntryTypes.YSON,
      new TableReaderOptions
    )
    .asScala

  var successCount = 0
  var part = 0

  var errorsCount = 0

  resultsIterator
    .grouped(50000)
    .foreach(group => {
      part += 1
      println(s"Start process part $part")
      val start = System.currentTimeMillis()

      if (part == 29) {
        val nodes = group.par
          .flatMap(node => {
            val vin = VinCode(node.getString("vin"))
            val rawData = node.getString("data")

            val optRes = MosAutocodeRawModel.apply(vin, 200, rawData)
            if (optRes.isEmpty) errorsCount += 1 else successCount += 1

            optRes match {
              case Some(res) =>
                val newNode = YTree
                  .mapBuilder()
                  .key("vin")
                  .value(res.identifier.toString)
                  .key("data")
                  .value(node.getString("data"))
                  .key("timestamp")
                  .value(dateFormat.parseMillis(node.getString("created")))
                  .key("created")
                  .value(node.getString("created"))
                  .key("hash")
                  .value(res.hash)
                  .buildMap()

                Some(newNode)
              case _ => None
            }
          })

        println(s"Finish preparing for part $part. Stat write results to yt")

        yt.tables()
          .write(
            YPath.simple(s"//home/verticals/carfax/CARFAX-888/prepared/valid_records_part$part"),
            YTableEntryTypes.YSON,
            buildYtIterator(nodes.toIterator)
          )
      }

      println(s"Finish process part - $part (${(System.currentTimeMillis() - start) / 1000} s)")

    })

  println(s"Success - $successCount")
  println(s"Errors - $errorsCount")

  private def buildYtIterator(records: Iterator[YTreeMapNode]): IteratorF[YTreeMapNode] = {
    new IteratorF[YTreeMapNode] {
      override def hasNext: Boolean = records.hasNext

      override def next(): YTreeMapNode = records.next()
    }
  }

}
