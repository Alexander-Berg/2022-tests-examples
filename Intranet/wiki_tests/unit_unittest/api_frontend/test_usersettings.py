import ujson as json
from django.conf import settings
from django.test import override_settings
from mock import patch

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

patched_module = 'wiki.api_frontend.views.usersettings.'

TEST_DATA = {
    'code_theme': 'dark',
    'use_nodejs_frontend': True,
    'use_new_wf': True,
}


def fake_get_user_settings(*args, **kwargs):
    return TEST_DATA


class APISettingsViewTest(BaseApiTestCase):
    def setUp(self):
        super(APISettingsViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    @patch(patched_module + 'settings_logic.get_user_settings', fake_get_user_settings)
    def test_get_settings(self):
        url = '{api_url}/.settings'.format(api_url=self.api_url)

        response = self.client.get(url)

        self.assertEqual(200, response.status_code)
        data = json.loads(response.content)['data']

        self.assertEqual(data, TEST_DATA)

    @patch(patched_module + 'settings_logic.update_user_settings')
    def test_post_one_setting(self, update_user_settings_mock):
        url = '{api_url}/.settings'.format(api_url=self.api_url)

        response = self.client.post(url, data={'code_theme': 'github'})

        self.assertEqual(200, response.status_code)
        self.assertEqual(update_user_settings_mock.call_count, 1)

    @override_settings(AUTH_TEST_MECHANISM='tvm')
    @patch(patched_module + 'staff.change_user_language')
    def test_post_language(self, change_user_language_mock):
        url = '{api_url}/.settings'.format(api_url=self.api_url)

        response = self.client.post(
            url,
            data={
                'language': 'ru',
            },
        )

        self.assertEqual(200, response.status_code)
        self.assertEqual(change_user_language_mock.call_count, 1 if settings.IS_INTRANET else 0)
