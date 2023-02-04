import pytest
from source.queue_auto.wait_for_equipment import (PreparedForIssueRobot,
                                                  WaitingForEquipmentRobot)

@pytest.fixture()
def mock_first_hop(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 1

    monkeypatch.setattr(PreparedForIssueRobot, '_first_hop_function', mockreturn)

@pytest.fixture()
def mock_second_hop(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 2

    monkeypatch.setattr(PreparedForIssueRobot, '_second_hop_function', mockreturn)

@pytest.fixture()
def mock_third_hop(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 3

    monkeypatch.setattr(PreparedForIssueRobot, '_third_hop_function', mockreturn)

@pytest.fixture()
def mock_first_hop_waiting_for(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 1

    monkeypatch.setattr(WaitingForEquipmentRobot, '_first_hop_function', mockreturn)

@pytest.fixture()
def mock_second_hop_waiting_for(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 2

    monkeypatch.setattr(WaitingForEquipmentRobot, '_second_hop_function', mockreturn)

@pytest.fixture()
def mock_third_hop_waiting_for(monkeypatch):

    def mockreturn(self, **kwargs):
        issue = kwargs.get('issue')
        issue.result = 3

    monkeypatch.setattr(WaitingForEquipmentRobot, '_third_hop_function', mockreturn)
