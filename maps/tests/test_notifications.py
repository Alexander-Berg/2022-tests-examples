import logging
import rstr

from .basic_test import BasicTest

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

import lib.remote_access_server as server
import lib.db as db

from collections import OrderedDict as odict

logger = logging.getLogger()


class TestNotifications(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def check_subscription_info(self, request_info, result_info):
        response = server.subscribe(self.user.oauth, request_info) >> 200
        response = db.query("""
            SELECT device_id, app_name, uuid, client_type, push_token,
                app_version, sdk_version
            FROM devices_data WHERE id = '%s'
        """ % response["subscriptionId"])
        assert response == [tuple([v for _, v in result_info.items()])]

    def test_full_subscription_info(self):
        subscription_info = odict([
            ("deviceId", rstr.letters(6)),
            ("appName", rstr.letters(6)),
            ("uuid", rstr.letters(6)),
            ("clientType", rstr.letters(6)),
            ("pushToken", rstr.letters(6)),
            ("appVersion", rstr.letters(6)),
            ("sdkVersion", rstr.letters(6))
        ])
        self.check_subscription_info(subscription_info, subscription_info)

    def test_subscription_info_without_versions(self):
        subscription_info = odict([
            ("deviceId", rstr.letters(6)),
            ("appName", rstr.letters(6)),
            ("uuid", rstr.letters(6)),
            ("clientType", rstr.letters(6)),
            ("pushToken", rstr.letters(6)),
        ])
        result = subscription_info.copy()
        result["appVersion"] = 'NULL'
        result["sdkVersion"] = 'NULL'
        self.check_subscription_info(subscription_info, result)
