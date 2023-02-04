from typing import Callable, Dict, Any
from unittest.mock import MagicMock

import pytest
from starlette.datastructures import QueryParams
from starlette.requests import Request

from src.common.authentication_interface import AuthenticationInterface
from src.config import settings


class MockedAuthentication(AuthenticationInterface):
    def __init__(self, login: str) -> None:
        self.login = login

    async def user_info_by_ticket(self, ticket: str) -> Dict[str, Any]:
        return {'users': [{'login': self.login}]}

    async def check_user_ticket(self, ticket: str) -> bool:
        return True


def create_request(login: str, query_params: QueryParams) -> Request:
    tvm_header = (
        settings.TVM_USER_TICKET_HEADER.lower().encode('latin-1'),
        'test_tvm_ticket'.encode('latin-1'),
    )
    result = Request(scope={
        'type': 'http',
        'app': MagicMock(),
        'query_string': str(query_params),
        'headers': [tvm_header],
    })
    result.app.state.authentication = MockedAuthentication(login)
    return result
