package vertis.anubis.api.test

import ru.yandex.vertis.broker.broker_options.ClickhouseConfig
import ru.yandex.vertis.broker.{ClickhouseConfig, Partitioning}
import ru.yandex.vertis.validation.model.MissingRequiredField
import vertis.anubis.api.services.validate.errors.BrokerValidationError._
import vertis.anubis.api.services.validate.validators.broker.{BrokerConfigChecks, BrokerValidator}
import vertis.anubis.api.test.evolution.EvolutionValidatorSpecBase.MadWorld
import vertis.zio.test.ZioSpecBase
import vertis.anubis.api.test.broker._

/** @author kusaeva
  */
class BrokerValidatorSpec extends ZioSpecBase with ValidationTestSupport {
  private val validator = new BrokerValidator(world, Set.empty)

  "BrokerValidator" should {
    "fail when spawn and repartition are set for not a daily partitioning" in ioTest {
      val descriptor = IllegalPartitioningMessage.getDescriptor

      val expectedErrors = List(
        IllegalPartitioning(descriptor, Partitioning.BY_MONTH, "spawn"),
        IllegalPartitioning(descriptor, Partitioning.BY_MONTH, "repartition"),
        NonUniqueYtPath(descriptor, "anubis/illegal_partitioning/events")
      )

      checkFail(validator, descriptor, expectedErrors)
    }
    "succeed if clickhouse has expire_in_days > than in yt" in ioTest {
      checkSucceed(
        validator,
        CorrectClickhouseConfigMessage.getDescriptor
      )
    }
    "fail if max_lag > expire_in_days and partitioned by days" in ioTest {
      val descriptor = InvalidMaxLagMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          IllegalMaxLag(descriptor, 60, 30, Partitioning.BY_DAY)
        )
      )
    }
    "fail if max_lag > expire_in_days and partitioned by month" in ioTest {
      val descriptor = InvalidMaxLagMonthMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          IllegalMaxLag(descriptor, 2, 30, Partitioning.BY_MONTH)
        )
      )
    }
    "fail if max_lag > expire_in_days and partitioned by year" in ioTest {
      val descriptor = InvalidMaxLagYearMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          IllegalMaxLag(descriptor, 1, 300, Partitioning.BY_YEAR)
        )
      )
    }
    "not fail if max_lag is set and expire_in_days is not set" in ioTest {
      checkSucceed(
        validator,
        MaxLagNoExpireMessage.getDescriptor
      )
    }
    "fail if sort by unknown field" in ioTest {
      val descriptor = SortByUnknownFieldMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          UnknownField(descriptor, "sort", Seq("foo"))
        )
      )
    }
    "fail for non-unique delivery name" in ioTest {
      val descriptor = NonUniqueNameMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          NonUniqueYtPath(descriptor, "anubis/non_unique")
        )
      )
    }
    "fail for deep delivery name" in ioTest {
      val descriptor = DeepNameMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          DeepStreamName(descriptor, "anubis/i/am/too/deep/for/this", BrokerConfigChecks.MaxNameDepth)
        )
      )
    }
    "fail for non-unique clickhouse table name" in ioTest {
      val descriptor = NonUniqueTableMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          NonUniqueChTableName(descriptor, "test.stats.non_unique")
        )
      )
    }
    "fail on non-unique clickhouse cluster name" in ioTest {
      val descriptor = EventWithSameCh.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          SameClusterChDeliveries(descriptor, Seq("test"))
        )
      )
    }
    "fail on clickhouse fields in both annotations" in ioTest {
      val descriptor = InvalidEventWithCh.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          AmbiguousChFields(descriptor)
        )
      )
    }
    "fail for non-unique repartition path" in ioTest {
      val descriptor = NonUniqueRepartitionPath.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          NonUniqueYtPath(descriptor, "anubis/repartition/events"),
          UnknownField(descriptor, "repartition", Seq("event_timestamp"))
        )
      )
    }
    "validate required config fields are set" in ioTest {
      val descriptor = NoNameMessage.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          UnsatisfiedRequirement(
            descriptor,
            Seq(
              MissingRequiredField("broker.MessageBrokerConfig.name"),
              MissingRequiredField("broker.ClickhouseConfig.table"),
              MissingRequiredField("broker.ClickhouseConfig.cluster")
            )
          )
        )
      )
    }
    "not fail for a custom timestamp field" in ioTest {
      val descriptor = CustomTimestampMessage.getDescriptor
      checkSucceed(
        validator,
        descriptor
      )
    }
    "warn on delivery renaming" in ioTest {
      val descriptor = ValidMessage.getDescriptor
      val masterDescriptor = CustomTimestampMessage.getDescriptor
      val world = new MadWorld(Map(descriptor -> masterDescriptor))
      val validator = new BrokerValidator(world, Set("broker-cannon"))
      checkFail(
        validator,
        descriptor,
        List(RenamedDeliveryWarning(descriptor, "anubis/custom-timestamp", "anubis/api/test-new"))
      )
    }
    "succeed on empty substreams config" in ioTest {
      val descriptor = EmptySubsEvent.getDescriptor
      checkSucceed(
        validator,
        descriptor
      )
    }
    "succeed when filter substream by string value" in ioTest {
      val descriptor = SubCategoryEvent.getDescriptor
      checkSucceed(
        validator,
        descriptor
      )
    }
    "succeed when filter substream by enum value" in ioTest {
      val descriptor = SubEnumEvent.getDescriptor
      checkSucceed(
        validator,
        descriptor
      )
    }
    "fail on substream error" in ioTest {
      val descriptor = IllegalSubEvent.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          IllegalStreamName(descriptor, "auto_ru"),
          IllegalSubstreamFilterError(descriptor, "auto", "domain = AUTO"),
          IllegalSubstreamFilterError(descriptor, "auto_ru", "code = 500"),
          NonUniqueYtPath(descriptor, "anubis/illegal-subenum/auto"),
          UnknownField(descriptor, "primary_key in test test.codes", Seq("foo")),
          UnsatisfiedRequirement(
            descriptor,
            List(
              MissingRequiredField("broker.SubstreamConfig.filter")
            )
          )
        )
      )
    }
    "fail with error on multiple timestamps" in ioTest {
      val descriptor = MultipleTsEvent.getDescriptor
      checkFail(
        validator,
        descriptor,
        List(
          MultipleTimestamps(descriptor)
        )
      )
    }
    "fail on ch primary key change" in ioTest {
      val oldDescriptor = ValidEventWithCh.getDescriptor
      val newDescriptor = ValidEventWithChAndChangedPk.getDescriptor
      val world = new MadWorld(Map(newDescriptor -> oldDescriptor))
      val validator = new BrokerValidator(world, Set("anubis/valid-with-ch"))
      checkFail(
        validator,
        newDescriptor,
        List(
          IllegalPkChange(newDescriptor, "test_cluster_2", "test_db.test_table", Seq("name"), Seq("name", "id"))
        )
      )
    }

  }
}
