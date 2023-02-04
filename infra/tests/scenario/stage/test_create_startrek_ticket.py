from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_function
from walle.clients import startrek
from walle.scenario.constants import TicketStatus
from walle.scenario.marker import MarkerStatus
from walle.scenario.scenario import Scenario
from walle.scenario.stage.create_startrek_ticket import CreateStartrekTicketStage, StartrekTicketParamsName
from walle.scenario.stage_info import StageInfo

MOCK_TICKET_KEY = "TEST-1"


class TestCreateStartrekTicket:
    @staticmethod
    def _get_mock_startrek_client(
        side_effect_for_create_issue=None, side_effect_for_get_issue=None, ticket_status=TicketStatus.CLOSED
    ):
        mock_startrek_client = Mock()

        if side_effect_for_create_issue:
            mock_startrek_client.attach_mock(Mock(side_effect=side_effect_for_create_issue), "create_issue")
        else:
            mock_startrek_client.attach_mock(
                Mock(return_value={"status": {"key": TicketStatus.OPEN}, "key": MOCK_TICKET_KEY}), "create_issue"
            )

        if side_effect_for_get_issue:
            mock_startrek_client.attach_mock(Mock(side_effect=side_effect_for_get_issue), "get_issue")
        else:
            mock_startrek_client.attach_mock(
                Mock(return_value={"status": {"key": ticket_status}, "key": MOCK_TICKET_KEY}), "get_issue"
            )

        return mock_startrek_client

    def test_run_with_creating_ticket_successfully(self, walle_test, mp):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = CreateStartrekTicketStage(ticket_params_func_name=StartrekTicketParamsName.DUMMY)
        stage_info = StageInfo()
        result = stage.run(stage_info, Scenario())

        assert result.status == MarkerStatus.IN_PROGRESS
        assert stage_info.get_data(CreateStartrekTicketStage.IS_TICKET_CREATED) is True
        assert stage_info.get_data(CreateStartrekTicketStage.TICKET_ID) == MOCK_TICKET_KEY

        mock_startrek_client.create_issue.assert_called()
        mock_startrek_client.get_issue.assert_not_called()

    def test_run_without_creating_ticket_successfully(self, walle_test, mp):
        mock_startrek_client = self._get_mock_startrek_client()
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = CreateStartrekTicketStage(ticket_params_func_name=StartrekTicketParamsName.DUMMY)
        stage_info = StageInfo()
        stage_info.set_data(CreateStartrekTicketStage.IS_TICKET_CREATED, True)
        stage_info.set_data(CreateStartrekTicketStage.TICKET_ID, MOCK_TICKET_KEY)
        result = stage.run(stage_info, Scenario())

        assert result.status == MarkerStatus.SUCCESS

        mock_startrek_client.create_issue.assert_not_called()
        mock_startrek_client.get_issue.assert_not_called()

    def test_startrek_error_during_creating_ticket(self, walle_test, mp):
        mock_startrek_client = self._get_mock_startrek_client(side_effect_for_create_issue=startrek.StartrekClientError)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = CreateStartrekTicketStage(ticket_params_func_name=StartrekTicketParamsName.DUMMY)
        stage_info = StageInfo()

        with pytest.raises(startrek.StartrekClientError):
            stage.run(stage_info, Scenario())

        assert stage_info.get_data(CreateStartrekTicketStage.IS_TICKET_CREATED, False) is False
        assert stage_info.get_data(CreateStartrekTicketStage.TICKET_ID, None) is None

        mock_startrek_client.create_issue.assert_called()
        mock_startrek_client.get_issue.assert_not_called()

    @pytest.mark.parametrize("ticket_status", [TicketStatus.OPEN, TicketStatus.CLOSED])
    def test_run_with_checking_ticket_status(self, walle_test, mp, ticket_status):
        mock_startrek_client = self._get_mock_startrek_client(ticket_status=ticket_status)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = CreateStartrekTicketStage(
            ticket_params_func_name=StartrekTicketParamsName.DUMMY, wait_for_ticket_to_close=True
        )
        stage_info = StageInfo()
        stage_info.set_data(CreateStartrekTicketStage.IS_TICKET_CREATED, True)
        stage_info.set_data(CreateStartrekTicketStage.TICKET_ID, MOCK_TICKET_KEY)
        result = stage.run(stage_info, Scenario())

        if ticket_status == TicketStatus.CLOSED:
            assert result.status == MarkerStatus.SUCCESS
        else:
            assert result.status == MarkerStatus.IN_PROGRESS

        mock_startrek_client.create_issue.assert_not_called()
        mock_startrek_client.get_issue.assert_called()

    def test_startrek_error_during_getting_ticket(self, walle_test, mp):
        mock_startrek_client = self._get_mock_startrek_client(side_effect_for_get_issue=startrek.StartrekClientError)
        monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

        stage = CreateStartrekTicketStage(
            ticket_params_func_name=StartrekTicketParamsName.DUMMY, wait_for_ticket_to_close=True
        )
        stage_info = StageInfo()
        stage_info.set_data(CreateStartrekTicketStage.IS_TICKET_CREATED, True)
        stage_info.set_data(CreateStartrekTicketStage.TICKET_ID, MOCK_TICKET_KEY)

        with pytest.raises(startrek.StartrekClientError):
            stage.run(stage_info, Scenario())

        assert stage_info.get_data(CreateStartrekTicketStage.IS_TICKET_CREATED) is True
        assert stage_info.get_data(CreateStartrekTicketStage.TICKET_ID) is MOCK_TICKET_KEY

        mock_startrek_client.create_issue.assert_not_called()
        mock_startrek_client.get_issue.assert_called()
