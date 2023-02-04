from ads.bsyeti.caesar.libs.profiles.proto.goal_context_pb2 import TGoalContextProfileProto
from ads.bsyeti.libs.events.proto.goal_context_pb2 import TGoalContext
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseGoalContexts:
    table = "GoalContexts"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        expression = f"{profile_id}:{profile_id%10}"
        body = TGoalContext()
        body.Expression = expression
        body.Version = time
        event = make_event(profile_id, time, body)

        if event.ProfileID not in self.expected:
            self.expected[profile_id] = TGoalContextProfileProto()

        goal_context = self.expected[profile_id]

        if goal_context.GoalContext.Version <= time:
            goal_context.GoalContextID = profile_id
            goal_context.GoalContext.Expression = expression
            goal_context.GoalContext.Version = time
            expression_impl = goal_context.GoalContext.ParsedExpression.Expression.Or.add().And.add()
            expression_impl.ExpressionAtom.Value.Goal.Id = profile_id
            expression_impl.ExpressionAtom.Value.Goal.FreshTime = profile_id % 10

        yield event

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert self.expected[profile.GoalContextID] == profile
