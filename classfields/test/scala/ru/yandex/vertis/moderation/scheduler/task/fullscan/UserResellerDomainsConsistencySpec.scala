package ru.yandex.vertis.moderation.scheduler.task.fullscan

import org.joda.time.DateTime
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest, SignalKey}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru._

class UserResellerDomainsConsistencySpec extends SpecBase {

  private case class TestCase(description: String, instance: Instance, check: Seq[ModerationRequest] => Boolean)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "Adding switch offs for Auto",
        instance = InstanceGen.next.copy(signals = SignalSet(Seq(banSignalOff(CARS), banSignal(LCV)))),
        check = actual => actual.size == 1 && actual.exists(_.isInstanceOf[ModerationRequest.AddSwitchOffs])
      ),
      TestCase(
        description = "Adding swithced off ban signals for Moto",
        instance = InstanceGen.next.copy(signals = SignalSet(Seq(banSignalOff(MOTORCYCLE)))),
        check =
          actual =>
            actual.size == 1 &&
              actual.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              extractAppendSignalsDomains(actual) == Seq(
                Domain.UsersAutoru(SCOOTERS),
                Domain.UsersAutoru(SNOWMOBILE),
                Domain.UsersAutoru(ATV)
              )
      ),
      TestCase(
        description = "Adding ban signals for Trucks",
        instance = InstanceGen.next.copy(signals = SignalSet(Seq(banSignal(TRUCK)))),
        check =
          actual =>
            actual.size == 1 &&
              actual.exists(_.isInstanceOf[ModerationRequest.AppendSignals]) &&
              extractAppendSignalsDomains(actual) == Seq(
                Domain.UsersAutoru(BUS),
                Domain.UsersAutoru(ARTIC),
                Domain.UsersAutoru(TRAILER),
                Domain.UsersAutoru(SPECIAL)
              )
      ),
      TestCase(
        description = "Do nothing on non reseller reasons",
        instance =
          InstanceGen.next.copy(
            signals =
              SignalSet(Seq(banSignal(TRUCK, DetailedReason.BadPhoto), banSignalOff(CARS, DetailedReason.BlockedIp)))
          ),
        check = actual => actual.isEmpty
      )
    )

  private val timestamp: DateTime = DateTimeGen.next

  private def extractAppendSignalsDomains(requests: Seq[ModerationRequest]): Seq[Domain] =
    requests.flatMap {
      case ModerationRequest.AppendSignals(_, s, _, _, _) => s.map(_.domain)
      case _                                              => Seq.empty
    }

  private def banSignal(domain: Model.Domain.UsersAutoru,
                        detailedReason: DetailedReason = DetailedReason.UserReseller(None, Seq.empty)
                       ): BanSignal =
    BanSignalGen.next.copy(
      domain = Domain.UsersAutoru(domain),
      detailedReason = detailedReason,
      switchOff = None
    )

  private def banSignalOff(domain: Model.Domain.UsersAutoru,
                           detailedReason: DetailedReason = DetailedReason.UserReseller(None, Seq.empty)
                          ): BanSignal = {
    val signal = banSignal(domain, detailedReason)
    signal.withSwitchOff(Some(SignalSwitchOff(signalSwitchOffSource(signal.key), timestamp)))
  }

  private def hoboSignal(value: Model.Domain.UsersAutoru): HoboSignal =
    HoboSignalGen.next.copy(
      domain = Domain.UsersAutoru(value),
      result = HoboSignal.Result.Bad(detailedReasons = Set(DetailedReason.UserReseller(None, Seq.empty)), None),
      switchOff = None
    )

  private def hoboSignalOff(value: Model.Domain.UsersAutoru): HoboSignal = {
    val signal = hoboSignal(value)
    signal.withSwitchOff(Some(SignalSwitchOff(signalSwitchOffSource(signal.key), timestamp)))
  }

  private val source: Source = AutomaticSource(application = Model.AutomaticSource.Application.MODERATION)

  private def signalSwitchOffSource(key: SignalKey): SignalSwitchOffSource =
    SignalSwitchOffSource(key = key, source = source, comment = None, ttl = None)

  "UserResellerDomainsConsistency.decide" should {
    testCases.foreach { case TestCase(description, instance, check) =>
      description in {
        check(UserResellerDomainsConsistency.decide(instance)) shouldBe true
      }
    }
  }
}
