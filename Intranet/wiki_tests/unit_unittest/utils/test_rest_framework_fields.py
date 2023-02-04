
from rest_framework.exceptions import ValidationError

from wiki.utils.rest_framework import fields
from intranet.wiki.tests.wiki_tests.common.fixture import FixtureMixin
from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase


class TagFieldTest(FixtureMixin, WikiDjangoTestCase):
    def test_clean_tag(self):
        field = fields.TagField()
        self.assertEqual(field.to_internal_value('/Тег/shmeg/'), 'Тег/shmeg')

    def test_invalid_tag(self):
        field = fields.TagField()
        with self.assertRaises(ValidationError):
            field.to_internal_value('')
            field.to_internal_value('&')

    def test_page_should_exist(self):
        field = fields.TagField(page_should_exist=True)
        self.create_page(supertag='vodstvo')

        with self.assertNotRaises(ValidationError):
            field.to_internal_value('Водство')

        with self.assertRaises(ValidationError):
            field.to_internal_value('ПлодоовощнойЖурнал')

    def test_page_shouldnt_exist(self):
        field = fields.TagField(page_should_exist=False)
        self.create_page(supertag='vodstvo')

        with self.assertNotRaises(ValidationError):
            field.to_internal_value('ПлодоовощнойЖурнал')

        with self.assertRaises(ValidationError):
            field.to_internal_value('Водство')
