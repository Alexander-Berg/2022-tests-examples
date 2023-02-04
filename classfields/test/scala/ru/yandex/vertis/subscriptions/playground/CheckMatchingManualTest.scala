package ru.yandex.vertis.subscriptions.playground

import com.google.protobuf.util.JsonFormat
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.Model.Document
import ru.yandex.vertis.subscriptions.Model.Subscription
import ru.yandex.vertis.subscriptions.core.matcher.SubscriptionByDocument
import ru.yandex.vertis.subscriptions.core.matcher.qbd.QbdProtoFormats

import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

@RunWith(classOf[JUnitRunner])
class CheckMatchingManualTest extends FlatSpec {

  "A Document" should "match on subs" in {
    // paste here your document (json)
    val jsonDocument =
      """
          |{
          | "id": "778553/rooms-3/series-41-test-9",
          | "term": [{
          |        "name": "site_id",
          |        "point": {
          |          "value": "778553"
          |        }
          |      }],
          | "rawContent": "",
          | "createTimestamp": 1646246316830,
          | "updateTimestamp": 1646246316830,
          | "qualifier": "site-mean-price-update"
          |}
          |""".stripMargin

    val builderOfferDocs = Document.newBuilder()
    JsonFormat.parser().ignoringUnknownFields.merge(jsonDocument, builderOfferDocs)

    // paste here your subs (json)
    val jsonSubscription =
      """
          |{
          |  "id": "9201a8826bc5854ef5d00371bbec8fe80de9c673",
          |  "service": "realty",
          |  "user": {
          |    "uid": "4074044058"
          |  },
          |  "request": {
          |    "text": "",
          |    "query": {
          |      "term": {
          |        "term": {
          |          "name": "site_id",
          |          "point": {
          |            "value": "778553"
          |          }
          |        }
          |      }
          |    },
          |    "source": {
          |      "httpQuery": "siteId=778553"
          |    },
          |    "lastUpdated": "1645786158759"
          |  },
          |  "delivery": {
          |    "email": {
          |      "address": "ivanp1994@yandex-team.ru",
          |      "period": {
          |        "length": "5",
          |        "timeUnit": "MINUTES"
          |      }
          |    }
          |  },
          |  "view": {
          |    "title": "Строительство в ЖК «Октябрьское поле» приостановлено",
          |    "body": "Москва, ул. Берзарина, метро «Октябрьское Поле»",
          |    "topLevelDomain": "ru",
          |    "language": "ru",
          |    "currency": "RUR",
          |    "frontendHttpQuery": "/newbuilding/778553"
          |  },
          |  "state": {
          |    "value": "ACTIVE",
          |    "timestamp": "1645786158732"
          |  },
          |  "internalSettings": {
          |    "sendEmailInTesting": true
          |  },
          |  "instanceId": "-1920971867",
          |  "qualifier": "site-mean-price-update"
          |}
          |
          |""".stripMargin

    val subBuilder = Subscription.newBuilder()
    JsonFormat.parser().ignoringUnknownFields.merge(jsonSubscription, subBuilder)

    val qbd = new SubscriptionByDocument(
      List(
        "all_region_codes",
        "state",
        "mark_model_code",
        "mark_code",
        "model_code",
        "vendor",
        "price_rur",
        "year",
        "transmission",
        "run",
        "body_type",
        "engine_type"
      )
    )

    qbd.add(subBuilder.build())

    println {
      "finalResult: \n" +
        qbd.find(builderOfferDocs.getTermList.asScala.map(QbdProtoFormats.TermProtoFormat.read))
    }

    assert(qbd.find(builderOfferDocs.getTermList.asScala.map(QbdProtoFormats.TermProtoFormat.read)).nonEmpty)
  }

}
