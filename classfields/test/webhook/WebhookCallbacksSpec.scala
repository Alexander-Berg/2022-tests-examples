package amogus.logic.webhook

import amogus.logic.WebhookMessageProducerMock
import amogus.logic.producer.{WebhookMessageProducer, WebhookMessageProducerError}
import amogus.logic.webhook.WebhookCallbacksErrors.CallbackError.{DecodeMessageError, ProduceMessageError}
import amogus.model.AmogusConfig.AmogusServiceConfig
import amogus.model.ValueTypes.{ServiceId, ServiceName}
import amogus.model.amo.CustomFieldTextValue
import amogus.model.company.CompanyChangedModel.{Account, Contact, CustomField, StringField, Update}
import amogus.model.company.CompanyChangedModel
import ru.yandex.vertis.amogus.company_event.CompanyEvent
import common.zio.kafka.testkit.TestKafka
import common.zio.logging.Logging
import common.zio.sttp.endpoint.Endpoint
import common.zio.testkit.failsWith
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.{Serde, Serializer}
import zio.magic._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test._
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

object WebhookCallbacksSpec extends DefaultRunnableSpec {
  private val uuid = UUID.fromString("1f5c13dc-a793-47cd-b616-8def01003244")
  private val topic = "topic"

  private val serviceConfig = AmogusServiceConfig(
    serviceId = ServiceId(uuid),
    serviceName = ServiceName("some-service"),
    host = Endpoint(host = "autorutesting.amocrm.ru", port = 443, schema = "https"),
    topic = topic,
    webhooks = Set.empty,
    credentials = Seq.empty,
    robotManagerEmail = None
  )

  private val defaultTimeout = 30.seconds

  private val kafkaProducer = (for {
    producerSettings <- TestKafka.bootstrapServers.map { servers =>
      ProducerSettings(servers)
        .withCloseTimeout(defaultTimeout)
        .withProperties(Map.empty[String, AnyRef])
    }.toManaged_

    producer <- Producer.make(producerSettings).orDie
  } yield producer).toLayer

  private val kafkaConsumer = (for {
    consumerSetting <- TestKafka.bootstrapServers.map { servers =>
      ConsumerSettings(servers)
        .withCloseTimeout(defaultTimeout)
        .withGroupId("test")
        .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
    }.toManaged_

    consumer <- Consumer.make(consumerSetting).orDie
  } yield consumer).toLayer

  private val kafkaEnv = ZLayer.fromSomeMagic[TestEnvironment, Has[WebhookCallbacks] with Has[Consumer] with Clock](
    kafkaProducer,
    Logging.live,
    WebhookMessageProducer.live,
    kafkaConsumer,
    TestKafka.live,
    WebhookCallbacksLive.layer
  )

