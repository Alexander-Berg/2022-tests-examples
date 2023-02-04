
from datetime import datetime
from unittest import skip, skipIf

from django.conf import settings
from django.test.utils import override_settings
from mock import Mock, patch

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class StatsTest(BaseApiTestCase):
    def setUp(self):
        super(StatsTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic
        self.url = '{api_url}/.stats'.format(api_url=self.api_url)

    @skip('temporary disabled after py3 migration, fix ASAP')
    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    @patch('requests.post')
    @patch('time.time', lambda: 42)
    @patch('wiki.utils.timezone.now', lambda: datetime(year=1042, month=4, day=2))
    @override_settings(SEND_STATS=True)
    @override_settings(SEND_STATS_TOKEN='Megasecret')
    @override_settings(YAUTH_REAL_IP_HEADERS=['HTTP_X_REAL_IP'])
    def test_ok(self, requests_post):
        requests_post.return_value = Mock(text='{"text": "Success"}')
        response = self.client.post(
            self.url,
            data={'some_key': 'some_data'},
            HTTP_USER_AGENT='spaceship',
            HTTP_REFERER='topor.com',
            HTTP_X_REAL_IP='22.33.44.55',
        )
        self.assertEqual(response.status_code, 200)
        requests_post.assert_called_with(
            'https://hatch.yandex.net/services/collector',
            data='{"event": '
            '{"status": "1042-04-02 00:00:00", "backend_timestamp": 42, "http_user_agent": "spaceship", '
            '"staffuid": "' + self.user.staff.uid + '", "some_key": "some_data", "user_ip": "22.33.44.55", '
            '"referer": "topor.com", "user": "thasonic"}}',
            headers={'Authorization': 'Splunk Megasecret'},
            timeout=10,
            verify=False,
        )

    def test_not_json(self):
        response = self.client.post(
            self.url, data={'some_key': 'some_data'}, content_type='application/x-www-form-urlencoded'
        )
        self.assertEqual(response.status_code, 400)

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_anonymous_user(self):
        self.client.logout()
        response = self.client.post(self.url, data={'some_key': 'some_data'})
        self.assertEqual(response.status_code, 404 if settings.IS_BUSINESS else 403)
