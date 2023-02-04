from django.test import TestCase

from yaphone.advisor.advisor.models.profile import Locale


class LocaleParserTest(TestCase):
    def test_lang_only(self):
        self.assertEqual(Locale('ru').to_dict(), {'lang': 'ru'})

    def test_wrong_separator(self):
        self.assertNotEqual(Locale('ru-RU').to_dict(), {'lang': 'ru', 'territory': 'RU'})

    def test_lang_territory(self):
        self.assertEqual(Locale('ru_RU').to_dict(), {'lang': 'ru', 'territory': 'RU'})

    def test_all_extra_fields(self):
        self.assertEqual(str(Locale('en_AU.UTF-8@test')), 'en_AU.UTF-8@test')
