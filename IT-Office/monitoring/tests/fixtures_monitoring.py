import pytest
from .data_monitoring import data_crm_api
from source.monitoring.monitoring import CrmStat

@pytest.fixture
def fakeCRMStat(monkeypatch):
    def fakeFetch(self, role):
        return data_crm_api[role]
    
    monkeypatch.setattr(CrmStat, '_fetch', fakeFetch)