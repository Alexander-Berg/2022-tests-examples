import pytest
import time

from adv.direct.proto.ad_group.ad_group_pb2 import AdGroup, OptionalInternalLevel

from ads.bsyeti.caesar.libs.profiles.proto.ad_group_pb2 import TAdGroupProfileProto
from ads.bsyeti.libs.events.proto.joined_efh_profile_pb2 import TJoinedEFHProfileHitLog
from ads.bsyeti.libs.events.proto.biddable_show_condition_with_phraseid_pb2 import (
    TBiddableShowConditionWithPhraseID,
)
from ads.bsyeti.libs.counter_lib.proto import message_pb2 as counter_pb2
from ads.bsyeti.libs.primitives.counter_proto import counter_ids_pb2
from ads.quality.adv_machine.lib.catmachine.protos.protos_pb2 import (
    TCatEngineProfile,
    TCatEngineItem,
    ECategoryType,
    ECatProfileType,
)
from yabs.proto import user_profile_pb2
from yabs.server.proto.keywords import keywords_data_pb2

from ads.bsyeti.caesar.tests.ft.common.event import make_event


class TestCaseAdGroupsCommon:
    table = "AdGroups"

    extra_config_args = {"max_not_active_phrases": 10000}

    def __init__(self):
        self.expected = {}
        self.time = int(time.time())

    def make_biddabe_show_conditions(self, profile_id, ad_group_id, time):
        expected = self.expected[ad_group_id]["BiddableShowCondition"]

        body = TBiddableShowConditionWithPhraseID()
        body.PhraseID = profile_id + 10
        body.BiddableShowCondition.iter_id = profile_id
        body.BiddableShowCondition.id = 1 + (profile_id % 10)
        body.BiddableShowCondition.context_type = 1 + (profile_id % 11)
        body.BiddableShowCondition.ad_group_id = ad_group_id
        body.BiddableShowCondition.order_id = ad_group_id % 100
        body.BiddableShowCondition.update_time = self.time + profile_id % 20
        body.BiddableShowCondition.delete_time = 1 if profile_id % 15 == 0 else 0
        if body.BiddableShowCondition.context_type == 1:
            body.BiddableShowCondition.keyword_data.text = "Text%d" % profile_id
            body.BiddableShowCondition.keyword_data.hits = 5 * profile_id
        elif body.BiddableShowCondition.context_type == 2:
            body.BiddableShowCondition.retargeting_data.ret_cond_id = 6 * profile_id
        elif body.BiddableShowCondition.context_type == 7:
            body.BiddableShowCondition.dynamic_condition_data.dyn_cond_id = 7 * profile_id
        elif body.BiddableShowCondition.context_type == 8:
            body.BiddableShowCondition.feed_filter_data.price_cpc = 10 * profile_id
            body.BiddableShowCondition.feed_filter_data.price_cpa = 11 * profile_id
            body.BiddableShowCondition.feed_filter_data.only_offer_retargeting = False
            body.BiddableShowCondition.feed_filter_data.only_new_auditory = True
        elif body.BiddableShowCondition.context_type == 11:
            if profile_id % 2:
                body.BiddableShowCondition.feed_filter_data.price_cpc = 10 * profile_id
            for i in range(profile_id % 6):
                body.BiddableShowCondition.relevance_match_data.relevance_match_categories.append(
                    (7 * profile_id * (i + 1)) % 6
                )

        event = make_event(ad_group_id, time, body)

        key = "%s-%s" % (
            body.BiddableShowCondition.id,
            body.BiddableShowCondition.context_type,
        )
        if key not in expected:
            expected[key] = body.BiddableShowCondition
        elif body.BiddableShowCondition.iter_id > expected[key].iter_id:
            expected[key] = body.BiddableShowCondition
        return event

    def mock_international_expression(self, expression, seed_id):
        for seed_id in range(seed_id % 2):
            disjunction = getattr(expression, "and").add()
            atom = getattr(disjunction, "or").add()
            atom.keyword = 1
            atom.operation = 3
            atom.value = "Value"
        if seed_id % 3 == 0:
            disjunction = getattr(expression, "and").add()
            atom = getattr(disjunction, "or").add()
            atom.keyword = 17
            if seed_id % 2 == 0:
                atom.operation = 4
                atom.value = "225"
                if seed_id % 7 == 0:
                    atom.value = "20001"
            else:
                atom.operation = 5
                atom.value = "983"
        else:
            disjunction = getattr(expression, "and").add()
            atom = getattr(disjunction, "or").add()
            atom.keyword = 17
            atom.operation = 4
            atom.value = "983"

    def mock_expression(self, expression, seed_id):
        and_len = seed_id % 4 + 1

        for and_idx in range(and_len):
            disjunction = getattr(expression, "and").add()
            or_len = (and_len + and_idx) % 7 + 1

            for or_idx in range(or_len):
                atom = getattr(disjunction, "or").add()
                atom.keyword = seed_id % 113
                if atom.keyword == 17:
                    atom.keyword = 18
                atom.operation = seed_id % 73
                atom.value = "Value{}_{}_{}".format(seed_id, and_idx, or_idx)

    def mock_rf_options(self, rf_options):
        rf_options.max_shows_count = 2
        rf_options.max_shows_period = 10
        rf_options.stop_shows_period = 5
        rf_options.max_clicks_count = 4
        rf_options.max_clicks_period = 20

    def make_common_fields(self, profile_id, ad_group_id, time):
        expected = self.expected[ad_group_id]["CommonFields"]

        body = AdGroup()
        body.ad_group_id = ad_group_id
        body.iter_id = 100 + profile_id
        body.update_time = profile_id % 100 + 1
        if profile_id % 3 > 0:
            body.delete_time = profile_id % 3
        body.order_id = ad_group_id % 100
        body.minus_phrases.extend(["minusphrase1", "minusphrase2"] + [str(profile_id)])
        body.page_group_tags.extend(["pgt"] + [str(profile_id + 1)])
        body.target_tags.extend(["tt"] + [str(profile_id + 2)])
        body.match_priority = 5
        body.product_gallery_only = True
        body.click_url_tail = "clickurltail"
        body.field_to_use_as_name = "use_as_name"
        body.field_to_use_as_body = "use_as_body"
        body.feed_id = profile_id + 1
        body.context_id = 0
        if profile_id % 5 > 3:
            self.mock_rf_options(body.rf_options)
        if profile_id % 5 > 3:
            self.mock_expression(body.show_conditions, profile_id)
            body.context_id = 13 + profile_id
        if profile_id % 5 == 0:
            self.mock_international_expression(body.show_conditions, profile_id)

        if profile_id % 3:
            body.engine_id = profile_id % 3
        body.type = profile_id % 20
        if profile_id % 2 == 0:
            if profile_id % 3 != 0:
                body.internal_level.value = profile_id % 13
            else:
                # delete value
                level = OptionalInternalLevel()
                body.internal_level.CopyFrom(level)

        if profile_id % 5 > 3:
            multiplier = body.multipliers.add()
            self.mock_expression(multiplier.condition, profile_id)
            multiplier.value = profile_id % 10

        if body.type == 2:
            for i in range(profile_id % 6):
                body.relevance_match_data.relevance_match_categories.append((7 * profile_id * (i + 1)) % 6)

        if expected.Timestamp <= body.iter_id:
            expected.Timestamp = body.iter_id
            expected.UpdateTime = body.update_time
            expected.DeleteTime = body.delete_time
            expected.Type = body.type
            expected.EngineID = body.engine_id
            expected.InternalLevel = body.internal_level.value
            expected.ClearField("MinusPhrases")
            expected.ClearField("PageGroupTags")
            expected.MinusPhrases.extend(body.minus_phrases)
            expected.PageGroupTags.extend(body.page_group_tags)
            expected.MatchPriority = body.match_priority
            expected.ProductGalleryOnly = body.product_gallery_only
            expected.SerpPlacementType = body.serp_placement_type
            expected.ClickUrlTail = body.click_url_tail
            expected.FeedFieldForTitle = body.field_to_use_as_name
            expected.FeedFieldForBody = body.field_to_use_as_body
            expected.FeedID = body.feed_id
            expected.ClearField("Multipliers")
            expected.Multipliers.extend(body.multipliers)
            expected.RelevanceMatchData.CopyFrom(body.relevance_match_data)
            expected.FrequencyShowsOptions.CopyFrom(body.rf_options)

            expected_show_condition = self.expected[ad_group_id]["ShowConditions"]
            expected_show_condition.Expression.CopyFrom(body.show_conditions)
            expected_show_condition.ContextID = body.context_id
            expected_show_condition.IsInternational = profile_id % 5 == 0 and profile_id % 3 != 0

        return make_event(ad_group_id, time, body)

    def make_events(self, time, shard, profile_id):
        ad_group_id = 10 * shard + profile_id % 2
        if ad_group_id not in self.expected:
            self.expected[ad_group_id] = {
                "BiddableShowCondition": {},
                "CommonFields": TAdGroupProfileProto.TCommonFields(),
                "ShowConditions": TAdGroupProfileProto.TShowConditions(),
            }

        yield self.make_common_fields(profile_id, ad_group_id, time)
        yield self.make_biddabe_show_conditions(profile_id, ad_group_id, time)

    def check_profiles(self, profiles):
        expected_converted = self._convert_expected()
        assert len(expected_converted) == len(profiles)

        for profile in profiles:
            expected = expected_converted[profile.AdGroupID]
            assert len(expected.Phrases.PhrasesList) == len(profile.Phrases.PhrasesList)
            for i in range(len(expected.Phrases.PhrasesList)):
                assert expected.Phrases.PhrasesList[i] == profile.Phrases.PhrasesList[i]
            assert expected.CommonFields == profile.CommonFields
            assert expected == profile

    def _convert_expected(self):
        expected_converted = {}
        for ad_group_id in self.expected:
            profile = TAdGroupProfileProto()

            for biddable_show_condition in self.expected[ad_group_id]["BiddableShowCondition"].values():
                profile.OrderID = biddable_show_condition.order_id
                profile.AdGroupID = ad_group_id
                if biddable_show_condition.update_time > 0 and biddable_show_condition.delete_time == 0:
                    phrase = profile.Phrases.PhrasesList.add()
                    phrase.PhraseID = biddable_show_condition.iter_id + 10
                    if biddable_show_condition.context_type == 2:
                        phrase.PhraseID = biddable_show_condition.retargeting_data.ret_cond_id
                    if biddable_show_condition.context_type == 7:
                        phrase.PhraseID = biddable_show_condition.dynamic_condition_data.dyn_cond_id
                    if biddable_show_condition.context_type == 11:
                        phrase.PhraseID = 11
                        phrase.RelevanceMatchData.CopyFrom(biddable_show_condition.relevance_match_data)
                        if biddable_show_condition.feed_filter_data.price_cpc > 0:
                            phrase.OnlyNewAuditory = True
                    if biddable_show_condition.context_type == 1:
                        phrase.Text = biddable_show_condition.keyword_data.text
                        phrase.Hits = biddable_show_condition.keyword_data.hits
                    if biddable_show_condition.context_type == 8:
                        phrase.AutoBudgetAvgCPC = biddable_show_condition.feed_filter_data.price_cpc
                        phrase.AutoBudgetAvgCPA = biddable_show_condition.feed_filter_data.price_cpa
                        phrase.OnlyOfferRetargeting = biddable_show_condition.feed_filter_data.only_offer_retargeting
                        phrase.OnlyNewAuditory = biddable_show_condition.feed_filter_data.only_new_auditory
                    phrase.DirectPhraseID = biddable_show_condition.id
                    phrase.UpdateTime = biddable_show_condition.update_time
                    phrase.DeleteTime = biddable_show_condition.delete_time
                    phrase.IterID = biddable_show_condition.iter_id
                    phrase.ContextType = biddable_show_condition.context_type
                    phrase.Cost = biddable_show_condition.bid
                    phrase.FlatCost = biddable_show_condition.bid_context
                    phrase.Suspended = biddable_show_condition.suspended
            profile.Phrases.PhrasesList.sort(key=lambda x: x.IterID)

            profile.ShowConditions.CopyFrom(self.expected[ad_group_id]["ShowConditions"])
            profile.CommonFields.CopyFrom(self.expected[ad_group_id]["CommonFields"])

            expected_converted[ad_group_id] = profile
        return expected_converted


