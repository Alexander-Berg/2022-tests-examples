import logging
import random

from .basic_test import BasicTest, RealEnvTest
import lib.remote_access_server as server

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User
from data_types.car_settings import CarSettings

logger = logging.getLogger("TestAutostarts")


VOLTAGE_CHECK_PRECISION = 0.01


class TestAutostarts(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def test_initial_autostart_settings(self):
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        autostart_settings = settings['engine']['autostartConditions']
        settings_stop_time = settings['engine']['stopConditions']['timeout']['currentSeconds']

        assert settings_stop_time == 20 * 60

        assert autostart_settings['engineTemperature']['isEnabled'] is False
        assert autostart_settings['engineTemperature']['value'] == -20

        assert autostart_settings['voltage']['isEnabled'] is False
        assert abs(autostart_settings['voltage']['value'] - 11.8) < VOLTAGE_CHECK_PRECISION

        assert autostart_settings['timeInterval']['isEnabled'] is False
        assert autostart_settings['timeInterval']['valueSeconds'] == 1 * 60 * 60

        assert len(autostart_settings['schedules']) == 0

    def test_autostart_settings_restore_after_rebind(self):
        dummy_schedule = CarSettings.get_dummy_autostarts_schedule()
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=10, is_enabled=True) >> 200
        server.create_schedule(self.user.oauth, self.car, dummy_schedule) >> 200

        server.delete_car(self.user.oauth, self.car)
        rebound_car = Car()
        rebound_car.registrate(self.user, self.telematics)

        settings = server.get_car_settings(self.user.oauth, rebound_car) >> 200
        voltage_settings = settings['engine']['autostartConditions']['voltage']
        schedules = settings['engine']['autostartConditions']['schedules']
        assert abs(voltage_settings['value'] - 11.8) < VOLTAGE_CHECK_PRECISION
        assert len(schedules) == 0


class TestAutostartsOnRealEnv(RealEnvTest):
    def setup(self):
        self.user, self.car, _ = self.get_test_units()

    def test_set_valid_autostart_settings(self):
        MIN_VOLTAGE = 10
        MAX_VOLTAGE = 12.5
        voltage = int(random.uniform(MIN_VOLTAGE, MAX_VOLTAGE) / 0.1) * 0.1
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=voltage, is_enabled=True) >> 200
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        voltage_settings = settings['engine']['autostartConditions']['voltage']
        assert voltage_settings['isEnabled'] is True
        assert abs(voltage_settings['value'] - voltage) < VOLTAGE_CHECK_PRECISION

        server.set_autostart_voltage(self.user.oauth, self.car, voltage=MIN_VOLTAGE, is_enabled=True) >> 200
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=MAX_VOLTAGE, is_enabled=True) >> 200

        MIN_TEMPERATURE = -40
        MAX_TEMPERATURE = 0
        temperature = random.randint(MIN_TEMPERATURE, MAX_TEMPERATURE)
        server.set_autostart_temperature(self.user.oauth, self.car, temperature=temperature, is_enabled=True) >> 200
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        temperature_settings = settings['engine']['autostartConditions']['engineTemperature']
        assert temperature_settings['isEnabled'] is True
        assert temperature_settings['value'] == temperature

        server.set_autostart_temperature(self.user.oauth, self.car, temperature=MIN_TEMPERATURE, is_enabled=True) >> 200
        server.set_autostart_temperature(self.user.oauth, self.car, temperature=MAX_TEMPERATURE, is_enabled=True) >> 200

        SECONDS_IN_HOUR = 60 * 60
        MIN_INTERVAL_HOUR = 1
        MAX_INTERVAL_HOUR = 23
        interval = random.randint(MIN_INTERVAL_HOUR, MAX_INTERVAL_HOUR) * SECONDS_IN_HOUR
        server.set_autostart_interval(self.user.oauth, self.car, interval=interval, is_enabled=True) >> 200
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        interval_settings = settings['engine']['autostartConditions']['timeInterval']
        assert interval_settings['isEnabled'] is True
        assert interval_settings['valueSeconds'] == interval

        server.set_autostart_interval(self.user.oauth, self.car, interval=MIN_INTERVAL_HOUR * SECONDS_IN_HOUR, is_enabled=True) >> 200
        server.set_autostart_interval(self.user.oauth, self.car, interval=MAX_INTERVAL_HOUR * SECONDS_IN_HOUR, is_enabled=True) >> 200

        MIN_STOP_TIME = 1 * 10 * 60
        MAX_STOP_TIME = 3 * 10 * 60
        stop_time = random.randint(1, 3) * 10 * 60
        server.set_engine_stop_time(self.user.oauth, self.car, stop_time) >> 200
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        settings_stop_time = settings['engine']['stopConditions']['timeout']['currentSeconds']
        assert settings_stop_time == stop_time

        server.set_engine_stop_time(self.user.oauth, self.car, MIN_STOP_TIME) >> 200
        server.set_engine_stop_time(self.user.oauth, self.car, MAX_STOP_TIME) >> 200

    def test_set_invalid_autostart_settings(self):
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=13, is_enabled=True) >> 422
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=9, is_enabled=True) >> 422
        server.set_autostart_voltage(self.user.oauth, self.car, voltage=10.15, is_enabled=True) >> 422

        server.set_autostart_temperature(self.user.oauth, self.car, temperature=-50, is_enabled=True) >> 422
        server.set_autostart_temperature(self.user.oauth, self.car, temperature=10, is_enabled=True) >> 422

        server.set_autostart_interval(self.user.oauth, self.car, interval=0, is_enabled=True) >> 422
        server.set_autostart_interval(self.user.oauth, self.car, interval=24, is_enabled=True) >> 422

        server.set_engine_stop_time(self.user.oauth, self.car, 500) >> 422

    def test_operations_with_schedule(self):
        dummy_schedule = CarSettings.get_dummy_autostarts_schedule()
        schedule_id = (server.create_schedule(self.user.oauth, self.car, dummy_schedule) >> 200)['id']
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        schedules = settings['engine']['autostartConditions']['schedules']
        assert len(schedules) == 1
        dummy_schedule['id'] = schedule_id
        assert schedules[0] == dummy_schedule
        dummy_schedule.pop('id')

        days_patch = {'days': CarSettings.random_sorted_weekdays(3)}
        server.edit_schedule(self.user.oauth, self.car, days_patch, schedule_id) >> 200

        enabled_patch = {'isEnabled': not dummy_schedule['isEnabled']}
        server.edit_schedule(self.user.oauth, self.car, enabled_patch, schedule_id) >> 200

        hours_patch = {'time': {'hours': random.randint(0, 23)}}
        server.edit_schedule(self.user.oauth, self.car, hours_patch, schedule_id) >> 200

        minutes_patch = {'time': {'minutes': random.randint(0, 59)}}
        server.edit_schedule(self.user.oauth, self.car, minutes_patch, schedule_id) >> 200

        patched_schedule = {
            'id': schedule_id,
            'isEnabled': enabled_patch['isEnabled'],
            'days': days_patch['days'],
            'time': {
                'hours': hours_patch['time']['hours'],
                'minutes': minutes_patch['time']['minutes']
            }
        }

        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        schedules = settings['engine']['autostartConditions']['schedules']
        assert schedules[0] == patched_schedule

        server.delete_schedule(self.user.oauth, self.car, schedule_id) >> 200
        settings = server.get_car_settings(self.user.oauth, self.car) >> 200
        schedules = settings['engine']['autostartConditions']['schedules']
        assert len(schedules) == 0
