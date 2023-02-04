import random

from ads.bsyeti.libs.events.proto.biddable_show_condition_with_phraseid_pb2 import (
    TBiddableShowConditionWithPhraseID,
)
from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCasePhrases:
    table = "Phrases"

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        body = TBiddableShowConditionWithPhraseID()
        body.PhraseID = profile_id
        body.BiddableShowCondition.keyword_data.text = "Text%d" % profile_id
        body.BiddableShowCondition.iter_id = (shard + 1) * profile_id
        body.BiddableShowCondition.id = profile_id + 100
        body.BiddableShowCondition.context_type = random.randint(1, 3)
        body.BiddableShowCondition.order_id = 1
        body.BiddableShowCondition.ad_group_id = 1

        yield make_event(profile_id, time, body)

        if 1 == body.BiddableShowCondition.context_type:
            self.expected[profile_id] = body.BiddableShowCondition.keyword_data.text

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)
        for profile in profiles:
            expected = self.expected[profile.PhraseID]
            assert expected == profile.Text.decode()
