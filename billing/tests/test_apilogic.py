from unittest import mock
import pytest
from datetime import datetime
import pytz

from . import service_builder as sb
from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.apilogic import Logic
from billing.apikeys.apikeys.http_core import HttpError
from billing.apikeys.apikeys.rpcutil import XMLRPCInvalidParam
from utils import mock_datetime


@pytest.fixture()
def self(logic_base):
    class TestApiLogic(logic_base):
        logic = None
        service_with_active_key = None
        service_with_inactive_key = None
        service_with_several_counters = None
        key_detached = None
        key_baned = None
        key_for_ban = None

        @classmethod
        def setUpClass(cls):
            cls.logic = Logic()

            auto_approved_service_builder_director = sb.ServiceBuilderDirector(sb.AutoApprovedServiceBuilder)
            cls.service_with_active_key = auto_approved_service_builder_director.build()
            manually_approved_service_builder_director = sb.ServiceBuilderDirector(sb.ManuallyApprovedServiceBuilder)
            cls.service_with_inactive_key = manually_approved_service_builder_director.build()
            multi_counter_service_builder_director = sb.ServiceBuilderDirector(sb.MultiCounterServiceBuilder)
            cls.service_with_several_counters = multi_counter_service_builder_director.build()
            service_builder_director = sb.ServiceBuilderDirector(sb.LimitedServiceBuilder)
            cls.service_with_limits = service_builder_director.build()
            service_builder_director = sb.ServiceBuilderDirector(sb.MultiLimitedServiceBuilder)
            cls.service_with_multi_limits = service_builder_director.build()

            services = [
                cls.service_with_active_key,
                cls.service_with_inactive_key,
                cls.service_with_several_counters,
                cls.service_with_limits,
                cls.service_with_multi_limits
            ]

            cls.key_baned = mapper.Key.create(cls.external_user.get_default_project())
            cls.key_for_ban = mapper.Key.create(cls.external_user.get_default_project())
            cls.key_detached = mapper.Key.create(cls.external_user.get_default_project())

            cls.key_regular.get_service_config(cls.service_regular)

            for service in services:
                cls.key_regular.attach_to_service(service)

            ksc = cls.key_baned.attach_to_service(cls.service_with_inactive_key)
            link = ksc.link
            link.config.banned = True
            link.save()

            cls.key_for_ban.attach_to_service(cls.service_with_active_key)

        @classmethod
        def tearDownClass(cls):
            cls.key_regular.get_service_config(cls.service_with_active_key).delete()
            cls.key_regular.get_service_config(cls.service_with_inactive_key).delete()
            cls.key_regular.get_service_config(cls.service_with_several_counters).delete()
            cls.key_baned.get_service_config(cls.service_with_inactive_key).delete()
            cls.key_for_ban.get_service_config(cls.service_with_active_key).delete()
            cls.service_with_several_counters.delete()
            cls.service_with_inactive_key.delete()
            cls.service_with_active_key.delete()
            cls.key_detached.delete()
            cls.key_baned.delete()
            cls.key_for_ban.delete()

        def wrap_create_key(self, token, user_uid):
            key = self.logic.create_key({
                'service_token': token,
                'user_uid': user_uid,
            })
            assert 'result' in key and 'key' in key['result']
            key = mapper.Key.getone(id=key['result']['key'])
            assert isinstance(key, mapper.Key)
            return key

        def wrap_error_on_create_key(self, token, user_uid, message):
            with pytest.raises(HttpError, match=message):
                self.wrap_create_key(token, user_uid)

        def wrap_check_key(self, token, key):
            response = self.logic.check_key({
                'service_token': token,
                'user_ip': '0.0.0.0',
                'key': key
            })
            assert response['result'] == 'OK'
            assert response['key_info']['id'] == key

        def wrap_get_link_info(self, token, key):
            response = self.logic.get_link_info({
                'service_token': token,
                'user_ip': '0.0.0.0',
                'key': key
            })
            assert response['result'] == 'OK'
            assert response['link_info']['key'] == key
            return response

        def wrap_error_on_check_key(self, token, key, message):
            with pytest.raises(HttpError, match=message):
                self.wrap_check_key(token, key)

        def wrap_get_counter(self, cc, service_id, key):
            return mapper.KeyServiceCounter.getone(
                unit_id=mapper.Unit.getone(cc=cc).id,
                service_id=service_id,
                key=key).counter

        def wrap_update_counter(self, token, key, values):
            self.logic.update_counters(dict(
                service_token=token,
                key=key,
                **values
            ))

        def wrap_error_on_update_counter(self, token, key, values, message):
            with pytest.raises(HttpError, match=message):
                self.wrap_update_counter(token, key, values)

    TestApiLogic.setUpClass()
    yield TestApiLogic()
    TestApiLogic.tearDownClass()


