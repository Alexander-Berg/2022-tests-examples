from copy import deepcopy
from datetime import datetime
from time import time
from uuid import UUID

from django.test import TestCase
from rest_framework.exceptions import ValidationError

from yaphone.advisor.advisor.models.client import Client, ViewConfig, UserAgent
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.advisor.serializers.device import ClientSerializer
from yaphone.advisor.advisor.views.device import update_profile
from yaphone.advisor.common.mocks.test_client_data import (ANDROID_CLIENT_REQUEST_DATA, SOME_DEVICE,
                                                           SOME_DEVICE_ID, SOME_UUID,
                                                           SOME_PHONE_ID)

SOME_CRYPTA_LOYALTY = 0.666

PACKAGES = [
    {"package_name": "com.slack", "first_install_time": 8, "last_update_time": 14,
     "is_system": False, "is_disabled": False},
    {"package_name": "com.yandex.browser", "first_install_time": 3, "last_update_time": 4,
     "is_system": False, "is_disabled": False},
    {"package_name": "ru.yandex.mail", "first_install_time": 1, "last_update_time": 2,
     "is_system": True, "is_disabled": False},
    {"package_name": "ru.yandex.taxi", "first_install_time": 5, "last_update_time": 6,
     "is_system": False, "is_disabled": True},
    {"package_name": "com.yandex.market", "first_install_time": 5, "last_update_time": 6,
     "is_system": True, "is_disabled": True},
]

SOME_OTHER_UUID = "6666d3cff3ff5de96c2f69d4497063d6"
SOME_USER_AGENT = UserAgent.from_string("com.yandex.launcher/2.00.qa2147483647 (Yandex Nexus 9; Android 9.2)")


