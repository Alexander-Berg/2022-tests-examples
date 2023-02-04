import random

from ads.bsyeti.caesar.bin.compute_offer_counters.proto.logs_pb2 import TOfferCountersProto
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseCounterOffers:
    table = "CounterOffers"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = TOfferCountersProto.TData()

        body.CounterOfferID = (shard + 1) * profile_id
        body.Timestamp = time

        body.TurboOfferCounters.AnyAppearancesCounter = random.randint(0, 10000000000)
        body.TurboOfferCounters.SessionsWithAnyAppearancesCounter = random.randint(0, 10000000000)

        body.EcomOfferCounters.AnyAppearancesCounter = random.randint(0, 10000000000)
        body.EcomOfferCounters.SessionsWithAnyAppearancesCounter = random.randint(0, 10000000000)

        yield make_event(profile_id, time, body)

        if profile_id not in self.expected:
            self.expected[profile_id] = {"Counters": TOfferCountersProto.TData()}
        expected = self.expected[profile_id]
        if expected["Counters"].Timestamp < time:
            expected["Counters"] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.CounterOfferID]["Counters"]
            assert expected.Timestamp == profile.Counters.UpdateTime
            for counter_name in ("EcomOfferCounters", "TurboOfferCounters"):
                expected_counter = getattr(expected, counter_name)
                actual_counter = getattr(profile.Counters, counter_name)

                for field in ("AnyAppearancesCounter", "SessionsWithAnyAppearancesCounter"):
                    expected_value = getattr(expected_counter, field)
                    actual_value = getattr(actual_counter, field)
                    assert expected_value == actual_value, "%s != %s for %s.%s" % (
                        expected_value,
                        actual_value,
                        counter_name,
                        field,
                    )
