package ru.yandex.vertis.parsing.scheduler

import com.google.common.util.concurrent.MoreExecutors
import play.api.libs.json._
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.clients.searchline.MarkModel
import ru.yandex.vertis.parsing.auto.clients.searchline.SearchlineClient.ParsedSearchlineResponse
import ru.yandex.vertis.parsing.auto.components.ParsingAutoComponents
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.parsers.WebminerJsonUtils.{RichImportRow, _}
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.importrow.ImportRow
import ru.yandex.vertis.parsing.lifecycle.DefaultApplication
import ru.yandex.vertis.parsing.util.{FileUtils, IO, RandomUtil}
import ru.yandex.vertis.tracing.Traced

import java.io.InputStream
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Created by andrey on 10/24/17.
  */
object Tests extends DefaultApplication {
  private val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  implicit private val sameThreadEc: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(MoreExecutors.directExecutor())
  val components = new ParsingAutoComponents(this)

  afterStart {
    run()
    System.exit(0)
  }

  // TODO выгрузить все урлы с информацием, распознали или нет и в какую категорию и марку модель распознали

  private def run(): Unit = {
    implicit val trace: Traced = components.traceCreator.trace
    val f = Await.result(components.importClient.saveParsedOffers(DromTrucksParser), 2.hours)
    IO.usingTmpGzipFile(f) { in =>
      val it = ImportRow.fromStream(in)
      dromUrlCategoriesRaw(it)
    }
    /*val f1 = siteImport(AvitoParser)
    Await.result(f1, 2.hours)
    val f2 = siteImport(DromParser)
    Await.result(f2, 2.hours)*/
  }

  private def dromUrlCategoriesRaw(in: Iterator[ImportRow]): Unit = {
    implicit val trace: Traced = components.traceCreator.trace
    in.toSeq
      .groupBy(row => row.rawUrl.split("/")(4))
      .mapValues(rows =>
        rows.map(row => {
          val url = row.rawUrl
          val card = components.dataReceiver
            .enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
            .map(validImportRow => {
              val category = validImportRow.catalogData.asTrucksData.card.truckCategory
              val name = validImportRow.catalogData.asTrucksData.card.name
              (category, name)
            })
          (url, card.map(c => c._1.name() + " " + c._2).getOrElse("N/A"))
        })
      )
      .foreach(kv => {
        FileUtils.save("dromUrlCategoriesRaw.txt", append = true) { write =>
          val seq: Seq[(String, String)] = kv._2
          seq.foreach(x => {
            write(x._1 + " -> " + x._2)
          })
          write("====================================")
        }
      })
  }

  private def dromUrlCategories(in: Iterator[ImportRow]): Unit = {
    implicit val trace: Traced = components.traceCreator.trace
    in.toSeq
      .groupBy(row => row.rawUrl.split("/")(4))
      .mapValues(rows =>
        rows.map(row => {
          components.dataReceiver
            .enrichRow(row, None)(DromTrucksParser, CommonModel.Source.IMPORT, trace)
            .map(validImportRow => {
              val category = validImportRow.catalogData.asTrucksData.card.truckCategory
              val name = validImportRow.catalogData.asTrucksData.card.name
              (category, name)
            })
        })
      )
      .foreach(kv => {
        val distinctCategories = kv._2
          .flatMap(x => x.toOption.map(_._1.name()))
          .groupBy(x => x)
          .mapValues(_.length)
          .toSeq
          .sortBy(-_._2)
          .map(kv => kv._1 + "(" + kv._2 + ")")
          .mkString(", ")
        val str = kv._1 + ": " + kv._2.count(_.toOption.nonEmpty) + "/" + kv._2.length + "; " + distinctCategories
        println(str + "\n")
      })
  }

  private def siteImport(parser: CommonAutoParser)(implicit trace: Traced): Future[Unit] = {
    components.importClient
      .saveParsedOffers(parser)
      .map(f => {
        IO.usingTmpGzipFile(f) { in =>
          val it = ImportRow.fromStream(in)
          toloka(parser, it)
        }
      })(ec)
    /*val in = importRowStream(new FileInputStream("avitoru_spets"))
    distinctFnFieldsAndWizard(site, in)*/
  }