def test_logic_create_key(self):
    key = self.wrap_create_key(self.service_regular.token, self.external_user.uid)
    self.wrap_check_key(self.service_regular.token, key.id)
    key.delete()


def test_logic_create_key_not_exists_uid(self, empty_blackbox_mock):
    self.wrap_error_on_create_key(
        self.service_with_active_key.token, self.not_exists_user_uid, 'User not found')


def test_logic_get_link_info(self):
    self.wrap_get_link_info(self.service_with_active_key.token, self.key_regular.id)


def test_logic_get_link_info_with_limits(self):
    for counter in self.key_regular.get_service_config(self.service_with_limits).get_counters():
        mapper.HourlyStat(dt=datetime(2021, 1, 1, 12, tzinfo=pytz.utc), counter_id=counter.id, value=1).save()
    with mock.patch('billing.apikeys.apikeys.mapper.keys.datetime',
                    new=mock_datetime(datetime(2020, 1, 1, 20, tzinfo=pytz.UTC))):
        response = self.wrap_get_link_info(self.service_with_limits.token, self.key_regular.id)
    assert response['link_info']['limit_stats'] == {
        'autotest_autotest_limited_service': {
            'value_unrolled': 0,
            'value_rolled': 1
        }
    }


def test_logic_check_key_active(self):
    self.wrap_check_key(self.service_with_several_counters.token, self.key_regular.id)


def test_logic_check_key_not_approved(self):
    self.wrap_error_on_check_key(
        self.service_with_inactive_key.token, self.key_regular.id, 'Key is not active')


def test_logic_check_key_detached(self):
    self.wrap_error_on_check_key(
        self.service_with_active_key.token, self.key_detached.id, 'Key is not attached to the service')


def test_logic_check_key_baned(self):
    self.wrap_error_on_check_key(
        self.service_with_inactive_key.token, self.key_baned.id, 'Key is not active')


def test_logic_ban_key(self):
    self.logic.ban_key({
        'service_token': self.service_with_active_key.token,
        'key': self.key_for_ban.id
    })
    ksc = self.key_for_ban.get_service_config(self.service_with_active_key)
    assert ksc.link.config.banned
    self.wrap_error_on_check_key(
        self.service_with_active_key.token, self.key_for_ban.id, 'Key is not active')
    with pytest.raises(HttpError, match='Key is already banned'):
        self.logic.ban_key({
            'service_token': self.service_with_active_key.token,
            'key': self.key_for_ban.id
        })


def test_logic_update_counters(self):
    self.wrap_update_counter(self.service_with_active_key.token, self.key_regular.id, {'hits': 0})
    counter = self.wrap_get_counter('hits', self.service_with_active_key.id, self.key_regular.id)
    current_value = counter.value
    self.wrap_update_counter(self.service_with_active_key.token, self.key_regular.id, {'hits': 20})
    assert counter.value == current_value + 20
    self.wrap_update_counter(self.service_with_active_key.token, self.key_regular.id, {'hits': 30})
    assert counter.value == current_value + 50


