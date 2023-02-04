package ru.yandex.vertis.moderation.model

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.converters.Protobuf
import ru.yandex.vertis.moderation.converters.Protobuf._
import ru.yandex.vertis.moderation.extdatacore.Dsl
import ru.yandex.vertis.moderation.extdatacore.Extdata.Stocks.{Currency => ProtoCurrency}
import ru.yandex.vertis.moderation.model.context.{Context, ContextSource}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance._
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.currency.{Currency, ExchangeRate}

/**
  * Base spec for model classes conversions into protobuf
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class ProtobufConversionsSpec extends SpecBase {

  "Opinions" should {
    "correctly converts" in {
      check(forAll(OpinionsGen)(x => x == fromMessage(toMessage(x))))
    }
  }

  "DetailedReason" should {
    "correctly convert" in {
      check(forAll(DetailedReasonGen)(x => x == fromMessage(toMessage(x))))
    }
  }

  "InstanceSource" should {
    "correctly converts" in {
      check(forAll(InstanceSourceGen) { instanceSource =>
        val service = Essentials.getService(instanceSource.essentials)
        instanceSource == fromMessage(service, toMessage(instanceSource))
      })
    }
  }

  "InstanceIdImpl" should {
    "correctly converts" in {
      check(forAll(InstanceIdImplGen)(x => x == fromMessage(toMessage(x))))
    }
  }

  "ContextSource" should {
    "correctly converts" in {
      check(forAll(ContextSourceGen)(x => x == fromMessage(toMessage(x))))
    }
    "convert from empty message" in {
      fromMessage(Model.ContextSource.newBuilder.setVersion(1).build()) should be(ContextSource.Default)
    }
  }

  "Context" should {
    "correctly converts" in {
      check(forAll(ContextGen)(x => x == fromMessage(toMessage(x))))
    }
    "convert from empty message" in {
      fromMessage(Model.Context.newBuilder.setVersion(1).build()) should be(Context.Default)
    }
  }

  "OwnerJournalRecord" should {
    "successfully convert AUTORU owners" in {
      check(forAll(OwnerJournalRecordGen)(x => x == fromMessage(Service.AUTORU, toMessage(x))))
    }
    "successfully convert REALTY owners" in {
      check(forAll(OwnerJournalRecordGen)(x => x == fromMessage(Service.REALTY, toMessage(x))))
    }
  }

  "InstanceOpinion" should {
    "correctly converts" in {
      check(forAll(InstanceOpinionGen)(x => x == fromMessage(toMessage(x))))
    }
  }

  "Diff" should {
    "correctly converts" in {
      check(forAll(DiffGen)(x => x == fromMessage(toMessage(x))))
    }
  }

  "convert stocks correctly" in {
    val toRubProto =
      List(
        Dsl.newExchangeRate(ProtoCurrency.KZT, ProtoCurrency.RUR, 1.0),
        Dsl.newExchangeRate(ProtoCurrency.UAH, ProtoCurrency.RUR, 2.0),
        Dsl.newExchangeRate(ProtoCurrency.USD, ProtoCurrency.RUR, 30.0),
        Dsl.newExchangeRate(ProtoCurrency.EUR, ProtoCurrency.RUR, 40.0),
        Dsl.newExchangeRate(ProtoCurrency.BYN, ProtoCurrency.RUR, 5.0),
        Dsl.newExchangeRate(ProtoCurrency.BYR, ProtoCurrency.RUR, 3.0)
      )
    val protoStocks = Dsl.newStocks(toRubProto)

    val toRub =
      Set(
        ExchangeRate(Currency.Kzt, Currency.Rur, 1.0),
        ExchangeRate(Currency.Uah, Currency.Rur, 2.0),
        ExchangeRate(Currency.Usd, Currency.Rur, 30.0),
        ExchangeRate(Currency.Eur, Currency.Rur, 40.0),
        ExchangeRate(Currency.Byn, Currency.Rur, 5.0),
        ExchangeRate(Currency.Byr, Currency.Rur, 3.0)
      )
    val fromRub = toRub.map(_.inversed)
    val identities = ExchangeRate.Identities
    val expectedResult = toRub ++ fromRub ++ identities

    Protobuf.fromMessage(protoStocks) shouldBe expectedResult
  }

  "throw ProtobufConversionException when convert ModerationRequest with several requests from proto to model" in {
    val instanceSource = InstanceSourceGen.next
    val contextSource = ContextSourceGen.next
    val externalId = ExternalIdGen.next
    val protoRequest =
      Model.ModerationRequest.newBuilder
        .setVersion(1)
        .setPushInstance(
          Model.ModerationRequest.PushInstance.newBuilder
            .setVersion(1)
            .setInstanceSource(Protobuf.toMessage(instanceSource))
            .build()
        )
        .setChangeContext(
          Model.ModerationRequest.ChangeContext.newBuilder
            .setVersion(1)
            .setContext(Protobuf.toMessage(contextSource))
            .setExternalId(Protobuf.toMessage(externalId))
            .build()
        )
        .build()
    intercept[ProtobufConversionException] {
      Protobuf.fromMessage(Model.Service.REALTY, protoRequest)
    }
  }

  "convert ModerationRequest correctly" in {
    check(forAll(ModerationRequestGen)(x => x == fromMessage(Service.REALTY, toMessage(x))))
  }

  "throw ProtobufConversionException when convert empty ModerationRequest from proto to model" in {
    val protoRequest = Model.ModerationRequest.newBuilder.setVersion(1).build()
    intercept[ProtobufConversionException] {
      Protobuf.fromMessage(Model.Service.REALTY, protoRequest)
    }
  }

  "UpdateJournalRecord" should {
    "correctly converts" in {
      check(forAll(UpdateJournalRecordGen)(x => x == fromMessage(toMessage(x))))
    }
  }
}
