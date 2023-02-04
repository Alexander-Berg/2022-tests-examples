import time
import datetime
import pytest
from unittest import mock

from . import service_builder as sb

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.http_core import HttpError

from .utils import FakeReadPreferenceSettings


@pytest.fixture()
def self(logic_base):
    class TestBoLogic(logic_base):
        logic = None

        @classmethod
        def setUpClass(cls):
            with mock.patch('billing.apikeys.apikeys.mapper.context.ReadPreferenceSettings', new=FakeReadPreferenceSettings):
                from billing.apikeys.apikeys.bologic import Logic
                cls.logic = Logic()

        @classmethod
        def tearDownClass(cls):
            pass
    TestBoLogic.setUpClass()
    yield TestBoLogic()
    TestBoLogic.tearDownClass()


def test_get_services(self):
    response = self.logic.get_services({})
    assert response


def test_list_keys(self):
    response = self.logic.list_keys({
        'oper_uid': self.internal_user.uid,
    })
    assert response


def test_get_link_info(self):
    # Just get key info
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_regular.id,
    })
    assert response['result']['link']['key'] == self.key_regular.id

    assert 'tarifficator_state' in response['result']['link'], 'Ключ tarifficator_state отсутствует в ответе'

    # Send unavailable service_id
    with pytest.raises(HttpError, match='Link not found'):
        self.logic.get_link_info({
            'oper_uid': self.internal_user.uid,
            'key': self.key_regular.id,
            'service_id': sb.ServiceBuilderDirector._service_id + 100,
        })

    # Send unavailable key
    with pytest.raises(HttpError, match='Link not found'):
        self.logic.get_link_info({
            'oper_uid': self.internal_user.uid,
            'key': self.key_regular.id + ' unavailable key',
            'service_id': self.service_regular.id,
        })


def test_audit_trail(self):
    # Create key and link with AuditTrails
    response = self.logic.create_key({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'user_uid': self.external_user.uid,
    })
    key = mapper.Key.getone(id=response['result']['key'])

    self.logic.update_service_link({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'service_id': self.service_regular.id,
    })

    # Ban/Unban link
    self.logic.update_ban({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'service_id': self.service_regular.id,
        'ban': True,
        'reason_id': self.service_regular.lock_reasons[0],
        'reason_memo': 'AUTOTEST ban',
    })
    self.logic.update_ban({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'service_id': self.service_regular.id,
        'ban': False,
        'reason_id': self.service_regular.unlock_reasons[0],
        'reason_memo': 'AUTOTEST unban',
    })

    time.sleep(1)
    # Get audit
    response = self.logic.get_audit_trail({
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'uid': self.internal_user.uid,
    })
    assert response['result']['pager']['total_count'] == 5

    # Get audit for link
    response = self.logic.get_audit_trail({
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'services': str(self.service_regular.id),
    })
    assert response['result']['pager']['total_count'] == 5

    # Get ban history
    response = self.logic.get_audit_trail({
        'oper_uid': self.internal_user.uid,
        'key': key.id,
        'services': str(self.service_regular.id),
        'tags': 'ban,unban',
    })
    assert response['result']['pager']['total_count'] == 2


def test_questionnaire_storage(self):
    ksc = self.key_regular.attach_to_service(self.service_with_questionnaire)
    task_id = 'YANDEX-12345'

    response = self.logic.list_keys({
        'oper_uid': self.internal_user.uid,
        'questionnaire_storage_contains': task_id[2:-2],
    })
    current_count = len(response['result']['keys'])

    self.logic.update_service_link({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_questionnaire.id,
        'questionnaire_storage': task_id,
    })
    ksc.reload()
    assert task_id == ksc.link.questionnaire_storage

    response = self.logic.list_keys({
        'oper_uid': self.internal_user.uid,
        'questionnaire_storage_contains': task_id[2:-2],
    })
    assert len(response['result']['keys']) == current_count + 1


def test_update_limit_inherits(self):
    # Check link without limit inheritance
    self.key_regular.attach_to_service(self.service_with_limits)
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
    })
    assert response['result']['link']['limits']['autotest_autotest_limited_service']['limit'] == 3000

    # Add limit inheritance
    limit_inherits = '{"autotest_%s":{"limit":100,"memo":"AUTOTEST","expire":"%s"}}'
    limit_inherits %= (
        self.service_with_limits.cc,
        (datetime.datetime.utcnow() + datetime.timedelta(hours=1)).strftime('%Y-%m-%dT%H:%M:%S')
    )
    self.logic.update_service_link({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
        'limit_inherits': limit_inherits,
    })
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
    })
    assert response['result']['link']['limits']['autotest_autotest_limited_service']['limit'] == 100

    # Change limit inheritance
    limit_inherits = '{"autotest_%s":{"limit":200}}'
    limit_inherits %= (
        self.service_with_limits.cc,
    )
    self.logic.update_service_link({
        '_http_method': 'PATCH',
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
        'limit_inherits': limit_inherits,
    })
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
    })
    assert response['result']['link']['limits']['autotest_autotest_limited_service']['limit'] == 200

    # Change limit inheritance expire
    limit_inherits = '{"autotest_%s":{"expire":"%s"}}'
    limit_inherits %= (
        self.service_with_limits.cc,
        (datetime.datetime.utcnow() - datetime.timedelta(hours=1)).strftime('%Y-%m-%dT%H:%M:%S'),
    )
    self.logic.update_service_link({
        '_http_method': 'PATCH',
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
        'limit_inherits': limit_inherits,
    })
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
    })
    assert response['result']['link']['limits']['autotest_autotest_limited_service']['limit'] == 3000
    assert response['result']['link']['limit_inherits']['autotest_autotest_limited_service']['limit'] == 200

    # Remove limit inheritance expire
    limit_inherits = '{}'
    self.logic.update_service_link({
        '_http_method': 'POST',
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
        'limit_inherits': limit_inherits,
    })
    response = self.logic.get_link_info({
        'oper_uid': self.internal_user.uid,
        'key': self.key_regular.id,
        'service_id': self.service_with_limits.id,
    })
    assert response['result']['link']['limits']['autotest_autotest_limited_service']['limit'] == 3000
    assert len(response['result']['link']['limit_inherits']) == 0