class TestCaseAdGroupsRemoveSpammerPhrases:
    table = "AdGroups"
    SHARDS_COUNT = 1

    extra_config_args = {"max_not_active_phrases": 15}

    def __init__(self):
        self.expected = {}
        self.time = int(time.time())

    def make_biddabe_show_conditions(self, time, ad_group_id, direct_phrase_id, delete_time, update_time, suspended):
        body = TBiddableShowConditionWithPhraseID()
        body.PhraseID = 1
        body.BiddableShowCondition.order_id = 1
        body.BiddableShowCondition.iter_id = 1
        body.BiddableShowCondition.context_type = 1
        body.BiddableShowCondition.keyword_data.text = "Text1"
        body.BiddableShowCondition.keyword_data.hits = 1
        body.BiddableShowCondition.id = direct_phrase_id
        body.BiddableShowCondition.ad_group_id = ad_group_id
        body.BiddableShowCondition.update_time = self.time + update_time
        body.BiddableShowCondition.delete_time = self.time + delete_time if delete_time else 0
        body.BiddableShowCondition.suspended = suspended

        return make_event(ad_group_id, time, body)

    def make_events(self, time, shard, profile_id):
        if len(self.expected) == 0:
            ad_group_id = profile_id
            self.expected[ad_group_id] = {}

            for i in range(10):
                yield self.make_biddabe_show_conditions(
                    time, ad_group_id, i, int(i / 2), 10 + i % 2, True if i % 3 == 0 else False
                )
            for i in range(10, 20):
                yield self.make_biddabe_show_conditions(time, ad_group_id, i, int(i / 2), 10 + i % 2, False)
            for i in range(20, 30):
                yield self.make_biddabe_show_conditions(time, ad_group_id, i, 0, 10 + i, True if i % 3 == 0 else False)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert 23 == len(profile.Phrases.PhrasesList)
            expected = [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 0, 21, 24, 27, 1, 20, 22, 23, 25, 26, 28, 29]
            expected.reverse()
            assert expected == [phrase.DirectPhraseID for phrase in profile.Phrases.PhrasesList]


