package ru.yandex.vertis.picapica.misc.marshalling

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.picapica.actor.AsyncRequestActor.Result.ImagesResult
import ru.yandex.vertis.picapica.client.ApiVersion
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.{MetaEntry, Metadata, Response}
import ru.yandex.vertis.picapica.model.AvatarsResponse.{AvatarsData, Timeout}
import ru.yandex.vertis.picapica.model.{Id, StoredAvatarsData}
import ru.yandex.vertis.picapica.misc.marshalling.DomainMarshallers._
import spray.httpx.marshalling.CollectingMarshallingContext

/**
  * @author evans
  */
//scalastyle:off
@RunWith(classOf[JUnitRunner])
class DomainMarshallersSpec
    extends WordSpec
        with Matchers {
  "Domain marshallers" should {
    val meta = Metadata.newBuilder().setVersion(5).setIsFinished(true).build()
    val data = ImagesResult(Map(
      Id("123", "hash") -> StoredAvatarsData(AvatarsData(2, "fffuuu", None), Map("key"->"value"), Some(meta)),
      Id("124", "hash1") -> StoredAvatarsData(Timeout, Map("key2"->"value2"), None)))

    "marshall v1" in {
      val ctx = new CollectingMarshallingContext
      documentMarshaller(ApiVersion.V1)(data.images, ctx)
      val response = Response.parseFrom(ctx.entity.get.data.toByteArray)

      val expectedResponse = Response.newBuilder()
          .setVersion(1).addElement(
        Response.Element.newBuilder().setKey("124")
      ).addElement(
        Response.Element.newBuilder()
            .setKey("123")
            .addValue(
              Response.Value.newBuilder().setKey("hash").setAvatarsGroup("2").setAvatarsName("fffuuu")
            )
      ).build()
      response shouldEqual expectedResponse
    }
    "marshall v2" in {
      val ctx = new CollectingMarshallingContext
      documentMarshaller(ApiVersion.V2)(data.images, ctx)
      val response = Response.parseFrom(ctx.entity.get.data.toByteArray)

      val expectedResponse = Response.newBuilder()
          .setVersion(2).addElement(
        Response.Element.newBuilder()
            .setKey("124")
            .addValue(
              Response.Value.newBuilder()
                  .setKey("hash1")
            )
      ).addElement(
        Response.Element.newBuilder()
            .setKey("123")
            .addValue(
              Response.Value.newBuilder()
                  .setKey("hash")
                  .setAvatarsGroup("2")
                  .setAvatarsName("fffuuu")
                  .addMeta(MetaEntry.newBuilder().setKey("key").setValue("value"))
            )
      ).build()
      response shouldEqual expectedResponse
    }

    "marshall v3" in {
      val ctx = new CollectingMarshallingContext
      documentMarshaller(ApiVersion.V3)(data.images, ctx)
      val response = Response.parseFrom(ctx.entity.get.data.toByteArray)

      val expectedResponse = Response.newBuilder()
        .setVersion(3).addElement(
        Response.Element.newBuilder()
          .setKey("124")
          .addValue(
            Response.Value.newBuilder()
              .setKey("hash1").setErrorMessage("TimedOut")
          )
      ).addElement(
        Response.Element.newBuilder()
          .setKey("123")
          .addValue(
            Response.Value.newBuilder()
              .setKey("hash")
              .setAvatarsGroup("2")
              .setAvatarsName("fffuuu")
              .setMetadata(Metadata.newBuilder().setVersion(5).setIsFinished(true))
          )
      ).build()
      response shouldEqual expectedResponse
    }
  }
}

//scalastyle:on
