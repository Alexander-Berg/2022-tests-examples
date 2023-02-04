import logging
import mock
from copy import deepcopy
from uuid import UUID

from django.test import TestCase
from rest_framework.exceptions import ValidationError

from yaphone.advisor.advisor.models import check_mock
from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.lbs import DEFAULT_COUNTRY
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.advisor.serializers.device import LbsInfoSerializer
from yaphone.advisor.advisor.tests.profile import (ANDROID_CLIENT_REQUEST_DATA, SOME_UUID, SOME_DEVICE_ID,
                                                   SOME_CRYPTA_LOYALTY)
from yaphone.advisor.advisor.views.device import update_lbs, update_profile
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.common.mocks.test_client_data import (LBS_REQUEST_DATA, MOBILE_COUNTRY_CODE,
                                                           MOBILE_NETWORK_CODE, RUSSIAN_OPERATOR, UKRAINIAN_OPERATOR,
                                                           SOME_RUSSIAN_IP)

logger = logging.getLogger(__name__)

WIFI_LATITIDE, WIFI_LONGITUDE = 55.25, 37.37
GSM_LATITIDE, GSM_LONGITUDE = 55.58, 37.28
IP_LATITUDE, IP_LONGITUDE = 55.00, 37.00


def update_lbs_info(data, *args, **kwargs):
    serializer = LbsInfoSerializer(data=data)
    serializer.is_valid(raise_exception=True)
    validated_data = serializer.validated_data
    return update_lbs(validated_data, *args, **kwargs)


# noinspection PyMethodMayBeStatic,PyUnusedLocal
class LbsLocatorMock(object):
    def locate(self, gsm_cells=None, wifi_networks=None, ip=None, uuid=None):
        if wifi_networks is not None:
            return {
                "latitude": WIFI_LATITIDE,
                "longitude": WIFI_LONGITUDE,
                "altitude": 0.0,
                "precision": 0.0,
                "altitude_precision": 30.0,
                "type": "wifi"
            }
        if gsm_cells is not None:
            return {
                "latitude": GSM_LATITIDE,
                "longitude": GSM_LONGITUDE,
                "altitude": 1.0,
                "precision": 5.0,
                "altitude_precision": 130.0,
                "type": "gsm"
            }
        if ip is not None:
            return {
                "latitude": IP_LATITUDE,
                "longitude": IP_LONGITUDE,
                "altitude": 2.0,
                "precision": 10.0,
                "altitude_precision": 230.0,
                "type": "ip"
            }


def get_client(uuid):
    return Client.objects.get(uuid=uuid)


class LBSBase(TestCase):
    def check_location(self, location, lat, lon):
        self.assertAlmostEqual(location['latitude'], lat, 2)
        self.assertAlmostEqual(location['longitude'], lon, 2)

    @classmethod
    def setUpClass(cls):
        check_mock()

    @classmethod
    def tearDownClass(cls):
        pass

    def setUp(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), SOME_UUID)

    def tearDown(self):
        # delete created Client and Profiles
        Profile.objects(device_id=SOME_DEVICE_ID).delete()
        Client.objects(uuid=SOME_UUID).delete()


