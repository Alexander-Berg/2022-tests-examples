import logging
import rstr
import time

from .basic_test import BasicTest
import lib.remote_access_server as server
import lib.pandora as pandora

from data_types.telematics import Telematics
from data_types.car import Car
from data_types.user import User
from data_types.car_device import CarDevice

# service ticket with infinite lifetime
TVM_SERVICE_TICKET = ("3:serv:CBAQ__________9_IgYIexC9hns:KOZDFn7TyUk_NGuZw70BOQYY_lDZ7WMBvk_S0PETFIpGRvnnaQZ503Uq2-"
                      "6R9E4J2xypBWGCkWjwPQMqRtsiUt_Hk62rs61VIpxWsZ-Dono4y5tVcQPALD5Rc5PLaBuPGiqHR47c5nODvb00RbL1ourZhLaH1vH-AQBcgC6Fhwc")

logger = logging.getLogger()

EXPECTED_SYNCH_PERIOD_SECONDS = 2
POLL_PERIOD_SECONDS = 0.5
POLL_ITERATIONS_COUNT = int(EXPECTED_SYNCH_PERIOD_SECONDS / POLL_PERIOD_SECONDS)


def with_request_id(func, *vargs, **kwargs):
    request_id = rstr.digits(10)
    kwargs["request_id"] = request_id

    result = func(*vargs, **kwargs)
    if result.code == 200:
        assert result.body["request_id"] == request_id
        del result.body["request_id"]
        return result
    else:
        return result


def get_devices(user):
    response = with_request_id(server.alice_get_devices, user, service_ticket=TVM_SERVICE_TICKET)
    if response.code != 200:
        return response
    assert response.body["payload"]["user_id"] == user.oauth
    return list([CarDevice.from_json(dev) for dev in response.body["payload"]["devices"]])


def query_devices_state(user, devices):
    response = with_request_id(
        server.alice_query_devices, user,
        devices={"devices": [dict(id=dev.id) for dev in devices]})

    if response.code != 200:
        return response

    dev_states = response.body["payload"]["devices"]
    assert len(dev_states) == len(devices)
    for dev_state in dev_states:
        dev = list(filter(lambda d: d.id == dev_state["id"], devices))
        assert len(dev) == 1
        dev[0].read_state_from_json(dev_state)


def perform_actions(user, actions):
    devices = {}
    for action in actions:
        dev_query = action.get_query()
        if dev_query["id"] in devices:
            devices[dev_query["id"]]["capabilities"] += dev_query["capabilities"]
        else:
            devices[dev_query["id"]] = dev_query

    payload = {
        "payload": {
            "devices": [dev for _, dev in devices.items()]
        }
    }
    response = with_request_id(
        server.alice_action, user, payload=payload)

    if response.code != 200:
        return response

    for action in actions:
        action.save_result(response.body)


