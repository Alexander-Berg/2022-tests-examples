package ru.yandex.vertis.telepony.tools

import java.io.PrintWriter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ru.yandex.vertis.telepony.geo.impl.{CachedGeocodeClient, GeocodeClientImpl}
import ru.yandex.vertis.telepony.geo.model.{GeocodePhoneInterval, PhoneInterval, PhoneIntervalLine, TskvGeocodePhoneInterval}
import ru.yandex.vertis.telepony.model.PhoneTypes
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder
import akka.stream.scaladsl

import scala.concurrent.{Await, Future}
import scala.io.{Codec, Source}
import scala.util.Failure

/**
  * Get phones from http://www.rossvyaz.ru/activity/num_resurs/registerNum/
  * process with geocode.map.yandex.net
  * and store as regions.csv.
  *
  * @author evans
  */
//ssh -L 8123:addrs-testing.search.yandex.net:80 vertical-stat-01-dev.sas.yp-c.yandex.net
object RegionsFilePrepareApp extends App {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  implicit val as = ActorSystem("test", ConfigHelper.load(Seq("application-test.conf")))
  implicit val am = ActorMaterializer()

  val sendReceive = PipelineBuilder.buildSendReceive(None, -1)(am)

  val geoClient =
    new GeocodeClientImpl(
      sendReceive = sendReceive,
      scheme = "http",
      host = "localhost",
      port = 8123,
      basePath = "/search/stable/yandsearch",
      headerHost = "addrs-testing.search.yandex.net"
    ) with CachedGeocodeClient

  //Actual files with codes you can find here:
  //http://www.rossvyaz.ru/activity/num_resurs/registerNum/
  val files = Seq(
    "data/ABC-3xx.csv",
    "data/ABC-4xx.csv",
    "data/ABC-8xx.csv",
    "data/DEF-9xx.csv"
  )

  val intervals =
    files
      .flatMap { filename =>
        Source.fromFile(filename)(Codec("utf-8")).getLines().drop(1)
      }
      .collect { case PhoneIntervalLine(pi) => pi }
      .filter(_.capacity > 50) //ignore small amounts

  val graph = scaladsl.Source
    .fromIterator(() => intervals.iterator)
    .mapAsync(100)(transform)
    .toMat(scaladsl.Sink.seq)(scaladsl.Keep.both)
  val (_, f) = graph.run()
  val geoIntervals = Await.result(f, 100.second)

  val compacted = compact(geoIntervals.flatten)(merge)

  val pw = new PrintWriter("telepony-dao/src/main/resources/regions.csv")
  try {
    compacted.map(TskvGeocodePhoneInterval.apply).foreach(pw.println)
  } finally {
    pw.close()
  }
  System.exit(0)

  def transform(interval: PhoneInterval): Future[Option[GeocodePhoneInterval]] = {
    import interval._
    val phoneType = if (interval.code >= 900) {
      PhoneTypes.Mobile
    } else {
      PhoneTypes.Local
    }
    geoClient
      .findGeoId(interval.region)
      .map(geoId => GeocodePhoneInterval(code, from, end, geoId, name, phoneType))
      .map(Some.apply)
      .andThen {
        case Failure(th) =>
          println(s"ERROR: on[$interval] ${th.getMessage}")
      }
      .recover[Option[GeocodePhoneInterval]] { case e => None }
  }

  //works only with sorted phone intervals
  def merge: PartialFunction[(GeocodePhoneInterval, GeocodePhoneInterval), GeocodePhoneInterval] = {
    case (a, b) if a.code == b.code && a.geoId == b.geoId =>
      require(a.phoneType == b.phoneType, s"Expected equal phone type $a and $b")
      GeocodePhoneInterval(a.code, a.from, b.end, a.geoId, a.name, a.phoneType)
  }

  def compact[A](phoneIntervals: Seq[A])(merge: PartialFunction[(A, A), A]): Seq[A] = {
    var current: Option[A] = None
    val compacted = Seq.newBuilder[A]

    phoneIntervals.foreach { pi =>
      current match {
        case None =>
          current = Some(pi)
        case Some(acc) if merge.isDefinedAt((acc, pi)) =>
          current = Some(merge((acc, pi)))
        case Some(acc) =>
          current = Some(pi)
          compacted += acc
      }
    }
    current.foreach(compacted.+=)
    compacted.result()
  }
}
