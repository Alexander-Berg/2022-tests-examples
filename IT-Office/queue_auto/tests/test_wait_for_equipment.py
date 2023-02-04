from source.tests.fixtures import (FakeFabric,
                                   FakeFabricIterator)

from source.queue_auto.tests.fixtures import *
from source.queue_auto.wait_for_equipment import PreparedForIssueRobot
from source.queue_auto.tests.testdata import *
import pytest

@pytest.mark.parametrize("issue, expected_hop", PREPARED_FOR_ISSUES_TESTDATA)
def test_PreparedForIssueRobot(mock_first_hop, mock_second_hop, mock_third_hop, issue, expected_hop):
    issue = issue
    issue.result = None
    robot = PreparedForIssueRobot()
    robot._process_issue(issue)
    assert  issue.result == expected_hop

@pytest.mark.parametrize("issue, result", WAITING_FOR_PREPARED_TESTDATA)
def test_WaitingForEquipmentRobot(mock_first_hop_waiting_for,
                                  mock_second_hop_waiting_for,
                                  mock_third_hop_waiting_for, issue, result):

    expected_hop = result.get("expected_hop")
    expected_summonees = result.get("summonees")

    issue = issue
    issue.result = None

    robot = WaitingForEquipmentRobot()
    robot._process_issue(issue)
    summonees = robot._generate_summon_list(issue)

    assert issue.result == expected_hop
    assert summonees == expected_summonees
