package ru.yandex.vertis.moderation.scheduler.task.rules

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.model._
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.rule.RuleId
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Task
import ru.yandex.vertis.moderation.model.signal.SignalInfo.ModerationRules
import ru.yandex.vertis.moderation.model.signal.{SignalInfoSet, _}
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.rule.Generators._
import ru.yandex.vertis.moderation.rule.{ModerationAction, ModerationRule, RuleApplyingPolicy, State}
import ru.yandex.vertis.moderation.service.ModerationRuleService.Rules
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * Spec for [[ModerationRuleDecider]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class ModerationRuleDeciderSpec extends SpecBase {

  private val service = Service.REALTY

  case class ModerationRuleSource(id: RuleId,
                                  service: Service,
                                  action: ModerationAction,
                                  state: State = State.withId(1)
                                 ) {
    def genRule: ModerationRule =
      moderationRuleGen(service)
        .map(
          _.copy(
            id = id,
            action = action,
            state = state
          )
        )
        .next
  }

  private def banAction(service: Service, reason: DetailedReason) =
    ModerationAction.Ban(Domain.forServiceOrDefault(service), reason, None)

  private def banAction(reason: DetailedReason, domains: Set[Domain]) = ModerationAction.Ban(domains, reason, None)

  private def hoboAction(service: Service, checkType: HoboCheckType, taskPriority: Option[Int] = None) =
    ModerationAction.Hobo(checkType, taskPriority, None)

  private def warnAction(service: Service, reason: DetailedReason) =
    ModerationAction.Warn(Domain.forServiceOrDefault(service), reason, None)

  case class TestCase(description: String,
                      instanceSignals: SignalSet,
                      visibility: Visibility = Visibility.VISIBLE,
                      opinions: Opinions = Opinions.unknown(service),
                      applicableRules: Seq[ModerationRuleSource],
                      notApplicableRules: Seq[ModerationRuleSource],
                      expectedToRemove: Set[SignalKey],
                      expectedToAppend: Seq[SignalSource],
                      expectedToHobo: Set[HoboSignalSource],
                      expectedToCancelHobo: Map[SignalKey, Option[Task]]
                     )

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "get signals to append correctly",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = None,
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet.Empty
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(0, service, banAction(service, DetailedReason.WrongPrice)),
            ModerationRuleSource(1, service, banAction(service, DetailedReason.WrongPrice)),
            ModerationRuleSource(3, service, banAction(service, DetailedReason.DoNotExist)),
            ModerationRuleSource(4, service, warnAction(service, DetailedReason.WrongArea)),
            ModerationRuleSource(5, service, warnAction(service, DetailedReason.WrongArea)),
            ModerationRuleSource(6, service, hoboAction(service, HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL, Some(1))),
            ModerationRuleSource(7, service, hoboAction(service, HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL, Some(2))),
            ModerationRuleSource(8, service, hoboAction(service, HoboCheckType.MODERATION_RULES))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend =
          Seq(
            WarnSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongArea,
              info = Some("4,5"),
              ttl = None,
              timestamp = None,
              weight = 1.0,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(4, 5)))
            ),
            BanSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongPrice,
              info = Some("0,1"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(0, 1)))
            ),
            BanSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.DoNotExist,
              info = Some("3"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(3)))
            )
          ),
        expectedToHobo =
          Set(
            HoboSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              info = Some("6,7"),
              `type` = HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL,
              comment = Some("6,7"),
              taskPriority = Some(2),
              ttl = None,
              timestamp = None,
              switchOffSource = None,
              labels = Set("6", "7"),
              allowResultAfter = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(6, 7))),
              snapshotPayload = None
            ),
            HoboSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              info = Some("8"),
              `type` = HoboCheckType.MODERATION_RULES,
              comment = Some("8"),
              taskPriority = None,
              ttl = None,
              timestamp = None,
              switchOffSource = None,
              labels = Set("8"),
              allowResultAfter = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(8))),
              snapshotPayload = None
            )
          ),
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "do nothing if no rules",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = None,
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet.Empty
              ),
              DateTimeUtil.now()
            ),
            HoboSignalGen.next.copy(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              `type` = HoboCheckType.MODERATION_RULES,
              task = Some(Task("queue", "key")),
              result = HoboSignal.Result.Undefined
            )
          ),
        applicableRules = Seq.empty,
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "get signals to remove correctly",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = Some("1"),
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet(ModerationRules(Set(1)))
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules = Seq.empty,
        notApplicableRules =
          Seq(
            ModerationRuleSource(1, service, warnAction(service, DetailedReason.WrongArea))
          ),
        expectedToRemove =
          Set(
            WarnSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongArea,
              info = None,
              ttl = None,
              timestamp = None,
              weight = 1.0,
              outerComment = None,
              auxInfo = SignalInfoSet.Empty
            ).getKey
          ),
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "not remove signal with different rule id",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = Some("0"),
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet(ModerationRules(Set(0)))
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules = Seq.empty,
        notApplicableRules =
          Seq(
            ModerationRuleSource(1, service, warnAction(service, DetailedReason.WrongArea))
          ),
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "make rule ids in signal consistent with actual rule ids",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = Some("0,1,2"),
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet.Empty
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(2, service, warnAction(service, DetailedReason.WrongArea))
          ),
        notApplicableRules =
          Seq(
            ModerationRuleSource(1, service, warnAction(service, DetailedReason.WrongArea))
          ),
        expectedToRemove = Set.empty,
        expectedToAppend =
          Seq(
            WarnSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongArea,
              info = Some("2"),
              ttl = None,
              timestamp = None,
              weight = 1.0,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(2)))
            )
          ),
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "do not remove if first rule has another signal type and second has ignore applying policy",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              WarnSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                detailedReason = DetailedReason.WrongArea,
                info = Some("2"),
                ttl = None,
                timestamp = None,
                weight = 1.0,
                outerComment = None,
                auxInfo = SignalInfoSet(ModerationRules(Set(2)))
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules = Seq.empty,
        notApplicableRules =
          Seq(
            ModerationRuleSource(1, service, banAction(service, DetailedReason.WrongArea)),
            ModerationRuleSource(
              2,
              service,
              warnAction(service, DetailedReason.WrongArea),
              state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Ignore)
            )
          ),
        expectedToRemove = Set.empty,
        expectedToAppend =
          Seq(
            WarnSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongArea,
              info = Some("2"),
              ttl = None,
              timestamp = None,
              weight = 1.0,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(2)))
            )
          ),
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "do not remove hobo signal if exists applicable rule with the same check type",
        instanceSignals =
          SignalSet(
            HoboSignalGen.next.copy(
              `type` = HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL,
              source = AutomaticSource(Application.MODERATION_RULES),
              result = HoboSignal.Result.Undefined
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(1, service, hoboAction(service, HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "not send to hobo if instance isn't visible",
        instanceSignals = SignalSet.Empty,
        visibility = VisibilityGen.suchThat(_ != Visibility.VISIBLE).next,
        applicableRules =
          Seq(
            ModerationRuleSource(0, service, hoboAction(service, HoboCheckType.MODERATION_RULES, None))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "not send to hobo if opinion is failed",
        instanceSignals = SignalSet.Empty,
        opinions = Opinions(Domain.default(service) -> Opinion.Failed(Set.empty, Set.empty)),
        applicableRules =
          Seq(
            ModerationRuleSource(0, service, hoboAction(service, HoboCheckType.MODERATION_RULES, None))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description =
          "not send to hobo if instance has related uncompleted hobo signal (even in case it is switched off)",
        instanceSignals =
          SignalSet(
            HoboSignalGen.next.copy(
              source = SourceGen.next.withMarker(NoMarker),
              `type` = HoboCheckType.MODERATION_RULES,
              task = Some(HoboSignalTaskGen.next),
              switchOff = Some(SignalSwitchOffGen.next),
              result = HoboSignal.Result.Undefined,
              ttl = None,
              timestamp = DateTimeUtil.now()
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(0, service, hoboAction(service, HoboCheckType.MODERATION_RULES, None))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "not send to hobo if contains hobo signal from this rule",
        instanceSignals =
          SignalSet(
            HoboSignalGen.next.copy(
              source = AutomaticSourceGen.next.copy(application = Application.MODERATION_RULES),
              `type` = HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL,
              auxInfo = SignalInfoSet(ModerationRules(Set(123))),
              info = Some("123")
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(123, service, hoboAction(service, HoboCheckType.MODERATION_RULES, None))
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "handle domains correctly",
        instanceSignals = SignalSet.Empty,
        applicableRules =
          Seq(
            ModerationRuleSource(
              0,
              Service.USERS_AUTORU, {
                val domains: Set[Domain] =
                  Set(Domain.UsersAutoru(UsersAutoru.ARTIC), Domain.UsersAutoru(UsersAutoru.LCV))
                banAction(DetailedReason.WrongPrice, domains)
              }
            ),
            ModerationRuleSource(1, Service.USERS_AUTORU, warnAction(Service.USERS_AUTORU, DetailedReason.WrongArea)),
            ModerationRuleSource(
              2,
              Service.USERS_AUTORU,
              hoboAction(Service.USERS_AUTORU, HoboCheckType.MODERATION_RULES)
            )
          ),
        notApplicableRules = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToAppend =
          Seq(
            BanSignalSource(
              domain = Domain.UsersAutoru(UsersAutoru.ARTIC),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongPrice,
              info = Some("0"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(0)))
            ),
            BanSignalSource(
              domain = Domain.UsersAutoru(UsersAutoru.LCV),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongPrice,
              info = Some("0"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(0)))
            )
          ) ++ Domain.forServiceOrDefault(Service.USERS_AUTORU).map { domain =>
            WarnSignalSource(
              domain = domain,
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.WrongArea,
              info = Some("1"),
              ttl = None,
              timestamp = None,
              weight = 1.0,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(1)))
            )
          },
        expectedToHobo =
          Set(
            HoboSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              info = Some("2"),
              `type` = HoboCheckType.MODERATION_RULES,
              comment = Some("2"),
              taskPriority = None,
              ttl = None,
              timestamp = None,
              switchOffSource = None,
              labels = Set("2"),
              allowResultAfter = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(2))),
              snapshotPayload = None
            )
          ),
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "cancel hobo task if there is rule that should undo it",
        instanceSignals =
          SignalSet(
            HoboSignalGen.next.copy(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              `type` = HoboCheckType.MODERATION_RULES,
              task = Some(Task("queue", "key")),
              result = HoboSignal.Result.Undefined,
              auxInfo = SignalInfoSet(ModerationRules(Set(1, 2, 3))),
              info = Some("1,2,3")
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(
              id = 1,
              service = service,
              action = hoboAction(service, HoboCheckType.AUTOMATIC_QUALITY_CHECK_CALL),
              state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Undo)
            )
          ),
        notApplicableRules =
          Seq(
            ModerationRuleSource(
              id = 2,
              service = service,
              action = hoboAction(service, HoboCheckType.MODERATION_RULES),
              state = State(RuleApplyingPolicy.Undo, RuleApplyingPolicy.Undo)
            )
          ),
        expectedToRemove = Set.empty,
        expectedToAppend = Seq.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo =
          Map(
            HoboSignalSource(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              info = None,
              `type` = HoboCheckType.MODERATION_RULES,
              comment = None,
              taskPriority = None,
              ttl = None,
              timestamp = None,
              switchOffSource = None,
              outerComment = None,
              auxInfo = SignalInfoSet.Empty,
              snapshotPayload = None
            ).getKey -> Some(Task("queue", "key"))
          )
      ),
      TestCase(
        description = "work correctly with several rules in different states",
        instanceSignals =
          SignalSet(
            WarnSignal(
              weight = 1.0,
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.Sold,
              info = Some("1,2,3"),
              ttl = None,
              timestamp = DateTimeGen.next,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(1, 2, 3))),
              switchOff = None
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(
              id = 1,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Undo)
            ),
            ModerationRuleSource(
              id = 2,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Undo)
            ),
            ModerationRuleSource(
              id = 3,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Undo, RuleApplyingPolicy.Undo)
            ),
            ModerationRuleSource(
              id = 4,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Ignore, RuleApplyingPolicy.Do)
            )
          ),
        notApplicableRules =
          Seq(
            ModerationRuleSource(
              id = 10,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Do, RuleApplyingPolicy.Undo)
            )
          ),
        expectedToAppend =
          Seq(
            WarnSignalSource(
              weight = 1.0,
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.Sold,
              info = Some("1,2"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(1, 2)))
            )
          ),
        expectedToRemove = Set.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "ignore rules with ignore policy when deciding if another rule should delete it",
        instanceSignals =
          SignalSet(
            WarnSignal(
              weight = 1.0,
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.Sold,
              info = Some("1,2"),
              ttl = None,
              timestamp = DateTimeGen.next,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(1, 2))),
              switchOff = None
            ),
            HoboSignalGen.next.copy(
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              `type` = HoboCheckType.MODERATION_RULES,
              task = Some(Task("queue", "key")),
              result = HoboSignal.Result.Undefined,
              auxInfo = SignalInfoSet(ModerationRules(Set(3, 4))),
              info = Some("3,4")
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(
              id = 1,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Ignore, RuleApplyingPolicyGen.next)
            ),
            ModerationRuleSource(
              id = 2,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicy.Undo, RuleApplyingPolicyGen.next)
            ),
            ModerationRuleSource(
              id = 3,
              service = service,
              action = hoboAction(service, HoboCheckType.MODERATION_RULES),
              state = State(RuleApplyingPolicy.Ignore, RuleApplyingPolicyGen.next)
            ),
            ModerationRuleSource(
              id = 4,
              service = service,
              action = hoboAction(service, HoboCheckType.MODERATION_RULES),
              state = State(RuleApplyingPolicy.Undo, RuleApplyingPolicyGen.next)
            )
          ),
        notApplicableRules = Seq.empty,
        expectedToAppend =
          Seq(
            WarnSignalSource(
              weight = 1.0,
              domain = Domain.default(service),
              source = AutomaticSource(Application.MODERATION_RULES),
              detailedReason = DetailedReason.Sold,
              info = Some("1"),
              ttl = None,
              timestamp = None,
              outerComment = None,
              auxInfo = SignalInfoSet(ModerationRules(Set(1)))
            )
          ),
        expectedToRemove = Set.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map("hobo_MODERATION_RULES" -> Some(Task("queue", "key")))
      ),
      TestCase(
        description = "not append signal if rule has Ignore applying policy",
        instanceSignals = SignalSet.Empty,
        applicableRules = Seq.empty,
        notApplicableRules =
          Seq(
            ModerationRuleSource(
              id = 1,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicyGen.next, RuleApplyingPolicy.Ignore)
            ),
            ModerationRuleSource(
              id = 2,
              service = service,
              action = warnAction(service, DetailedReason.Another),
              state = State(RuleApplyingPolicyGen.next, RuleApplyingPolicy.Undo)
            ),
            ModerationRuleSource(
              id = 3,
              service = service,
              action = warnAction(service, DetailedReason.Sold),
              state = State(RuleApplyingPolicyGen.next, RuleApplyingPolicy.Ignore)
            )
          ),
        expectedToAppend = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      ),
      TestCase(
        description = "do not remove existed hobo signal if there is rule that should append it",
        instanceSignals =
          SignalSet(
            SignalFactory.newSignal(
              HoboSignalSource(
                domain = Domain.default(service),
                source = AutomaticSource(Application.MODERATION_RULES),
                info = Some("8"),
                `type` = HoboCheckType.MODERATION_RULES,
                comment = Some("8"),
                taskPriority = None,
                ttl = None,
                timestamp = None,
                switchOffSource = None,
                labels = Set("8"),
                allowResultAfter = None,
                outerComment = None,
                auxInfo = SignalInfoSet(ModerationRules(Set(8))),
                snapshotPayload = None
              ),
              DateTimeUtil.now()
            )
          ),
        applicableRules =
          Seq(
            ModerationRuleSource(8, service, hoboAction(service, HoboCheckType.MODERATION_RULES))
          ),
        notApplicableRules =
          Seq(
            ModerationRuleSource(0, service, hoboAction(service, HoboCheckType.MODERATION_RULES))
          ),
        expectedToAppend = Seq.empty,
        expectedToRemove = Set.empty,
        expectedToHobo = Set.empty,
        expectedToCancelHobo = Map.empty
      )
    )

  "ModerationRuleDecider" should {

    testCases.foreach { testCase =>
      import testCase._
      description in {
        val instance =
          InstanceGen.next.copy(
            signals = instanceSignals,
            context = Context(visibility, None, None)
          )
        val opinionCalculator = mock[OpinionCalculator]
        doReturn(opinions).when(opinionCalculator).apply(instance)
        val decider = new ModerationRuleDeciderImpl(service, opinionCalculator, EmptyFeatureRegistry)
        val rules =
          Rules(
            applicable = applicableRules.map(_.genRule),
            notApplicable = notApplicableRules.map(_.genRule)
          )
        val source = ModerationRuleDecider.Source(instance, rules, signalSourceTag = None)
        val verdict = decider(source)
        verdict.toRemove shouldBe expectedToRemove
        verdict.toAppend.toSet shouldBe expectedToAppend.toSet
        verdict.toHobo shouldBe expectedToHobo
        verdict.toCancelHobo shouldBe expectedToCancelHobo
      }
    }
  }
}
