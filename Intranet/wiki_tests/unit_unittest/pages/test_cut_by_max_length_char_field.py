
from wiki.pages.models.model_fields import CutByMaxLengthCharField
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class CutByMaxLengthCharFieldTestCase(BaseApiTestCase):
    def test_it_strips_in_clean(self):
        field = CutByMaxLengthCharField(max_length=6)

        self.assertEqual('txt', field.clean('txt', object()))
        self.assertEqual('txtxt', field.clean('txtxt', object()))
        self.assertEqual('Onegin', field.clean('Onegin, good friend of mine,', object()))
