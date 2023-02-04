import json
import mock
import requests_mock
from copy import deepcopy
from datetime import datetime
from django.conf import settings
from django.test import Client as DjangoClient, TestCase
from rest_framework import status
from uuid import uuid4, UUID

from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.crypta import _get_crypta_info
from yaphone.advisor.advisor.models.lbs import Location, DEFAULT_COUNTRY
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest, HTTP_418_CLIENT_INFO_REQUIRED
from yaphone.advisor.common.mocks.geobase import LookupMock


class DeviceViewTest(BasicAdvisorViewTest):
    default_data = None

    def post(self, data=None, **kwargs):
        return self.client.post(
            path=self.endpoint,
            data=json.dumps(data or self.default_data),
            content_type='application/json',
            **kwargs
        )

    def reload_client_model(self):
        self.client_model = Client.objects.get(uuid=self.client_model.uuid)

    def test_ok(self):
        response = self.post()
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_uuid_is_required(self):
        response = self.post(HTTP_X_YAUUID=None)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_badly_formed_uuid(self):
        response = self.post(HTTP_X_YAUUID='some_string_that_is_not_uuid')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_unknown_uuid(self):
        response = self.post(HTTP_X_YAUUID=uuid4().hex)
        self.assertEqual(response.status_code, HTTP_418_CLIENT_INFO_REQUIRED)


@mock.patch('yaphone.advisor.common.updates_manager.UpdatesManager._reinit_required',
            lambda self: True)  # force cache reinit
