import json
import mock
from django.test import TestCase

from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest, DatabaseFixturesMixin
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.tests.base import StatelessViewMixin


class ExperimentsTest(BasicAdvisorViewTest, TestCase):
    endpoint = '/api/v1/experiments/'
    default_params = {}

    def test_content(self):
        response = self.get()
        response_content = json.loads(response.content)
        self.assertGreater(response_content, 0)


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class ExperimentsTestV2(StatelessViewMixin, TestCase, DatabaseFixturesMixin):
    endpoint = '/api/v2/experiments/'
    default_params = {}

    @staticmethod
    def get_experiments(response):
        response_content = json.loads(response.content)
        return {item['name']: item for item in response_content}

    def test_content(self):
        response = self.get()
        response_content = json.loads(response.content)
        self.assertGreater(response_content, 0)

    def test_clid_targeting(self):
        self.set_localization_key(key='clid_targeted', value='wtf', user_info={'clids': {1: 12345}})
        experiments = self.get_experiments(response=self.get(HTTP_X_YACLID1='12345'))
        self.assertIn('clid_targeted', experiments)
        self.assertEqual(experiments['clid_targeted']['value'], 'wtf')

    def test_clid_targeting_existing_user(self):
        self.load_fixtures()
        try:
            uuid = self.client_model.uuid.hex
            self.set_localization_key(key='clid_targeted', value='wtf', user_info={'clids': {1: 2247990}})
            experiments = self.get_experiments(response=self.get(HTTP_X_YACLID1='2247990', HTTP_X_YAUUID=uuid))
            self.assertIn('clid_targeted', experiments)
            self.assertEqual(experiments['clid_targeted']['value'], 'wtf')
        finally:
            self.cleanup_fixtures()

    def test_key_translation(self):
        translation_key = 'backend_jafar_tab_name_1'
        self.set_localization_key(
            key='rec.feed.tab_name',
            value='''{
                      "value": {
                          "translate_key":"%s"
                      },
                      "report_to_appmetrika": true,
                      "report_to_appmetrika_environment": true
                    }''' % translation_key
        )
        self.set_translation_key(key=translation_key, value='Discover')
        experiments = self.get_experiments(response=self.get())
        self.assertEqual(experiments['rec.feed.tab_name']['value'], translation_key)
        self.assertEqual(experiments['rec.feed.tab_name.translated']['value'], 'Discover')
