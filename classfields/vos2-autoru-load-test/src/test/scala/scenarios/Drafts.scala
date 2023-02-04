package scenarios

import java.net.URL
import java.util.zip.GZIPInputStream

import com.google.protobuf.util.JsonFormat
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import ru.auto.api.ApiOfferModel.Offer
import util.RandomBatchedSeparatedValuesFeeder

import scala.concurrent.duration._
import scala.io.Source

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-22
  */
class Drafts extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net")
    .contentTypeHeader("application/protobuf")
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")

  val url = "https://s3.mds.yandex.net/auto/vos/drafts_ammo.jsonl.gz"

  val ammo = new RandomBatchedSeparatedValuesFeeder[Any](
    new GZIPInputStream(new URL(url).openStream()),
    is =>
      Source
        .fromInputStream(is)
        .getLines()
        .map { line =>
          val builder = Offer.newBuilder()
          JsonFormat.parser().merge(line, builder)
          val form = builder.build
          Map(
            "draft" -> form,
            "userId" -> form.getUserRef
          )
        },
    bufferSize = 500
  )

  val privates: ScenarioBuilder = scenario("DraftPublish")
    .feed(ammo)
    .exec(
      http("create_draft")
        .post("/api/v1/draft/cars/${userId}")
        .body(ByteArrayBody(s => s("draft").as[Offer].toByteArray()))
        .check(jsonPath("$.offerId").saveAs("draftId"))
    )
    .pause(1.second)
    .repeat(5) {
      exec(
        http("update_draft")
          .put("/api/v1/draft/cars/${userId}/${draftId}")
          .body(ByteArrayBody(s => s("draft").as[Offer].toByteArray()))
      ).pause(1.second)
    }
    .exec(
      http("publish_draft")
        .post("/api/v1/draft/cars/${draftId}/publish/${userId}")
    )

  setUp(
    privates
      .inject(
        rampUsersPerSec(1).to(10).during(30.seconds),
        constantUsersPerSec(10).during(10.minutes)
      )
  ).protocols(httpProtocol)
}
