from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, OfficeFactory, CityFactory
from staff.whistlah.tests.factories import StaffLastOfficeFactory
from staff.apicenter.tests.base import BaseApiTestCase


class ApiV1UserWhereTestCase(BaseApiTestCase):

    def prepare_fixtures(self):
        city = CityFactory(name='Kingston')
        self.office = OfficeFactory(name='Swamp81, Kingston', city=city)
        self.kolomeetz = StaffFactory(login='kolomeetz', office=self.office)
        self.thasonic = StaffFactory(login='thasonic', office=self.office)
        self.bolk = StaffFactory(login='bolk', office=self.office)
        StaffLastOfficeFactory(staff=self.kolomeetz, office=self.office, is_vpn=False)
        StaffLastOfficeFactory(staff=self.bolk, office=None, is_vpn=True)

    def test(self):
        # test json
        url = reverse(
            'api_v1_user_where',
            args=['kolomeetz|bolk|thasonic', 'json']
        )
        users = self.get_json(url)
        self.assertEqual(len(users), 2)
