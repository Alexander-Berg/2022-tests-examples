package vertis.palma.external.broker

import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.TypedBroker
import common.zio.events_broker.testkit.TestBroker
import common.zio.events_broker.testkit.TestBroker.NoOpBroker
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent

import java.time.Instant
import vertis.palma.dao.model.DictionaryItem
import vertis.palma.external.broker.ChangelogService.UnexpectedMessageType
import vertis.palma.service.model.DictionaryException.MissingDictionaryOptions
import vertis.palma.service.model.RequestContext
import vertis.zio.test.ZioSpecBase

class ChangelogServiceImplSpec extends ZioSpecBase {

  private val service =
    new ChangelogServiceImpl(new NoOpBroker().typed[MutationEvent])

  "ChangelogService" should {
    "log changes for known messages" in ioTest {
      val cat = ru.yandex.vertis.verba.auto.Options.Category.newBuilder().setCode("1").build()
      service
        .logUpdate(
          ru.yandex.vertis.verba.auto.Options.Category.getDescriptor,
          DictionaryItem.Id("cat", "1"),
          RequestContext(Some("user"), None, None, None),
          Instant.now(),
          cat,
          cat.toBuilder.setName("cat 1").build()
        )
        .either
        .flatMap { res =>
          check(res shouldBe Right(()))
        }
    }

    "failed on non dictionary descriptor" in ioTest {
      val d = ru.yandex.vertis.telepony.model.proto.TagFilter.getDescriptor
      service
        .logCreate(
          d,
          DictionaryItem.Id("TagFilter", "none"),
          RequestContext.Empty,
          Instant.now(),
          ru.yandex.vertis.telepony.model.proto.TagFilter.getDefaultInstance
        )
        .either
        .flatMap { res =>
          check(res shouldBe Left(MissingDictionaryOptions(d)))
        }
    }

    "failed on different message types" in ioTest {
      val expectedDescriptor = ru.yandex.vertis.verba.auto.Options.Category.getDescriptor
      service
        .logUpdate(
          expectedDescriptor,
          DictionaryItem.Id("cat", "1"),
          RequestContext.Empty,
          Instant.now(),
          ru.yandex.vertis.verba.auto.Options.Category.newBuilder().build(),
          ru.yandex.vertis.verba.auto.Options.Option.newBuilder().build()
        )
        .either
        .flatMap { res =>
          val actualDescriptor = ru.yandex.vertis.verba.auto.Options.Option.getDescriptor
          check(res shouldBe Left(UnexpectedMessageType(actualDescriptor, expectedDescriptor)))
        }
    }

  }
}
