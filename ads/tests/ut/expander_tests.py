# coding: utf-8

import pytest
from urlparse import urlparse, parse_qs

from ads.quality.phf.phf_direct_loader.lib.campaign_generation import expander
from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    TestExpanderBuilder
)


@pytest.mark.parametrize("region_count, max_group_campaign", [(1, 1), (10, 100), (100, 7)])
def test_ad_groups_count_correct(region_count, max_group_campaign):
    builder = TestExpanderBuilder().add_banner_template(5, 5, 1, 1).add_phrase_group(1).set_regions(region_count)
    builder.set_max_ad_group(max_group_campaign)
    test_expander = builder.build()

    group_count = 0
    for camp in test_expander.iter_campaigns():
        camp_ag_count = len(list(test_expander.iter_ad_groups(camp.id)))
        assert camp_ag_count <= max_group_campaign
        group_count += camp_ag_count

    assert test_expander.campaign_count >= group_count / max_group_campaign

    total_ad_groups = 0
    for campaign in test_expander.iter_campaigns():
        assert campaign.ad_group_count <= max_group_campaign
        total_ad_groups += campaign.ad_group_count

    assert total_ad_groups == group_count


@pytest.mark.parametrize("phrase_group_count, region_count", [(1, 1), (7, 5), (10, 11)])
def test_expected_group_count_match_real(phrase_group_count, region_count):
    builder = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(1, phrase_group_count)
    builder.set_regions(region_count)
    test_expander = builder.build()

    group_count = 0
    for camp in test_expander.iter_campaigns():
        group_count += len(list(test_expander.iter_ad_groups(camp.id)))

    expected_group_count = phrase_group_count * region_count
    assert expected_group_count == group_count


def test_preview_iter_campaigns_identical():
    campaign_expander = TestExpanderBuilder().add_banner_template(4, 4, 4, 4).add_phrase_group(4).set_regions(
        25).build()
    for i, campaign in enumerate(campaign_expander.iter_campaigns()):
        assert campaign == campaign_expander.get_campaign_preview(i)


def test_campaign_names_unique():
    campaign_expander = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(1).set_regions(100).build()
    names = set()

    for campaign in campaign_expander.iter_campaigns():
        names.add(campaign.name)
    assert len(names) == campaign_expander.campaign_count


@pytest.mark.parametrize("banner_atr_cnt, group_banner_limit", [(1, 1), (2, 5), (4, 10)])
def test_ad_groups_banner_count_correct(banner_atr_cnt, group_banner_limit):
    campaign_expander = TestExpanderBuilder().add_banner_template(
        banner_atr_cnt,
        banner_atr_cnt,
        banner_atr_cnt,
        banner_atr_cnt).add_phrase_group(1).set_regions(1).set_max_banner(group_banner_limit).build()

    total_banners = 0
    for camp in campaign_expander.iter_campaigns():
        for ag in campaign_expander.iter_ad_groups(camp.id):
            assert ag.banner_count <= group_banner_limit
            total_banners += ag.banner_count
    assert total_banners == banner_atr_cnt ** 4


def test_ad_group_iter_preview_identical():
    campaign_expander = TestExpanderBuilder().add_banner_template(4, 4, 4, 4).add_phrase_group(4).set_regions(
        25).build()
    for camp in campaign_expander.iter_campaigns():
        for ag in campaign_expander.iter_ad_groups(camp.id):
            assert ag == campaign_expander.get_ad_group_preview(camp.id, ag.id)


def test_ad_group_names_unique():
    names = set()
    campaign_expander = TestExpanderBuilder().add_banner_template(4, 4, 4, 4).add_phrase_group(4).set_regions(
        25).build()
    total = 0
    for camp in campaign_expander.iter_campaigns():
        for ag in campaign_expander.iter_ad_groups(camp.id):
            names.add(ag.name)
            total += 1
    assert len(names) == total


def test_ad_group_iter_list_preview_identical():
    campaign_expander = TestExpanderBuilder().add_banner_template(4, 4, 4, 4).add_phrase_group(4).set_regions(
        25).build()
    for camp in campaign_expander.iter_campaigns():
        assert list(campaign_expander.iter_ad_groups(camp.id)) == campaign_expander.get_ad_group_list_preview(camp.id)


def test_wrong_index_campaign_preview():
    campaign_expander = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(1).set_regions(10).build()

    for val in (-1, campaign_expander.campaign_count):
        with pytest.raises(expander.PreviewElementNotFound):
            campaign_expander.get_campaign_preview(val)


def test_wrong_index_ad_group_preview():
    campaign_expander = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(1).set_regions(10).build()

    for val in (-1, campaign_expander.campaign_count):
        with pytest.raises(expander.PreviewElementNotFound):
            campaign_expander.get_ad_group_preview(val, 0)

    for i, camp in enumerate(campaign_expander.iter_campaigns()):
        for val in (-1, camp.ad_group_count):
            with pytest.raises(expander.PreviewElementNotFound):
                campaign_expander.get_ad_group_preview(i, val)


