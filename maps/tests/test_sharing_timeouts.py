import logging
import time

from .basic_test import BasicTest
import lib.remote_access_server as server
import lib.fakeenv as fakeenv

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestSharingTimeouts")

SMS_RETRY_TIMEOUT = 2
LONG_ACTION_TIMEOUT = 4


class TestSharingTimeouts(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def get_sms(self, phone):
        sms_list = fakeenv.read_sms(phone)
        assert len(sms_list) == 1, str(sms_list)
        return sms_list[0][-4:]

    def test_retry_share(self):
        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.confirm_share_phone(
            self.user.oauth, self.car,
            self.get_sms(self.another_user.phone)) >> 200

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 429

        time.sleep(SMS_RETRY_TIMEOUT + 0.1)

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

    def test_complete_outdated_share_request(self):
        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.confirm_share_phone(
            self.user.oauth, self.car,
            self.get_sms(self.another_user.phone)) >> 200

        time.sleep(LONG_ACTION_TIMEOUT + 0.1)

        server.complete_share(
            self.user.oauth, self.car, self.another_user.phone) >> 404
