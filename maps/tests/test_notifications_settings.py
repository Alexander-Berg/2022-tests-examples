import logging

from .basic_test import BasicTest

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.car_settings import CarSettings
from data_types.common_types import StringId
from data_types.user import User

logger = logging.getLogger()


class TestCarSettings(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def test_check_settings(self):
        settings = CarSettings(self.user)

        assert settings.to_dict() == self.car.get_settings(self.user).to_dict()

        settings.notifications_random_change()

        self.car.set_notifications_settings(self.user, settings.value['notifications'])

        assert settings.to_dict() == self.car.get_settings(self.user).to_dict()

    def test_check_settings_on_sharing(self):
        user_settings = CarSettings(self.user)
        another_settings = CarSettings(self.another_user)

        assert user_settings.to_dict() == self.car.get_settings(self.user).to_dict()

        self.car.share_access(self.user, self.another_user)
        user_settings.value['sharedAccess'] = [{
            'id': StringId(),
            'name': self.another_user.name,
            'phone': self.another_user.get_masked_phone()
        }]
        another_settings.value['isOwner'] = False

        assert user_settings.to_dict() == self.car.get_settings(self.user).to_dict()
        assert another_settings.to_dict() == self.car.get_settings(self.another_user).to_dict()

        while user_settings.to_dict() == another_settings.to_dict():
            user_settings.notifications_random_change()
            another_settings.notifications_random_change()

        self.car.set_notifications_settings(self.user, user_settings.value['notifications'])
        self.car.set_notifications_settings(self.another_user, another_settings.value['notifications'])

        assert user_settings.to_dict() == self.car.get_settings(self.user).to_dict()
        assert another_settings.to_dict() == self.car.get_settings(self.another_user).to_dict()

    def check_phone_in_pandora(self, user, target_phone, unused_phone):
        target_events_field = self.telematics.get_events_field(target_phone)
        unused_events_field = self.telematics.get_events_field(unused_phone)

        settings = CarSettings(user)
        settings.value['notifications']["phoneCalls"]["isEnabled"] = False
        settings.value['notifications']["sms"]["isEnabled"] = False

        DISABLED_MASK = 0
        PHONE_MASK = 1
        SMS_MASK = 2

        self.car.set_notifications_settings(user, settings.value['notifications'])

        alarm_settings_before = self.telematics.get_alarm_settings()
        assert user.phone == alarm_settings_before[target_phone]
        assert alarm_settings_before[target_events_field] == DISABLED_MASK

        settings.value['notifications']["sms"]["isEnabled"] = True

        self.car.set_notifications_settings(user, settings.value['notifications'])
        alarm_settings_after = self.telematics.get_alarm_settings()
        assert user.phone == alarm_settings_after[target_phone]
        assert alarm_settings_after[target_events_field] == SMS_MASK

        assert alarm_settings_before[unused_phone] == alarm_settings_after[unused_phone]
        assert alarm_settings_before[unused_events_field] == \
            alarm_settings_after[unused_events_field]

        settings.value['notifications']["phoneCalls"]["isEnabled"] = True

        self.car.set_notifications_settings(user, settings.value['notifications'])
        alarm_settings_sms_calls = self.telematics.get_alarm_settings()
        assert alarm_settings_sms_calls[target_events_field] == SMS_MASK | PHONE_MASK

        settings.value['notifications']["sms"]["isEnabled"] = False

        self.car.set_notifications_settings(user, settings.value['notifications'])
        alarm_settings_calls = self.telematics.get_alarm_settings()
        assert alarm_settings_calls[target_events_field] == PHONE_MASK

    def test_check_settings_passed_to_pandora(self):
        self.check_phone_in_pandora(
            self.user, target_phone="phone_2", unused_phone="phone_3")

    def test_check_settings_passed_to_pandora_for_shared_user(self):
        self.car.share_access(self.user, self.another_user)
        self.check_phone_in_pandora(
            self.another_user, target_phone="phone_3", unused_phone="phone_2")

    def test_shared_phone_changed(self):
        self.car.share_access(self.user, self.another_user)

        alarm_settings_after = self.telematics.get_alarm_settings()
        assert self.another_user.phone == alarm_settings_after["phone_3"]

        self.another_user.change_phone()
        alarm_settings_after = self.telematics.get_alarm_settings()
        assert self.another_user.phone == alarm_settings_after["phone_3"]