  private val rawFormData = Map(
    "contacts[update][0][id]" -> "7793457",
    "contacts[update][0][name]" -> "????????????????????-????????",
    "contacts[update][0][responsible_user_id]" -> "6101560",
    "contacts[update][0][date_create]" -> "1608138130",
    "contacts[update][0][last_modified]" -> "1642684812",
    "contacts[update][0][created_user_id]" -> "6507739",
    "contacts[update][0][modified_user_id]" -> "6507739",
    "contacts[update][0][custom_fields][0][id]" -> "293097",
    "contacts[update][0][custom_fields][0][name]" -> "?????? ????????????????",
    "contacts[update][0][custom_fields][0][values][0][value]" -> "????",
    "contacts[update][0][custom_fields][0][values][0][enum]" -> "395207",
    "contacts[update][0][custom_fields][1][id]" -> "291939",
    "contacts[update][0][custom_fields][1][name]" -> "????????????",
    "contacts[update][0][custom_fields][1][values][0][value]" -> "??????????????????????????",
    "contacts[update][0][custom_fields][1][values][0][enum]" -> "394953",
    "contacts[update][0][custom_fields][2][id]" -> "295329",
    "contacts[update][0][custom_fields][2][name]" -> "????????????",
    "contacts[update][0][custom_fields][2][values][0][value]" -> "??????????-?????????????????? ?? ?????????????????????????? ??????????????",
    "contacts[update][0][custom_fields][2][values][0][enum]" -> "395865",
    "contacts[update][0][custom_fields][3][id]" -> "293209",
    "contacts[update][0][custom_fields][3][name]" -> "??????????2",
    "contacts[update][0][custom_fields][3][values][0][value]" -> "??????????-??????????????????",
    "contacts[update][0][custom_fields][4][id]" -> "293119",
    "contacts[update][0][custom_fields][4][name]" -> "ID",
    "contacts[update][0][custom_fields][4][values][0][value]" -> "client-23144",
    "contacts[update][0][custom_fields][5][id]" -> "293121",
    "contacts[update][0][custom_fields][5][name]" -> "Origin",
    "contacts[update][0][custom_fields][5][values][0][value]" -> "spb0951",
    "contacts[update][0][custom_fields][6][id]" -> "293141",
    "contacts[update][0][custom_fields][6][name]" -> "??????????????????",
    "contacts[update][0][custom_fields][6][values][0][value]" -> "??????",
    "contacts[update][0][custom_fields][7][id]" -> "293207",
    "contacts[update][0][custom_fields][7][name]" -> "???????? ??????????????????????",
    "contacts[update][0][custom_fields][7][values][0]" -> "1471813200",
    "contacts[update][0][custom_fields][8][id]" -> "293147",
    "contacts[update][0][custom_fields][8][name]" -> "?????????????????? ??????????????????????",
    "contacts[update][0][custom_fields][8][values][0][value]" -> "??????",
    "contacts[update][0][custom_fields][9][id]" -> "293143",
    "contacts[update][0][custom_fields][9][name]" -> "ID ??????????????????",
    "contacts[update][0][custom_fields][9][values][0][value]" -> "agency-35250",
    "contacts[update][0][custom_fields][10][id]" -> "293145",
    "contacts[update][0][custom_fields][10][name]" -> "ID ???????????????? (??????)",
    "contacts[update][0][custom_fields][10][values][0][value]" -> "7252519",
    "contacts[update][0][custom_fields][11][id]" -> "301669",
    "contacts[update][0][custom_fields][11][name]" -> "Timestamp",
    "contacts[update][0][custom_fields][11][values][0]" -> "1642684740",
    "contacts[update][0][custom_fields][12][id]" -> "295335",
    "contacts[update][0][custom_fields][12][name]" -> "??????????",
    "contacts[update][0][custom_fields][12][values][0][value]" -> "???????????????????? ?????????? 157??",
    "contacts[update][0][custom_fields][13][id]" -> "853635",
    "contacts[update][0][custom_fields][13][name]" -> "??????-???? ???????????????? ????????????????????",
    "contacts[update][0][custom_fields][13][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][14][id]" -> "857423",
    "contacts[update][0][custom_fields][14][name]" -> "???????????????? ????",
    "contacts[update][0][custom_fields][14][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][15][id]" -> "857425",
    "contacts[update][0][custom_fields][15][name]" -> "???????????????? ??????????",
    "contacts[update][0][custom_fields][15][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][16][id]" -> "857427",
    "contacts[update][0][custom_fields][16][name]" -> "???????????????????????? ??????????????????",
    "contacts[update][0][custom_fields][16][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][17][id]" -> "857429",
    "contacts[update][0][custom_fields][17][name]" -> "????????",
    "contacts[update][0][custom_fields][17][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][18][id]" -> "853637",
    "contacts[update][0][custom_fields][18][name]" -> "??????-???? VAS",
    "contacts[update][0][custom_fields][18][values][0][value]" -> "0",
    "contacts[update][0][custom_fields][19][id]" -> "293073",
    "contacts[update][0][custom_fields][19][name]" -> "?????????????????? ??????????????????",
    "contacts[update][0][custom_fields][19][values][0][value]" -> "????",
    "contacts[update][0][custom_fields][19][values][0][enum]" -> "395199",
    "contacts[update][0][custom_fields][20][id]" -> "293075",
    "contacts[update][0][custom_fields][20][name]" -> "???????????? ???? ??????????????????",
    "contacts[update][0][custom_fields][20][values][0][value]" -> "??????",
    "contacts[update][0][custom_fields][20][values][0][enum]" -> "395205",
    "contacts[update][0][created_at]" -> "1608138130",
    "contacts[update][0][updated_at]" -> "1642684812",
    "contacts[update][0][type]" -> "company",
    "account[subdomain]" -> "autorutesting",
    "account[id]" -> "29138932",
    "account[_links][self]" -> "https://autorutesting.amocrm.ru"
  )

