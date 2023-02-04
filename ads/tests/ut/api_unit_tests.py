# coding: utf-8

import json
import os
from copy import deepcopy
from dateutil.parser import parse

import yatest


from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    BaseTestCase,
    CORRECT_TEMPLATES_PATH,
    NO_CLIENT_TEMPLATES_PATH,
    TEST_IMAGE_FILE,
    TEST_TEMPLATE
)

from ads.quality.phf.phf_direct_loader.lib.modules.templates.controllers.errors import (
    NoClientError,
    NoClientOrTemplateError
)

DATA_PATH = yatest.common.source_path('ads/quality/phf/phf_direct_loader/tests/resources')


class TestClientList(BaseTestCase):
    def test_200_on_get(self):
        self.assertEqual(self.client.get("clients").status_code, 200)


class TestCountryList(BaseTestCase):
    def test_200_on_get(self):
        self.assertEqual(self.client.get("regions/countries").status_code, 200)


class TestCampaignTemplateList(BaseTestCase):
    def test_200_on_post(self):
        res = self.client.post(CORRECT_TEMPLATES_PATH,
                               data=json.dumps(TEST_TEMPLATE),
                               content_type='application/json')
        assert res.status_code == 200

    def test_no_client_error_post(self):
        resp = self.client.post(NO_CLIENT_TEMPLATES_PATH,
                                data=json.dumps(TEST_TEMPLATE),
                                content_type='application/json')
        self.check_error(resp, NoClientError)

    def test_post_response_name_correct(self):
        res = self.client.post(CORRECT_TEMPLATES_PATH,
                               data=json.dumps(TEST_TEMPLATE),
                               content_type='application/json')
        self.assertEqual(TEST_TEMPLATE['name'], json.loads(res.data)['name'])

    def test_200_on_get(self):
        self.assertEqual(self.client.get(CORRECT_TEMPLATES_PATH).status_code, 200)

    def test_no_client_error_get(self):
        resp = self.client.get(NO_CLIENT_TEMPLATES_PATH)
        self.check_error(resp, NoClientError)

    def test_template_in_get_results_after_post(self):
        self.client.post(CORRECT_TEMPLATES_PATH,
                         data=json.dumps(TEST_TEMPLATE),
                         content_type='application/json')
        resp = self.client.get(CORRECT_TEMPLATES_PATH)
        resp_name = json.loads(resp.data)[-1]['name']

        self.assertEqual(TEST_TEMPLATE['name'], resp_name)

    def test_templates_sorted_by_time(self):
        for i in xrange(10):
            # test works correctly since time is measured and stored with maximal precision
            # in general one should mock datetime.now() or use other solution that allows to set arbitrary time
            self.client.post(CORRECT_TEMPLATES_PATH,
                             data=json.dumps(TEST_TEMPLATE),
                             content_type='application/json')
        resp = self.client.get(CORRECT_TEMPLATES_PATH)
        dates = [parse(r['creation_time']) for r in json.loads(resp.data)]

        for i in xrange(len(dates) - 1):
            assert dates[i] >= dates[i+1]


class TestCampaignList(BaseTestCase):
    def test_200_on_get(self):
        template_id = self._get_test_template_id()
        self.assertEqual(self.client.get(CORRECT_TEMPLATES_PATH + "/{}/campaigns".format(template_id)).status_code, 200)


class TestAdGroupList(BaseTestCase):
    def test_200_on_get(self):
        template_id = self._get_test_template_id()

        self.assertEqual(self.client.get(CORRECT_TEMPLATES_PATH +
                                         "/{}/campaigns/0/ad_groups".format(template_id)).status_code, 200)

    def test_no_client_or_template_error_get(self):
        resp = self.client.get(NO_CLIENT_TEMPLATES_PATH + "/1/campaigns/1/ad_groups")
        self.check_error(resp, NoClientOrTemplateError)


