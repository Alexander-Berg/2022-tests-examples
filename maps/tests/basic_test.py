from dateutil import parser, tz
import json
import yatest
import time
import os
import polib
import pytest

from library.python import resource

from maps.automotive.libs.large_tests.lib.yacare import wait_for_yacare_startup
import maps.automotive.libs.large_tests.lib.docker as docker
import lib.fakeenv as fakeenv
import lib.remote_access_server as server
import lib.db as db
import lib.pandora as pandora
import lib.poller as poller

from data_types.car import Car
from data_types.user import User
from data_types.telematics import Telematics

ENV_TYPE = os.getenv('TEST_ENV_TYPE') or 'autotests'
IS_REAL_ENV = ENV_TYPE != 'autotests'


class BasicTest:
    def get_text(self, keyset, key, lang='ru', project='maps-auto-remote_access-texts'):
        po_resource_name = '{}_{}_{}_po'.format(project, keyset, lang)
        if po_resource_name not in self.po_cache:
            po_data = polib.pofile(resource.find(po_resource_name).decode('utf-8'))
            self.po_cache[po_resource_name] = {entry.msgid: entry.msgstr for entry in po_data}
        return self.po_cache[po_resource_name][key]

    def load_poller_config(self):
        file = "maps/automotive/remote_access/pandora_poller/config/config.json.%s" % ENV_TYPE
        config = yatest.common.source_path(file)
        assert os.path.exists(config), "cannot find config file '{}'".format(file)
        with open(config) as f:
            data = json.load(f)
            self.poll_period = float(data["poller"]["pollPeriod_sec"])
            self.selling_period = float(data["healthchecker"]["sellRequestLifetime"])

    def wait_for(self, timeout, split_factor=4):
        sleep_step = timeout / split_factor
        for i in range(split_factor):
            time.sleep(sleep_step)
            yield i

    def wait_for_poller(self, poll_iterations=2, split_factor=4):
        return self.wait_for(
            timeout=self.poll_period * poll_iterations,
            split_factor=split_factor)

    def wait_for_sell(self, split_factor=8):
        return self.wait_for(
            timeout=self.selling_period * 2,
            split_factor=split_factor)

    def setup_method(self):
        self.load_poller_config()
        self.po_cache = {}

    def setup_class(self):
        if IS_REAL_ENV:
            pytest.skip('Test do not support real env')
        docker.set_config_dir("maps/automotive/remote_access/autotests/config/")
        db.initialize()
        wait_for_yacare_startup(server.get_url(), server.get_host())
        wait_for_yacare_startup(pandora.get_url(), pandora.get_host())
        wait_for_yacare_startup(poller.get_url(), poller.get_host())

    def teardown_method(self):
        poller.stop()
        fakeenv.reset()
        db.reset()
        poller.start()

    def get_sms_code(self, phone):
        sms_list = fakeenv.read_sms(phone)
        assert len(sms_list) == 1, str(sms_list)
        return sms_list[0][-4:]

    def get_datasync_flag(self, user):
        response = fakeenv.get_datasync_flag(user.oauth) >> 200
        return response.get("user_has_car")

    def parse_datetime(self, str_time):
        return int(parser.parse(str_time).astimezone(tz=tz.tzlocal()).strftime('%s'))

    def is_alice_notified(self, token):
        requests_list = fakeenv.get_alice_requests(token) >> 200
        assert len(requests_list) < 2, requests_list
        return len(requests_list) == 1


class RealEnvTest(BasicTest):
    def setup_class(cls):
        if IS_REAL_ENV:
            test_data_file = yatest.common.source_path("maps/automotive/remote_access/release_tests/data.json")
            with open(test_data_file) as f:
                cls.test_data = json.load(f)
            server.set_url("https://auto-remote-access-server.%s.maps.yandex.net" % ENV_TYPE)
            pandora.set_url("https://auto-lab-pandora-emulator.%s.maps.yandex.net" % ENV_TYPE)
        else:
            BasicTest().setup_class()

    def teardown_method(self):
        if not IS_REAL_ENV:
            BasicTest().teardown_method()

    def get_test_units(self):
        if IS_REAL_ENV:
            assert len(self.test_data) > 0
            unit = self.test_data[0]
            token = os.getenv(unit["user"]["oauth"])
            assert token, "No token found: " + unit["user"]["oauth"]
            user = User(
                oauth=token,
                name=unit["user"]["name"],
                phone=unit["user"]["phone"])
            telematics = Telematics(
                hwid=unit["telematics"]["hwid"],
                login=unit["telematics"]["login"],
                password=unit["telematics"]["password"],
                phone=unit["telematics"]["phone"],
                pin=unit["telematics"]["pin"])
            car = Car(
                car_id=unit["car"]["uuid"],
                brand=unit["car"]["brand"],
                model=unit["car"]["model"],
                year=unit["car"]["year"],
                plate=unit["car"]["plate"])
            return user, car, telematics
        else:
            user = User()
            user.registrate()
            telematics = Telematics()
            telematics.registrate()
            car = Car()
            car.registrate(user, telematics)
            return user, car, telematics