  private val expectedMessage = CompanyChangedModel(
    Contact(
      update = List(
        Update(
          id = 7793457,
          responsibleUserId = "6101560",
          lastModified = "1642684812",
          modifiedUserId = Some("6507739"),
          createdAt = "1608138130",
          updatedAt = "1642684812",
          name = "????????????????????-????????",
          oldResponsibleUserId = None,
          dateCreate = "1608138130",
          customFields = List(
            CustomField(293097, "?????? ????????????????", None, List(CustomFieldTextValue("????"))),
            CustomField(293121, "Origin", None, List(CustomFieldTextValue("spb0951"))),
            CustomField(293145, "ID ???????????????? (??????)", None, List(CustomFieldTextValue("7252519"))),
            CustomField(857423, "???????????????? ????", None, List(CustomFieldTextValue("0"))),
            CustomField(293075, "???????????? ???? ??????????????????", None, List(CustomFieldTextValue("??????"))),
            CustomField(291939, "????????????", None, List(CustomFieldTextValue("??????????????????????????"))),
            CustomField(293141, "??????????????????", None, List(CustomFieldTextValue("??????"))),
            CustomField(293143, "ID ??????????????????", None, List(CustomFieldTextValue("agency-35250"))),
            CustomField(853635, "??????-???? ???????????????? ????????????????????", None, List(CustomFieldTextValue("0"))),
            CustomField(
              295329,
              "????????????",
              None,
              List(CustomFieldTextValue("??????????-?????????????????? ?? ?????????????????????????? ??????????????"))
            ),
            CustomField(857429, "????????", None, List(CustomFieldTextValue("0"))),
            CustomField(295335, "??????????", None, List(CustomFieldTextValue("???????????????????? ?????????? 157??"))),
            StringField(293207, "???????? ??????????????????????", None, List("1471813200")),
            CustomField(293209, "??????????2", None, List(CustomFieldTextValue("??????????-??????????????????"))),
            CustomField(853637, "??????-???? VAS", None, List(CustomFieldTextValue("0"))),
            CustomField(857427, "???????????????????????? ??????????????????", None, List(CustomFieldTextValue("0"))),
            StringField(301669, "Timestamp", None, List("1642684740")),
            CustomField(293147, "?????????????????? ??????????????????????", None, List(CustomFieldTextValue("??????"))),
            CustomField(293073, "?????????????????? ??????????????????", None, List(CustomFieldTextValue("????"))),
            CustomField(293119, "ID", None, List(CustomFieldTextValue("client-23144"))),
            CustomField(857425, "???????????????? ??????????", None, List(CustomFieldTextValue("0")))
          )
        )
      )
    ),
    Account("29138932", "autorutesting")
  )

  override def spec: ZSpec[TestEnvironment, Any] = suite("webhook callbacks")(
    suite("with container kafka")(testM("handle callback") {
      val protoMessage = expectedMessage.toProtoMessages match {
        case Seq(msg) => msg
        case other =>
          throw new IllegalArgumentException(s"Expected single proto message in tests, got ${other.length}")
      }

      for {
        handleResult <- WebhookCallbacks(_.handleCallback(serviceConfig, rawFormData))
        consumingResult <- Consumer
          .subscribeAnd(Subscription.topics(topic))
          .plainStream(Serde.string, Serde.byteArray)
          .take(1)
          .runCollect
        firstMessage = consumingResult.head
      } yield {
        assert(handleResult)(isUnit) &&
        assert(firstMessage.key)(equalTo(protoMessage.id)) &&
        assert(CompanyEvent.parseFrom(firstMessage.value))(equalTo(protoMessage.data))
      }
    })
      .provideCustomLayerShared(kafkaEnv)
      .provideCustomLayerShared(Clock.live),
    testM("handles error from send") {
      val messageProducer = WebhookMessageProducerMock.Send(
        anything,
        failure(WebhookMessageProducerError.SendToKafkaError("some topic", None))
      )
      assertM(WebhookCallbacks(_.handleCallback(serviceConfig, rawFormData)).run)(failsWith[ProduceMessageError])
        .provideLayer(messageProducer >>> WebhookCallbacksLive.layer)
    },
    testM("handles error from parsing") {
      val messageProducer = WebhookMessageProducerMock.empty
      assertM(WebhookCallbacks(_.handleCallback(serviceConfig, Map())).run)(failsWith[DecodeMessageError])
        .provideLayer(messageProducer >>> WebhookCallbacksLive.layer)
    }
  )
}
