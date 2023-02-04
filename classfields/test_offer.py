from django.test import SimpleTestCase as TestCase
from .vos_offer import ManualOffer


class TestOffer(TestCase):
    def test_is_new(self):
        for val in (False, True):
            off = ManualOffer(1, [{'name': 'isNew', 'value': val}], None, None, None, None, None, None, None, None,
                              None)
            self.assertEqual(off.is_new, val)
