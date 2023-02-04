import logging
import uuid

from .basic_test import BasicTest
import lib.remote_access_server as server
import lib.fakeenv as fakeenv
import lib.pandora as pandora

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User
from data_types.car_settings import CarSettings

logger = logging.getLogger()

RECIPIENT_INITIAL_SMS = "Процесс передачи прав может занять несколько минут. Дождитесь смс о завершении операции."
RECIPIENT_SUCCESS_SMS = "Операция продажи завершена успешно. Для получения доступа к авто: https://30488.redirect.appmetrica.yandex.com/show_ui/menu?appmetrica_tracking_id=242665341795212810"
RECIPIENT_FAILURE_SMS = "Не удалось завершить продажу авто, попробуйте снова"
OWNER_SUCCESS_SMS = "Операция продажи завершена успешно."
OWNER_FAILURE_SMS = "Не удалось завершить продажу авто, попробуйте снова"


class TestSellCar(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.shared_user = User()
        self.shared_user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.notifications_user = User()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

        assert self.is_alice_notified(self.user.oauth)

        self.car.share_access(self.user, self.shared_user)
        server.get_cars(self.shared_user.oauth) >> 200

        assert self.is_alice_notified(self.shared_user.oauth)

        self.car.add_notifications_phone(self.user, self.notifications_user)

        self.user_has_full_access_to_car(self.user, self.car)
        self.user_has_limited_access_to_car(self.shared_user, self.car)

        self.check_initial_telematics_phones()

    def perform_sell(self, another_user):
        server.sell_car(
            self.user.oauth, self.car,
            another_user.name, another_user.phone) >> 200

        server.confirm_sell_phone(
            self.user.oauth, self.car,
            self.get_sms_code(another_user.phone)) >> 200

        server.complete_sell(
            self.user.oauth, self.car, another_user.phone) >> 200

        server.confirm_sell(
            self.user.oauth, self.car,
            self.get_sms_code(self.user.phone)) >> 200

        assert fakeenv.read_sms(self.another_user.phone) == [RECIPIENT_INITIAL_SMS]

    def check_initial_telematics_phones(self):
        settings = self.telematics.get_alarm_settings()
        assert settings["phone_2"] == self.user.phone
        assert settings["phone_3"] == self.shared_user.phone
        assert settings["phone_rescue1"] == self.notifications_user.phone
        assert settings["phone_rescue2"] == ""

    def check_telematics_phone_set(self, user):
        settings = self.telematics.get_alarm_settings()
        assert settings["phone_2"] == user.phone
        assert settings["phone_3"] == ""
        assert settings["phone_rescue1"] == ""
        assert settings["phone_rescue2"] == ""

    def user_has_full_access_to_car(self, user, car):
        settings = server.get_car_settings(user.oauth, car) >> 200
        assert settings["isOwner"], settings

    def user_has_limited_access_to_car(self, user, car):
        settings = server.get_car_settings(user.oauth, car) >> 200
        assert not settings["isOwner"], settings

    def user_has_no_access_to_car(self, user, car):
        cars = server.get_cars(user.oauth) >> 200
        for car_elem in cars:
            assert Car.from_json(car_elem).id != car.id
        server.get_car_settings(user.oauth, car) >> 404

    def test_sell_to_registered_user(self):
        dummy_schedule = CarSettings.get_dummy_autostarts_schedule()
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=10, is_enabled=True) >> 200
        server.create_schedule(self.user.oauth, self.car, dummy_schedule) >> 200

        self.perform_sell(self.another_user)

        self.user_has_no_access_to_car(self.user, self.car)
        self.user_has_no_access_to_car(self.shared_user, self.car)

        car_list = server.get_cars(self.another_user.oauth) >> 200
        assert len(car_list) == 0, car_list

        last_user_sms = []
        for _ in self.wait_for_poller():
            transfer_complete = self.get_datasync_flag(self.another_user)
            last_user_sms = fakeenv.read_sms(self.user.phone)
            if transfer_complete and last_user_sms:
                break

        assert last_user_sms == [OWNER_SUCCESS_SMS]
        assert fakeenv.read_sms(self.another_user.phone) == [RECIPIENT_SUCCESS_SMS]

        assert self.is_alice_notified(self.user.oauth)
        assert self.is_alice_notified(self.shared_user.oauth)

        assert not self.get_datasync_flag(self.user)
        assert self.get_datasync_flag(self.another_user)

        default_settings = CarSettings.get_default_settings(self.another_user)
        default_voltage = default_settings['engine']['autostartConditions']['voltage']['value']
        for _ in self.wait_for_poller():
            alarm_settings = self.telematics.get_alarm_settings()
            if abs(alarm_settings['engine_start_voltage_value'] - default_voltage) < 0.01:
                break
        alarm_settings = self.telematics.get_alarm_settings()
        assert abs(alarm_settings['engine_start_voltage_value'] - default_voltage) < 0.01
        for weekday in range(1, 8):
            assert alarm_settings['enstart_day{}1'.format(weekday)] == 0
            assert alarm_settings['enstart_day{}2'.format(weekday)] == 0
        assert alarm_settings['enstart_day0'] == 0

        self.check_telematics_phone_set(self.another_user)

        car_list = server.get_cars(self.another_user.oauth) >> 200
        assert len(car_list) == 1, car_list
        assert Car.from_json(car_list[0]).id == self.car.id

        assert self.is_alice_notified(self.another_user.oauth)

        self.user_has_full_access_to_car(self.another_user, self.car)
        self.check_telematics_phone_set(self.another_user)

        car_settings = self.car.get_settings(self.another_user).to_dict()
        assert car_settings == default_settings

    def test_sell_car_offline(self):
        pandora.set_car_online(self.telematics, False)

        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 403

    def test_sell_to_yourself(self):
        server.sell_car(
            self.user.oauth, self.car,
            self.user.name, self.user.phone) >> 412

    def test_sell_car_cannot_sync(self):
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        server.confirm_sell_phone(
            self.user.oauth, self.car,
            self.get_sms_code(self.another_user.phone)) >> 200

        server.complete_sell(
            self.user.oauth, self.car, self.another_user.phone) >> 200

        pandora.set_car_online(self.telematics, False)

        server.confirm_sell(
            self.user.oauth, self.car,
            self.get_sms_code(self.user.phone)) >> 200

        assert fakeenv.read_sms(self.another_user.phone) == [RECIPIENT_INITIAL_SMS]

        recipient_sms = []
        owner_sms = []
        for _ in self.wait_for_sell():
            if not recipient_sms:
                recipient_sms = fakeenv.read_sms(self.another_user.phone)
            if not owner_sms:
                owner_sms = fakeenv.read_sms(self.user.phone)

            if recipient_sms and owner_sms:
                break

        assert recipient_sms == [RECIPIENT_FAILURE_SMS]
        assert owner_sms == [OWNER_FAILURE_SMS]

        self.user_has_no_access_to_car(self.another_user, self.car)

        for _ in self.wait_for_poller():
            car_list = server.get_cars(self.user.oauth) >> 200
            if car_list:
                break

        self.user_has_full_access_to_car(self.user, self.car)
        self.user_has_limited_access_to_car(self.shared_user, self.car)
        self.check_initial_telematics_phones()

    def test_sell_in_parallel_first_step(self):
        one_more_user = User()
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 208
        server.sell_car(
            self.user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 429

    def test_sell_in_parallel_before_complete_operation(self):
        one_more_user = User()
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200
        server.confirm_sell_phone(
            self.user.oauth, self.car,
            self.get_sms_code(self.another_user.phone)) >> 200
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 429
        server.sell_car(
            self.user.oauth, self.car,
            one_more_user.name, one_more_user.phone) >> 429

    def test_wrong_car_uuid_errors(self):
        another_car = Car(car_id=uuid.uuid4())
        invalid_car = Car(car_id='000')

        server.sell_car(
            self.user.oauth, another_car,
            self.another_user.name, self.another_user.phone) >> 404
        server.sell_car(
            self.user.oauth, invalid_car,
            self.another_user.name, self.another_user.phone) >> 404
        server.sell_car(
            self.user.oauth, self.car,
            self.another_user.name, self.another_user.phone) >> 200

        sms = self.get_sms_code(self.another_user.phone)
        server.confirm_sell_phone(self.user.oauth, another_car, sms) >> 404
        server.confirm_sell_phone(self.user.oauth, invalid_car, sms) >> 404
        server.confirm_sell_phone(self.user.oauth, self.car, sms) >> 200

        server.complete_sell(self.user.oauth, another_car, self.another_user.phone) >> 404
        server.complete_sell(self.user.oauth, invalid_car, self.another_user.phone) >> 404
        server.complete_sell(self.user.oauth, self.car, self.another_user.phone) >> 200

        sms = self.get_sms_code(self.user.phone)
        server.confirm_sell(self.user.oauth, another_car, sms) >> 404
        server.confirm_sell(self.user.oauth, invalid_car, sms) >> 404
        server.confirm_sell(self.user.oauth, self.car, sms) >> 200

        assert fakeenv.read_sms(self.another_user.phone) == [RECIPIENT_INITIAL_SMS]