def test_logic_update_several_counters(self):
    counter_hits = self.wrap_get_counter('hits', self.service_with_several_counters.id, self.key_regular.id)
    counter_bytes = self.wrap_get_counter('bytes', self.service_with_several_counters.id, self.key_regular.id)
    current_value_hits = counter_hits.value
    current_value_bytes = counter_bytes.value
    self.wrap_update_counter(self.service_with_several_counters.token, self.key_regular.id, {'hits': 70, 'bytes': 40})
    assert counter_hits.value == current_value_hits + 70
    assert counter_bytes.value == current_value_bytes + 40


def test_logic_update_nonexistent_counters(self):
    self.wrap_error_on_update_counter(self.service_with_active_key.token, self.key_regular.id, {
        self.not_exists_unit_cc: 50,
    }, 'Must provide one of the units')


def test_logic_update_no_counters(self):
    self.wrap_error_on_update_counter(self.service_with_active_key.token, self.key_regular.id, {},
                                      'Must provide one of the units')


def test_logic_update_not_attached_counters_ignore(self):
    attached_counter = self.wrap_get_counter('hits', self.service_with_active_key.id, self.key_regular.id)
    current_value = attached_counter.value
    self.wrap_update_counter(self.service_with_active_key.token, self.key_regular.id, {
        'bytes': 100,
        'hits': 50,
    })
    assert attached_counter.value == current_value + 50


def test_logic_call_without_mandatory_arg(self):
    with pytest.raises(XMLRPCInvalidParam):
        self.logic.create_key({
            'service_token': self.service_with_active_key.token,
        })
    with pytest.raises(XMLRPCInvalidParam):
        self.logic.check_key({
            'service_token': self.service_with_active_key.token,
        })
    with pytest.raises(XMLRPCInvalidParam):
        self.logic.update_counters({
            'service_token': self.service_with_active_key.token,
        })
    with pytest.raises(XMLRPCInvalidParam):
        self.logic.ban_key({
            'service_token': self.service_with_active_key.token,
        })


def test_logic_call_with_wrong_service_token(self):
    with pytest.raises(HttpError, match='Service not found'):
        self.logic.create_key({
            'service_token': self.not_exists_service_token,
            'user_uid': self.external_user.uid,
        })
    with pytest.raises(HttpError, match='Service not found'):
        self.logic.check_key({
            'service_token': self.not_exists_service_token,
            'key': self.key_regular.id,
            'user_ip': '0.0.0.0',
        })
    with pytest.raises(HttpError, match='Service not found'):
        self.logic.update_counters({
            'service_token': self.not_exists_service_token,
            'key': self.key_regular.id,
            'hits': '10',
        })
    with pytest.raises(HttpError, match='Service not found'):
        self.logic.ban_key({
            'service_token': self.not_exists_service_token,
            'key': self.key_regular.id,
        })


def test_limit_checker(self):
    service = self.service_with_limits
    key = self.key_regular
    limit_checker = mapper.LimitChecker.getone(link_id=key.get_service_config(service).link_id)
    self.wrap_update_counter(service.token, key.id, {'hits': 20})
    assert limit_checker.check(True)
    self.wrap_check_key(service.token, key.id)
    self.wrap_update_counter(service.token, key.id, {'hits': 3000})
    assert not limit_checker.check(True)
    self.wrap_error_on_check_key(service.token, key.id, 'Key is not active')


def test_multi_limit_checker(self):
    service = self.service_with_multi_limits
    key = self.key_regular
    limit_checker = mapper.LimitChecker.getone(link_id=key.get_service_config(service).link_id)
    self.wrap_update_counter(service.token, key.id, {'hits': 1499, 'bytes': 2998})
    assert (limit_checker.check(True))
    self.wrap_check_key(service.token, key.id)
    self.wrap_update_counter(service.token, key.id, {'hits': 1499})
    assert (limit_checker.check(True))
    self.wrap_check_key(service.token, key.id)
    self.wrap_update_counter(service.token, key.id, {'bytes': 2})
    assert not (limit_checker.check(True))

    with pytest.raises(HttpError, match='Key is not active'):
        self.wrap_check_key(service.token, key.id)