class TestCaseAdGroupCatProfiles:
    table = "AdGroups"

    extra_config_args = {"max_not_active_phrases": 15}

    def __init__(self):
        self.expected = {}

    def make_events(self, time, shard, profile_id):
        if profile_id not in self.expected:
            item = user_profile_pb2.Profile.ProfileItem()
            item.keyword_id = keywords_data_pb2.KW_KRYPTA_TOP_DOMAINS
            item.uint_values.append(5)

            counter = counter_pb2.Counter()
            counter.counter_id = counter_ids_pb2.BIGB_COUNTER_87
            counter.key.append(43)

            profile = user_profile_pb2.Profile()
            profile.items.append(item)
            profile.counters.append(counter)

            body = TJoinedEFHProfileHitLog()
            body.EventTime = 1629753192
            body.GroupExportID = profile_id
            body.PageID = 3
            body.QTailID = 4
            body.RegionID = 6
            body.PlaceID = 542
            body.SelectType = 82
            body.CounterType = 2
            body.ProfileDump = profile.SerializeToString()

            yield make_event(profile_id, time, body)

            catItem1 = TCatEngineItem()
            catItem1.Type = ECategoryType.KryptaTopDomains
            catItem1.Ids.append(5)
            catItem1.Tfs.append(0.01)

            catItem2 = TCatEngineItem()
            catItem2.Type = ECategoryType.RegionID
            catItem2.Ids.append(6)
            catItem2.Tfs.append(0.01)

            catItem3 = TCatEngineItem()
            catItem3.Type = ECategoryType.ShownOrders
            catItem3.Ids.append(43)
            catItem3.Tfs.append(0.01)

            catItem4 = TCatEngineItem()
            catItem4.Type = ECategoryType.PageID
            catItem4.Ids.append(3)
            catItem4.Tfs.append(0.01)

            catItem5 = TCatEngineItem()
            catItem5.Type = ECategoryType.HourOfWeek
            catItem5.Ids.append(24)
            catItem5.Tfs.append(0.01)

            catProfile = TCatEngineProfile()
            catProfile.ProfileType = ECatProfileType.GroupExportSmoothing
            catProfile.Items.append(catItem1)
            catProfile.Items.append(catItem2)
            catProfile.Items.append(catItem3)
            catProfile.Items.append(catItem4)
            catProfile.Items.append(catItem5)

            self.expected[profile_id] = []
            self.expected[profile_id].append(catProfile)

    def check_profiles(self, profiles):
        assert len(self.expected) == len(profiles)

        for profile in profiles:
            assert len(self.expected[profile.AdGroupID]) == len(profile.CatProfiles.Values)

            for i in range(len(profile.CatProfiles.Values)):
                expected = self.expected[profile.AdGroupID][i]
                actual = profile.CatProfiles.Values[i]

                actual.Items.sort(key=lambda item: item.Type)

                assert expected.ProfileType == actual.ProfileType
                assert len(expected.Items) == len(actual.Items)

                for j in range(len(expected.Items)):
                    assert expected.Items[j].Type == actual.Items[j].Type

                    assert len(expected.Items[j].Ids) == len(actual.Items[j].Ids)
                    assert len(expected.Items[j].Tfs) == len(actual.Items[j].Tfs)

                    for z in range(len(expected.Items[j].Ids)):
                        assert expected.Items[j].Ids[z] == actual.Items[j].Ids[z]
                        assert expected.Items[j].Tfs[z] == pytest.approx(actual.Items[j].Tfs[z])