class AndroidClientInfoTest(DeviceViewTest, TestCase):
    endpoint = '/api/v1/android_client_info/'
    uuid = '123456789012345678901234567890ab'
    default_data = {
        'os_build': {
            'string_fields': [
                {'key': 'VERSION.SDK_INT', 'value': '27'},
                {'key': 'BOARD', 'value': 'GOLDFISH'}
            ],
            'version': {
                'codename': 'cdname',
                'incremental': 'incrm',
                'release': 'rls',
                'sdk_int': 22
            }
        },
        'display_metrics': {
            'width_pixels': 720,
            'height_pixels': 1184,
            'density': 2.0,
            'density_dpi': 320,
            'scaled_density': 2,
            'xdpi': 315.31033,
            'ydpi': 318.7451
        },
        'user_settings': {
            'locale': 'ru_RU'
        },
        'gl_es_version': 128,
        'gl_extensions': ['extension1', 'extension2'],
        'features': ['feature1', 'feature2'],
        'shared_libraries': ['library1', 'library2'],
        'device_id': '1111111111111111111111111111111a',
        'ad_id': '96bd03b6-defc-4203-83d3-dc1c730801f7',
        'android_id': '1af1af1af1af1af1',
        'rec_views_config': {
            'feed_view': [
                {
                    "card_type": "Scrollable",
                    "count": 16
                },
                {
                    "card_type": "Single",
                    "count": 3
                }
            ],
            "app_rec_view": [
                {
                    "card_type": "Multi_apps_Rich",
                    "count": 4
                }
            ]
        },
        "clids": {
            "clid1": "9912",
            "clid1010": "1af1af1af1af1af1"
        }
    }

    def setUp(self):
        self.client = DjangoClient(
            HTTP_USER_AGENT='com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2.1beta)',
            HTTP_X_YAUUID=self.uuid,
            HTTP_HOST='localhost'
        )

    def tearDown(self):
        Client.objects(uuid=self.uuid).delete()

    def make_data_with_locale(self, locale_str):
        data = self.default_data.copy()
        data['user_settings']['locale'] = locale_str
        return data

    def test_unknown_uuid(self):
        """ This request should come first. Should not fail on unknown uuid """
        some_new_uuid = uuid4().hex
        response = self.post(HTTP_X_YAUUID=some_new_uuid)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        Client.objects(uuid=some_new_uuid).delete()

    def test_device_id_required(self):
        data = self.default_data.copy()
        del data['device_id']
        response = self.post(data)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_general(self):
        response = self.post(self.make_data_with_locale('en_US'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_uncomplete_profile(self):
        device_id = self.default_data['device_id']
        Profile.objects(device_id=device_id).delete()
        Profile.objects(device_id=device_id).update(phone_id='123', upsert=True)
        response = self.post()
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        p = Profile.objects.get(device_id=device_id)
        p.validate()

        # https://st.yandex-team.ru/ADVISOR-2020
        coll = Profile._get_collection()
        doc = coll.find_one({'_id': UUID(device_id)})
        self.assertIn('created_at', doc)
        self.assertIsInstance(doc['created_at'], datetime)

    def test_short_locale(self):
        response = self.post(self.make_data_with_locale('ru'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_empty_locale(self):
        data = self.default_data.copy()
        del data['user_settings']['locale']
        response = self.post(data)
        self.assertNotEqual(response.status_code, status.HTTP_200_OK)

    def test_client_model_is_created(self):
        self.post()
        try:
            Client.objects.get(uuid=self.uuid)
        except Client.DoesNotExist:
            self.fail("Client model is not created")

    def test_profile_model_is_created(self):
        self.post()
        try:
            Profile.objects.get(device_id=self.default_data['device_id'])
        except Profile.DoesNotExist:
            self.fail("Profile model is not created")

    def test_view_type_count_not_required(self):
        data = deepcopy(self.default_data)
        config = data['rec_views_config']['feed_view'][1]
        del config['count']
        response = self.post(data)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    @mock.patch('yaphone.advisor.common.passport.get_info', mock.MagicMock(return_value={'passport_uid': 88005553535}))
    def test_passport_auth(self):
        response = self.post(HTTP_AUTHORIZATION='OAuth some_passport_token')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        try:
            profile = Profile.objects.get(device_id=self.default_data['device_id'])
        except Profile.DoesNotExist:
            self.fail("Profile model is not created")
        self.assertEqual(profile.passport_uid, 88005553535)


class AndroidClientInfoV2Test(AndroidClientInfoTest):
    """ For now, api/v1 and v2 for this endpoint are the same """
    endpoint = '/api/v2/android_client_info/'


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class LbsInfoTest(DeviceViewTest, TestCase):
    endpoint = '/api/v1/lbs_info/'
    yandex_ip = '93.158.190.198'
    default_data = {
        'time_zone': {
            'name': 'Europe/Moscow',
            'utc_offset': 3600
        }
    }

    def set_location_to_zero(self):
        self.client_model.profile.lbs_info.location = None
        self.client_model.profile.save()

    def test_location_had_not_set_before(self):
        lat = 55.16
        lon = 37.73

        lbs_info_data = {
            'location': {
                'latitude': lat,
                'longitude': lon
            },
            'time_zone': {
                'name': 'Europe/Moscow',
                'utc_offset': 3600
            },
            'cells': [
                {
                    'country_code': 123,
                    'operator_id': 456,
                    'cell_id': 789,
                    'lac': 1011,
                    'signal_strength': -1213
                }
            ],
            'wifi_networks': [
                {
                    'mac': '12-34-56-78-9A-BC',
                    'signal_strength': 0
                }
            ]
        }

        self.set_location_to_zero()
        self.post(lbs_info_data)
        self.reload_client_model()

        self.assertEqual(self.client_model.profile.lbs_info.location.latitude, lat)
        self.assertEqual(self.client_model.profile.lbs_info.location.longitude, lon)

    def test_location_had_not_set_before_ip_only(self):
        self.set_location_to_zero()
        self.post(REMOTE_ADDR=self.yandex_ip)
        self.reload_client_model()
        self.assertNotIn('location', self.client_model.profile.lbs_info)

    def test_location_set_before_ip_only(self):
        lat, lon = 55.01, 82.55

        # set novosibirsk location
        self.client_model.profile.lbs_info.location = Location(latitude=lat, longitude=lon)
        self.client_model.profile.save()

        # post moscow ip
        self.post(REMOTE_ADDR=self.yandex_ip)
        self.reload_client_model()

        # expect novosibirsk location in db
        self.assertEqual(self.client_model.profile.lbs_info.location.latitude, lat)
        self.assertEqual(self.client_model.profile.lbs_info.location.longitude, lon)

    @requests_mock.mock()
    def test_country_init_by_gsm(self, lbs_mock):
        lbs_mock.post(settings.LBS_URL, text='{"position": {"type":"gsm", "latitude":55.1, "longitude":37.2}}')
        gsm_only_data = {
            'time_zone': {
                'name': 'Europe/Moscow',
                'utc_offset': 3600
            },
            'cells': [
                {
                    "country_code": 250,  # Russia
                    "operator_id": 1,
                    "cell_id": 67790523,
                    "lac": 235,
                    "signal_strength": 0
                }
            ]
        }

        self.client_model.profile.lbs_info.country_init = 'US'
        self.client_model.profile.lbs_info.fix_country_init = True
        self.client_model.profile.save()
        self.post(gsm_only_data)
        self.reload_client_model()
        self.assertEqual(self.client_model.profile.lbs_info.country_init, 'RU')

    def test_is_country_default(self):
        self.client_model.profile.lbs_info.country_init = None
        self.client_model.profile.lbs_info.fix_country_init = True
        self.client_model.profile.save()

        self.post()
        self.reload_client_model()

        self.assertEqual(self.client_model.profile.lbs_info.country_init, DEFAULT_COUNTRY)


class LbsInfoV2Test(LbsInfoTest):
    """ For now, api/v1 and v2 for this endpoint are the same """
    endpoint = '/api/v2/lbs_info/'

    def test_unexisting_profile(self):
        Profile.objects(pk=self.client_model.profile.pk).delete()
        response = self.post()
        self.assertEqual(response.status_code, HTTP_418_CLIENT_INFO_REQUIRED)


mock_crypta_data = {
    u'krypta-user-gender': {
        u'0': 0.271613,
        u'1': 0.728386
    },
    u'krypta-user-age': {
        u'0': 0.018921999999999998,
        u'1': 0.079738,
        u'2': 0.30710699999999996,
        u'3': 0.42588899999999996,
        u'4': 0.168342
    },
    u'user-loyalty': 0.833333,
    u'krypta-user-revenue': {
        u'0': 0.019053999999999998,
        u'1': 0.329635,
        u'2': 0.6513089999999999
    }}


@mock.patch('yaphone.advisor.advisor.models.crypta._get_crypta_info', lambda *args, **kwargs: mock_crypta_data)
class PackagesInfoTest(DeviceViewTest, TestCase):
    endpoint = '/api/v1/packages_info/'
    default_data = {"packages_info": [
        {
            "package_name": "ru.yandex.mail",
            "first_install_time": 123123,
            "last_update_time": 123123,
            "is_system": False,
            "is_disabled": False
        },
        {
            "package_name": "com.yandex.launcher",
            "first_install_time": 123123,
            "last_update_time": 123123,
            "is_system": True,
            "is_disabled": False
        },
        {
            "package_name": "com.yandex.browser",
            "first_install_time": 123123,
            "last_update_time": 123123,
            "is_system": False,
            "is_disabled": False
        },
    ]}

    def test_unexisting_profile(self):
        Profile.objects(pk=self.client_model.profile.pk).delete()
        response = self.post()
        self.assertEqual(response.status_code, HTTP_418_CLIENT_INFO_REQUIRED)

    def test_packages_list(self):
        self.post()
        self.reload_client_model()
        self.assertItemsEqual(self.client_model.profile.user_apps, ["ru.yandex.mail", "com.yandex.browser"])
        self.assertItemsEqual(self.client_model.profile.all_apps, ["ru.yandex.mail", "com.yandex.browser",
                                                                   "com.yandex.launcher"])

    def test_remove_apps(self):
        self.post()
        data = {"packages_info": self.default_data["packages_info"][:]}
        del data["packages_info"][0]
        self.post(data)
        self.reload_client_model()
        self.assertItemsEqual(self.client_model.profile.user_apps, ["com.yandex.browser"])

    def test_crypta_update(self):
        self.post()
        self.reload_client_model()
        self.assertEqual(self.client_model.profile.crypta.gender, mock_crypta_data['krypta-user-gender']['0'])
        self.assertEqual(self.client_model.profile.crypta.loyalty, mock_crypta_data['user-loyalty'])
        for key, value in mock_crypta_data['krypta-user-age'].iteritems():
            self.assertEqual(self.client_model.profile.crypta.age[key], value)
        for key, value in mock_crypta_data['krypta-user-revenue'].iteritems():
            self.assertEqual(self.client_model.profile.crypta.revenue[key], value)


class PackagesInfoV2Test(PackagesInfoTest, TestCase):
    """ For now, api/v1 and v2 for this endpoint are the same """
    endpoint = '/api/v2/packages_info/'


BIGB_MOCK = """{
    "id": "1899838521379856871",
    "hash": "77f0a042",
    "behave_param": "442",
    "data": [
        {
            "id": "1",
            "name": "big brother",
            "segment": [
                {
                    "id": "174",
                    "name": "krypta-user-gender",
                    "value": "1",
                    "weight": "728386",
                    "time": "1379975376"
                },
                {
                    "id": "174",
                    "name": "krypta-user-gender",
                    "value": "0",
                    "weight": "271613",
                    "time": "1379975376"
                },
                {
                    "id": "175",
                    "name": "krypta-user-age",
                    "value": "1",
                    "weight": "79738",
                    "time": "1379898764"
                },
                {
                    "id": "175",
                    "name": "krypta-user-age",
                    "value": "4",
                    "weight": "168342",
                    "time": "1379898764"
                },
                {
                    "id": "175",
                    "name": "krypta-user-age",
                    "value": "0",
                    "weight": "18922",
                    "time": "1379898764"
                },
                {
                    "id": "175",
                    "name": "krypta-user-age",
                    "value": "3",
                    "weight": "425889",
                    "time": "1379898764"
                },
                {
                    "id": "175",
                    "name": "krypta-user-age",
                    "value": "2",
                    "weight": "307107",
                    "time": "1379898764"
                },
                {
                    "id": "176",
                    "name": "krypta-user-revenue",
                    "value": "1",
                    "weight": "329635",
                    "time": "1379975376"
                },
                {
                    "id": "176",
                    "name": "krypta-user-revenue",
                    "value": "0",
                    "weight": "19054",
                    "time": "1379975376"
                },
                {
                    "id": "176",
                    "name": "krypta-user-revenue",
                    "value": "2",
                    "weight": "651309",
                    "time": "1379975376"
                },
                {
                    "id": "220",
                    "name": "user-loyalty",
                    "value": "833333",
                    "time": "1379898764"
                }
            ]
        }
    ]
}
"""


@mock.patch('yaphone.advisor.advisor.models.crypta.add_service_ticket_header', mock.MagicMock)
class BigBParserTest(TestCase):
    def test_parser(self):
        with requests_mock.mock() as m:
            m.get(settings.BIGB_URL, text=BIGB_MOCK)
            result = _get_crypta_info(device_id=uuid4(), uuid=uuid4())
            self.assertEqual(result, mock_crypta_data)
