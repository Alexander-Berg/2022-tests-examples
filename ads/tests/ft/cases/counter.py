import time

from ads.bsyeti.caesar.libs.profiles.proto.counter_pb2 import TCounterProfileProto
from ads.bsyeti.libs.events.proto.counter_pb2 import TCounter
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseCounters:
    table = "Counters"

    def __init__(self):
        self.expected = {}
        self.time = int(time.time())

    def make_events(self, time, shard, profile_id):
        counter_name = "some_name {}".format(profile_id)

        body = TCounter()
        body.name = counter_name
        event = make_event(profile_id, self.time, body)

        if event.ProfileID not in self.expected:
            self.expected[event.ProfileID] = TCounterProfileProto()

        counter = self.expected[event.ProfileID]

        if counter.Counter.Timestamp <= self.time:
            counter.CounterID = profile_id
            counter.Counter.CounterName = counter_name
            counter.Counter.Timestamp = self.time

        yield event

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert self.expected[str(profile.CounterID).encode("utf-8")] == profile
