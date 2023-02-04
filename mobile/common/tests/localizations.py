from unittest import TestCase

from yaphone.advisor.common.localization_helpers import get_impersonal_config_value


def localizations_test_value(localizations_test_key):
    return '%s_value' % localizations_test_key


class LocalizationsTest(TestCase):
    def test_impersonal_config(self):
        key = 'test_id'
        config_value = get_impersonal_config_value(key, log_missing=False)
        self.assertEqual(config_value, localizations_test_value(key))

    def test_impersonal_config_default_value(self):
        config_value = get_impersonal_config_value('unknown_key', log_missing=False)
        self.assertIs(config_value, None)

    def test_impersonal_config_not_default_value(self):
        value = [{'test': 1}]  # something non standart
        config_value = get_impersonal_config_value('unknown_key', default_value=value, log_missing=False)
        self.assertEqual(config_value, value)
