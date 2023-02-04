
from mock import patch
from pretend import stub

from wiki.users.logic import settings
from wiki.utils.errors import ValidationError
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


@patch(
    'wiki.users.logic.settings.SETTINGS_WITH_DEFAULTS',
    {
        'code_theme': 'idea',
        'use_nodejs_frontend': True,
        'use_new_wf': True,
    },
)
class SettingsLogicTest(BaseApiTestCase):
    def _get_user(self, **user_settings):
        return stub(
            profile=user_settings,
            save=lambda: None,
        )

    def test_get_user_setting_unknown(self):
        user = self._get_user()
        with self.assertRaises(ValidationError):
            settings.get_user_setting(user, 'unknown')

    def test_get_user_setting_default(self):
        user = self._get_user()
        value = settings.get_user_setting(user, 'code_theme')
        self.assertEqual(value, 'idea')

    def test_get_user_setting_from_user(self):
        user = self._get_user(code_theme='github')
        value = settings.get_user_setting(user, 'code_theme')
        self.assertEqual(value, 'github')

    def test_get_user_settings(self):
        user = self._get_user(use_nodejs_frontend=False, use_new_wf=False)
        user_settings = settings.get_user_settings(user)

        self.assertEqual(
            user_settings,
            {
                'code_theme': 'idea',
                'use_nodejs_frontend': False,
                'use_new_wf': False,
            },
        )

    def test_set_user_setting_unknown(self):
        user = self._get_user()
        with self.assertRaises(ValidationError):
            settings.set_user_setting(user, 'unknown', 'something')

    def test_set_user_setting_known(self):
        user = self._get_user()
        settings.set_user_setting(user, 'code_theme', 'github')
        self.assertEqual(user.profile['code_theme'], 'github')

    def test_update_user_setting_unknown(self):
        user = self._get_user()
        data = {'unknown': 'something', 'code_theme': 'github'}
        with self.assertRaises(ValidationError):
            settings.update_user_settings(user, data)

    def test_update_user_settings_one(self):
        user = self._get_user(code_theme='dark')

        settings.update_user_settings(user, {'code_theme': 'github'})

        self.assertEqual(user.profile['code_theme'], 'github')

    def test_update_user_settings_all(self):
        user = self._get_user(code_theme='dark', use_nodejs_frontend=False, use_new_wf=False)

        data = {'code_theme': 'github', 'use_nodejs_frontend': True, 'use_new_wf': True}
        settings.update_user_settings(user, data)

        self.assertEqual(user.profile, data)
