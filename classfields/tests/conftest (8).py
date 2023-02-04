from unittest.mock import AsyncMock, Mock

import pytest
from pytest_mock import MockerFixture


@pytest.fixture(autouse=True)
def boto_client_mock(mocker: MockerFixture):
    m = AsyncMock()
    client_mock = AsyncMock()
    client_mock.__aenter__ = AsyncMock(return_value=m)
    mocker.patch("smartagent_realty_converter.s3.boto_client", Mock(return_value=client_mock))
    mocker.patch("smartagent_realty_converter.main.boto_client", Mock(return_value=client_mock))
    return m
