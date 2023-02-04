# coding: utf-8

import unittest
import json
from copy import deepcopy
import logging
import sys
from contextlib import contextmanager

from ads.quality.phf.phf_direct_loader.lib.app import make_app, db
from ads.quality.phf.phf_direct_loader.lib.extensions.database import ClientDBO, RegionDBO
from ads.quality.phf.phf_direct_loader.lib.modules.regions.controllers import CITY_REGION, DISTRICT_REGION
from ads.quality.phf.phf_direct_loader.lib.config import TestingConfig
from ads.quality.phf.phf_direct_loader.lib.campaign_generation import expander
from ads.quality.phf.phf_direct_loader.lib import logger as phf_logger
import ads.quality.phf.phf_direct_loader.lib.extensions.direct as direct_extension

TEST_CLIENT = {
    "name": "test",
    "direct_id": 8800,
    "client_login": 'some_login',
    "oauth_token": 'some_token',
    'metrika_counter': 88005553,
    'mobile_bid_modifier': 50,
}

TEST_TEMPLATE = {
    u'name': u'test_template',
    u'regions': [1],
    u'region_expand_type': u'По регионам',
    u'banner_templates': [{
        u'titles': [u'test title', u'test title 2'],
        u'texts': [u'test'],
        u'hrefs': [u'www.buy-everything.sale', u'www.buy-everything.sale/better_landing'],
        u'href_params': {
            u'utm_term': u'test',
            u'utm_source': u'test',
            u'utm_campaign': u'test',
            u'utm_medium': u'test'
        },
        u'uploaded_images': [{
            u'hash': u'test',
            u'url': u'test'}]
    }],
    u'phrase_groups': [{u'name': u'test', u'phrases': [u'p1', u'p2']}],
    u'bid': 100,
    u'country': u'Россия',
}

TEST_REGIONS = [
    {
        'name': u"Новосибирская область",
        'direct_id': 1,
        'nom_case': u"Новосибирская область",
        'loc_case': u"в Новосибирской области",
        'gen_case': u"Новосибирской области",
        'parent_direct_id': 0,
        'country': u'Россия',
        'region_type': DISTRICT_REGION,
        'is_capital': False
    },
    {
        'name': u"Новосибирск",
        'direct_id': 2,
        'nom_case': u"Новосибирск",
        'loc_case': u"в Новосибирске",
        'gen_case': u"Новосибирска",
        'parent_direct_id': 1,
        'country': u'Россия',
        'region_type': CITY_REGION,
        'is_capital': True
    },
    {
        'name': u"Бердск",
        'direct_id': 3,
        'nom_case': u"Бердск",
        'loc_case': u"в Бердске",
        'gen_case': u"Бердска",
        'parent_direct_id': 1,
        'country': u'Россия',
        'region_type': CITY_REGION,
        'is_capital': False
    },
    {
        'name': u"Иркутская область",
        'direct_id': 4,
        'nom_case': u"Иркутская область",
        'loc_case': u"в Иркутской области",
        'gen_case': u"Иркутской области",
        'parent_direct_id': 0,
        'country': u'Россия',
        'region_type': DISTRICT_REGION,
        'is_capital': False
    },
    {
        'name': u"Иркутск",
        'direct_id': 5,
        'nom_case': u"Иркутск",
        'loc_case': u"в Иркутске",
        'gen_case': u"Иркутска",
        'parent_direct_id': 4,
        'country': u'Россия',
        'region_type': CITY_REGION,
        'is_capital': True
    },
    {
        'name': u"Усолье-Сибирское",
        'direct_id': 6,
        'nom_case': u"Усолье-Сибирское",
        'loc_case': u"в Усолье-Сибирском",
        'gen_case': u"Усолья-Сибирского",
        'parent_direct_id': 4,
        'country': u'Россия',
        'region_type': CITY_REGION,
        'is_capital': False
    },
    {
        'name': u"Тьмутараканская область",
        'direct_id': 7,
        'nom_case': u"Тьмутараканская область",
        'loc_case': u"в Тьмутараканской области",
        'gen_case': u"Тьмутараканской области",
        'parent_direct_id': 0,
        'country': u'Беларусь',
        'region_type': DISTRICT_REGION,
        'is_capital': False
    },
    {
        'name': u"Такая-то область",
        'direct_id': 8,
        'nom_case': u"Такая-то область",
        'loc_case': u"в Такой-то области",
        'gen_case': u"Такой-то области",
        'parent_direct_id': 0,
        'country': u'Беларусь',
        'region_type': DISTRICT_REGION,
        'is_capital': False
    }
]

CORRECT_TEMPLATES_PATH = "clients/{}/campaign_templates".format(TEST_CLIENT['direct_id'])
NO_CLIENT_TEMPLATES_PATH = "clients/1233212/campaign_templates"

TEST_IMAGE_FILE = 'test_banner_image.jpg'

phf_logger.addHandler(logging.StreamHandler(sys.stderr))


def init_test_database():
    db.Base.metadata.reflect(bind=db.engine)
    db.Base.metadata.drop_all(bind=db.engine)

    db.init_db()
    db.db_session.add(ClientDBO(TEST_CLIENT['direct_id'], TEST_CLIENT['name'],
                                client_login=TEST_CLIENT['client_login'],
                                client_token=TEST_CLIENT['oauth_token']))

    for region in TEST_REGIONS:
        db.db_session.add(RegionDBO(**region))
    db.db_session.commit()


