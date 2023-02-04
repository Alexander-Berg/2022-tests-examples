import pytest
import re
import requests

from source.utils import (OtherUtils,
                   StartrekUtils,
                   get_staff_office_id_by_login
                   )
from source.dismissal_miracle_folder import processing_task_miracle_folder
from startrek_client import Startrek
from source.tests.fixtures import *
from source.tests.testdata import (DISMISSAL_MIRACLE_FOLDER)

@pytest.fixture()
def mock_st(monkeypatch):
    def mockreturn(self, **kwargs):
        self.issues = MockIssues()
        return None

    monkeypatch.setattr(Startrek, '__init__', mockreturn)


class SearchOfficeByLogin():
    @property
    def status_code(self):
        return 200
    
    def json(self):
        return {"location": 
                    {"office": 
                        {"id": 2}
                    }
               }

@pytest.fixture()
def fake_search(monkeypatch):
    def mockreturn(url, headers = "token", verify=False):
        return SearchOfficeByLogin()
    monkeypatch.setattr(requests, 'get', mockreturn)

@pytest.fixture()
def fake_login_is_dismissed(monkeypatch):
    def mockreturn(self, login):
        dict_to_return = {
            "efim": {
                "official": 
                    {"is_dismissed": True}
            },
            "volozh": {
                "official": 
                    {"is_dismissed": False}
            }
        }
        return dict_to_return.get(login)
    
    monkeypatch.setattr(OtherUtils, 'login_is_dismissed', mockreturn)

@pytest.fixture()
def fake_folder_exist(monkeypatch):
    def mockreturn(self, login):
        return False
    
    monkeypatch.setattr(OtherUtils, 'check_miracle_users_folder', mockreturn)

@pytest.fixture()
def fake_search_fixversion(monkeypatch):
    def mockreturn(self, office_id):
        return {'fixVersions': [{'id': '53054'}]}
    
    monkeypatch.setattr(OtherUtils, 'get_fix_version_by_office_id', mockreturn)

@pytest.fixture()
def fake_macros_action(monkeypatch):
    def mockreturn(self, macros_id):
        return {'Error': False,
                'Result': {'components': [{'self': 'https://st-api.yandex-team.ru/v2/components/31135',
                    'id': '31135',
                    'display': 'Dostup_Folders'},
                {'self': 'https://st-api.yandex-team.ru/v2/components/52682',
                    'id': '52682',
                    'display': 'MIR'}],
                'tags': ['Service:Dismissal',
                'Subservice:Dismissal_PersonalFile',
                'MIR_FileDisUser'],
                'abcService': [{'id': 3599}]}}
    
    monkeypatch.setattr(StartrekUtils, 'get_st_macros_actions', mockreturn)

@pytest.mark.parametrize("input_data, ex_result", DISMISSAL_MIRACLE_FOLDER)
def test_processing_task_miracle_folder(fake_search, fake_search_fixversion, fake_macros_action, fake_login_is_dismissed, fake_folder_exist, mock_st, input_data, ex_result):
    issue = FakeFabric(
        key = 'HDRFS-5',
        description = input_data.get('description'),
        comments = input_data['comments'],
        transitions = input_data["transitions"],
        fixVersions = input_data.get("fixVersions"),
    )

    processing_task_miracle_folder(issue)
    assert issue.comments.created_dict['text'] == ex_result.get('comments')