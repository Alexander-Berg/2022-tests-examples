import random

from ads.bsyeti.caesar.bin.prepare_jupiter_factors.proto.factors_pb2 import THostRobotFactorsProto
from ads.bsyeti.caesar.libs.profiles.proto.host_pb2 import THostProfileProto
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseNormalizedHosts:
    table = "NormalizedHosts"
    profile_class = THostProfileProto

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = THostRobotFactorsProto.TData()
        body.Host = str((shard + 1) * profile_id)
        body.Timestamp = time
        body.HerfFactors.Mascot02 = random.randint(0, 100000000)
        body.HerfFactors.Mascot04 = random.randint(0, 100000000)
        body.HerfFactors.Mascot07 = random.randint(0, 100000000)
        body.HerfFactors.Mascot10 = random.randint(0, 100000000)

        event = make_event(profile_id, time, body)
        yield event

        if event.ProfileID not in self.expected or self.expected[event.ProfileID].Timestamp < body.Timestamp:
            self.expected[event.ProfileID] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.NormalizedHost.encode("utf-8")]
            assert expected.Timestamp == profile.RobotFactors.UpdateTime
            for field in ("Mascot02", "Mascot04", "Mascot07", "Mascot10"):
                expected_value = getattr(expected.HerfFactors, field)
                actual_value = getattr(profile.RobotFactors.Herf, field)
                assert expected_value == actual_value, "%s != %s for %s" % (expected_value, actual_value, field)