@mock.patch('yaphone.utils.geo.lbs_locator', new=LbsLocatorMock())
@mock.patch('yaphone.utils.geo.geobase_lookuper', new=LookupMock())
class LBSInfoTest(LBSBase):
    def test_update_lbs_all(self):
        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.check_location(mongodoc['location'], 55.123456, 37.123456)
        self.assertEqual(mongodoc['country'], 'RU')
        self.assertEqual(mongodoc['country_init'], 'RU')
        self.assertEqual(mongodoc['fix_country_init'], False)
        self.assertEqual(mongodoc['region_ids'], [162180, 98614, 1, 3, 225])
        self.assertEqual(mongodoc['region_types'], [-1, 10, 5, 4, 3])

    def test_data_not_lost(self):
        profile = get_client(SOME_UUID).profile
        profile.crypta.loyalty = SOME_CRYPTA_LOYALTY
        profile.save()

        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)

        profile = get_client(SOME_UUID).profile
        self.assertIn('display_metrics', profile.to_mongo()['android_info'])
        self.assertEqual(profile.crypta.loyalty, SOME_CRYPTA_LOYALTY)

    def test_update_without_location(self):
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.check_location(mongodoc['location'], WIFI_LATITIDE, WIFI_LONGITUDE)
        self.assertEqual(mongodoc['country'], 'RU')
        self.assertEqual(mongodoc['country_init'], 'RU')
        self.assertEqual(mongodoc['fix_country_init'], False)

    def test_update_without_wifi(self):
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        del data['wifi_networks']
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.check_location(mongodoc['location'], GSM_LATITIDE, GSM_LONGITUDE)
        self.assertEqual(mongodoc['country'], 'RU')
        self.assertEqual(mongodoc['country_init'], 'RU')
        self.assertEqual(mongodoc['fix_country_init'], False)

    def test_update_by_ip_with_previous_location_set(self):
        """
        Check case when we are updating location with only IP data.
        Location should not change
        """
        # update db with gps location
        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        # save old location
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        old_latitude = mongodoc['location']['latitude']
        old_longitude = mongodoc['location']['longitude']
        # prepare data, use only IP localization
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        del data['wifi_networks']
        del data['cells']
        # update db with IP-only data
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        # check that location is old
        self.check_location(mongodoc['location'], old_latitude, old_longitude)
        self.assertEqual(mongodoc['country'], 'RU')
        self.assertEqual(mongodoc['country_init'], 'RU')
        self.assertEqual(mongodoc['fix_country_init'], False)

    def test_update_without_cells(self):
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        del data['wifi_networks']
        del data['cells']
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country'], 'RU')
        self.assertEqual(mongodoc['fix_country_init'], False)
        self.assertFalse('location' in mongodoc)

    def test_update_without_ip(self):
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        del data['wifi_networks']
        del data['cells']
        update_lbs_info(data, UUID(SOME_UUID), '127.0.0.1')
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.assertNotIn('location', mongodoc)
        self.assertEqual(mongodoc['country'], DEFAULT_COUNTRY)
        self.assertEqual(mongodoc['fix_country_init'], True)

    def test_update_gps_loc_with_loc_by_ip(self):
        data = deepcopy(LBS_REQUEST_DATA)
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        gps_location = mongodoc['location']

        del data['location']
        del data['wifi_networks']
        del data['cells']
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['location'], gps_location)

    def test_invalid_location(self):
        data = deepcopy(LBS_REQUEST_DATA)
        data['location'] = {"longitude": 2000, "latitude": 0}
        self.assertRaises(ValidationError, update_lbs_info, data, UUID(SOME_UUID), SOME_RUSSIAN_IP)

    def test_country_init_with_fix(self):
        self.profile = get_client(SOME_UUID).profile
        self.profile.lbs_info.fix_country_init = True
        self.profile.lbs_info.country_init = 'US'
        self.profile.save()
        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country_init'], 'RU')

    def test_country_init_without_fix(self):
        self.profile = get_client(SOME_UUID).profile
        self.profile.lbs_info.fix_country_init = False
        self.profile.lbs_info.country_init = 'US'
        self.profile.save()
        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country_init'], 'US')

    def test_abhazia(self):
        data = deepcopy(LBS_REQUEST_DATA)
        data['location'] = {'latitude': 43.020748, 'longitude': 41.020341}  # Sunny Sukhumi
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country'], 'AB')

    def test_cells(self):
        update_lbs_info(LBS_REQUEST_DATA, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(len(mongodoc['cells']), 1)
        self.assertEqual(mongodoc['cells'][0]['mcc'], MOBILE_COUNTRY_CODE)
        self.assertEqual(mongodoc['cells'][0]['mnc'], MOBILE_NETWORK_CODE)

    def test_update_region_ids_with_only_ip(self):
        data = deepcopy(LBS_REQUEST_DATA)
        del data['location']
        del data['wifi_networks']
        del data['cells']
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        self.profile = get_client(SOME_UUID).profile
        mongodoc = self.profile.lbs_info.to_mongo()
        self.assertNotIn('location', mongodoc)
        self.assertEqual(mongodoc['region_ids'], [225, 98614, 1, 3, 225])
        self.assertEqual(mongodoc['region_types'], [3, 10, 5, 4, 3])


@mock.patch('yaphone.utils.geo.lbs_locator', new=LbsLocatorMock())
@mock.patch('yaphone.utils.geo.geobase_lookuper', new=LookupMock())
class LBSInfoCountryInitTest(LBSBase):
    def setUp(self):
        pass

    def test_country_init_same_country_operators(self):
        new_android_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_android_data['operators'] = [UKRAINIAN_OPERATOR, UKRAINIAN_OPERATOR]
        update_profile(new_android_data, SOME_UUID)
        data = deepcopy(LBS_REQUEST_DATA)
        self.profile = get_client(SOME_UUID).profile
        self.profile.lbs_info.fix_country_init = True
        self.profile.lbs_info.country_init = 'US'
        self.profile.save()
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country_init'], 'UA')

    def test_country_init_empty_country_iso(self):
        new_android_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        empty_operator = deepcopy(RUSSIAN_OPERATOR)
        empty_operator['country_iso'] = ''
        new_android_data['operators'] = [RUSSIAN_OPERATOR, empty_operator]
        update_profile(new_android_data, SOME_UUID)
        data = deepcopy(LBS_REQUEST_DATA)
        self.profile = get_client(SOME_UUID).profile
        self.profile.lbs_info.fix_country_init = True
        self.profile.lbs_info.country_init = 'US'
        self.profile.save()
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country_init'], 'RU')

    def test_country_init_different_countries_operators(self):
        new_android_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_android_data['operators'] = [RUSSIAN_OPERATOR, UKRAINIAN_OPERATOR]
        update_profile(new_android_data, SOME_UUID)
        data = deepcopy(LBS_REQUEST_DATA)
        data['location'] = {'latitude': 43.020748, 'longitude': 41.020341}  # location from Abkhazia (AB)
        self.profile = get_client(SOME_UUID).profile
        self.profile.lbs_info.fix_country_init = True
        self.profile.lbs_info.country_init = 'US'
        self.profile.save()
        update_lbs_info(data, UUID(SOME_UUID), SOME_RUSSIAN_IP)
        mongodoc = get_client(SOME_UUID).profile.lbs_info.to_mongo()
        self.assertEqual(mongodoc['country_init'], 'AB')
