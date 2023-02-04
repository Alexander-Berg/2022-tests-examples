package ru.yandex.vertis.moderation.picapica

import java.io.{File, FileOutputStream}

import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.runner.RunWith
import org.junit.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.picapica.impl.HttpPicaClient
import ru.yandex.vertis.picapica.client.PicaPicaClient.Options
import ru.yandex.vertis.picapica.client._
import ru.yandex.vertis.picapica.model.Task.DownloadImage
import ru.yandex.vertis.picapica.model.UploadInfo.OkUpload
import ru.yandex.vertis.picapica.model.{Metadata, Task, TaskResult}

/**
  * Specs for [[ru.yandex.vertis.moderation.picapica.impl.HttpPicaClient]] for autoru
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore("For manually running")
class AutoruPicaPicaClientSpec extends SpecBase {

  "PicaPicaClientImpl" should {

    import scala.concurrent.ExecutionContext.Implicits.global

    val options =
      Options(
// ssh sashagrey.vertis.yandex.net -L 35020:shard-01-myt.prod.vertis.yandex.net:35020
//      "http://localhost:35020",
        "http://realty3-pica-01-sas.test.vertis.yandex.net:35020",
        "autoru-all",
        PicaPicaClient.Mode.Async,
        Compression.GZipCompression
      )
    val client = new HttpPicaClient(options, new DefaultAsyncHttpClient())

    "get metadata and save it" in {

      val tasks =
        Iterable(
          Task(
            "a_47665632",
            Iterable(
              DownloadImage(
                "c2251be05d23e5a23c4997e77e779ac3",
                "http://avatars-int.mdst.yandex.net/get-autoru-orig/2079031/c2251be05d23e5a23c4997e77e779ac3/1200x900"
              )
            )
          )
        )
      client
        .send(
          tasks,
          partitioningId = "a_47665632",
          version = Some(5),
          existing = None,
          namespace = Some("autoru-orig"),
          priority = None
        )
        .futureValue match {
        case TaskResult(map) =>
          map should not be empty
          map.values.head.foreach {
            case (_, OkUpload(_, _, Metadata.V3(meta))) =>
              val os = new FileOutputStream(new File("1061304058-a17264_1.bin"))
              meta.writeTo(os)
              os.close()
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
