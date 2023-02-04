package ru.yandex.realty.api

import java.io.ByteArrayOutputStream

import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.message.IndexerApi.OfferForReindexMessage

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 15.12.16
  */
class CommonAPITest extends FlatSpec with Matchers {
  "CommonAPI" should "correct work" in {
    val bos = new ByteArrayOutputStream
    for (offerId <- Seq(7500041077288651569L)) {
      val builder = OfferForReindexMessage.newBuilder()
      builder.setOfferId(s"$offerId")
      val msg = builder.build()
      msg.writeDelimitedTo(bos)
    }
    val data = bos.toByteArray

    val httpPut = new HttpPut("http://localhost:36600/api/v1/reindex")
    httpPut.setEntity(new ByteArrayEntity(data))

    val client = HttpClients.createDefault()
    val response = client.execute(httpPut)
    println(s"response: $response")
    response.close()
    client.close()
  }
}
