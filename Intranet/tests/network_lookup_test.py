from django.test import TestCase

from staff.lib.testing import OfficeFactory
from staff.whistlah.utils import get_offices

from .factories import OfficeNetFactory


class NetworkLookupTest(TestCase):

    def setUp(self):
        self.redrose = OfficeFactory(name='redrose')
        self.redrose_net = OfficeNetFactory(
            name='redrose', office=self.redrose, net='5.45.209.0/26',
            first_ip_value=86888704, last_ip_value=86888767,
        )

        self.simf = OfficeFactory(name='simf')
        self.simf_net = OfficeNetFactory(
            name='simf',
            office=self.simf,
            net='2a02:6b8:0:21f::/64',
            first_ip_value=55838096689123070868459650444314017792,
            last_ip_value=55838096689123070886906394518023569407,
        )

    def tearDown(self):
        from django.core.cache import cache
        cache.clear()

    def test_successfull_ipv4_lookup(self):
        user_ipv4 = '5.45.209.1'

        office_id = get_offices(ips=[user_ipv4])[user_ipv4]

        self.assertEqual(office_id, self.redrose.id)

    def test_unsuccessfull_ipv4_lookup(self):
        user_ipv4 = '5.45.209.100'

        office_id = get_offices(ips=[user_ipv4])[user_ipv4]

        self.assertIsNone(office_id)

    def test_successfull_ipv6_lookup(self):
        user_ipv6 = '2a02:6b8:0:21f::100'

        office_id = get_offices(ips=[user_ipv6])[user_ipv6]

        self.assertEqual(office_id, self.simf.id)

    def test_unsuccessfull_ipv6_lookup(self):
        user_ipv6 = '2a02:6b8:0:21e::'

        office_id = get_offices(ips=[user_ipv6])[user_ipv6]

        self.assertIsNone(office_id)
