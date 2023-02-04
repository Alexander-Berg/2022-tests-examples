package ru.yandex.auto.searcher.http.action

import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.api.{ApiOfferModel, ResponseModel}
import ru.yandex.auto.searcher.http.action.ApiCarsComparisonScript.{
  base,
  dev,
  doRequest,
  getClass,
  getRequests,
  getResourcesFromDirectory,
  headRestrict
}
import sttp.client._
import sttp.model.Uri.QuerySegment

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import scala.io.Source

/**
  *  https://st.yandex-team.ru/VS-602
  *
  * 1. Поднять базовый и dev серчер на фиксированных индексах.
  * fe: bean carsAsyncS3IndexConsumer.prefix  = "autoru-index-bp"
  * 2. Некоторый расчет fresh_date привязан к загрузке индексов. Базовую версию время от времени нужно рестартить.
  */
object ApiCarsComparisonScript {
  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  /** Серчер с новой функциональностью. */
  val dev = "http://b-petr-01-dev.sas.yp-c.yandex.net:34389"

  /** Серчер в тестинге. */
  val test = "http://auto2-searcher-api.vrts-slb.test.vertis.yandex.net"

  /** Базовый серчер. Основа для сравнения. */
  val base = "http://auto2-searcher-VS-1377-base-branch-api.vrts-slb.test.vertis.yandex.net"
  val base2 = "http://auto2-searcher-VS-1377-base-2-api.vrts-slb.test.vertis.yandex.net"

  val headRestrict = 50

  def main(args: Array[String]): Unit = {
    val aims = List(
      (base, "_base"),
      (dev, "_dev")
    )

//    val offerToString: ApiOfferModel.Offer => String = (o: ApiOfferModel.Offer) => o.getId
    val offerToString: ApiOfferModel.Offer => String = { o: ApiOfferModel.Offer =>
      val metas = o.getRelevance.getCatboostMetaList.asScala
      s"${o.getId} ${metas.lastOption.getOrElse("")}"
    }

    val url = getClass.getResource("/ammo/sorting/")
    val ammoList = getResourcesFromDirectory(
      Paths.get(url.toURI).toFile,
      Pattern.compile("[^_].*\\.txt")
    )
//      .filter(_.getName.contains("without"))

    for (ammo <- ammoList) {
      println(s"process $ammo")
      val reqs = getRequests(ammo, Some(headRestrict)).toList
      aims.foreach { case (_, prefix) => rmOldOutput(prefix, ammo) }

      for {
        req <- reqs
        (host, prefix) <- aims
      } {
        val output = new File(s"${ammo.getParent}/$prefix/_${ammo.getName}").toPath
        val ans = doRequest(host, req)
        log(output, req, ans, offerToString)
      }
    }
  }

  def rmOldOutput(prefix: String, ammo: File): Unit = {
    new File(s"${ammo.getParent}/$prefix").mkdirs()
    val output = new File(s"${ammo.getParent}/$prefix/_${ammo.getName}")
    output.delete()
  }

  def getRequests(ammo: File, headRestrict: Option[Int] = None): Iterator[String] = {
    val strings = Source
      .fromInputStream(new FileInputStream(ammo))
      .getLines()

    headRestrict.map(h => strings.take(h)).getOrElse(strings)
  }

  def log(
      output: Path,
      req: String,
      ans: Either[String, OfferListingResponse],
      offerToString: ApiOfferModel.Offer => String
  ): Unit = {
    val b = ans.fold(err => err, res => res.getOffersList.asScala.map(offerToString).mkString("\n"))
    Files.write(
      output,
      s"""$req
         |$b
         |
         |""".stripMargin.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
  }

  def doRequest(host: String, req: String): Either[String, OfferListingResponse] = {
    var url = s"$host$req"
    url = url + (if (url.endsWith("?")) "" else "&") + "shuffle_seed=42&log_sorting=true"

    val kv = url
      .dropWhile(_ != '?')
      .drop(1)
      .split("&")
      .map(_.split("=", 2))
      .map { case Array(k, v) => QuerySegment.KeyValue(k, v) }
      .toList

    val fixedUri = uri"$url".copy(querySegments = kv)

    val request = basicRequest
      .get(fixedUri)
      .header("Content-Type", "application/protobuf")
      .header("Accept", "application/protobuf")
      .response(asByteArray)
    val bArr = request
      .send()
      .body

    bArr.right
      .map(ResponseModel.OfferListingResponse.parseFrom)
  }

  def getResourcesFromDirectory(directory: File, pattern: Pattern): List[File] = {
    for {
      file <- directory.listFiles.toList if !file.isDirectory
      fileName = file.getCanonicalPath if pattern.matcher(fileName).matches
    } yield file
  }
}

case class Instance(host: String, prefix: String)

/**
  * Сравнивает офферы по id.
  */
object CompareById {
  import ApiCarsComparisonScript._

  def main(args: Array[String]): Unit = {
    val baseI = Instance(base, "_base")
    val devI = Instance(dev, "_dev")

    val url = getClass.getResource("/ammo/sorting/")
    val ammoList = getResourcesFromDirectory(
      Paths.get(url.toURI).toFile,
      Pattern.compile("[^_].*\\.txt")
    )
//      .filter(_.getName.contains("price"))

    val ex = (olr: OfferListingResponse) => olr.getOffersList.asScala.filterNot(_.getIsNotOnSale)

    var wrongAmmoSet = Set[String]()

    for {
      ammo <- ammoList
      req <- getRequests(ammo, Some(headRestrict)).toList
    } {
      val baseAns = doRequest(baseI.host, req).fold(_ => None, Some(_))
      val devAns = doRequest(devI.host, req).fold(_ => None, Some(_))

      baseAns
        .zip(devAns)
        .map { case (base, dev) => (ex(base), ex(dev)) }
        .foreach {
          case (baseOffers, devOffers) =>
            if (baseOffers.size != devOffers.size) {
              println(s"$ammo")
//              println(s"size not eq of req: $req")
              wrongAmmoSet += ammo.getName
            } else {
              baseOffers.zip(devOffers).foreach {
                case (b, d) =>
                  if (b.getId != d.getId) {
//                    println(s" ${b.getId} != ${d.getId} for req: $req")
                    wrongAmmoSet += ammo.getName
                  }
              }
            }
        }
    }
    println(wrongAmmoSet)
    println("done")
  }
}
