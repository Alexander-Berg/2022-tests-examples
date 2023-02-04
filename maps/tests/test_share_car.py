import logging
import rstr
import uuid

from .basic_test import BasicTest
import lib.fakeenv as fakeenv
import lib.remote_access_server as server
import lib.pandora as pandora

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestCars")

SHARED_ACCESS_SMS_DEEP_LINK = "Вам предоставлен доступ к авто: https://30488.redirect.appmetrica.yandex.com/show_ui/menu?appmetrica_tracking_id=242665341795212810"


def secure_phone(phone):
    return phone[:5] + "*" * 5 + phone[-2:]


class BasicShareTest(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

        assert self.is_alice_notified(self.user.oauth)

    def perform_share(self, another_user):
        server.share_car(
            self.user.oauth, self.car,
            another_user.name, another_user.phone) >> 200

        server.confirm_share_phone(
            self.user.oauth, self.car,
            self.get_sms_code(another_user.phone)) >> 200

        server.complete_share(
            self.user.oauth, self.car, another_user.phone) >> 200

        response = server.confirm_share(
            self.user.oauth, self.car,
            self.get_sms_code(self.user.phone)) >> 200

        assert isinstance(response, list), response
        assert len(response) == 1, response
        assert int(response[0]["id"]) > 0, response
        assert response[0]["phone"] == another_user.get_masked_phone(), response
        assert response[0]["name"] == another_user.name, response

        sms_list = fakeenv.read_sms(another_user.phone)
        assert sms_list == [SHARED_ACCESS_SMS_DEEP_LINK]

    def check_shared_with(self, another_user):
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        assert settings["isOwner"]

        accesses = settings["sharedAccess"]
        assert isinstance(accesses, list), str(accesses)
        assert len(accesses) == 1, str(accesses)

        access = accesses[0]
        assert int(access["id"]) > 0, access["id"]
        assert access["name"] == another_user.name
        assert access["phone"] == secure_phone(another_user.phone)

        settings = server.get_car_settings(another_user.oauth, self.car) >> 200
        assert not settings["isOwner"]
        assert len(settings["sharedAccess"]) == 0, settings

        response = fakeenv.get_datasync_flag(another_user.oauth) >> 200
        assert response["user_has_car"], response

        alarm_settings = self.telematics.get_alarm_settings()
        assert another_user.phone == alarm_settings["phone_3"]
        assert alarm_settings[self.telematics.get_events_field("phone_3")] > 0

        return access["id"]

    def check_not_shared_with(self, another_user, has_other_car=False):
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        assert settings["isOwner"]
        assert len(settings["sharedAccess"]) == 0, str(settings)

        cars = server.get_cars(self.another_user.oauth) >> 200
        if not has_other_car:
            assert len(cars) == 0, str(cars)
        else:
            assert next((car for car in cars if Car.from_json(car).id == self.car.id), None) is None

        if not has_other_car:
            response = fakeenv.get_datasync_flag(self.another_user.oauth) >> 200
            assert not response["user_has_car"], response

        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings["phone_3"] == ''
        assert alarm_settings[self.telematics.get_events_field("phone_3")] == 0


class TestShareCar(BasicShareTest):
    def check_phones_settings(self, shared_phone='', notifications_phone=''):
        settings = self.car.get_settings(self.user).to_dict()
        additional_phones_count = 1 if len(notifications_phone) > 0 else 0
        assert len(settings['notifications']['additionalPhones']) == additional_phones_count
        if shared_phone:
            assert settings["sharedAccess"][0]["phone"] == secure_phone(shared_phone)
        else:
            assert len(settings["sharedAccess"]) == 0

        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings["phone_3"] == shared_phone
        assert alarm_settings["phone_rescue1"] == notifications_phone

    def test_share_car_with_registered_user(self):
        self.perform_share(self.another_user)

        assert self.get_datasync_flag(self.another_user)

        car_list = server.get_cars(self.another_user.oauth) >> 200
        assert len(car_list) == 1, car_list
        assert Car.from_json(car_list[0]).id == self.car.id

        assert self.is_alice_notified(self.another_user.oauth)
        self.check_shared_with(self.another_user)

    def test_share_car_with_unknown_user(self):
        another_user = User()
        self.perform_share(another_user)
        another_user.registrate()

        car_list = server.get_cars(another_user.oauth) >> 200
        assert len(car_list) == 1, car_list
        assert Car.from_json(car_list[0]).id == self.car.id

        assert self.is_alice_notified(another_user.oauth)

    def test_share_car_two_users(self):
        another_user1 = User()
        another_user2 = User()
        another_user2.phone = another_user1.phone

        another_user1.registrate()
        another_user2.registrate()

        self.perform_share(another_user1)

        assert self.get_datasync_flag(another_user1)
        assert self.get_datasync_flag(another_user2)
        assert not self.is_alice_notified(another_user1.oauth)
        assert not self.is_alice_notified(another_user2.oauth)

        car_list = server.get_cars(another_user2.oauth) >> 200
        assert len(car_list) == 1, car_list
        assert Car.from_json(car_list[0]).id == self.car.id

        assert self.is_alice_notified(another_user2.oauth)

        car_list = server.get_cars(another_user1.oauth) >> 200
        assert len(car_list) == 0, car_list

        assert not self.is_alice_notified(another_user1.oauth)

        assert not self.get_datasync_flag(another_user1)
        assert self.get_datasync_flag(another_user2)

    def test_share_car_one_more_time(self):
        self.perform_share(self.another_user)

        one_more_user = User()
        response = server.share_car(
            self.user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 417
        assert response["code"] == "noSlotForSharedAccess", response

    def test_share_in_parallel_first_step(self):
        one_more_user = User()

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 208

        server.share_car(
            self.user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 429

    def test_share_in_parallel_before_complete_operation(self):
        one_more_user = User()

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.confirm_share_phone(
            self.user.oauth, self.car,
            self.get_sms_code(self.another_user.phone)) >> 200

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 429

        server.share_car(
            self.user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 429

    def test_second_user_has_no_access(self):
        self.perform_share(self.another_user)
        server.get_cars(self.another_user.oauth) >> 200

        one_more_user = User()
        server.share_car(
            self.another_user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 403

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        access_id = settings["sharedAccess"][0]["id"]

        server.delete_shared_access(
            self.another_user.oauth, self.car, access_id) >> 403

    def test_delete_shared_access(self):
        self.perform_share(self.another_user)

        server.get_cars(self.another_user.oauth) >> 200

        assert self.is_alice_notified(self.another_user.oauth)

        access_id = self.check_shared_with(self.another_user)

        response = server.delete_shared_access(
            self.user.oauth, self.car, access_id) >> 200
        assert response == []

        assert self.is_alice_notified(self.another_user.oauth)
        self.check_not_shared_with(self.another_user)

    def test_delete_shared_access_of_unknown_user(self):
        another_user = User()
        self.perform_share(another_user)

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        access_id = settings["sharedAccess"][0]["id"]

        assert not self.is_alice_notified(another_user.oauth)

        server.delete_shared_access(
            self.user.oauth, self.car, access_id) >> 200

        another_user.registrate()

        assert not self.is_alice_notified(another_user.oauth)
        self.check_not_shared_with(another_user)

    def test_delete_shared_car(self):
        self.perform_share(self.another_user)

        server.get_cars(self.another_user.oauth) >> 200

        assert self.is_alice_notified(self.another_user.oauth)

        self.check_shared_with(self.another_user)

        server.delete_car(self.user.oauth, self.car) >> 200

        assert self.is_alice_notified(self.user.oauth)
        assert self.is_alice_notified(self.another_user.oauth)

        cars = server.get_cars(self.user.oauth) >> 200
        assert len(cars) == 0, str(cars)

        cars = server.get_cars(self.another_user.oauth) >> 200
        assert len(cars) == 0, str(cars)

        assert not self.get_datasync_flag(self.another_user) >> 200

        rebound_car = Car()
        rebound_car.registrate(self.user, self.telematics)

        settings = server.get_car_settings(self.user.oauth, rebound_car) >> 200
        assert len(settings["sharedAccess"]) == 0, settings

        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings['phone_3'] == '', alarm_settings

    def test_user_changed_phone(self):
        self.perform_share(self.another_user)
        server.get_cars(self.another_user.oauth) >> 200

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        assert settings["sharedAccess"][0]["phone"] == secure_phone(self.another_user.phone)

        self.another_user.change_phone()

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        assert settings["sharedAccess"][0]["phone"] == secure_phone(self.another_user.phone)

    def test_not_confirmed_yet(self):
        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.confirm_share_phone(
            self.user.oauth, self.car,
            self.get_sms_code(self.another_user.phone)) >> 200

        cars_list = server.get_cars(self.another_user.oauth) >> 200
        assert len(cars_list) == 0, cars_list

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        assert len(settings["sharedAccess"]) == 0, settings

    def test_share_to_yourself(self):
        server.share_car(
            self.user.oauth, self.car,
            self.user.name, self.user.phone) >> 412

    def test_change_phone_number_to_sharing(self):
        self.perform_share(self.another_user)
        server.get_cars(self.another_user.oauth) >> 200
        self.check_shared_with(self.another_user)

        self.user.change_phone(self.another_user.phone)

        self.check_not_shared_with(self.another_user)

    def test_notifications_phone_cannot_match_sharing(self):
        self.perform_share(self.another_user)

        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=self.another_user.phone) >> 412

    def test_notifications_phone_removed_after_sharing(self):
        self.car.add_notifications_phone(self.user, self.another_user)
        self.check_phones_settings(notifications_phone=self.another_user.phone)

        self.perform_share(self.another_user)
        self.check_phones_settings(shared_phone=self.another_user.phone)

        server.get_cars(self.another_user.oauth) >> 200
        self.check_shared_with(self.another_user)
        self.check_phones_settings(shared_phone=self.another_user.phone)

    def test_phones_persist_after_shared_user_phone_unbind(self):
        self.perform_share(self.another_user)
        server.get_cars(self.another_user.oauth) >> 200

        new_phone = "+7" + rstr.digits(10)
        fakeenv.set_user_phone(self.another_user, new_phone)

        self.another_user.unbind_phone()

        self.check_phones_settings(shared_phone=self.another_user.phone)

        self.another_user.bind_phone(new_phone)
        self.another_user.unbind_phone()

        self.check_phones_settings(shared_phone=new_phone)


class TestSharingToUserWithCar(BasicShareTest):
    def setup(self):
        BasicShareTest.setup(self)

        self.another_telematics = Telematics()
        self.another_telematics.registrate()

        self.another_car = Car()
        self.another_car.registrate(self.another_user, self.another_telematics)

        assert self.is_alice_notified(self.another_user.oauth)

        response = fakeenv.get_datasync_flag(self.another_user.oauth) >> 200
        assert response["user_has_car"], response

    def check_user_owns_car(self, user, car):
        cars = server.get_cars(user.oauth) >> 200
        assert len(cars) == 1, str(cars)
        assert Car.from_json(cars[0]).id == car.id

        response = fakeenv.get_datasync_flag(user.oauth) >> 200
        assert response["user_has_car"], response

    def test_delete_shared_access(self):
        self.perform_share(self.another_user)

        server.get_cars(self.another_user.oauth) >> 200
        assert self.is_alice_notified(self.another_user.oauth)

        access_id = self.check_shared_with(self.another_user)

        server.delete_shared_access(
            self.user.oauth, self.car, access_id) >> 200

        assert self.is_alice_notified(self.another_user.oauth)

        self.check_not_shared_with(self.another_user, has_other_car=True)
        self.check_user_owns_car(self.another_user, self.another_car)

    def test_delete_shared_car(self):
        self.perform_share(self.another_user)

        server.get_cars(self.another_user.oauth) >> 200

        assert self.is_alice_notified(self.another_user.oauth)

        self.check_shared_with(self.another_user)

        server.delete_car(self.user.oauth, self.car) >> 200

        assert self.is_alice_notified(self.user.oauth)
        assert self.is_alice_notified(self.another_user.oauth)

        cars = server.get_cars(self.user.oauth) >> 200
        assert len(cars) == 0, str(cars)

        self.check_user_owns_car(self.another_user, self.another_car)

    def test_share_car_offline(self):
        pandora.set_car_online(self.telematics, False)

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 403

    def test_wrong_car_uuid_errors(self):
        another_car = Car(car_id=uuid.uuid4())
        invalid_car = Car(car_id='000')

        server.share_car(
            self.user.oauth, another_car,
            self.another_user.name, self.another_user.phone) >> 404
        server.share_car(
            self.user.oauth, invalid_car,
            self.another_user.name, self.another_user.phone) >> 404

        server.share_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        sms = self.get_sms_code(self.another_user.phone)
        server.confirm_share_phone(self.user.oauth, another_car, sms) >> 404
        server.confirm_share_phone(self.user.oauth, invalid_car, sms) >> 404
        server.confirm_share_phone(self.user.oauth, self.car, sms) >> 200

        server.complete_share(self.user.oauth, another_car, self.another_user.phone) >> 404
        server.complete_share(self.user.oauth, invalid_car, self.another_user.phone) >> 404
        server.complete_share(self.user.oauth, self.car, self.another_user.phone) >> 200

        sms = self.get_sms_code(self.user.phone)
        server.confirm_share(self.user.oauth, another_car, sms) >> 404
        server.confirm_share(self.user.oauth, invalid_car, sms) >> 404
        server.confirm_share(self.user.oauth, self.car, sms) >> 200

        sms_list = fakeenv.read_sms(self.another_user.phone)
        assert sms_list == [SHARED_ACCESS_SMS_DEEP_LINK]
