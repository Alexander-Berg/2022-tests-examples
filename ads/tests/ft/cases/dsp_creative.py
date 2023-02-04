import random

from adv.direct.proto.dsp_creative.dsp_creative_pb2 import TDspCreative
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseDspCreatives:
    table = "DspCreatives"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = TDspCreative()
        body.IterID = (shard + 1) * profile_id
        body.CreativeID = 0
        body.DSPID = random.randint(1, 100)
        body.Geo.extend(random.sample([1, 2, 3, 4, 5], random.randint(1, 4)))
        yield make_event(profile_id, time, body)

        if profile_id not in self.expected or self.expected[profile_id].IterID < body.IterID:
            self.expected[profile_id] = body

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            assert self.expected[profile.CreativeID] == profile.Data