class BaseTestCase(unittest.TestCase):
    def setUp(self):
        self.client = make_app(TestingConfig).test_client()
        init_test_database()

    def check_error(self, resp, target_exception):
        self.assertEqual(json.loads(resp.data)['error_code'], target_exception.code)
        self.assertEqual(target_exception.name, json.loads(resp.data)['error_name'])

    def tearDown(self):
        db.db_session.remove()
        db.Base.metadata.reflect(bind=db.engine)
        db.Base.metadata.drop_all(bind=db.engine)

    def _get_test_template_id(self):
        res = self.client.post(CORRECT_TEMPLATES_PATH,
                               data=json.dumps(TEST_TEMPLATE),
                               content_type='application/json')
        return json.loads(res.data)['id']


def generate_numbered_list(word, count):
    return [word + '_{}'.format(i) for i in xrange(count)]


def generate_test_images(count):
    return [expander.UploadedImage(w, w) for w in generate_numbered_list('img', count)]


def generate_lettered_list(word, count):
    """
    Generates list of distinct phrases that acceptable and not shortened by Yandex.Direct
    """
    letters = u"щшюйцчяьъх"

    words = []
    for i in xrange(count):
        words.append(unicode(word) + u' ' + u''.join(letters[int(char)] for char in str(i)))
    return words


def generate_banner_template(title_cnt, text_cnt, image_cnt, href_cnt):
    titles = generate_numbered_list('title', title_cnt)
    texts = generate_numbered_list('text', text_cnt)
    images = generate_test_images(image_cnt)
    hrefs = generate_numbered_list('href', href_cnt)

    return expander.BannerTemplate(titles,
                                   texts,
                                   hrefs,
                                   href_params=None,
                                   uploaded_images=images)


def make_dummy_region(name, direct_id):
    return expander.RegionWithCases(name, {case.value: name for case in expander.GrammaticalCases}, direct_id)


class TestDirectLimits(object):
    def __init__(self):
        self.MAX_PHRASE_GROUP = 5
        self.MAX_BANNER_GROUP = 5
        self.MAX_GROUP_CAMPAIGN = 5

        self.MAX_BANNER_TITLE_LEN = 33
        self.MAX_BANNER_TEXT_LEN = 75


class TestExpanderBuilder(object):
    def __init__(self):
        self._banner_templates = []
        self._direct_limits = TestDirectLimits()
        self._template_name = 'name'
        self._regions = []
        self._phrase_groups = []

    def add_phrase_group(self, length, group_count=1):
        for i in xrange(group_count):
            self._phrase_groups.append(expander.PhraseGroup('group_{}'.format(len(self._phrase_groups)),
                                                            generate_lettered_list('phrase', length)))
        return self

    def add_custom_phrase_group(self, phrase_group):
        self._phrase_groups.append(phrase_group)
        return self

    def add_banner_template(self, title_cnt, text_cnt, image_cnt, href_cnt):
        self._banner_templates.append(generate_banner_template(title_cnt, text_cnt, image_cnt, href_cnt))
        return self

    def add_banner_template_with_text(self, title, text, href, href_params=None, uploaded_image=None):
        if uploaded_image is None:
            banner_template = expander.BannerTemplate([title], [text], [href], href_params)
        else:
            banner_template = expander.BannerTemplate([title], [text], [href], href_params, uploaded_image)
        self._banner_templates.append(banner_template)
        return self

    def add_dumb_banner_template(self):
        self.add_banner_template(1, 1, 1, 1)
        return self

    def set_regions(self, length):
        self._regions = generate_numbered_list('region', length)
        self._regions = [make_dummy_region(name, i)
                         for i, name in enumerate(self._regions)]

        return self

    def add_region(self, region):
        self._regions.append(region)
        return self

    def set_max_phrase(self, max_phrase):
        self._direct_limits.MAX_PHRASE_GROUP = max_phrase
        return self

    def set_max_banner(self, max_banner):
        self._direct_limits.MAX_BANNER_GROUP = max_banner
        return self

    def set_max_ad_group(self, max_ad_group):
        self._direct_limits.MAX_GROUP_CAMPAIGN = max_ad_group
        return self

    def build(self):
        template = expander.CampaignTemplate(self._template_name,
                                             deepcopy(self._banner_templates),
                                             deepcopy(self._phrase_groups))

        return expander.DirectCampaignTemplateExpander(template,
                                                       deepcopy(self._regions),
                                                       deepcopy(self._direct_limits))


class DirectApiMock(object):
    def __init__(self):
        self.campaigns = []
        self.keywords = []
        self.ad_groups = []
        self.banners = []

        self._id_counter = 0

    def _save_with_id(self, objects, storage_list):
        ids = []
        for o in objects:
            uploaded_o = deepcopy(o)
            uploaded_o['id'] = self._id_counter
            storage_list.append(uploaded_o)

            ids.append(self._id_counter)
            self._id_counter += 1
        return ids

    def add_campaigns_dict(self, camps):
        ids = self._save_with_id(camps, self.campaigns)
        return ids, [None for _ in ids]

    def get_campaign_ids(self):
        return [c['id'] for c in self.campaigns]

    def add_ads_dict(self, ads):
        ids = self._save_with_id(ads, self.banners)
        return ids, [None for _ in ids]

    def add_keywords_dict(self, keywords):
        ids = self._save_with_id(keywords, self.keywords)
        return ids, [None for _ in ids]

    def add_ad_groups(self, ad_groups):
        ids = self._save_with_id(ad_groups, self.ad_groups)
        return ids, [None for _ in ids]

    def config_for_client(self, client_login, oauth_token):
        pass

    def reset_client(self):
        pass

    def update_ads(self, ads):
        pass


@contextmanager
def fake_direct_api():
    old_api = direct_extension.direct_api
    mock_api = DirectApiMock()
    direct_extension.direct_api = mock_api
    yield mock_api
    direct_extension.direct_api = old_api
