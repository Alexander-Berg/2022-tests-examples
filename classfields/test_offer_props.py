import copy

from django.test import SimpleTestCase as TestCase
from .vos_offer import OfferProps

key_extractor = lambda e: e["key"]
key = 'keyOne'


class TestOfferProps(TestCase):
    @property
    def vos_props(self):
        return copy.deepcopy([{"key": "keyOne", "value": "valueOne"}, {"key": "keyTwo", "value": "valueTwo"}])

    def _are_equal(self, props, vos_props):
        self.assertSequenceEqual(
            sorted(vos_props, key=key_extractor),
            sorted(props, key=key_extractor),
        )

    def test_add(self):
        vos_props = self.vos_props
        props = OfferProps(vos_props)
        props["keyThree"] = "valueThree"
        self._are_equal(props.value, vos_props + [{"key": "keyThree", "value": "valueThree"}])

    def test_del(self):
        vos_props = self.vos_props
        props = OfferProps(vos_props)
        del props[key]
        self.del_key_from_vos_props(vos_props, key)
        self._are_equal(props.value, vos_props)

    def test_pop(self):
        vos_props = self.vos_props
        props = OfferProps(vos_props)
        props.pop(key)
        self.del_key_from_vos_props(vos_props, key)
        self._are_equal(props.value, vos_props)

    @staticmethod
    def del_key_from_vos_props(vos_props, key):
        for i in range(-1, -len(vos_props) - 1, -1):
            entry = vos_props[i]
            if entry['key'] == key:
                vos_props.pop(i)