class TestAliceWithTrunkSupport(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car(brand="Nissan", model="Qashqai", features=["trunk"])  # car with trunk support
        self.car.registrate(self.user, self.telematics)

        self.device = CarDevice.from_car(self.car)
        assert get_devices(self.user) == [self.device]

        query_devices_state(self.user, [self.device])
        assert self.device.get_lock_state()
        assert not self.device.get_engine_state()
        assert not self.device.get_trunk_state()

        car_state = server.get_car(self.user.oauth, self.car) >> 200
        assert car_state["lockStatus"] == "locked"
        assert car_state["engine"]["status"] == "off"

    def wait_for(self, synch, check):
        for _ in range(POLL_ITERATIONS_COUNT):
            state = synch()
            res = check(state) if state else check()
            if res:
                return res
            else:
                time.sleep(POLL_PERIOD_SECONDS)

        return False

    def wait_for_device_state(self, device, clbk):
        return self.wait_for(lambda: query_devices_state(self.user, [device]), clbk)

    def wait_for_car_state(self, clbk):
        return self.wait_for(lambda: server.get_car(self.user.oauth, self.car) >> 200, clbk)

    def test_ping(self):
        server.alice_ping() >> 200

    def test_start_unlocked(self):
        lock_action = self.device.get_lock_action(False)
        perform_actions(self.user, [lock_action])
        assert lock_action.succeded()
        assert self.wait_for_car_state(lambda state: state["lockStatus"] == "unlocked")
        query_devices_state(self.user, [self.device])
        assert not self.device.get_lock_state()
        assert not self.device.get_engine_state()
        state = server.get_car(self.user.oauth, self.car) >> 200
        assert state["lockStatus"] == "unlocked"
        assert state["engine"]["status"] == "off"

        start_action = self.device.get_engine_action(True)
        perform_actions(self.user, [start_action])
        assert not start_action.succeded()
        assert start_action.error_code() == "DEVICE_BUSY"
        query_devices_state(self.user, [self.device])
        assert not self.device.get_engine_state()
        state = server.get_car(self.user.oauth, self.car) >> 200
        assert state["lockStatus"] == "unlocked"
        assert state["engine"]["status"] == "off"

        lock_action = self.device.get_lock_action(True)
        perform_actions(self.user, [lock_action])
        assert lock_action.succeded()
        assert self.wait_for_car_state(lambda state: state["lockStatus"] == "locked")
        query_devices_state(self.user, [self.device])
        assert self.device.get_lock_state()
        state = server.get_car(self.user.oauth, self.car) >> 200
        assert state["lockStatus"] == "locked"
        assert state["engine"]["status"] == "off"

        start_action = self.device.get_engine_action(True)
        perform_actions(self.user, [start_action])
        assert start_action.succeded()
        assert self.wait_for_car_state(lambda state: state["engine"]["status"] == "on")
        query_devices_state(self.user, [self.device])
        assert self.device.get_lock_state()
        assert self.device.get_engine_state()
        state = server.get_car(self.user.oauth, self.car) >> 200
        assert state["lockStatus"] == "locked"
        assert state["engine"]["status"] == "on"

    def test_unlock_openned_car(self):
        server.open_trunk(self.user.oauth, self.car) >> 200

        assert self.wait_for_car_state(lambda state: state["trunk"] == "unlocked")

        lock_action = self.device.get_lock_action(False)
        perform_actions(self.user, [lock_action])
        assert not lock_action.succeded()
        assert lock_action.error_code() == "DEVICE_BUSY"

    def test_cannot_close_trunk(self):
        open_trunk = self.device.get_trunk_action(True)
        perform_actions(self.user, [open_trunk])
        assert open_trunk.succeded()

        assert self.wait_for_car_state(lambda state: state["trunk"] == "unlocked")
        query_devices_state(self.user, [self.device])
        assert self.device.get_trunk_state()

        close_trunk = self.device.get_trunk_action(False)
        perform_actions(self.user, [close_trunk])
        assert not close_trunk.succeded()
        assert close_trunk.error_code() == "INVALID_ACTION"

    def test_car_offline(self):
        pandora.set_car_online(self.telematics, False)

        supported_actions = [
            self.device.get_lock_action(False),
            self.device.get_engine_action(True),
            self.device.get_trunk_action(True)
        ]
        for action in supported_actions:
            perform_actions(self.user, [action])
            assert not action.succeded()
            assert action.error_code() == "DEVICE_UNREACHABLE"

    def test_get_devices_with_device_id(self):
        response = server.alice_get_devices(
            self.user,
            service_ticket=TVM_SERVICE_TICKET,
            device_id=self.user.device_id,
            request_id="1") >> 200

        actual_devices = list([CarDevice.from_json(dev) for dev in response["payload"]["devices"]])
        assert actual_devices == [CarDevice.from_car(self.car)]

        wrong_device_id = self.user.device_id + "0"
        server.alice_get_devices(
            self.user,
            service_ticket=TVM_SERVICE_TICKET,
            device_id=wrong_device_id,
            request_id="1") >> 403


class TestAliceWithoutTrunkSupport(BasicTest):
    def setup(self):
        self.user = User()
        self.user.registrate()

        self.another_user = User()
        self.another_user.registrate()

        self.telematics = Telematics()
        self.telematics.registrate()

        self.car = Car(brand="Nissan", model="X-Trail", features=[])  # car without trunk support
        self.car.registrate(self.user, self.telematics)

        self.device = CarDevice.from_car(self.car)
        assert get_devices(self.user) == [self.device]

        query_devices_state(self.user, [self.device])
        assert self.device.get_lock_state()
        assert not self.device.get_engine_state()
        assert not self.device.has_trunk_support()

        car_state = server.get_car(self.user.oauth, self.car) >> 200
        assert car_state["lockStatus"] == "locked"
        assert car_state["engine"]["status"] == "off"

    def test_trunk_is_not_supported(self):
        open_trunk = self.device.get_trunk_action(True)
        response = perform_actions(self.user, [open_trunk])
        assert response.code == 404

        close_trunk = self.device.get_trunk_action(False)
        response = perform_actions(self.user, [close_trunk])
        assert response.code == 404
        # assert close_trunk.error_code() == "INVALID_ACTION"
