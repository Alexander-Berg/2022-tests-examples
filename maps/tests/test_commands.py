import logging
import pytest

from .basic_test import RealEnvTest
from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User

import lib.remote_access_server as server
import lib.pandora as pandora
import time
import copy

logger = logging.getLogger("TestCars")
FIELDS_TO_REMOVE = [["lastCommandTime"], ["updatedAt"]]
EVENTS_NUMBER_TO_TEST = 10
EVENTS_LIMIT = 5


class TestCommands(RealEnvTest):
    def setup(self):
        self.requests_tries_count = 5
        self.seconds_for_waiting = self.poll_period * 2 / self.requests_tries_count
        self.user, self.car, self.telematics = self.get_test_units()
        pandora.set_default_state(self.telematics)

    class TurningData:
        class FixedSettings:
            def __init__(self, turning_data):
                self.car_state = turning_data.get_car()

            def check_values(self, response, checking_list=[], remove_list=FIELDS_TO_REMOVE):
                car_state = copy.deepcopy(self.car_state)
                for (name_list, value) in checking_list:
                    state_last = None
                    last_name = None
                    cur = car_state
                    for name in name_list:
                        state_last = cur
                        last_name = name
                        if name not in cur:
                            cur[name] = {}
                        cur = cur[name]

                    state_last[last_name] = value

                for name_list in remove_list:
                    state_cur = car_state
                    response_cur = response
                    state_last = None
                    response_last = None
                    last_name = None
                    for name in name_list:
                        state_last = state_cur
                        response_last = response_cur
                        last_name = name
                        if name not in state_cur:
                            state_cur[name] = {}
                        state_cur = state_cur[name]
                        if name not in response_cur:
                            response_cur[name] = {}
                        response_cur = response_cur[name]

                    del state_last[last_name]
                    del response_last[last_name]

                assert response == car_state

        def set_test(self, test):
            self.test_ = test
            self.settings_ = self.FixedSettings(self)

        def get_car(self):
            return server.get_car(self.test_.user.oauth, self.test_.car) >> 200

        def get_car_events(self, command_timestamp):
            return server.get_car_events(
                self.test_.user.oauth, self.test_.car, command_timestamp) >> 200

        def filter_events(self, response):
            return [
                {
                    name: event[name]
                    for name in event
                    if name not in ['timestamp']
                }
                for event in response]

        def waiting_for(self, condition, tries_count, waiting_time):
            for counter in range(0, tries_count):
                time.sleep(waiting_time)
                if condition():
                    break

        def waiting_for_state(self, waiting_state):
            self.waiting_for(lambda: self.get_status() != waiting_state,
                             self.test_.requests_tries_count, self.test_.seconds_for_waiting)

        def prepare(self):
            pass

    class ImpossibleMaintenanceData(TurningData):
        def get_status(self):
            return str(self.get_car()["isInServiceMode"])

        def impossible_command(self):
            timestamp = self.get_car()["updatedAt"]
            response = server.turn_service_mode_on(self.test_.user.oauth, self.test_.car) >> 406
            assert response == {
                "code": "cannotTurnOnServiceMode",
                "description": "Нужно сначала открыть машину и завести двигатель"
            }
            return timestamp

        def waiting(self):
            self.waiting_for_state("False")

        def assert_result(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["isInServiceMode"], False)])

        def assert_result_events(self, command_timestamp):
            assert self.filter_events(self.get_car_events(command_timestamp)) == [{
                "target": "maintenance",
                "severity": "error",
                "code": "cannotTurnOnServiceMode",
                "command": "setting",
                "description": "Нужно сначала открыть машину и завести двигатель"}]

    class ImpossibleLockData(TurningData):
        def get_status(self):
            return self.get_car()["lockStatus"]

        def prepare(self):
            pandora.set_lock(self.test_.telematics, False) >> 200
            self.pandora_door_unlock()
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["lockStatus"], "unlocked"), (self.door_status_fields_, "unlocked")])

        def impossible_command(self):
            response = server.lock(self.test_.user.oauth, self.test_.car) >> 200
            return response["timestamp"]

        def waiting(self):
            self.waiting_for_state("locking")

        def assert_result(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["lockStatus"], "unlocked"), (self.door_status_fields_, "unlocked")])

        def assert_result_events(self, command_timestamp):
            assert self.filter_events(self.get_car_events(command_timestamp)) == [{
                "target": "lock",
                "severity": "error",
                "code": "cannotLock",
                "command": "locking",
                "description": "Не удалось закрыть машину. Проверьте двери, капот и багажник"}]

    class ImpossibleUnlockData(TurningData):
        def prepare(self):
            pandora.set_alarm(self.test_.telematics, True) >> 200
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["alarmStatus"], "set")],
                )

        def get_status(self):
            return self.get_car()["alarmStatus"]

        def impossible_command(self):
            response = server.unlock(self.test_.user.oauth, self.test_.car) >> 200
            return response["timestamp"]

        def waiting(self):
            self.waiting_for_state("unsetting")

        def assert_result(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["alarmStatus"], "unset"), (["lockStatus"], "locked")])

        def assert_result_events(self, command_timestamp):
            assert self.filter_events(self.get_car_events(command_timestamp)) == [{
                "target": "lock",
                "severity": "error",
                "code": "alarmIsUnset",
                "command": "unsetting",
                "description": "Тревога выключена"}]

    class ImpossibleLockFrontLeft(ImpossibleLockData):
        def __init__(self):
            self.door_status_fields_ = ["doors", "frontLeft"]

        def pandora_door_unlock(self):
            pandora.set_front_left_door(self.test_.telematics, True) >> 200

    class ImpossibleLockFrontRight(ImpossibleLockData):
        def __init__(self):
            self.door_status_fields_ = ["doors", "frontRight"]

        def pandora_door_unlock(self):
            pandora.set_front_right_door(self.test_.telematics, True) >> 200

    class ImpossibleLockRearLeft(ImpossibleLockData):
        def __init__(self):
            self.door_status_fields_ = ["doors", "rearLeft"]

        def pandora_door_unlock(self):
            pandora.set_back_left_door(self.test_.telematics, True) >> 200

    class ImpossibleLockRearRight(ImpossibleLockData):
        def __init__(self):
            self.door_status_fields_ = ["doors", "rearRight"]

        def pandora_door_unlock(self):
            pandora.set_back_right_door(self.test_.telematics, True) >> 200

    class ImpossibleLockTrunk(ImpossibleLockData):
        def __init__(self):
            self.door_status_fields_ = ["trunk"]

        def pandora_door_unlock(self):
            pandora.set_trunk(self.test_.telematics, True) >> 200

    IMPOSSIBLE_TURNING_DATA = [
        ImpossibleMaintenanceData(),
        ImpossibleLockFrontLeft(),
        ImpossibleLockFrontRight(),
        ImpossibleLockRearLeft(),
        ImpossibleLockRearRight(),
        ImpossibleLockTrunk(),
        ImpossibleUnlockData()
    ]

    @pytest.mark.parametrize("turning_data", IMPOSSIBLE_TURNING_DATA)
    def test_impossible_turning(self, turning_data):
        turning_data.set_test(self)
        turning_data.prepare()
        command_timestamp = turning_data.impossible_command()
        turning_data.waiting()
        turning_data.assert_result()
        waiting_condition = lambda: turning_data.get_car_events(command_timestamp)
        turning_data.waiting_for(waiting_condition, self.requests_tries_count, self.seconds_for_waiting)
        turning_data.assert_result_events(command_timestamp)

    class SuccessfulEngineProcessingData(TurningData):
        def get_status(self):
            return self.get_car()["engine"]["status"]

        def assert_off(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["engine", "status"], "off")])

        def turning_on_command(self):
            server.start_engine(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_on(self):
            self.waiting_for_state("starting")

        def assert_on(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[
                    (["engine", "status"], "on")],
                remove_list=FIELDS_TO_REMOVE + [
                    ["engine", "startMode"],
                    ["engine", "warmupTime", "elapsedSeconds"],
                    ["engine", "warmupTime", "remainingSeconds"],
                    ["engine", "warmupTime", "totalSeconds"]])

        def turning_off_command(self):
            server.stop_engine(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_off(self):
            self.waiting_for_state("stopping")

    class SuccessfulLockProcessingData(TurningData):
        def get_status(self):
            return self.get_car()["lockStatus"]

        def assert_off(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["lockStatus"], "locked")])

        def turning_on_command(self):
            server.unlock(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_on(self):
            self.waiting_for_state("unlocking")

        def assert_on(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["lockStatus"], "unlocked")])

        def turning_off_command(self):
            server.lock(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_off(self):
            self.waiting_for_state("locking")

    class SuccessfulTrunkProcessingData(TurningData):
        def get_status(self):
            return self.get_car()["trunk"]

        def assert_off(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["trunk"], "locked")])

        def turning_on_command(self):
            server.open_trunk(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_on(self):
            self.waiting_for_state("unlocking")

        def assert_on(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["trunk"], "unlocked")])

        def turning_off_command(self):
            server.close_trunk(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_off(self):
            self.waiting_for_state("locking")

    class SuccessfulMaintenanceProcessingData(TurningData):
        def get_status(self):
            return str(self.get_car()["isInServiceMode"])

        def prepare(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[(["isInServiceMode"], False)])
            pandora.set_engine(self.test_.telematics, True) >> 200
            pandora.set_lock(self.test_.telematics, False) >> 200

        def assert_off(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[
                    (["lockStatus"], "unlocked"),
                    (["isInServiceMode"], False),
                    (["engine", "status"], "on"),
                    (["engine", "startMode"], "manual")],
                remove_list=FIELDS_TO_REMOVE)

        def turning_on_command(self):
            server.turn_service_mode_on(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_on(self):
            self.waiting_for_state("True")

        def assert_on(self):
            self.settings_.check_values(
                self.get_car(),
                checking_list=[
                    (["lockStatus"], "unlocked"),
                    (["isInServiceMode"], True),
                    (["engine", "status"], "on"),
                    (["engine", "startMode"], "manual")],
                remove_list=FIELDS_TO_REMOVE)

        def turning_off_command(self):
            server.turn_service_mode_off(self.test_.user.oauth, self.test_.car) >> 200

        def waiting_off(self):
            self.waiting_for_state("False")

    SUCCESSFULL_TURNING_DATA = [
        SuccessfulEngineProcessingData(),
        SuccessfulLockProcessingData(),
        SuccessfulTrunkProcessingData(),
        SuccessfulMaintenanceProcessingData()
    ]

    @pytest.mark.parametrize("turning_data", SUCCESSFULL_TURNING_DATA)
    def test_successful_turning(self, turning_data):
        turning_data.set_test(self)
        turning_data.prepare()
        turning_data.assert_off()
        turning_data.turning_on_command()
        turning_data.waiting_on()
        turning_data.assert_on()
        turning_data.turning_off_command()
        turning_data.waiting_off()
        turning_data.assert_off()


class TestCommandsEvents(RealEnvTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car()
        self.car.registrate(self.user, self.telematics)

    def test_events_limit(self):
        car = server.get_car(self.user.oauth, self.car) >> 200
        command_timestamp = car["updatedAt"]

        pandora.set_lock(self.telematics, False) >> 200
        pandora.set_front_left_door(self.telematics, True) >> 200

        for i in range(EVENTS_NUMBER_TO_TEST):
            server.lock(self.user.oauth, self.car) >> 200

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp) >> 200

        assert len(events) == EVENTS_NUMBER_TO_TEST

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp, limit=EVENTS_LIMIT) >> 200

        assert len(events) == EVENTS_LIMIT

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp,
            limit=EVENTS_LIMIT,
            target="lock") >> 200

        assert len(events) == EVENTS_LIMIT

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp,
            limit=EVENTS_LIMIT,
            target="engine") >> 200

        assert len(events) == 0

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp,
            limit=EVENTS_LIMIT,
            severity="error") >> 200

        assert len(events) == EVENTS_LIMIT

        events = server.get_car_events(
            self.user.oauth, self.car, command_timestamp,
            limit=EVENTS_LIMIT,
            severity="non_error") >> 400
