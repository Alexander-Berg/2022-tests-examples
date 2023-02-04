from mock import patch

from django.test import TestCase

from staff.lib.testing import StaffFactory
from staff.person_profile.controllers.ware import Software, Hardware, BotWareLoader

DATA = {
    'model': 'model',
    'group': 'group',
    'sub_group': 'sub_group',
    'instance_number': 'instance_number',
    'code': 'code',
    'name': 'name',
    'producer': 'producer',
    'type': 'SOFT'
}
EXPECTED_DATA = {
    'model': 'Sub_group Model',
    'group': 'Group',
    'sub_group': 'Sub_group',
    'code': 'instance_number',
    'name': 'Model',
    'producer': 'sub_group'
}


class SoftWareTester(TestCase):

    def test_software_with_data(self):
        s = Software(DATA)
        self.assertEqual(s.name, EXPECTED_DATA['name'])
        self.assertEqual(s.producer, EXPECTED_DATA['producer'])

    def test_software_without_data(self):
        data = {
            'NO_model': 1,
            'NO_sub_group': 3,
        }

        s = Software(data)
        self.assertEqual(s.name, '')
        self.assertEqual(s.producer, '')

    def test_software_with_bad_data(self):
        data = {
            'model': None,
            'sub_group': None,
        }

        s = Software(data)
        self.assertEqual(s.name, '')
        self.assertEqual(s.producer, '')


class HardWareTester(TestCase):

    def test_hardware_with_data(self):
        h = Hardware(DATA)
        self.assertEqual(h.group, EXPECTED_DATA['group'])
        self.assertEqual(h.code, EXPECTED_DATA['code'])

    def test_hardware_without_data(self):
        data = {
            'NO_model': 1,
            'NO_group': 2,
            'NO_sub_group': 3,
            'NO_instance_number': 4,
        }

        h = Hardware(data)
        self.assertEqual(h.group, '')
        self.assertEqual(h.code, '')
        self.assertEqual(h.model, '')

    def test_hardware_with_bad_data(self):
        data = {
            'model': None,
            'group': None,
            'sub_group': None,
            'instance_number': None,
        }

        h = Hardware(data)
        self.assertEqual(h.group, '')
        self.assertEqual(h.code, '')
        self.assertEqual(h.model, '')


class BotWareLoaderTester(TestCase):

    def setUp(self):
        self.test_user = StaffFactory(login='test_login')

    class _MockHttpResponseClass():
        _json = []

        def json(self):
            return self._json

        def __init__(self, wares=list()):
            self._json = wares

        def get_mock(self, arg1, params=None, timeout=None):
            return self

    def check_bw(self, data):

        bwl = BotWareLoader(self.test_user.login)
        m = self._MockHttpResponseClass(data)
        with patch('requests.get', side_effect=m.get_mock):
            self.assertEqual(data, bwl.load())
            for ware in data:
                if bwl.is_software(ware):
                    self.assertIn(
                        str(Software(ware)),
                        [str(s) for s in bwl.software]
                    )
                else:
                    self.assertIn(
                        str(Hardware(ware)),
                        [str(s) for s in bwl.hardware]
                    )

    def test_botwareloader_on_correct_data(self):
        self.check_bw([DATA])
        self.check_bw([])

        d1, d2 = DATA.copy(), DATA.copy()
        d2['type'] = 'HARD'
        self.check_bw([d1, d2])

    def test_botwareloader_on_invalid_data(self):
        bw_loader = BotWareLoader(self.test_user.login)

        m = self._MockHttpResponseClass('not list')
        with patch('requests.get', side_effect=m.get_mock):
            self.assertEqual([], bw_loader.load())

        m = self._MockHttpResponseClass(123)
        with patch('requests.get', side_effect=m.get_mock):
            self.assertEqual([], bw_loader.load())

        m = self._MockHttpResponseClass(['wrong', 'list'])
        with patch('requests.get', side_effect=m.get_mock):
            self.assertEqual([], bw_loader.software)
            self.assertEqual([], bw_loader.hardware)
            self.assertEqual([], list(bw_loader))
