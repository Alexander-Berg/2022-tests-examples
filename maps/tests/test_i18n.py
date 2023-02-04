import logging
import pytest
import random

from .basic_test import BasicTest
import lib.fakeenv as fakeenv
import lib.remote_access_server as server

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

logger = logging.getLogger("TestI18n")


class TestI18n(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.car = Car()

        self.telematics.registrate()
        self.car.registrate(self.user, self.telematics)

    def get_alarm_notification_title(self, settings):
        return settings['notifications']['phoneCalls']['items'][0]['title']

    def test_settings_i18n(self):
        ALARM_NOTIFICATION_TITLE_RU = self.get_text(keyset='settings', lang='ru', key='Alarm')
        ALARM_NOTIFICATION_TITLE_EN = self.get_text(keyset='settings', lang='en', key='Alarm')

        settings = server.get_car_settings(self.user.oauth, self.car, lang=None) >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_RU

        settings = server.get_car_settings(self.user.oauth, self.car, lang='ru') >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_RU

        settings = server.get_car_settings(self.user.oauth, self.car, lang='en_US') >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_EN

        settings = server.get_car_settings(self.user.oauth, self.car, lang=None) >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_EN

        settings = server.get_car_settings(self.user.oauth, self.car, lang='invalidLang') >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_RU

        settings = server.get_car_settings(
            self.user.oauth,
            self.car,
            lang='en_US',
            lang_as_get_parameter=True) >> 200
        title = self.get_alarm_notification_title(settings)
        assert title == ALARM_NOTIFICATION_TITLE_EN

    LANGUAGE_DATA = [
        ('en_US', 'en'),
        ('ru_RU', 'ru'),
    ]

    @pytest.mark.parametrize('lang_data', LANGUAGE_DATA)
    def test_sms_i18n(self, lang_data):
        server.get_car_settings(self.user.oauth, self.car, lang=lang_data[0]) >> 200
        server.unbind_phone(self.user.oauth) >> 200
        sms_list = fakeenv.read_sms(self.user.phone)
        assert len(sms_list) == 1, str(sms_list)
        sms = sms_list[0]
        canonical_text = self.get_text(keyset='SMS', lang=lang_data[1], key='CODE_REMOVE_PHONE_NUMBER')[:-2]
        assert sms.startswith(canonical_text), (sms, canonical_text)

    @pytest.mark.parametrize('lang_data', LANGUAGE_DATA)
    def test_error_codes_i18n(self, lang_data):
        wrong_oauth = ''.join(random.sample(self.user.oauth,  len(self.user.oauth)))
        response = server.get_car_settings(wrong_oauth, self.car, lang=lang_data[0]) >> 401
        description = response['description']
        canonical_text = self.get_text(keyset='error_texts', lang=lang_data[1], key='BAD_TOKEN')
        assert description == canonical_text, response