class TestBannerList(BaseTestCase):
    def test_200_on_get(self):
        template_id = self._get_test_template_id()
        self.assertEqual(self.client.get(CORRECT_TEMPLATES_PATH +
                                         "/{}/campaigns/0/ad_groups/0/banners".format(template_id)).status_code, 200)

    def test_no_client_or_template_error_get(self):
        resp = self.client.get(NO_CLIENT_TEMPLATES_PATH + "/1/campaigns/1/ad_groups/1/banners")
        self.check_error(resp, NoClientOrTemplateError)

    def test_banner_has_image_hashes(self):
        template_id = self._get_test_template_id()
        response_banners = self.client.get(CORRECT_TEMPLATES_PATH +
                                           "/{}/campaigns/0/ad_groups/0/banners".format(template_id)).data
        response_banners = json.loads(response_banners)
        assert response_banners

        expected_images = [h for b in TEST_TEMPLATE['banner_templates'] for h in b['uploaded_images']]
        for banner in response_banners:
            assert banner['uploaded_image'] in expected_images


class TestPhraseList(BaseTestCase):
    def test_200_on_get(self):
        template_id = self._get_test_template_id()
        self.assertEqual(self.client.get(CORRECT_TEMPLATES_PATH +
                                         "/{}/campaigns/0/ad_groups/0/phrases".format(template_id)).status_code, 200)

    def test_no_client_or_template_error_get(self):
        resp = self.client.get(NO_CLIENT_TEMPLATES_PATH + "/1/campaigns/1/ad_groups/1/phrases")
        self.check_error(resp, NoClientOrTemplateError)

    def test_phrase_list_correct(self):
        template_id = self._get_test_template_id()
        phrase_list_response = self.client.get(CORRECT_TEMPLATES_PATH +
                                               "/{}/campaigns/0/ad_groups/0/phrases".format(template_id))
        phrase_list = json.loads(phrase_list_response.data)

        assert set(phrase_list) == set(TEST_TEMPLATE['phrase_groups'][0]['phrases'])


class TestImageUpload(BaseTestCase):
    def test_no_client_error_post(self):
        with open(os.path.join(DATA_PATH, TEST_IMAGE_FILE)) as f:
            res = self.client.post('clients/100500/images',
                                   data={'image': (f, TEST_IMAGE_FILE)})

        self.check_error(res, NoClientError)


class TestTemplateController(BaseTestCase):
    def _put_template(self, template_id, template):
        return self.client.put(CORRECT_TEMPLATES_PATH + '/{}'.format(template_id),
                               data=json.dumps(template),
                               content_type='application/json')

    def test_200_on_put(self):
        template_id = self._get_test_template_id()
        res = self._put_template(template_id, TEST_TEMPLATE)

        self.assertEqual(res.status_code, 200)

    def test_same_template_id_in_response(self):
        template_id = self._get_test_template_id()
        res = self._put_template(template_id, TEST_TEMPLATE)

        res = json.loads(res.data)
        self.assertEqual(res['id'], template_id)

    def test_template_name_changed_on_put(self):
        template_id = self._get_test_template_id()
        mod_template = deepcopy(TEST_TEMPLATE)
        mod_template['name'] = 'Brave new name'

        res = self._put_template(template_id, mod_template)
        res = json.loads(res.data)

        self.assertEqual(res['name'], mod_template['name'])

    def test_template_modified_on_put(self):
        template_id = self._get_test_template_id()
        test_title = 'Brave new title'

        mod_template = deepcopy(TEST_TEMPLATE)
        mod_template['banner_templates'][0]['titles'].append(test_title)

        self._put_template(template_id, mod_template)
        banners = self.client.get(CORRECT_TEMPLATES_PATH +
                                  "/{}/campaigns/0/ad_groups/0/banners".format(template_id))

        banners = json.loads(banners.data)

        self.assertIn(test_title, {b['title'] for b in banners})

    def test_same_template_on_get(self):
        template_id = self._get_test_template_id()
        response_template = self.client.get(CORRECT_TEMPLATES_PATH +
                                            "/{}".format(template_id))

        response_template = json.loads(response_template.data)
        expected_template = json.loads(json.dumps(TEST_TEMPLATE))

        self.assertEqual(response_template, expected_template)


class TestValidationEndpoint(BaseTestCase):
    def test_200_on_post(self):
        template_id = self._get_test_template_id()
        self.assertEqual(self.client.post(CORRECT_TEMPLATES_PATH +
                         "/{}/validate_banners".format(template_id)).status_code,
                         200)
