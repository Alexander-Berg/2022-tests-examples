import pytest
from billing.apikeys.apikeys.controllers._validators import get_validator


class TestApplicationIdValidator:
    @pytest.fixture()
    def valid(self):
        AppId = get_validator('app_id')
        return AppId()

    def test_how_correct_app_id_should_look_like(self, valid):
        assert valid('com.domainname.applicationname')
        assert valid('com.domainname.application_name')
        assert valid('A1B2C3D4E5.com.domainname.applicationname')

    def test_dots_in_app_id_cannot_be_neither_beginning_nor_ending(self, valid):
        assert not valid('com.domainname.')
        assert not valid('.com.domainname.applicationname')
        assert not valid('.com.domainname.')

    def test_spaces_and_special_characters_in_app_id_not_allowed(self, valid):
        assert not valid('com.domainname.application name')
        assert not valid('com.domainname.applicationname!')

    def test_app_id_should_contain_dot(self, valid):
        assert not valid('applicationname')

    def test_app_id_should_start_and_finish_with_alphanumeric(self, valid):
        assert not valid('com._.application_name')
        assert not valid('com.domain_name._application_name')
