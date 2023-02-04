package ru.yandex.vertis.picapica.model

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Inside, Matchers, WordSpec}
import ru.yandex.vertis.picapica.client.ApiVersion
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema
import ru.yandex.vertis.picapica.model.UploadInfo.{FailedUpload, OkUpload}

/**
  * @author evans
  */
//scalastyle:off
@RunWith(classOf[JUnitRunner])
class TaskResultSpec extends WordSpec with Matchers with Inside {

  "Task result" should {
    "be parsed" in {
      val responseBase = PicaPicaSchema.Response.newBuilder()
        .addElement(
          PicaPicaSchema.Response.Element.newBuilder()
            .setKey("124")
            .addValue(
              PicaPicaSchema.Response.Value.newBuilder().setKey("hash1")
            )
        )
        .addElement(
          PicaPicaSchema.Response.Element.newBuilder()
            .setKey("123")
            .addValue(
              PicaPicaSchema.Response.Value.newBuilder().setKey("hash")
                .setAvatarsGroup("2")
                .addMeta(PicaPicaSchema.MetaEntry.newBuilder().setKey("foo").setValue("bar")) //V2 metadata
                .setMetadata(PicaPicaSchema.Metadata.newBuilder().setVersion(5).setIsFinished(true)) //V3 metadata
            )
        )
        .addElement(
          PicaPicaSchema.Response.Element.newBuilder()
            .setKey("122")
            .addValue(
              PicaPicaSchema.Response.Value.newBuilder().setKey("hash1").setErrorMessage("NotFound")
            )
        )

      val v2Response = responseBase
        .setVersion(ApiVersion.V2.getResponseVersion)
        .build

      val v3Response = responseBase
        .setVersion(ApiVersion.V3.getResponseVersion)
        .build

      inside(TaskResult.from(v3Response)(Versions.`3.x`).get) {
        case TaskResult(results) =>
          results should contain key ("122")
          results should contain key ("123")
          results should contain key ("124")
          results("122") shouldEqual Map("hash1" -> FailedUpload(Some("NotFound")))
          results("124") shouldEqual Map("hash1" -> FailedUpload(None))
          results("123") should contain key ("hash")
          inside(results("123")("hash")) {
            case OkUpload("2", _, Metadata.V3(value)) =>
              value.getVersion shouldEqual 5
              value.getIsFinished should be(true)
          }
      }

      inside(TaskResult.from(v2Response)(Versions.`2.x`).get) {
        case TaskResult(results) =>
          results should contain key ("122")
          results should contain key ("123")
          results should contain key ("124")
          results("122") shouldEqual Map("hash1" -> FailedUpload(Some("NotFound")))
          results("124") shouldEqual Map("hash1" -> FailedUpload(None))
          results("123") should contain key ("hash")
          inside(results("123")("hash")) {
            case OkUpload("2", _, Metadata.V2(value)) =>
              value should be(Map("foo" -> "bar"))
          }
      }
    }
  }
}

//scalastyle:on
