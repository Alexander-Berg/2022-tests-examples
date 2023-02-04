import logging

from .basic_test import BasicTest
import lib.fakeenv as fakeenv
import lib.remote_access_server as server
import lib.pandora as pandora
import maps.automotive.libs.large_tests.lib.docker as docker

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.car_settings import CarSettings
from data_types.user import User

logger = logging.getLogger("TestCars")


def read_code_from_sms(sms):
    return sms[-4:]


class TestCars(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

    def get_phone_in_pandora(self, telematics, phone='phone_2'):
        status, response = pandora.get_settings(telematics)
        assert status == 200, response
        return response['device_settings'][telematics.login][0]['alarm_settings'][phone]

    def filter(self, values):
        return {
            name: values[name]
            for name in values
            if name not in ['lastCommandTime', 'updatedAt']
        }

    def test_add_car(self):
        first_telematics = Telematics()
        first_telematics.registrate()

        assert not self.is_alice_notified(self.user.oauth)

        first_car = Car()
        first_car.registrate(self.user, first_telematics)

        assert self.is_alice_notified(self.user.oauth)

        response = server.get_cars(self.user.oauth) >> 200
        assert len(response) == 1, response
        assert Car.from_json(response[0]).details == first_car.details

        second_telematics = Telematics()
        second_telematics.registrate()

        second_car = Car()
        second_car.registrate(self.user, second_telematics)

        assert self.is_alice_notified(self.user.oauth)

        response = server.get_cars(self.user.oauth) >> 200
        assert len(response) == 2, response

        response = server.get_car(self.user.oauth, first_car) >> 200
        assert self.filter(response) == first_car.details

        response = server.get_car_settings(self.user.oauth, first_car) >> 200
        assert self.filter(response) == CarSettings.get_default_settings(self.user)

        pandora_phone = self.get_phone_in_pandora(first_telematics)
        assert self.user.phone == pandora_phone

        response = server.get_car(self.user.oauth, second_car) >> 200
        assert self.filter(response) == second_car.details

        response = server.get_car_settings(self.user.oauth, second_car) >> 200
        assert self.filter(response) == CarSettings.get_default_settings(self.user)

        pandora_phone = self.get_phone_in_pandora(second_telematics)
        assert self.user.phone == pandora_phone

        response = fakeenv.get_datasync_flag(self.user.oauth) >> 200
        assert response["user_has_car"] is True, response

    def test_phone_change_without_bind_confirmation(self):
        old_phone = self.user.phone

        first_telematics = Telematics()
        first_telematics.registrate()
        first_car = Car()
        first_car.registrate(self.user, first_telematics)

        second_telematics = Telematics()
        second_telematics.registrate()
        second_car = Car()
        second_car.registrate(self.user, second_telematics)

        new_phone = User().phone

        self.user.phone = new_phone

        fakeenv.add_user(self.user)

        sms_list = fakeenv.read_sms(old_phone)
        assert len(sms_list) == 0, str(sms_list)

        server.unbind_phone(self.user.oauth) >> 200

        code = self.get_sms_code(old_phone)
        server.confirm_phone(self.user.oauth, code) >> 200

        server.bind_phone(self.user.oauth) >> 200

        response = server.get_car(self.user.oauth, first_car) >> 200
        assert self.filter(response) == first_car.details

        response = server.get_car_settings(self.user.oauth, first_car) >> 200
        assert self.filter(response) == CarSettings.get_default_settings(self.user)

        pandora_phone = self.get_phone_in_pandora(first_telematics)
        assert self.user.phone == pandora_phone

        response = server.get_car(self.user.oauth, second_car) >> 200
        assert self.filter(response) == second_car.details

        response = server.get_car_settings(self.user.oauth, second_car) >> 200
        assert self.filter(response) == CarSettings.get_default_settings(self.user)

        pandora_phone = self.get_phone_in_pandora(second_telematics)
        assert self.user.phone == pandora_phone

    def test_regain_pandora_token(self):
        user = User()
        user.registrate()

        telematics = Telematics()
        telematics.registrate()

        tokens = pandora.get_tokens()
        token = next(token for token, car_id in tokens["assigned_cars"].items() if str(car_id) == telematics.login)

        assert token, 'token not found'
        pandora.mark_token_expired(token)

        car = Car()
        car.registrate(user, telematics)

    def test_add_telematics_when_pandora_unavailable(self):
        telematics = Telematics()

        pandora.add_car(telematics) >> 200

        docker.stop_container("auto-lab-pandora-emulator")
        server.add_telematics(telematics) >> 200
        docker.start_container("auto-lab-pandora-emulator")

        car = Car()
        car.registrate(self.user, telematics)

        response = server.get_car(self.user.oauth, car) >> 200
        assert self.filter(response) == car.details
