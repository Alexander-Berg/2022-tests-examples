import pytest
from unittest import mock


from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.http_core import HttpError
from billing.apikeys.apikeys.mapper import context as ctx

from .utils import FakeReadPreferenceSettings


@pytest.fixture()
def self(logic_base):
    class TestUnblockable(logic_base):
        api_logic = None
        bo_logic = None

        @classmethod
        def setUpClass(cls):
            with ctx.NoCacheSettings():
                with mock.patch('billing.apikeys.apikeys.mapper.context.ReadPreferenceSettings', new=FakeReadPreferenceSettings):
                    from billing.apikeys.apikeys.apilogic import Logic as ApiLogic
                    from billing.apikeys.apikeys.bologic import Logic as BoLogic
                    cls.api_logic = ApiLogic()
                    cls.bo_logic = BoLogic()

        def wrap_check_key(self, token, key):
            response = self.api_logic.check_key({
                'service_token': token,
                'user_ip': '0.0.0.0',
                'key': key
            })
            assert response['result'] == 'OK'
            assert response['key_info']['id'] == key

        def wrap_error_on_check_key(self, token, key, message):
            with pytest.raises(HttpError, match=message):
                self.wrap_check_key(token, key)

        def wrap_check_some_key_and_set_it_unblockable(self, service, key, error_message):
            self.wrap_error_on_check_key(service.token, key.id, error_message)

            self.bo_logic.update_unblockable({
                'oper_uid': self.internal_user.uid,
                'service_id': service.id,
                'key': key.id,
                'unblockable': True,
            })
            self.wrap_check_key(service.token, key.id)

            self.bo_logic.update_unblockable({
                'oper_uid': self.internal_user.uid,
                'service_id': service.id,
                'key': key.id,
                'unblockable': False,
            })
            self.wrap_error_on_check_key(service.token, key.id, error_message)

    TestUnblockable.setUpClass()
    yield TestUnblockable()


def test_check_link_unblockable(self):
    key_unblockable = mapper.Key.create(self.external_user.get_default_project())
    ksc = key_unblockable.attach_to_service(self.service_regular)
    ksc.unblockable = True
    ksc.save()
    self.wrap_check_key(self.service_regular.token, key_unblockable.id)


def test_check_banned_link_and_set_it_unblockable(self):
    key_baned = mapper.Key.create(self.external_user.get_default_project())
    ksc = key_baned.attach_to_service(self.service_regular)
    link = ksc.link
    link.config.banned = True
    link.save()
    self.wrap_check_some_key_and_set_it_unblockable(self.service_regular, key_baned, 'Key is not active')


def test_check_disapproved_link_and_set_it_unblockable(self):
    key_baned = mapper.Key.create(self.external_user.get_default_project())
    ksc = key_baned.attach_to_service(self.service_regular)
    link = ksc.link
    link.config.approved = False
    link.save()
    self.wrap_check_some_key_and_set_it_unblockable(self.service_regular, key_baned, 'Key is not active')


def test_check_inactive_key_and_set_its_link_unblockable(self):
    key_inactive = mapper.Key.create(self.external_user.get_default_project())
    key_inactive.active = False
    key_inactive.save()
    key_inactive.attach_to_service(self.service_regular)
    self.wrap_check_some_key_and_set_it_unblockable(self.service_regular, key_inactive, 'Key is not active')
