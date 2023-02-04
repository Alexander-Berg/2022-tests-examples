# coding: utf-8
from collections import Counter

import pytest
import yatest.common

from ads.quality.phf.phf_direct_loader.lib.extensions.direct import DirectApiExtension
from ads.quality.phf.phf_direct_loader.lib.campaign_generation.direct_managing import (
    DirectCampaignUploader,
    DirectObjectUploader,
    DirectModerator,
    DirectBidder
)
from ads.quality.phf.phf_direct_loader.lib.campaign_generation import expander
from ads.quality.phf.phf_direct_loader.lib.config import TestingConfig

from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    TestExpanderBuilder,
    generate_lettered_list
)


direct_api = DirectApiExtension()
direct_api.init_config(TestingConfig)
direct_api.config_for_client(yatest.common.get_param('DIRECT_TEST_CLIENT'), yatest.common.get_param('DIRECT_TEST_TOKEN'))


TEST_METRIKA_COUNTER = 100000
TEST_MOBILE_BID_MODIFIER = 50


class PrintingEventAggregator(object):
    def add_event(self, event_string):
        pass

    def add_direct_error(self, direct_error):
        print direct_error['Message']
        print direct_error['Details']


@pytest.fixture(scope="module", params=[{'phrase_count': 4, 'region_count': 1},
                                        {'phrase_count': 10, 'region_count': 10}])
def expander_and_campaign_ids(request):
    phrases = generate_lettered_list('phrase', request.param['phrase_count'])

    expander_builder = TestExpanderBuilder().add_banner_template_with_text(u"Тайтл", u"Текст",
                                                                           "https://goo.gle")
    expander_builder = expander_builder.add_custom_phrase_group(expander.PhraseGroup('test', phrases)
                                                                ).set_regions(request.param['region_count'])

    campaign_expander = expander_builder.build()
    direct_json_builder = DirectObjectUploader()
    direct_json_builder.set_metrika_counter(TEST_METRIKA_COUNTER)
    direct_json_builder.set_mobile_bid_modifier(TEST_MOBILE_BID_MODIFIER)
    uploader = DirectCampaignUploader(campaign_expander, direct_api, PrintingEventAggregator(), direct_json_builder)
    camp_ids = uploader.upload()

    def fin():
        direct_api.delete_campaigns([cid for cid in camp_ids if cid is not None])
    request.addfinalizer(fin)

    return campaign_expander, camp_ids


