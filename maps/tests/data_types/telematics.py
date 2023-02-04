import rstr

import lib.remote_access_server as server
import lib.pandora as pandora


class Telematics:
    def __init__(self, hwid=None, login=None, password=None, phone=None, pin=None):
        self.hwid = hwid or "1" + rstr.digits(15)
        self.login = login or "2" + rstr.digits(7, 10)
        self.password = password or rstr.letters(5, 15)
        self.phone = phone or "+7" + rstr.digits(10)
        self.pin = pin or rstr.digits(4)

    def registrate(self):
        status, response = pandora.add_car(self)
        assert status == 200, response

        status, response = server.add_telematics(self)
        assert status == 200, response

    def get_alarm_settings(self):
        response = pandora.get_settings(self) >> 200
        return response['device_settings'][self.login][0]['alarm_settings']

    @staticmethod
    def get_events_field(phone):
        phone_to_events_field = {
            # Not a typo, but a requirement: 2 => 1, 3 => 2
            'phone_2': 'phone_1_events',
            'phone_3': 'phone_2_events',
            'phone_rescue1': 'phone_rescue1',
            'phone_rescue2': 'phone_rescue2',
        }
        return phone_to_events_field[phone]