@pytest.mark.parametrize("phrase_limit, phrase_group_lengths", [(5, [4, 7, 10])])
def test_phrase_count_correct(phrase_limit, phrase_group_lengths):
    builder = TestExpanderBuilder().add_dumb_banner_template()
    for pg_length in phrase_group_lengths:
        builder.add_phrase_group(pg_length)
    builder.set_regions(1)
    builder.set_max_phrase(phrase_limit)

    campaign_expander = builder.build()

    total_phrases = 0
    for camp in campaign_expander.iter_campaigns():
        for ag in campaign_expander.iter_ad_groups(camp.id):
            assert ag.phrase_count <= phrase_limit
            phrase_list = campaign_expander.get_phrases_preview(camp.id, ag.id)
            assert len(phrase_list) == ag.phrase_count
            total_phrases += ag.phrase_count
    assert total_phrases == sum(phrase_group_lengths)


def test_phrase_preview_iter_identical():
    campaign_expander = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(8).set_regions(25).build()

    for camp in campaign_expander.iter_campaigns():
        for ag_id, phrase_list in enumerate(campaign_expander.iter_phrase_groups(camp.id)):
            assert phrase_list == campaign_expander.get_phrases_preview(camp.id, ag_id)


def test_wrong_index_phrase_list_preview():
    campaign_expander = TestExpanderBuilder().add_dumb_banner_template().add_phrase_group(1).set_regions(10).build()

    for val in (-1, campaign_expander.campaign_count):
        with pytest.raises(expander.PreviewElementNotFound):
            campaign_expander.get_phrases_preview(val, 0)

    for i, camp in enumerate(campaign_expander.iter_campaigns()):
        for val in (-1, camp.ad_group_count):
            with pytest.raises(expander.PreviewElementNotFound):
                campaign_expander.get_phrases_preview(i, val)


banner_count_test_params = [
    (2, 1, 1, 1, 1),
    (3, 3, 2, 4, 4),
    (3, 3, 2, 0, 4),
]


@pytest.mark.parametrize("num_titles, num_hrefs, num_texts, num_images, banner_limit", banner_count_test_params)
def test_banner_count_correct(num_titles, num_hrefs, num_texts, num_images, banner_limit):
    builder = TestExpanderBuilder().add_banner_template(num_titles, num_texts, num_images, num_hrefs)
    builder.add_phrase_group(1)
    builder.set_regions(1)
    builder.set_max_banner(banner_limit)

    campaign_expander = builder.build()

    total_banners = 0

    for camp in campaign_expander.iter_campaigns():
        ag_banner_counts = []

        for ag in campaign_expander.iter_ad_groups(camp.id):
            assert ag.banner_count <= banner_limit
            ag_banner_counts.append(ag.banner_count)

        for i, banner_list in enumerate(campaign_expander.iter_banner_groups(camp.id)):
            assert ag_banner_counts[i] == len(banner_list)

        total_banners += sum(ag_banner_counts)

    expected_banner_count = num_titles * num_hrefs * num_texts
    if num_images > 0:
        expected_banner_count *= num_images
    assert total_banners == expected_banner_count


def test_banner_iter_preview_identical():
    campaign_expander = TestExpanderBuilder().add_banner_template(2, 3, 2, 1).add_phrase_group(1).set_regions(5).build()

    for camp in campaign_expander.iter_campaigns():
        for ag_id, banner_list in enumerate(campaign_expander.iter_banner_groups(camp.id)):
            assert banner_list == campaign_expander.get_banners_preview(camp.id, ag_id)


TEST_REGION = expander.RegionWithCases(u'Новосибирск',
                                       {'nom': u'Новосибирск',
                                        'loc': u'в Новосибирске',
                                        'gen': u'Новосибирска'},
                                       10)


def test_region_is_substituted():
    assert TEST_REGION.cases['loc'] in u"{:loc}".format(TEST_REGION)


def test_region_is_upper_cased():
    expected_string = TEST_REGION.cases['loc'][0].upper() + TEST_REGION.cases['loc'][1:]
    assert expected_string in u"{:Loc}".format(TEST_REGION)


def test_multiple_placeholders_in_string():
    expected_count = 5
    formatted_string = (u"{0:nom}" * expected_count).format(TEST_REGION)
    expected_string = TEST_REGION.cases['nom']
    assert formatted_string.count(expected_string) == expected_count


@pytest.mark.parametrize("field", ['text', 'title'])
def test_banner_field_formatted(field):
    formatter = expander.UserFriendlyRegionFormatter()

    banner_init_dict = {'text': "", 'title': "", 'href': "", field: u"(В Москве)"}

    expander_builder = TestExpanderBuilder().add_banner_template_with_text(**banner_init_dict)
    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    formatted_banner_field = getattr(test_expander.get_banners_preview(0, 0)[0], field)

    assert formatted_banner_field == formatter.format_string(u"(В Москве)", TEST_REGION)