class ProfileUnitTest(TestCase):
    @classmethod
    def setUpClass(cls):
        # check if we are using mongomock
        assert Client._get_db().client.HOST == 'localhost'
        assert Profile._get_db().client.HOST == 'localhost'

    @classmethod
    def tearDownClass(cls):
        pass

    def setUp(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        self.profile = Client.objects.get(uuid=UUID(SOME_UUID)).profile
        self.profile.update_packages_info(PACKAGES)

    def test_all_apps(self):
        self.profile.update_packages_info(PACKAGES)
        self.assertItemsEqual(self.profile.all_apps, ['ru.yandex.mail', 'com.yandex.browser', 'com.slack',
                                                      'ru.yandex.taxi', 'com.yandex.market'])

    def test_non_system_apps(self):
        self.assertItemsEqual(self.profile.non_system_apps, ['com.yandex.browser', 'com.slack', 'ru.yandex.taxi'])

    def test_user_apps(self):
        self.assertItemsEqual(self.profile.user_apps, ['com.yandex.browser', 'com.slack'])

    def test_remove_app(self):
        self.profile.update_packages_info(PACKAGES[1:])
        self.assertItemsEqual(self.profile.all_apps, ['ru.yandex.mail', 'com.yandex.browser',
                                                      'ru.yandex.taxi', 'com.yandex.market'])
        self.assertItemsEqual(self.profile.removed_apps, ['com.slack'])

    def test_disable_app(self):
        new_packages = deepcopy(PACKAGES)
        new_packages[1]["is_disabled"] = True
        self.profile.update_packages_info(new_packages)
        self.assertItemsEqual(self.profile.all_apps, ['ru.yandex.mail', 'com.yandex.browser', 'com.slack',
                                                      'ru.yandex.taxi', 'com.yandex.market'])
        self.assertItemsEqual(self.profile.user_apps, ['com.slack'])
        self.assertItemsEqual(self.profile.removed_apps, ['com.yandex.browser'])

    def test_remove_install_back_app(self):
        self.profile.update_packages_info(PACKAGES[1:])
        self.assertItemsEqual(self.profile.removed_apps, ['com.slack'])
        self.assertNotIn('com.slack', self.profile.user_apps)
        self.profile.update_packages_info(PACKAGES)
        self.assertItemsEqual(self.profile.removed_apps, [])
        self.assertIn('com.slack', self.profile.user_apps)

    def test_remove_apps_in_two_steps(self):
        self.profile.update_packages_info(PACKAGES[1:])
        self.assertItemsEqual(self.profile.removed_apps, ['com.slack'])
        self.assertNotIn('com.slack', self.profile.user_apps)
        self.profile.update_packages_info(PACKAGES[2:])
        self.assertItemsEqual(self.profile.removed_apps, ['com.slack', 'com.yandex.browser'])
        self.assertItemsEqual(self.profile.user_apps, [])

    def test_removed_apps_empty(self):
        self.profile = Profile()
        self.assertItemsEqual(self.profile.removed_apps, [])

    def test_installed_apps_empty(self):
        self.profile = Profile()
        self.assertItemsEqual(self.profile.user_apps, [])
        self.assertItemsEqual(self.profile.non_system_apps, [])
        self.assertItemsEqual(self.profile.all_apps, [])

    def test_recently_removed_apps(self):
        current_time = int(time())
        self.profile.removed_apps_info = [
            {"package_name": "some.new.app", "removal_ts": current_time},
            {"package_name": "some.old.app", "removal_ts": current_time - 864000 * 365},
        ]
        self.assertItemsEqual(self.profile.removed_apps, ["some.new.app", "some.old.app"])

    def test_vendor_model(self):
        self.assertEqual(self.profile.device_vendor, SOME_DEVICE['vendor'])
        self.assertEqual(self.profile.device_model, SOME_DEVICE['models'])

    def test_feedbacks(self):
        # check feedback can be added
        self.profile.add_feedback({"package_name": "com.yandex.launcher", "reason": "dislike"})
        self.profile.save()
        self.profile = Client.objects.get(uuid=UUID(SOME_UUID)).profile
        self.assertEqual(len(self.profile.feedbacks), 1)
        # check that second feedback can be added
        self.profile.add_feedback({"package_name": "com.yandex.taxi", "reason": "complain"})
        self.profile.save()
        self.profile = Client.objects.get(uuid=UUID(SOME_UUID)).profile
        self.assertEqual(len(self.profile.feedbacks), 2)
        # check timestamp correctness
        self.assertLess(time() - self.profile.feedbacks[1]['timestamp'], 2)


class ProfileSerializationTest(TestCase):
    @classmethod
    def setUpClass(cls):
        # check if we are using mongomock
        assert Client._get_db().client.HOST == 'localhost'
        assert Profile._get_db().client.HOST == 'localhost'

    @classmethod
    def tearDownClass(cls):
        pass

    def setUp(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID), SOME_USER_AGENT)

    def tearDown(self):
        Client.objects.all().delete()
        Profile.objects.all().delete()

    def test_new_client(self):
        mongo_doc = Client.objects.get(uuid=SOME_UUID).to_mongo()
        self.assertEqual(mongo_doc['device_id'], UUID(SOME_DEVICE_ID))
        self.assertEqual(mongo_doc['_id'], UUID(SOME_UUID))
        self.assertEqual(mongo_doc['clids'], {"clid1": "9912", "clid1010": "1af1af1af1af1af1"})
        self.assertLess((datetime.utcnow().utcnow() - mongo_doc['updated_at']).total_seconds(), 1)

    def test_user_agent(self):
        mongo_doc = Client.objects.get(uuid=SOME_UUID).to_mongo()
        self.assertEqual(mongo_doc['user_agent']['raw'], SOME_USER_AGENT.raw)
        self.assertEqual(mongo_doc['user_agent']['app_name'], SOME_USER_AGENT.app_name)
        self.assertEqual(mongo_doc['user_agent']['app_version_string'], SOME_USER_AGENT.app_version_string)
        self.assertEqual(mongo_doc['user_agent']['device_manufacturer'], SOME_USER_AGENT.device_manufacturer)
        self.assertEqual(mongo_doc['user_agent']['device_model'], SOME_USER_AGENT.device_model)
        self.assertEqual(mongo_doc['user_agent']['os_name'], SOME_USER_AGENT.os_name)
        self.assertEqual(mongo_doc['user_agent']['os_version'], SOME_USER_AGENT.os_version)

    def test_new_profile(self):
        mongo_doc = Profile.objects.get(device_id=UUID(SOME_DEVICE_ID)).to_mongo()
        self.assertEqual(mongo_doc['_id'], UUID(SOME_DEVICE_ID))
        self.assertLess((datetime.utcnow().utcnow() - mongo_doc['updated_at']).total_seconds(), 1)

    def test_change_device_id(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        some_new_device_id = "abc11111111111111111111111111111"
        new_data['device_id'] = some_new_device_id
        update_profile(new_data, UUID(SOME_UUID))
        self.assertEqual(Client.objects.get(uuid=SOME_UUID).profile.device_id.hex, some_new_device_id)

    def test_change_uuid(self):
        some_new_uuid = "18770afc5658d839ef1881a15ddb4c84"
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(some_new_uuid))
        self.assertEqual(Client.objects.get(uuid=some_new_uuid).profile.device_id.hex, SOME_DEVICE_ID)

    def test_suddenly_deleted_profile(self):
        Profile.objects.delete()
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))

    def test_suddenly_deleted_client(self):
        Client.objects.delete()
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))

    def test_invalid_android_id(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['android_id'] = "ololo! im so wrong"
        update_profile(new_data, UUID(SOME_UUID))
        self.assertEqual(Client.objects.get(uuid=SOME_UUID).profile.android_info.android_id, None)

    def test_valid_android_id(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['android_id'] = "1abc"
        update_profile(new_data, UUID(SOME_UUID))
        self.assertEqual(Client.objects.get(uuid=SOME_UUID).profile.android_info.android_id, "1abc")

    def test_without_android_id(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        del new_data['android_id']
        update_profile(new_data, UUID(SOME_UUID))
        self.assertEqual(Client.objects.get(uuid=SOME_UUID).profile.android_info.android_id, None)

    def test_null_adid(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['ad_id'] = None
        update_profile(new_data, UUID(SOME_UUID))
        self.assertEqual(Client.objects.get(uuid=SOME_UUID).profile.android_info.ad_id, None)

    def test_only_one_clid(self):
        for clid1006, clid1 in ClientSerializer.clid_map.iteritems():
            new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
            new_data['clid'] = clid1006
            del new_data['clids']
            update_profile(new_data, UUID(SOME_UUID))
            self.assertEqual(Client.objects.get(uuid=SOME_UUID).clids['clid1'], clid1)

    def test_profile_data_persistence(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        profile = Client.objects.get(uuid=SOME_UUID).profile
        profile.crypta.loyalty = SOME_CRYPTA_LOYALTY
        profile.save()
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_OTHER_UUID))
        profile = Client.objects.get(uuid=SOME_OTHER_UUID).profile
        self.assertEqual(profile.crypta.loyalty, SOME_CRYPTA_LOYALTY)

    def test_clids(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        client = Client.objects.get(uuid=SOME_UUID)
        self.assertEqual(client.get_clid('clid1'), ANDROID_CLIENT_REQUEST_DATA['clids']['clid1'])
        self.assertEqual(client.get_clid('clid1010'), ANDROID_CLIENT_REQUEST_DATA['clids']['clid1010'])
        self.assertEqual(client.get_clid('clid9999'), None)

    def test_supported_cards_config(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        client = Client.objects.get(uuid=SOME_UUID)
        self.assertEqual(client.supported_card_types, ANDROID_CLIENT_REQUEST_DATA['supported_card_types'])

    def test_places_config(self):
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        client = Client.objects.get(uuid=SOME_UUID)
        places_configs = {k: [ViewConfig(**x) for x in v]
                          for k, v in ANDROID_CLIENT_REQUEST_DATA['rec_views_config'].iteritems()}
        self.assertEqual(client.rec_views_config, places_configs)

    def test_extra_data(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['wtf'] = 'some_extra_data'
        try:
            update_profile(new_data, UUID(SOME_UUID))
        except ValidationError:
            self.fail('Unexpected ValidationError on extra data')

    def test_extra_data_in_places(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['rec_views_config']['some_other'] = 'some_extra_data'
        self.assertRaises(ValidationError, update_profile, new_data, UUID(SOME_UUID))

    def test_phone_id(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['phone_id'] = SOME_PHONE_ID
        update_profile(new_data, UUID(SOME_UUID))
        profile = Client.objects.get(uuid=SOME_UUID).profile
        self.assertEqual(profile.phone_id, SOME_PHONE_ID)
        # check phone_id is not removed when missing in request
        update_profile(ANDROID_CLIENT_REQUEST_DATA.copy(), UUID(SOME_UUID))
        profile = Client.objects.get(uuid=SOME_UUID).profile
        self.assertEqual(profile.phone_id, SOME_PHONE_ID)

    def test_place_config_with_lack_data(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        del new_data['rec_views_config']['feed_view'][0]['card_type']
        self.assertRaises(ValidationError, update_profile, new_data, UUID(SOME_UUID))

    def test_place_config_with_wrong_type_data(self):
        new_data = deepcopy(ANDROID_CLIENT_REQUEST_DATA)
        new_data['rec_views_config']['feed_view'][0]['count'] = 'i am not a number'
        self.assertRaises(ValidationError, update_profile, new_data, UUID(SOME_UUID))
