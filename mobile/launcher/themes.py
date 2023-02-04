import datetime
import mock
from django.test import TestCase

from yaphone.advisor.advisor.models import check_mock
from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.models import db
from yaphone.advisor.launcher.tests.base import StatelessViewMixin


# noinspection PyPep8Naming
class ThemesMixin(object):
    several_screens_theme = {
        u'id': u'dark',
        u'screens': {u'en': [u'theme-dark-en-1_home.png',
                             u'theme-dark-en-2_allapps.png',
                             u'theme-dark-en-3_shtorka.png',
                             u'theme-dark-en-4_weather_widget.png',
                             u'theme-dark-en-5_launcher_settings.png']},
        u'updated_at': datetime.datetime(2016, 12, 29, 15, 10, 41, 53000),
        u'device_type': [u'phone', u'tablet'],
    }
    phone_only_theme = {
        u'id': u'phone',
        u'title': u'Phone Theme',
        u'screens': {u'en': []},
        u'updated_at': datetime.datetime(2011, 7, 22, 13, 33, 33, 33333),
        u'device_type': u'phone',
    }
    tablet_only_theme = {
        u'id': u'tablet',
        u'title': u'iPad Like',
        u'screens': {u'en': []},
        u'updated_at': datetime.datetime(2011, 7, 22, 13, 33, 33, 33333),
        u'device_type': u'tablet',
    }

    def setUp(self):
        self.set_localization_key(
            'themes',
            '''
            {
              "value": {
                "themes": [
                  "dark"
                ]
              },
              "report_to_appmetrika_environment": false,
              "report_to_appmetrika": false,
            }
            '''
        )
        check_mock()
        db.themes_new.insert_many([
            self.several_screens_theme,
            self.phone_only_theme,
            self.tablet_only_theme
        ])
        super(ThemesMixin, self).setUp()

    def tearDown(self):
        check_mock()
        db.themes_new.delete_many({})
        super(ThemesMixin, self).tearDown()


@mock.patch('yaphone.advisor.common.resizer.make_key', lambda *args: '')
class ThemesTest(ThemesMixin, BasicAdvisorViewTest, TestCase):
    endpoint = '/api/v1/themes'

    default_params = {}


@mock.patch('yaphone.advisor.common.resizer.make_key', lambda *args: '')
@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class ThemesV2Test(ThemesMixin, StatelessViewMixin, TestCase):
    endpoint = '/api/v2/themes'

    default_params = {
        'screen_height': 1920,
        'screen_width': 1080,
        'dpi': 480,
    }

    def test_request_phone_theme(self):
        self.client = self.create_client('phone')
        self.assertThemeInList(self.get().data, ['dark', 'phone'])

    def test_request_tablet_theme(self):
        self.client = self.create_client('tablet')
        self.assertThemeInList(self.get().data, ['dark', 'tablet'])

    def test_default_request__same_as_phone(self):
        self.assertThemeInList(self.get().data, ['dark', 'phone'])

    def test_required_params(self):
        self.assertParameterRequired('screen_height')
        self.assertParameterRequired('screen_width')
        self.assertParameterRequired('dpi')

    # Custom asserts

    def assertThemeInList(self, request_data, themes):
        for entry in request_data:
            self.assertTrue(
                entry['title'] in themes,
                msg='unexpected title: %s' % entry['title']
            )