def test_single_banner_campaign_upload(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    for camp_id in camp_ids:
        assert camp_id is not None
    assert len(camp_ids) == campaign_expander.campaign_count


def test_metrika_counter_set(expander_and_campaign_ids):
    _, camp_ids = expander_and_campaign_ids

    camps = direct_api.get_campaigns_with_fields(['Id'], ["CounterIds"])
    for camp in camps:
        if camp['Id'] in camp_ids:
            assert camp["TextCampaign"]["CounterIds"]['Items'][0] == TEST_METRIKA_COUNTER


def test_bid_modifier_set(expander_and_campaign_ids):
    _, camp_ids = expander_and_campaign_ids
    modifiers = direct_api.get_campaigns_bid_modifiers(fields=['Id', 'Type'],
                                                       campaign_ids=camp_ids,
                                                       mobile_fields=["BidModifier"])

    assert modifiers

    for m in modifiers:
        assert m["MobileAdjustment"]["BidModifier"] == TEST_MOBILE_BID_MODIFIER


def test_uploaded_phrases_same(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    expander_phrases_counter = Counter()
    for i in xrange(campaign_expander.campaign_count):
        for phrase_group in campaign_expander.iter_phrase_groups(i):
            for p in phrase_group:
                expander_phrases_counter[p.keyword] += 1

    uploaded_phrases_counter = Counter()

    for resp in direct_api.get_keywords(campaign_ids=camp_ids):
        uploaded_phrases_counter[resp[1]] += 1

    assert expander_phrases_counter == uploaded_phrases_counter


def test_uploaded_banners_are_same(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids
    expander_banner_counter = Counter()
    for i in xrange(campaign_expander.campaign_count):
        for banner_group in campaign_expander.iter_banner_groups(i):
            for b in banner_group:
                expander_banner_counter[(b.title, b.text, b.href)] += 1

    uploaded_banners_counter = Counter()
    for banner in direct_api.get_ads_with_fields(fields=['Id'],
                                                 text_ad_fields=["Text", "Title", "Href"],
                                                 campaign_ids=camp_ids):
        banner = banner['TextAd']
        uploaded_banners_counter[(banner['Title'],
                                  banner['Text'],
                                  banner['Href'])] += 1

    assert uploaded_banners_counter == expander_banner_counter


def test_region_ad_group_match(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    ag_to_region = {}

    for i in range(campaign_expander.campaign_count):
        for ag in campaign_expander.iter_ad_groups(i):
            ag_to_region[ag.name] = ag.region_id

    uploaded_ad_groups = {}

    for ag in direct_api.get_ad_groups_with_fields(["Name", "RegionIds"], campaign_ids=camp_ids):
        uploaded_ad_groups[ag["Name"]] = ag["RegionIds"][0]

    assert ag_to_region == uploaded_ad_groups


class MemorizingEventAggregator(object):
    def __init__(self):
        self._events = []
        self._errors = []

    def add_event(self, event_string):
        self._events.append(event_string)

    def add_direct_object_with_errors(self, direct_object, direct_errors):
        self._errors += direct_errors

    def get_error_code_set(self):
        return {m.code for m in self._errors}


@pytest.fixture(scope="module")
def error_agregator_and_ids(request):
    phrases = expander.PhraseGroup('test', ["8-800-555-35-35^^^^^"])

    expander_builder = TestExpanderBuilder()
    expander_builder.add_banner_template_with_text(u"Слишком Длинный тайтл для того чтобы влезать в директ", u"Текст",
                                                   "https://goo.gle")

    expander_builder.add_banner_template_with_text(u"Норм баннер", u"Текст",
                                                   "https://goo.gle")

    expander_builder = expander_builder.add_custom_phrase_group(phrases).set_regions(1)
    campaign_expander = expander_builder.build()
    event_aggregator = MemorizingEventAggregator()

    direct_json_builder = DirectObjectUploader()
    uploader = DirectCampaignUploader(campaign_expander, direct_api, event_aggregator, direct_json_builder)
    camp_ids = uploader.upload()

    def fin():
        direct_api.delete_campaigns([cid for cid in camp_ids if cid is not None])

    request.addfinalizer(fin)
    return event_aggregator, camp_ids


def test_error_on_incorrect_phrase(error_agregator_and_ids):
    event_aggregator, camp_ids = error_agregator_and_ids
    assert 5002 in event_aggregator.get_error_code_set()


def test_error_on_long_title(error_agregator_and_ids):
    event_aggregator, camp_ids = error_agregator_and_ids
    assert 5001 in event_aggregator.get_error_code_set()


def test_banners_moderated(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    DirectModerator(event_aggregator=MemorizingEventAggregator(),
                    direct_api=direct_api,
                    campaign_ids=camp_ids).moderate()

    banners = direct_api.get_ads_with_fields(fields=['Id', 'Status'], campaign_ids=camp_ids)
    assert banners
    assert all(b['Status'] == 'MODERATION' for b in banners)


def test_moderate_banners_stores_errors(error_agregator_and_ids):
    _, camp_ids = error_agregator_and_ids

    moderate_aggregator = MemorizingEventAggregator()
    DirectModerator(event_aggregator=moderate_aggregator,
                    direct_api=direct_api,
                    campaign_ids=camp_ids).moderate()

    assert 8300 in moderate_aggregator.get_error_code_set()


def test_keyword_bid_changes(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    test_bid = 400000
    DirectBidder(event_aggregator=MemorizingEventAggregator(),
                 direct_api=direct_api,
                 campaign_ids=camp_ids).make_bid(test_bid)

    keywords = direct_api.get_keywords_with_fields(field_names=['Id', 'ContextBid'], campaign_ids=camp_ids)
    assert keywords
    assert all(k['ContextBid'] == test_bid for k in keywords)


def test_error_on_incorrect_bid(expander_and_campaign_ids):
    campaign_expander, camp_ids = expander_and_campaign_ids

    test_bid = 100  # too low

    event_aggregator = MemorizingEventAggregator()
    DirectBidder(event_aggregator=event_aggregator,
                 direct_api=direct_api,
                 campaign_ids=camp_ids).make_bid(test_bid)

    assert 5005 in event_aggregator.get_error_code_set()
