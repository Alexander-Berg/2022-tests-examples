package ru.yandex.realty.rent.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.telegram.model.{ParseMode, SendMessage}
import ru.yandex.realty.clients.telegram.{HttpTelegramClient, TelegramBotConfig, TelegramClient}
import ru.yandex.realty.rent.proto.event.NetPromoterScoreEvent
import ru.yandex.realty.rent.service.impl.NetPromoterScoreProcessorImp
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.application.ng.features.FeaturesProvider
import ru.yandex.realty.features.{Features, FeaturesStubComponent}

import scala.collection.JavaConverters
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class NetPromotionScoreProcessorSpec extends AsyncSpecBase {
  implicit val trace = Traced.empty

  val token = "bot5555:44444"
  val chatId = "-333222111"

  "NetPromoterScoreProcessor.process" should {
    "generate msg to sent for TelegramClient" in new Wiring {

      val telegramBotConfig = new TelegramBotConfig(chatId, token)
      val telegramClient: TelegramClient = mock[TelegramClient]
      (telegramClient
        .sendMessage(_: String, _: SendMessage)(_: Traced))
        .expects(
          token,
          new SendMessage(
            chatId,
            s"*Роль:* Собственник,Жилец\n*Оценка:* 10\n*Комментарий:* nice\n*Ссылка на пользователя:* https://arenda.test.vertis.yandex.ru/management/manager/user/12345/",
            Some(ParseMode.Markdown)
          ),
          *
        )
        .returning(Future.successful(null))

      val processor = new NetPromoterScoreProcessorImp(telegramClient, telegramBotConfig, features)

      val roles = JavaConverters.setAsJavaSet(Set("owner", "tenant"))
      val netPromoterScoreEvent: NetPromoterScoreEvent =
        NetPromoterScoreEvent
          .newBuilder() setScore (10) addAllRoles (roles) setUid (123456789) setComment ("nice") setUserId ("12345") build ()
      processor.process(netPromoterScoreEvent).futureValue
    }
  }

  "NetPromoterScoreProcessor.process" should {
    "sent msg to sent for TelegramClient without user Comment" in new Wiring {

      val telegramBotConfig = new TelegramBotConfig(chatId, token)
      val telegramClient: TelegramClient = mock[TelegramClient]
      val roles = JavaConverters.setAsJavaSet(Set("owner"))
      (telegramClient
        .sendMessage(_: String, _: SendMessage)(_: Traced))
        .expects(
          token,
          new SendMessage(
            chatId,
            s"*Роль:* Собственник\n*Оценка:* 10\n*Ссылка на пользователя:* https://arenda.test.vertis.yandex.ru/management/manager/user/12345/",
            Some(ParseMode.Markdown)
          ),
          *
        )
        .returning(Future.successful(null))

      val processor = new NetPromoterScoreProcessorImp(telegramClient, telegramBotConfig, features)

      val netPromoterScoreEvent: NetPromoterScoreEvent =
        NetPromoterScoreEvent
          .newBuilder() addAllRoles (roles) setScore (10) setUid (123456789) setUserId ("12345") build ()
      processor.process(netPromoterScoreEvent).futureValue
    }
  }

  trait Wiring extends FeaturesStubComponent {
    features.NpsTelegramChatSender.setNewState(true)
  }

}
