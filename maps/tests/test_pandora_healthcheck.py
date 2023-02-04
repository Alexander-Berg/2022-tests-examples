import logging

from .basic_test import BasicTest
import lib.remote_access_server as server
import lib.pandora as pandora

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestPandoraHealthcheck")


class TestPandoraHealthcheck(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.car = Car()

        status, response = pandora.add_car(self.telematics)
        assert status == 200, response

        status, response = server.add_telematics(self.telematics)
        assert status == 200, response

        status, response = server.add_car(
            token=self.user.oauth, hwid=self.telematics.hwid, car=self.car)
        assert status == 201, response
        self.car.id = response["id"]

    def test_phone_number_restores(self):
        assert self.user.phone == pandora.get_phone(self.telematics)

        error_phone = User().phone
        assert error_phone != self.user.phone
        status, response = pandora.set_settings(self.telematics, {
            'phone_2': error_phone
        })
        assert status == 200, response

        for _ in self.wait_for_poller():
            if error_phone != pandora.get_phone(self.telematics):
                break
        assert self.user.phone == pandora.get_phone(self.telematics)

    def test_shared_access_phone_number_restores(self):
        another_user = User()
        another_user.registrate()
        self.car.share_access(self.user, another_user)
        assert another_user.phone == pandora.get_phone(self.telematics, 'phone_3')

        error_phone = User().phone
        assert error_phone != another_user.phone
        pandora.set_settings(self.telematics, {
            'phone_3': error_phone
        }) >> 200

        for _ in self.wait_for_poller():
            if error_phone != pandora.get_phone(self.telematics, 'phone_3'):
                break
        assert another_user.phone == pandora.get_phone(self.telematics, 'phone_3')

    def test_autostart_settings_restore(self):
        EXPECTED_STOP_TIME_SECONDS = 600
        status, response = server.set_engine_stop_time(self.user.oauth, self.car, EXPECTED_STOP_TIME_SECONDS)
        assert status == 200, response

        error_stop_time_minutes = (EXPECTED_STOP_TIME_SECONDS / 60) * 2
        status, response = pandora.set_settings(self.telematics, {
            'engine_stop_time_value': error_stop_time_minutes
        })
        assert status == 200, response

        for _ in self.wait_for_poller():
            if error_stop_time_minutes != pandora.get_setting(self.telematics, 'engine_stop_time_value'):
                break
        assert EXPECTED_STOP_TIME_SECONDS == 60 * pandora.get_setting(self.telematics, 'engine_stop_time_value')
