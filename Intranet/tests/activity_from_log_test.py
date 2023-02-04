from datetime import datetime

from django.test import TestCase
from django.core.cache import cache

from staff.lib.testing import StaffFactory, OfficeFactory

from .factories import OfficeNetFactory, StaffLastOfficeFactory
from ..utils import get_last_activity


# Вместо приведения времени
def localize_time(dt):
    from datetime import timedelta
    # если приводить через pytz - отнимется 2:30
    return dt - timedelta(hours=3, minutes=0)


class LastActivityTest(TestCase):

    def setUp(self):
        self.user1 = StaffFactory(login='user1', table=None)
        self.user2 = StaffFactory(login='user2', table=None)

        self.redrose = OfficeFactory(name='redrose', name_en='en_name')
        self.redrose_net = OfficeNetFactory(
            name='redrose',
            office=self.redrose,
            net='5.45.209.0/26',
            first_ip_value=86888704,
            last_ip_value=86888767,
        )
        self.vpn_net = OfficeNetFactory(
            name='VPN',
            office=None,
            net='6.46.209.0/26',
            first_ip_value=103731456,
            last_ip_value=103731519,
        )

    def tearDown(self):
        cache.clear()

    def test_activity_12none(self):
        expected_data = {
            self.user1.login: None,
            self.user2.login: None,
        }
        last_activity = get_last_activity([self.user1.login, self.user2.login])
        self.assertDictEqual(last_activity, expected_data)

    def test_activity_1none2_from_model(self):

        user2_last_office = StaffLastOfficeFactory(
            staff=self.user2,
            office=self.redrose,
            office_name=self.redrose.name,
            office_name_en=self.redrose.name_en,
            is_vpn=False,
        )
        time_format = '%Y-%m-%dT%H:%M:%S%f'
        expected_time = datetime.strptime(
            localize_time(user2_last_office.updated_at).strftime(time_format),
            time_format)
        expected_data = {
            self.user1.login: None,
            self.user2.login: {
                'updated_at': expected_time,
                'office_id': user2_last_office.office_id,
                'office_parts_codes': [user2_last_office.office.code],
                'name': user2_last_office.office.name,
                'name_en': user2_last_office.office.name_en,
                'is_vpn': user2_last_office.is_vpn,
            },
        }

        last_activity = get_last_activity([self.user1.login, self.user2.login])
        for login in last_activity:
            if last_activity[login]:
                last_activity[login]['updated_at'] = localize_time(
                    last_activity[login]['updated_at'])
        self.assertDictEqual(last_activity, expected_data)

    def test_activity_wrong_login(self):
        wrong_login = 'nobody'
        expected_data = {wrong_login: None}
        last_activity = get_last_activity([wrong_login])
        self.assertDictEqual(last_activity, expected_data)
