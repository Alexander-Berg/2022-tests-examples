import logging
import time

from .basic_test import BasicTest
import lib.fakeenv as fakeenv
import lib.remote_access_server as server
import lib.pandora as pandora

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestNotificationsPhones")


class TestNotificationsPhones(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def add_and_confirm_phone(self, user, name='My cool phone'):
        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=user.phone, name=name) >> 200
        code = self.get_sms_code(user.phone)
        response = server.confirm_notifications_phone(
            token=self.user.oauth, car=self.car, code=code) >> 200
        return next(element['id'] for element in response if element['phone'] == user.get_masked_phone())

    def test_add_notifications_phone(self):
        notifictions_user = User()

        server.add_notifications_phone(
            token=self.user.oauth,
            car=self.car,
            phone=notifictions_user.phone,
            name=notifictions_user.name) >> 200
        code = self.get_sms_code(notifictions_user.phone)
        response = server.confirm_notifications_phone(
            token=self.user.oauth, car=self.car, code=code) >> 200
        assert response[0]["phone"] == notifictions_user.get_masked_phone()
        assert response[0]["name"] == notifictions_user.name
        phone_id = response[0]["id"]

        response = server.delete_notifications_phone(
            token=self.user.oauth, car=self.car, phone_id=phone_id) >> 200
        assert(len(response) == 0)

    def test_notifications_phone_clears_on_rebind(self):
        first_notifications_user = User()
        self.add_and_confirm_phone(first_notifications_user)
        second_notifications_user = User()
        self.add_and_confirm_phone(second_notifications_user)

        settings = self.car.get_settings(self.user).to_dict()
        assert len(settings['notifications']['additionalPhones']) == 2, settings
        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings['phone_rescue1'] == first_notifications_user.phone, alarm_settings
        assert alarm_settings['phone_rescue2'] == second_notifications_user.phone, alarm_settings

        server.delete_car(self.user.oauth, self.car)
        rebound_car = Car()
        rebound_car.registrate(self.user, self.telematics)

        settings = rebound_car.get_settings(self.user).to_dict()
        assert len(settings['notifications']['additionalPhones']) == 0, settings
        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings['phone_rescue1'] == '', alarm_settings
        assert alarm_settings['phone_rescue2'] == '', alarm_settings

    def test_add_duplicate_notifications_phone_not_allowed(self):
        notifictions_user = User()
        self.add_and_confirm_phone(notifictions_user)

        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=notifictions_user.phone) >> 412

    def test_only_two_phones_may_be_added(self):
        first_notifications_user = User()
        self.add_and_confirm_phone(first_notifications_user)
        second_notifications_user = User()
        self.add_and_confirm_phone(second_notifications_user)

        alarm_settings = self.telematics.get_alarm_settings()
        assert alarm_settings['phone_rescue1'] == first_notifications_user.phone
        assert alarm_settings['phone_rescue2'] == second_notifications_user.phone

        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=User().phone) >> 417

    def test_phone_cannot_be_added_for_offline_car(self):
        pandora.set_car_online(self.telematics, False)

        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=User().phone) >> 403

    def test_phone_cannot_be_deleted_for_wrong_id(self):
        phone_id = self.add_and_confirm_phone(User())
        wrong_phone_id = phone_id + '_wrong_suffix'

        server.delete_notifications_phone(
            token=self.user.oauth, car=self.car, phone_id=wrong_phone_id) >> 404

    def test_notifications_phones_are_returned_in_car_settings(self):
        first_user = User()
        second_user = User()
        first_id = self.add_and_confirm_phone(first_user, first_user.name)
        second_id = self.add_and_confirm_phone(second_user, second_user.name)

        response = server.get_car_settings(token=self.user.oauth, car=self.car) >> 200
        additional_phones = response['notifications']['additionalPhones']
        assert len(additional_phones) == 2, additional_phones

        first_phone_element = next(
            element for element in additional_phones
            if element['phone'] == first_user.get_masked_phone()
        )
        assert first_phone_element['name'] == first_user.name, first_phone_element
        assert first_phone_element['id'] == first_id, first_phone_element

        second_phone_element = next(
            element for element in additional_phones
            if element['phone'] == second_user.get_masked_phone()
        )
        assert second_phone_element['name'] == second_user.name, second_phone_element
        assert second_phone_element['id'] == second_id, second_phone_element

    def test_phone_ids_are_unique(self):
        first_id = self.add_and_confirm_phone(User())
        second_user = User()
        second_id = self.add_and_confirm_phone(second_user)

        assert first_id != second_id

        server.delete_notifications_phone(
            token=self.user.oauth, car=self.car, phone_id=first_id) >> 200
        third_id = self.add_and_confirm_phone(User())

        assert third_id != first_id
        assert third_id != second_id

        response = server.get_car_settings(token=self.user.oauth, car=self.car) >> 200
        additional_phones = response['notifications']['additionalPhones']
        second_id_after_delete = next(
            element['id'] for element in additional_phones
            if element['phone'] == second_user.get_masked_phone()
        )

        assert second_id_after_delete == second_id

    def test_wrong_phone_format_is_forbidden(self):
        wrong_phone_format = "89876543210"

        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=wrong_phone_format) >> 400

    def test_phone_confirmation_works_when_there_are_multiple_telematics(self):
        one_more_telematics = Telematics()
        one_more_car = Car()

        pandora.add_car(one_more_telematics) >> 200
        server.add_telematics(one_more_telematics) >> 200
        server.add_car(
            token=self.user.oauth, hwid=one_more_telematics.hwid, car=one_more_car) >> 201

        notifictions_user = User()
        self.add_and_confirm_phone(notifictions_user)

    def test_phone_conflict(self):
        another_user = User()

        response = server.add_notifications_phone(
            token=self.user.oauth, car=self.car,
            phone=another_user.phone, name=another_user.name) >> 200

        assert self.parse_datetime(response["nextCodeAvailableAt"]) > time.time()

        response = server.add_notifications_phone(
            token=self.user.oauth, car=self.car,
            phone=another_user.phone, name=another_user.name) >> 208

        assert self.parse_datetime(response["nextCodeAvailableAt"]) > time.time()

        response = server.add_notifications_phone(
            token=self.user.oauth, car=self.car,
            phone=User().phone, name=another_user.name) >> 429

        assert response["code"] == "otherActiveRequest"
        assert self.parse_datetime(response["data"]["nextCodeAvailableAt"]) > time.time()

    def test_notifications_phone_cannot_match_primary(self):
        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=self.user.phone) >> 412

        notifications_phone = User().phone
        server.add_notifications_phone(
            token=self.user.oauth, car=self.car, phone=notifications_phone) >> 200
        notifications_code = self.get_sms_code(notifications_phone)
        old_phone = self.user.phone
        self.user.phone = notifications_phone
        fakeenv.add_user(self.user)
        response = server.unbind_phone(self.user.oauth) >> 429
        assert response["code"] == "otherActiveRequest"
        self.user.phone = old_phone
        fakeenv.add_user(self.user)
        server.confirm_notifications_phone(
            token=self.user.oauth, car=self.car, code=notifications_code) >> 200

        self.user.phone = notifications_phone
        fakeenv.add_user(self.user)
        server.unbind_phone(self.user.oauth) >> 200
        primary_change_code = self.get_sms_code(old_phone)
        server.confirm_phone(self.user.oauth, primary_change_code) >> 200
        server.bind_phone(self.user.oauth) >> 200
        settings_response = server.get_car_settings(
            token=self.user.oauth, car=self.car) >> 200
        additional_phones = settings_response['notifications']['additionalPhones']
        assert len(additional_phones) == 0
