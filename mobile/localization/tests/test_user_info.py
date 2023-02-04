from __future__ import division

import unittest
from datetime import datetime, timedelta
from distutils.version import LooseVersion

from mock import patch

from yaphone.localization.user_info import UserInfo, version_matches_range
from yaphone.localization.local_items import Application, Model, VersionRange
from yaphone.localization import LocalizationUser, UserSpecificConfig, in_serialized_list


class FakeUUID(object):
    def __init__(self, hex_str):
        self.hex = hex_str


class TestUserInfo(unittest.TestCase):

    def setUp(self):
        local_user = LocalizationUser()
        config = UserSpecificConfig(local_user, project='test')
        self.user = config.get_user_info()
        self.model_none = Model(name=None, vendor=None)
        self.model_no_name = Model(name=None, vendor='1_vendor')
        self.model_no_vendor = Model(name='2_name', vendor=None)
        self.model_both = Model(name='3_name', vendor='3_vendor')
        self.model_both_2 = Model(name='YNDX-000SB'.lower(), vendor='Yandex'.lower())

    def test_locale_part_matches_condition_part(self):
        assert UserInfo.locale_part_matches_condition_part('RU', '*')
        assert UserInfo.locale_part_matches_condition_part(None, '*')
        assert UserInfo.locale_part_matches_condition_part('abc', '*')
        assert UserInfo.locale_part_matches_condition_part('aa', 'aa')
        assert UserInfo.locale_part_matches_condition_part('aa', '!bb')

    def test_locale_part_does_not_match_condition_part(self):
        assert not UserInfo.locale_part_matches_condition_part(None, 'RU')
        assert not UserInfo.locale_part_matches_condition_part(None, 'abc')
        assert not UserInfo.locale_part_matches_condition_part(None, '*a')
        assert not UserInfo.locale_part_matches_condition_part('aa', 'bb')
        assert not UserInfo.locale_part_matches_condition_part('aa', '!aa')

    def test_is_geo_targeting_succeeded(self):

        assert UserInfo.is_geo_targeting_succeeded(set(), set(), set())
        assert UserInfo.is_geo_targeting_succeeded({1, 2, 3}, set(), set())

        assert not UserInfo.is_geo_targeting_succeeded(set(), {1}, set())
        assert not UserInfo.is_geo_targeting_succeeded(set(), set(), {2})

        assert not UserInfo.is_geo_targeting_succeeded({1, 2}, {1}, {2})

        assert UserInfo.is_geo_targeting_succeeded({1, 2}, {1}, {3})
        assert UserInfo.is_geo_targeting_succeeded({1, 2}, {1}, set())
        assert not UserInfo.is_geo_targeting_succeeded({1, 2}, set(), {2})
        assert not UserInfo.is_geo_targeting_succeeded({2}, {2}, {2})
        assert UserInfo.is_geo_targeting_succeeded({1, 2}, {3, 4, 1}, set())
        assert not UserInfo.is_geo_targeting_succeeded({1, 2}, set(), {5, 6, 2})

    def test_app_version_matches_condition_none(self):
        app_version = None
        assert version_matches_range(app_version, None)
        assert version_matches_range(app_version, VersionRange(low=None, high=None))
        assert not version_matches_range(app_version, VersionRange(low=None, high=u'1.2'))

    def test_app_version_matches_condition(self):
        app_version = u'1.delta'
        assert version_matches_range(app_version, VersionRange(low=None, high=u'1.2'))
        assert version_matches_range(app_version, VersionRange(low=u'1.czzzz', high=None))
        assert version_matches_range(app_version, VersionRange(low=u'0-9999', high=u'1.eaaaaa'))
        assert version_matches_range(app_version, VersionRange(low=u'z.9999', high=u'2-aaa'))
        assert version_matches_range(app_version, VersionRange(low=u'1.delta', high=u'1.delta'))
        assert version_matches_range(app_version, VersionRange(low=u'1.delta-alpha', high=u'1.delta.1'))

    def test_version_matches_condition_realdata(self):
        app_version = u'2.2.6-beta-feedbackLogs.7003615'
        assert version_matches_range(app_version, VersionRange(low=u'2.0.0', high=None))
        assert not version_matches_range(app_version, VersionRange(low=u'2.3.0', high=u'2.3.0'))
        assert version_matches_range(app_version, VersionRange(low=u'2.2-6.beta.feedbackLogs', high=u'2.2.6-beta'))

    @patch('yaphone.localization.user_info.version_matches_range', lambda *args, **kwargs: True)
    def test_application_matches_condition(self):
        assert self.user.application_matches_condition([])
        assert not self.user.application_matches_condition([None])
        user = UserInfo(app_name=u'name1', app_version=u'test.test')
        assert user.application_matches_condition([Application(aliases=['name1'], version=None)])
        assert not user.application_matches_condition([Application(aliases=['name2', 'name3'], version=None)])
        assert user.application_matches_condition([Application(aliases=['name2', 'name1', 'name3'], version=None)])
        assert not user.application_matches_condition([
            Application(aliases=['name2'], version=None),
            Application(aliases=['name3'], version=None),
        ])
        assert user.application_matches_condition([
            Application(aliases=['name2'], version=None),
            Application(aliases=['name1'], version=None),
        ])

    def test_uuid_matches_condition(self):
        assert not self.user.uuid_matches_condition({u'a', u'b'})
        assert self.user.uuid_matches_condition(set())
        user = UserInfo(uuid=FakeUUID(u'a'))
        assert user.uuid_matches_condition({u'a', u'b'})
        assert user.uuid_matches_condition(set())
        assert not user.uuid_matches_condition({u'b', u'c'})
        assert user.uuid_matches_condition({u'A', u'c'})

    def test_time_matches_condition(self):
        assert not UserInfo.time_matches_condition(datetime.min, datetime.utcnow())
        assert not UserInfo.time_matches_condition(datetime.utcnow() + timedelta(minutes=1), datetime.max)
        assert UserInfo.time_matches_condition(datetime.min, datetime.utcnow() + timedelta(minutes=1))
        assert UserInfo.time_matches_condition(datetime.utcnow() - timedelta(minutes=1), datetime.utcnow() + timedelta(minutes=1))

    @patch('yaphone.localization.user_info.compute_ratio', lambda uuid: int(uuid, 16) / 255)
    @patch('yaphone.localization.user_info.compute_salted_ratio', lambda uuid, salt: int(salt + uuid, 16) / 255)
    def test_audience_ratio_matches_condition(self):
        assert self.user.audience_ratio_matches_condition(1., 0., None)
        assert not self.user.audience_ratio_matches_condition(0.5, 0., None)
        user = UserInfo(uuid=FakeUUID(u'7F'))
        assert user.audience_ratio_matches_condition(0.5, 0, None)
        assert user.audience_ratio_matches_condition(0.1, 0.45, None)
        assert not user.audience_ratio_matches_condition(0.49, 0, None)

    @patch('yaphone.localization.user_info.compute_ratio', lambda uuid: int(uuid, 16) / 255)
    @patch('yaphone.localization.user_info.compute_salted_ratio', lambda uuid, salt: int(salt + uuid, 16) / 255)
    def test_salted_audience_ratio_matches_condition(self):
        user = UserInfo(uuid=FakeUUID(u'F'))
        assert user.audience_ratio_matches_condition(0.5, 0, u'7')
        assert user.audience_ratio_matches_condition(0.1, 0.45, u'7')
        assert not user.audience_ratio_matches_condition(0.49, 0, u'7')

    def test_device_type_matches_condition(self):
        assert self.user.device_type_matches_condition([])
        assert not self.user.device_type_matches_condition(['a'])
        user = UserInfo(device_type='type1')
        assert user.device_type_matches_condition([])
        assert not user.device_type_matches_condition(['type2', 'type3'])
        assert user.device_type_matches_condition(['type2', 'type3', 'type1'])

    def test_model_matches_condition_none(self):
        assert self.user.model_matches_condition(None)
        assert self.user.model_matches_condition([self.model_none])

    def test_model_matches_condition_no_name(self):
        user = UserInfo(device_vendor=self.model_both_2.vendor, device_model='NONEXSISTENT')
        assert user.model_matches_condition(None)
        assert user.model_matches_condition([self.model_none])
        assert user.model_matches_condition([self.model_both, Model(name=None, vendor=self.model_both_2.vendor)])
        assert not user.model_matches_condition([self.model_no_vendor, self.model_no_name, self.model_both_2])

    def test_model_matches_condition(self):
        user = UserInfo(device_vendor=self.model_both_2.vendor, device_model=self.model_both_2.name)
        assert user.model_matches_condition(None)
        assert user.model_matches_condition([self.model_none])
        assert user.model_matches_condition([self.model_both, self.model_both_2])
        assert not user.model_matches_condition([self.model_both, self.model_no_name, self.model_no_vendor])

    def test_clids_matches_condition(self):
        assert self.user.clids_matches_condition({})
        assert not self.user.clids_matches_condition({1: set()})
        user = UserInfo(clids={1: 2, 3: 4})
        assert user.clids_matches_condition({})
        assert user.clids_matches_condition({1: {2, 5}})
        assert user.clids_matches_condition({1: {2, 5}, 3: {4, 6}})
        assert not user.clids_matches_condition({1: {2, 5}, 3: {5, 6}})
        assert not user.clids_matches_condition({1: {2, 5}, 13: {5, 6}})

    def test_extended_params_matches_condition_none(self):
        assert self.user.extended_params_matches_condition({})
        assert not self.user.extended_params_matches_condition({'a': None})

    def test_extended_params_matches_condition(self):
        self.user.setExtendedParam('a', 'a', lambda u, v: u == v)
        self.user.setExtendedParam('b', 'bob', lambda u, v: u[0] == v[0])
        assert self.user.extended_params_matches_condition({'a': 'a', 'b': 'bar'})
        assert self.user.extended_params_matches_condition({'a': 'a'})
        assert self.user.extended_params_matches_condition({'b': 'bar'})
        assert not self.user.extended_params_matches_condition({'a': 'A'})
        assert not self.user.extended_params_matches_condition({'A': 'a'})
        assert not self.user.extended_params_matches_condition({'a': 'a', 'b': 'bar', 'c': 'carol'})

    def test_extended_params_matches_condition_os_version(self):
        self.user.setExtendedParam(
            'min_os_version', '8.1.0.go',
            lambda user_value, config_value: LooseVersion(user_value) >= LooseVersion(config_value)
        )
        self.user.setExtendedParam(
            'max_os_version', '8.1.0.go',
            lambda user_value, config_value: LooseVersion(user_value) <= LooseVersion(config_value)
        )
        assert self.user.extended_params_matches_condition({'min_os_version': '7.1.0', 'max_os_version': '8.1.1'})
        assert not self.user.extended_params_matches_condition({'min_os_version': '8.3'})
        assert not self.user.extended_params_matches_condition({'max_os_version': '8.0.go'})

    def test_extended_params_matches_condition_mac(self):
        self.user.setExtendedParam(
            'mac_address_wifi', 'ac:de:48:00:11:22',
            in_serialized_list
        )
        assert self.user.extended_params_matches_condition({'mac_address_wifi': 'ac:de:48:00:11:22'})
        assert not self.user.extended_params_matches_condition({'mac_address_wifi': 'ac:de:48:00:11:33'})

    def test_version_matches_range(self):
        version_range = VersionRange(low=u'1', high=u'10')
        assert not version_matches_range(None, version_range)
        assert version_matches_range(u'9.2', version_range)
        assert not version_matches_range(u'0.10', version_range)
        assert not version_matches_range(u'10.7', version_range)

        version_range = VersionRange(low=u'1', high=None)
        assert not version_matches_range(None, version_range)
        assert not version_matches_range(u'0.10', version_range)
        assert version_matches_range(u'10.7', version_range)

        version_range = VersionRange(low=None, high=u'100')
        assert not version_matches_range(None, version_range)
        assert version_matches_range(u'0.10', version_range)
        assert not version_matches_range(u'100.7', version_range)

    def test_os_version_matches_condition(self):
        os_version_range = VersionRange(low=u'5.0', high=u'7.1.5')
        build_version_range = VersionRange(low=u'0', high=u'300')
        assert not version_matches_range(self.user.os_version, os_version_range)
        user = UserInfo(os_version=u'7.1.1', build_version=u'230')
        assert version_matches_range(user.os_version, os_version_range)
        assert version_matches_range(user.build_version, build_version_range)

        user = UserInfo(os_version=u'4.5', build_version=u'330')
        assert not version_matches_range(user.os_version, os_version_range)
        assert not version_matches_range(user.build_version, build_version_range)

    def test_build_version_in_list(self):
        assert not self.user.build_version_in_list(['1'])
        user = UserInfo(os_version='4.5', build_version='230')
        assert user.build_version_in_list([])
        assert user.build_version_in_list(['1', '230'])
        assert not user.build_version_in_list(['nonexistent.build.number'])
