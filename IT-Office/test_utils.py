import pytest
from source.utils import (OtherUtils,
                   StartrekUtils)
import requests


class FakeStaffRespons():
    @property
    def status_code(self):
        return 200
    
    
    def json(self):
        return {
                "official": {
                "is_dismissed": False
                }
        }

class FakeStRespons():
    @property
    def status_code(self):
        return 200
    
    def macros_id(self):
        return 148
    
    def json(self):
        return {"fieldChanges":[{"field":{"self":"https://st-api.yandex-team.ru/v2/fields/abcService","id":"abcService","display":"ABC сервис"},"value":[{"id":3599}]}]}

class FakeMiracleRespons():
    def __init__(self):
        self.result = True
        

@pytest.fixture()
def staff_api_login(monkeypatch):
    def mockreturn(url, headers = "token"):
        return FakeStaffRespons()
    monkeypatch.setattr(requests, 'get', mockreturn)

@pytest.fixture()
def st_api_macros(monkeypatch):
    def mockreturn(url, headers = "token"):
        return FakeStRespons()
    monkeypatch.setattr(requests, 'get', mockreturn)

@pytest.fixture()
def mock_miracle_soap(monkeypatch):
    def mockreturn():
        return FakeMiracleRespons()
    monkeypatch.setattr(OtherUtils, 'check_miracle_users_folder', mockreturn)

def test_login_dismissed(staff_api_login):
    assert OtherUtils().login_is_dismissed("izmal") == False

def test_get_st_macros_actions(st_api_macros):
    macros_actions = StartrekUtils().get_st_macros_actions("149")
    assert macros_actions['Error'] == False

def test_check_miracle_users_folder(mock_miracle_soap):
    test_miracle = FakeMiracleRespons()
    assert test_miracle.result == True