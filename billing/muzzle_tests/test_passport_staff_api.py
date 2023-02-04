# -*- coding: utf-8 -*-
import pytest
import mock

UID = 1000


@pytest.mark.parametrize(
    'staff_data',
    [
        {
            'yandex_ru': 1234567,
            'betatest': 1,
            'yastaff': 1
        },
        {
            'yandex_ru': 1234567,
            'betatest': '',
            'yastaff': ''
        }
    ]
)
def test_get_is_staff(muzzle_logic, staff_data):
    with mock.patch('butils.passport.PassportBlackbox.get_passport_info_by_uid', return_value=staff_data):
        res = muzzle_logic.get_is_staff(UID)
        assert res.findtext('uid') == str(UID)
        assert res.find('sid').findtext('yandex-ru') == staff_data['yandex_ru']
        assert res.find('sid').findtext('betatest') == staff_data['betatest']
        assert res.find('sid').findtext('yastaff') == staff_data['yastaff']


def test_get_is_staff_error(muzzle_logic):
    with mock.patch('butils.passport.PassportBlackbox.get_passport_info_by_uid') as get_passport_info_by_uid:
        get_passport_info_by_uid.side_effect = Exception('Some error happened')
        res = muzzle_logic.get_is_staff(UID)
        assert res.find('sid').findtext('yastaff') is None
