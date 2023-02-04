
from django.test.utils import override_settings

from wiki.actions.classes.base_action import ParamsWrapper
from wiki.actions.classes.forms import Forms
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import HttpOldActionTestCase


class FormsTest(HttpOldActionTestCase):
    @override_settings(FORMS_URL_PATTERN='/cucumber/forms/{id}/?iframe=1')
    def test_override_forms_url(self):
        form_json = Forms(ParamsWrapper(dict={'id': '777'}), request=self.request).render()
        self.assertTrue('path' in form_json, '\'path\' must be in json')
        self.assertEqual('/cucumber/forms/777/?iframe=1', form_json['path'])
        self.assertTrue('name' in form_json, '\'name\' must be in json')
        self.assertTrue(form_json['name'].startswith('forms-777-'), '\'name\' must start with \'forms-777-\'')