  private def distinctFields(in: Iterator[ImportRow]): Unit = {
    val rows = in.toSeq
    val a: Seq[(String, JsValue)] = rows.flatMap(row => {
      row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject].value.toSeq
    })
    val b = a
      .groupBy(_._1)
      .mapValues(x => {
        x.length
      })
    b.toSeq
      .sortBy(_._1)
      .foreach(kv => {
        println(kv._1 + " : " + s"${kv._2}/${rows.length}")
      })
  }

  private def printNRandomOffers(n: Int, in: Iterator[ImportRow]): Unit = {
    val rows = in.toSeq
    val choose: Seq[ImportRow] = RandomUtil.chooseN(n, rows)
    choose.foreach(row => {
      println(row.rawUrl)
      println(row.rawJson)
      println("----------------------------")
    })
  }

  private def distinctOwnerFields(parser: CommonAutoParser, in: Iterator[ImportRow]): Unit = {
    /*val rows = in.toSeq
    val x: Seq[(String, Seq[(String, Int)])] = rows.flatMap(row => {
      val json = row.json
      parser.parseSingleJson(json, "owner").toSeq.flatMap(info => {
        info.value.flatMap { case (key, value) =>
          val str = parser.parseSingleString(info, key)
          str.map(s => (key, s))
        }.toSeq
      })
    }).groupBy(_._1)
      .mapValues(v => v.map(_._2).groupBy(vv => vv).mapValues(_.length).toSeq.sortBy(-_._2))
      .toSeq
      .sortBy(_._1)
    x.foreach(kv => {
      val str: String = kv._1 + " : " + s"${kv._2.length}/${rows.length};" +
        s" ${kv._2.map(b => s"${b._1}(${b._2})").take(10).mkString(", ")}"
      println(str)
    })*/
  }

  private def distinctInfoFields(parser: CommonAutoParser, in: Iterator[ImportRow]): Unit = {
    val rows = in.toSeq
    val x: Seq[(String, Seq[(String, Int)])] = rows
      .flatMap(row => {
        val json = row.webminerJson
        parseSingleJson(json, "info").toSeq.flatMap(info => {
          info.value.flatMap {
            case (key, value) =>
              val str = parseSingleString(info, key)
              str.map(s => (key, s))
          }.toSeq
        })
      })
      .groupBy(_._1)
      .mapValues(v => v.map(_._2).groupBy(vv => vv).mapValues(_.length).toSeq.sortBy(-_._2))
      .toSeq
      .sortBy(_._1)
    x.foreach(kv => {
      val str: String = if (kv._2.lengthCompare(10) <= 0) {
        kv._1 + " : " + s"${kv._2.map(b => s"${b._1}(${b._2})").mkString(", ")}"
      } else {
        kv._1 + " : " + s"${kv._2.take(10).map(b => s"${b._1}(${b._2})").mkString("", ", ", "...")}"
      }
      println(str)
    })
  }

  private def distinctInfoTypeFields(in: Iterator[ImportRow]): Unit = {
    val x: Seq[String] = in
      .flatMap(row => {
        val json = row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject]
        (json \ "info").toOption.toSeq.map(x => {
          val y = Json.parse(x.asInstanceOf[JsArray].value.head.asInstanceOf[JsString].value).asInstanceOf[JsObject]
          (y \ "type").get.asInstanceOf[JsArray].value.head.asInstanceOf[JsString].value
        })
      })
      .toSeq
      .distinct
      .sorted
    x.foreach(println)
  }

  private def distinctFnFields(parser: CommonAutoParser, in: Iterator[ImportRow]): Unit = {
    val x: Seq[String] = in
      .flatMap(row => {
        val json = row.webminerJson
        parser.parseFn(json).toOption
      })
      .toSeq
      .distinct
      .sorted
    FileUtils.append("fn.txt") { write =>
      x.foreach(write)
    }
  }

  /*private def distinctFnFieldsAndWizard(site: String, in: Iterator[ImportRow])
                                       (implicit trace: Traced): Unit = {
    FileUtils.save(s"fn_$site.txt") { write =>
      in.foreach(row => {
        val json = row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject]
        (json \ "fn").toOption.foreach(x => {
          val fn = x.asInstanceOf[JsArray].value.head.asInstanceOf[JsString].value
          val f = components.wizardClient.parseTrucks(fn).recover {
            case error =>
              log.error(s"Failed request: $fn", error)
              MarkModel(None, None, fn)
          }
          val markModel = Await.result(f, 2.hours)
          write(row.url + " | " + fn + " | " + markModel.text + " | " + markModel.name)
        })
      })
    }
  }*/

  private def toloka(parser: CommonAutoParser, in: Iterator[ImportRow])(implicit trace: Traced): Unit = {
    val filename = s"toloka_${parser.site}.txt"
    println(s"${Thread.currentThread()} saving $filename")
    FileUtils.save(filename) { write =>
      in.foreach(row => {
        val json = row.webminerJson
        (json \ "fn").toOption.foreach(x => {
          val hash =
            try {
              parser.hash(row.rawUrl).take(8)
            } catch {
              case _: Exception =>
                println(s"no hash from url: ${row.rawUrl}")
                ""
            }
          val text = x.asInstanceOf[JsArray].value.head.asInstanceOf[JsString].value
          val markModel: MarkModel = parseTrucks(text)
          val photos = parser.parsePhotos(json).getOrElse(Seq.empty).mkString(", ")
          if (photos.nonEmpty) {
            val markName = if (markModel.nonEmpty) {
              components.trucksCatalog
                .getCardByMarkModel(markModel.mark.fold("")(_.value), markModel.model.fold("")(_.value))
                .headOption
                .map(_.message.getMark.getCyrillicName)
                .getOrElse("N/A")
            } else "N/A"
            write(hash + " | " + text + " | " + markName + " | " + photos)
          }
        })
      })
    }
    tolokaSmall(parser, filename)
  }

  private def parseTrucks(text: String)(implicit trace: Traced): MarkModel = {
    val f = components.searchlineClient
      .suggest(text)
      .recover {
        case error =>
          log.error(s"${Thread.currentThread()} Failed request: $text", error)
          ParsedSearchlineResponse(
            MarkModel(None, None, text, category = Category.CARS),
            MarkModel(None, None, text, category = Category.TRUCKS)
          )
      }(sameThreadEc)
    val markModel = Await.result(f, 2.hours).trucksMarkModel
    markModel
  }

  private def tolokaSmall(parser: CommonAutoParser, file: String): Unit = {
    val smallFile = s"toloka_${parser.site}_small.txt"
    println(s"saving $smallFile")
    val lines = scala.io.Source.fromFile(file).getLines().length
    val buf = (0 until lines).toBuffer
    val randomLines = (1 to 5000)
      .map(_ => {
        buf.remove((math.random() * buf.length).toInt)
      })
      .toSet
    FileUtils.save(smallFile) { write =>
      scala.io.Source.fromFile(file).getLines().zipWithIndex.foreach {
        case (line, idx) =>
          if (randomLines(idx)) {
            write(line)
          }
      }
    }
  }

  /*private def letter(site: String, in: Iterator[ImportRow])
                    (implicit trace: Traced): Unit = {
    FileUtils.append("letter.txt") { write =>
      in.foreach(row => {
        val json = row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject]
        (json \ "fn").toOption.foreach(x => {
          lazy val phones: Seq[String] = parsePhones(json)

          lazy val address: Option[Region] = parseAddress(json, site, row.url)

          lazy val year = parseYear(json)

          lazy val price: String = parsePrice(json)

          lazy val fn = x.asInstanceOf[JsArray].value.head.asInstanceOf[JsString].value

          lazy val markModel = Await.result(
            components.wizardClient.parseTrucks(fn).recover {
              case NonFatal(error) => MarkModel(None, None, fn)
            }, 2.hours)

          lazy val optCard: Option[TruckCard] = for {
            mark <- markModel.mark
            model <- markModel.model
            card <- components.trucksCatalog.getCardByMarkModel(mark, model).find(_.isTrucks)
          } yield {
            card
          }

          if (checkUrl(site, row.url) && phones.nonEmpty && address.nonEmpty && price.nonEmpty && markModel.nonEmpty &&
            optCard.nonEmpty) {
            val name = optCard.get.name
            write(site +
              "|" + address.get.ruName +
              "|" + optCard.get.truckCategoryStr +
              "|" + name +
              "|" + year +
              "|" + price +
              "|" + phones.mkString(",") +
              "|" + row.url)
          }
        })
      })
    }
  }*/

  private def parsePrice(json: JsObject): String = {
    val price = (json \ "price").toOption.toSeq
      .flatMap(_.asInstanceOf[JsArray].value)
      .headOption
      .map(_.asInstanceOf[JsString].value.filter(_.isDigit))
      .filter(_.nonEmpty)
      .getOrElse("")
    price
  }

  private def checkUrl(site: String, url: String): Boolean = {
    if (site == "drom") {
      val category = url.split("/")(4)
      category match {
        case "truck" | "bus" | "trailer" => true
        case _ => false
      }
    } else true
  }

  /*private def parseAddress(json: JsObject, site: String, url: String)
                          (implicit trace: Traced): Option[Region] = {
    val address = (json \ "address").toOption.toSeq
      .flatMap(_.asInstanceOf[JsArray].value)
      .headOption
      .map(_.asInstanceOf[JsString].value)
      .filter(_.nonEmpty)
      .getOrElse("")
    if (address.nonEmpty) {
      Await.result(components.geocoder.getRegion(address).recover {
        case NonFatal(error) => None
      }, 2.hours)
    } else {
      val enName = url.drop(url.indexOf(site)).drop(site.length + 4).takeWhile(_ != '/').toUpperCase()
      Await.result(components.geocoder.getRegion(enName).recover {
        case NonFatal(error) => None
      }, 2.hours)
    }
  }*/

  private def parseYear(json: JsObject): String = {
    val year = (json \ "year").toOption.toSeq
      .flatMap(_.asInstanceOf[JsArray].value)
      .headOption
      .map(_.asInstanceOf[JsString].value.filter(_.isDigit))
      .filter(_.nonEmpty)
      .getOrElse("")
    year
  }

  private def parsePhones(json: JsObject): Seq[String] = {
    val phoneArray: Seq[JsValue] = (json \ "phone").toOption.toSeq.flatMap(_.asInstanceOf[JsArray].value)
    val phones = phoneArray.flatMap(s => {
      s.asInstanceOf[JsString]
        .value
        .replace("\"", "")
        .split(",")
        .map(normalizePhone)
        .filter(_.nonEmpty)
    })
    phones
  }

  private def phones(in: Iterator[ImportRow]): Unit = {
    val phones: Seq[(String, String, String)] = in
      .flatMap(row => {
        val json = row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject]
        val phoneArray: Seq[JsValue] = (json \ "phone").toOption.toSeq.flatMap(_.asInstanceOf[JsArray].value)
        phoneArray.flatMap(s => {
          val phones = s
            .asInstanceOf[JsString]
            .value
            .replace("\"", "")
            .split(",")
            .map(normalizePhone)
            .filter(_.nonEmpty)
          phones.map(phone => {
            (phone, row.rawUrl, phoneArray.toString())
          })
        })
      })
      .toSeq

    phones.groupBy(l => op(l._1)).mapValues(_.length).toSeq.sortBy(_._2).foreach(println)
    println("------------------")
    phones.groupBy(l => l._1.length).mapValues(_.length).toSeq.sortBy(_._2).foreach(println)
    println("------------------")

    FileUtils.save("phones.txt") { write =>
      phones.sortBy(_._1).sortBy(_._1.length).foreach {
        case (phone, url, phoneArray) =>
          write(s"$phone : $url : $phoneArray")
      }
    }
  }

  private def op(code: String): String = {
    val c = Try(code.slice(1, 4).toInt).toOption.getOrElse(sys.error(code))
    if (code.length == 11) {
      if ((c >= 902 && c <= 906) || (c >= 960 && c <= 969)) "beeline"
      else if (c >= 920 && c <= 939) "megafon"
      else if ((c >= 910 && c <= 919) || (c >= 980 && c <= 989)) "mts"
      else if (c.toString.startsWith("4")) "city"
      else if (c == 800) "free"
      else "N/A"
    } else "N/A"
  }

  private def phonesAmount(in: Iterator[ImportRow]): Unit = {
    val phones: Seq[(Int, String, String)] = in
      .flatMap(row => {
        val json = row.rawJson.asInstanceOf[JsArray].value.head.asInstanceOf[JsObject]
        val phoneArray: Seq[JsValue] = (json \ "phone").toOption.toSeq.flatMap(_.asInstanceOf[JsArray].value)
        phoneArray.map(s => {
          val phones = s
            .asInstanceOf[JsString]
            .value
            .replace("\"", "")
            .split(",")
            .map(normalizePhone)
            .filter(_.nonEmpty)
          (phones.length, row.rawUrl, row.rawJson.toString())
        })
      })
      .toSeq

    phones.groupBy(x => x._1).mapValues(_.length).toSeq.sortBy(_._2).foreach(println)
    println("------------------")
    phones.filter(_._1 == 0).map(x => (x._2, x._3)).foreach(println)
  }

  private val unmatchNumbersRegexp = """[^0-9]""".r

  private def normalizePhone(phone: String): String = {
    val n = unmatchNumbersRegexp.replaceAllIn(phone, "")
    if (n.length == 10) s"7$n"
    else if (n.length == 11 && n.head == '8') s"7${n.tail}"
    else n
  }

  private def importRowStream(in: InputStream): Iterator[ImportRow] = {
    scala.io.Source
      .fromInputStream(in)
      .getLines()
      .flatMap(l => {
        val s = l.split("\t").map(_.trim).filterNot(_.isEmpty)
        if (s.length != 2) {
          log.warn(s"unexpected row: $l")
          None
        } else {
          try {
            val url = s.head
            val json = Json.parse(s(1))
            Some(ImportRow(url, json))
          } catch {
            case NonFatal(error) =>
              log.error(s"failed to handle ${s.head}", error)
              throw error
              None
          }
        }
      })
  }
}
