import pytest
import re
import requests

from source.utils import (OtherUtils,
                   StartrekUtils,
                   get_staff_office_id_by_login
                   )

from startrek_client import Startrek
from source.tests.fixtures import *
from source.tests.testdata import (PRINT_MONITORING)
from source.prn_task_marking import set_attributes_for_printer_mon

class FakeIssue():
    def __init__(self, **kwargs):
        self.kwargs=kwargs
        self.fake_description = kwargs.get('description', [])

    @property
    def description(self):
        return self.fake_description
    
    def update(self, **kwargs):
        return True

class SearchLocation():
    def __init__(self, **kwargs):
        self.kwargs = kwargs

    @property
    def fields(self):
        return self.kwargs.get('fields')

    def group(self):
        return 'prn-123.yandex-team.ru'
    
    @property
    def status_code(self):
        return 200
    
    def json(self):
        return {"res":1,"os":[{"loc_segment2":"MOW","loc_segment3":"MOROZOV"}]}

    @property
    def office_id(self):
        return 1

    def fix_version_settings_list(self):
        return [{"name": "MSK Morozov", "fixver_staff_id": 1, "fixver_st_id": 53060}]

@pytest.fixture()
def fake_search_location_oebs(monkeypatch):
    def mockreturn(url, headers = "token", verify=False):
        return SearchLocation()
    monkeypatch.setattr(requests, 'get', mockreturn)

@pytest.fixture()
def mock_st(monkeypatch):
    def mockreturn(self, **kwargs):
        self.issues = MockIssues()
        return None

    monkeypatch.setattr(Startrek, '__init__', mockreturn)

@pytest.fixture()
def fake_macros_action(monkeypatch):
    def mockreturn(self, macros_id):
        return {'Error': False, 'Result': 
        {'abcService': [{'id': 3480}], 'components': 
        [{'self': 'https://st-api.yandex-team.ru/v2/components/50062', 'id': '50062', 'display': 'PRN'}],
        'tags': ['PRN_UsePrinter_MonDown'], 'type': 
        {'self': 'https://st-api.yandex-team.ru/v2/issuetypes/53', 'id': '53', 'key': 'incident', 'display': 'Инцидент'}}}
    
    monkeypatch.setattr(StartrekUtils, 'get_st_macros_actions', mockreturn)

@pytest.fixture()
def fake_get_office_for_location(monkeypatch):
    def mockreturn(self, loc):
        return {'fixVersions': [{'id': 53060}], 'currentOffice': 1}

    monkeypatch.setattr(OtherUtils, 'get_office_for_location', mockreturn)

@pytest.mark.parametrize("input_data, ex_result", PRINT_MONITORING)
def test_set_attributes_for_printer_mon(fake_search_location_oebs, fake_macros_action, mock_st, fake_get_office_for_location, input_data, ex_result):
    issue = FakeFabric(
        key = 'HDRFS-5',
        update = input_data.get('update'),
        description = input_data.get('description'),
        fixVersions = input_data.get("fixVersions"),
    )

    set_attributes_for_printer_mon(issue)
    print(issue)
    assert issue.fixVersions == ex_result.get('fixversions')