"""Tests for common functions used in host API methods."""

import pytest

from infra.walle.server.tests.lib.util import mock_startrek_client, Mock
from sepelib.yandex.startrek import StartrekConnectionError, StartrekRequestError
from walle.errors import RequestValidationError, ResourceConflictError
from walle.views.helpers.validators import validate_startrek_ticket_key


class TestTicketValidation:
    @pytest.mark.parametrize(
        "input_ticket_key,expected_ticket_key",
        (
            ("http://st.yandex-team.ru/MOCK-1234", "MOCK-1234"),
            ("https://st.yandex-team.ru/MOCK-1234", "MOCK-1234"),
            ("https://st.yandex-team.ru/MOCK-1234/", "MOCK-1234"),
            ("st.yandex-team.ru/MOCK-1234", "MOCK-1234"),
            ("st/MOCK-1234", "MOCK-1234"),
            ("st//MOCK-1234", "MOCK-1234"),
            ("st/MOCK-1234", "MOCK-1234"),
        ),
    )
    def test_valid_ticket_formats(self, input_ticket_key, expected_ticket_key):
        assert validate_startrek_ticket_key(input_ticket_key) == expected_ticket_key

    @pytest.mark.parametrize(
        "input_ticket_key",
        (
            "http://st.yandex-team.ru/MOCK",
            "https://st.yandex-team.ru/1234/",
            "https://st.yandex-team.ru/1234",
            "st/1234",
        ),
    )
    def test_invalid_ticket_formats(self, input_ticket_key):
        with pytest.raises(RequestValidationError):
            validate_startrek_ticket_key(input_ticket_key)

    def test_ticket_is_none(self):
        assert validate_startrek_ticket_key(None) is None

    def test_ticket_is_closed(self, mp):
        valid_ticket_key = "MOCK-1234"
        mock_startrek_client(mp, "closed")
        with pytest.raises(ResourceConflictError):
            validate_startrek_ticket_key(valid_ticket_key, True)

    @pytest.mark.slow
    def test_st_not_responding(self, mp):
        valid_ticket_key = "MOCK-1234"
        client = mock_startrek_client(mp)
        client.get_issue.side_effect = StartrekConnectionError("Some error.")
        with pytest.raises(RequestValidationError):
            validate_startrek_ticket_key(valid_ticket_key, True)

    def test_ticket_does_not_exist(self, mp):
        valid_ticket_key = "MOCK-1234"
        client = mock_startrek_client(mp)
        client.get_issue.side_effect = StartrekRequestError(Mock(), "Some error.")
        with pytest.raises(RequestValidationError):
            validate_startrek_ticket_key(valid_ticket_key, True)