def test_group_name_contains_region_name():
    expander_builder = TestExpanderBuilder().add_dumb_banner_template()
    test_expander = expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1).build()
    assert (TEST_REGION.name in test_expander.get_ad_group_list_preview(0)[0].name)


def test_group_region_id_correct():
    expander_builder = TestExpanderBuilder().add_dumb_banner_template()
    test_expander = expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1).build()
    assert TEST_REGION.direct_id == test_expander.get_ad_group_list_preview(0)[0].region_id


def test_phrase_formatted():
    formatter = expander.UserFriendlyRegionFormatter()
    expander_builder = TestExpanderBuilder().add_dumb_banner_template().add_region(TEST_REGION)
    test_expander = expander_builder.add_custom_phrase_group(expander.PhraseGroup('test', [u'(В Москве)'])).build()
    assert test_expander.get_phrases_preview(0, 0)[0].keyword == formatter.format_string(u"(В Москве)", TEST_REGION)


def test_formatter():
    formatter = expander.UserFriendlyRegionFormatter()
    test_s = u"(В Москве) лучшие новости! Слушайте новости (Москвы). Лучшие новости только (в Москве)"
    test_s = formatter.format_string(test_s, TEST_REGION)
    expected_s = u"{0:Loc} лучшие новости! Слушайте новости {0:gen}. Лучшие новости только {0:loc}".format(TEST_REGION)
    assert test_s == expected_s


def test_href_params_applied():
    test_url = "www.test.ru?state=2&state=3"
    test_params = {'test': 'abc'}

    expander_builder = TestExpanderBuilder().add_banner_template_with_text('test', 'test',
                                                                           test_url,
                                                                           test_params)
    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    formatted_href = test_expander.get_banners_preview(0, 0)[0].href

    for param in test_params:
        assert param in formatted_href
        assert test_params[param] in formatted_href


def test_href_no_unicode_error():
    test_url = "https://ya.ru/set/news/?utm_term=%D0%90%D0%BF%D0%B0%D1%82%D0%B8%D1%82%D0%BE%D0%B2&from=direct_rsy" \
               "a&utm_campaign=news_auto&utm_content=%7Bsource%7D&utm_source=yandex&utm_medium=rsya&y" \
               "clid=730884292162954&yclid=731735514476895"
    test_params = {'utm_term': u'(В Москве)',
                   'non_unicode_term': '(Москвы)',
                   'non_unicode_ascii': 'ascii'}

    expander_builder = TestExpanderBuilder().add_banner_template_with_text('test', 'test',
                                                                           test_url,
                                                                           test_params)
    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    href = test_expander.get_banners_preview(0, 0)[0].href
    assert 'ascii' in href


def test_empty_string_parameters_not_replaced():
    test_url = "https://ya.ru/set/news/?utm_term=%D0%90%D0%BF%D0%B0%D1%82%D0%B8%D1%82%D0%BE%D0%B2&from=direct_rsya&" \
               "utm_campaign=news_auto&utm_content=%7Bsource%7D&utm_source=yandex&utm_medium=rsya&" \
               "yclid=730884292162954&yclid=755753758596256"
    test_params = {'utm_campaign': ''}

    expander_builder = TestExpanderBuilder().add_banner_template_with_text('test', 'test',
                                                                           test_url,
                                                                           test_params)
    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    href = test_expander.get_banners_preview(0, 0)[0].href
    assert 'news_auto' in href


def test_non_ascii_params_in_unicode_url():
    test_url = u"https://avia.yandex.ru/search?toId=c146&utm_content=%7Bsource%7D&from=direct_rsya&fromId=c213&" \
               u"when=%D1%87%D0%B5%D1%80%D0%B5%D0%B7+%D0%BD%D0%B5%D0%B4%D0%B5%D0%BB%D1%8E"

    expander_builder = TestExpanderBuilder().add_banner_template_with_text('test', 'test',
                                                                           test_url,
                                                                           {})

    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    href = test_expander.get_banners_preview(0, 0)[0].href
    assert href


def test_non_ascii_term_correct():
    test_url = u"https://avia.yandex.ru/search"
    test_utm_term = u'Провiрка'

    expander_builder = TestExpanderBuilder().add_banner_template_with_text('test', 'test',
                                                                           test_url,
                                                                           {'utm_term': test_utm_term})

    expander_builder.add_region(TEST_REGION).add_phrase_group(1, 1)
    test_expander = expander_builder.build()

    href = test_expander.get_banners_preview(0, 0)[0].href
    parse_results = urlparse(href)

    query_dict = parse_qs(parse_results.query)
    assert test_utm_term.encode('utf-8') == query_dict['utm_term'][0]
