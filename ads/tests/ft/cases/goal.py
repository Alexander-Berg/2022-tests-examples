import time
import random

from ads.bsyeti.caesar.libs.profiles.proto.goal_pb2 import TGoalProfileProto
from ads.bsyeti.libs.events.proto.affinitive_sites_pb2 import TAffinitiveSites
from ads.bsyeti.libs.events.proto.goal_pb2 import TGoal
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class _TestCaseBase:
    table = "Goals"

    extra_profiles = {
        "Counters": [],
    }

    def __init__(self):
        self.expected = {}
        self.time = int(time.time())

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert self.expected[str(profile.GoalID).encode("utf-8")] == profile


class TestCaseAdGoals(_TestCaseBase):
    def make_events(self, time, shard, profile_id):
        goal_type = "url"
        counter_id = profile_id + 17
        goal_name = "some_goal_name"

        body_goal = TGoal()
        body_goal.counter_id = counter_id
        body_goal.goal_type = goal_type
        body_goal.name = goal_name

        event_goal = make_event(profile_id, self.time, body_goal)

        if event_goal.ProfileID not in self.expected:
            self.expected[event_goal.ProfileID] = TGoalProfileProto()

        goal = self.expected[event_goal.ProfileID]

        if goal.GoalType.Timestamp <= self.time:
            goal.GoalID = profile_id
            goal.GoalType.GoalType = 2
            goal.GoalType.GoalName = goal_name
            goal.GoalType.Timestamp = self.time
            goal.Counter.CounterID = counter_id

        yield event_goal


class TestCaseAffinitiveSites(_TestCaseBase):
    def make_affinitive_features(self, repeated_field, size, name):
        for i in range(size):
            repeated_field.add()
            setattr(repeated_field[-1], name, "{}_{}".format(name, i))
            setattr(repeated_field[-1], "Weight", random.randint(0, 100))

    def make_events(self, time, shard, profile_id):

        body = TAffinitiveSites()

        self.make_affinitive_features(body.AffinitiveSites, 2, "Site")
        self.make_affinitive_features(body.AffinitiveApps, 2, "App")
        self.make_affinitive_features(body.AffinitiveWords, 2, "Word")

        event = make_event(profile_id, self.time, body)

        if event.ProfileID not in self.expected:
            self.expected[event.ProfileID] = TGoalProfileProto()

        goal = self.expected[event.ProfileID]

        if goal.AffinitiveSites.Timestamp <= self.time:
            goal.GoalID = profile_id

            goal.AffinitiveFeatures.ClearField("AffinitiveSites")
            goal.AffinitiveFeatures.AffinitiveSites.extend(body.AffinitiveSites)
            goal.AffinitiveFeatures.ClearField("AffinitiveApps")
            goal.AffinitiveFeatures.AffinitiveApps.extend(body.AffinitiveApps)
            goal.AffinitiveFeatures.ClearField("AffinitiveWords")
            goal.AffinitiveFeatures.AffinitiveWords.extend(body.AffinitiveWords)

            goal.AffinitiveFeatures.Timestamp = self.time

        yield event
