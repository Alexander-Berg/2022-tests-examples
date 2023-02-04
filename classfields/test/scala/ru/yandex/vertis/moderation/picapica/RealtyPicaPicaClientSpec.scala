package ru.yandex.vertis.moderation.picapica

import java.io.{File, FileOutputStream}

import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.picapica.impl.HttpPicaClient
import ru.yandex.vertis.picapica.client.PicaPicaClient.Options
import ru.yandex.vertis.picapica.client._
import ru.yandex.vertis.picapica.model.Metadata.V3
import ru.yandex.vertis.picapica.model.Task.DownloadImage
import ru.yandex.vertis.picapica.model.UploadInfo.OkUpload
import ru.yandex.vertis.picapica.model.{Task, TaskResult}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs for [[HttpPicaClient]] for realty
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class RealtyPicaPicaClientSpec extends SpecBase {

  "PicaPicaClientImpl" should {

    val options =
      Options(
        // ssh sashagrey.vertis.yandex.net -L 35020:shard-01-myt.prod.vertis.yandex.net:35020
        //      "http://localhost:35020",
        "http://realty3-pica-01-sas.test.vertis.yandex.net:35020",
        "realty",
        PicaPicaClient.Mode.Async,
        Compression.GZipCompression
      )
    val client = new HttpPicaClient(options, new DefaultAsyncHttpClient())

    "provide partition hash for offer id" in {
      Math.floorMod("offer.6300461494541429459".hashCode, 1024) should be(704)
      Math.floorMod("offer.6300461494541429459".hashCode, Integer.MAX_VALUE) should be(929510079)
      Math.floorMod("6300461494541429459".hashCode, 1024) should be(338)
      Math.floorMod("6300461494541429459".hashCode, Integer.MAX_VALUE) should be(538925394)
      //      Math.floorMod("6300461494541429459".toInt, 1024) should be (723)

      Math.floorMod("i_4729787348356411905".hashCode, Integer.MAX_VALUE) should be(1467240316)
      Math.floorMod("i_6114158062166522625".hashCode, Integer.MAX_VALUE) should be(1723282952)
    }
    "privide image hash for indexer offer" in {
      def imageHash(imageUrl: String): Long = {
        Math.abs(imageUrl.foldLeft(0L) { case (h, c) => 31 * h + c })
      }

      imageHash("http://alexander.pro.bkn.ru/images/s_big/ad76ad95-0508-11e8-b300-448a5bd44c07.jpg?v=3").toString should
        be("1870183845150999684")
    }

    "get metadata for feed offer and save it" in {

      // http://realty-searcher-01-sas.test.vertis.yandex.net:36610/card.json?id=6300461494541429459
      // http://realty3-indexer-01-myt.test.vertis.yandex.net:36600/rawOffer/6300461494541429459
      // //avatars.mdst.yandex.net/get-realty/3019/offer.6300461494541429459.1870183845150999684
      // select * from picapica_realty.pica_pictures where p = 538925394 and g = 'offer.6300461494541429459' and id = '1870183845150999684';

      val tasks = Iterable(Task("offer.6300461494541429459", Iterable(DownloadImage("1870183845150999684", ""))))
      client
        .send(
          tasks,
          partitioningId = "6300461494541429459",
          version = Some(1), // default!!! (if realty use v2 protocol)
          existing = Some(true),
          priority = None,
          namespace = None
        )
        .futureValue match {
        case TaskResult(map) =>
          map should not be empty
          map.values.head.foreach {
            case (_, OkUpload(_, _, V3(meta))) =>
              val os = new FileOutputStream(new File("6300461494541429459.bin"))
              meta.writeTo(os)
              os.close()
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
    "get metadata for VOS offer and save it" in {

      // http://realty-searcher-01-sas.test.vertis.yandex.net:36610/card.json?id=4729787348356411905
      // http://realty3-indexer-01-myt.test.vertis.yandex.net:36600/rawOffer/4729787348356411905
      // //avatars.mdst.yandex.net/get-realty/3220/add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos/
      // select * from picapica_realty.pica_pictures where p = 1467240316 and g = 'i_4729787348356411905' and id = 'add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos';

      val tasks = Iterable(Task("i_6114158062166522625", Iterable(DownloadImage("add.1518462104179ae7f81dbfd", ""))))
      client
        .send(
          tasks,
          partitioningId = "i_6114158062166522625",
          version = Some(1), // default!!! (if realty use v2 protocol)
          existing = Some(true),
          priority = None,
          namespace = None
        )
        .futureValue match {
        case TaskResult(map) =>
          map should not be empty
          map.values.head.foreach {
            case (_, OkUpload(_, _, V3(meta))) =>
              val os = new FileOutputStream(new File("6114158062166522625.bin"))
              meta.writeTo(os)
              os.close()
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }

    "get realty metadata" in {
      def workLikeMetaStage(
          offerExternalId: String,
          offerVosId: String,
          isFromFeed: Boolean,
          externalUrl: Option[String],
          mdsInfo: Option[(String, String)]
      ) = {
        val partitionId = if (isFromFeed) offerExternalId else offerVosId
        val useOnlyUploadedPhotos = !isFromFeed
        val task =
          if (externalUrl.isDefined && !useOnlyUploadedPhotos) {
            val url = externalUrl.get
            val imageHash = Math.abs(url.foldLeft(0L) { case (h, c) => 31 * h + c })
            DownloadImage(imageHash.toString, url)
          } else {
            mdsInfo match {
              case Some((group, name)) =>
                DownloadImage(name, s"//avatars.mdst.yandex.net/get-realty/$group/$name/main")
              case _ => ???
            }
          }

        client.send(
          Seq(
            Task(
              id = if (isFromFeed) s"offer.$offerExternalId" else partitionId,
              urls = Seq(task),
              ttlDays = None
            )
          ),
          partitionId,
          version = Some(3),
          existing = if (useOnlyUploadedPhotos) Some(true) else None,
          priority = Some(0),
          None
        )
      }

      println(
        workLikeMetaStage(
          offerExternalId = "3789074792227018496",
          offerVosId = "i_3789074792227018496",
          isFromFeed = false,
          externalUrl = None,
          mdsInfo = Some(("3220", "add.1611834302485d1b2c2010c"))
        ).futureValue
      )
    }
  }
}
