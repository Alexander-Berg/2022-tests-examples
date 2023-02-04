from unittest.mock import AsyncMock, Mock

import pytest
from pytest_mock import MockerFixture


@pytest.fixture(autouse=True)
def boto_client_mock(mocker: MockerFixture):
    m = AsyncMock()
    mocker.patch("cm_avito_converter.s3.boto_client", Mock(return_value=m))
    return m


@pytest.fixture(autouse=True)
def producer_mock(mocker: MockerFixture):
    m = AsyncMock()
    mocker.patch("cm_avito_converter.globals.get_kafka_producer", AsyncMock(return_value=m))
    mocker.patch("cm_avito_converter.main.get_kafka_producer", AsyncMock(return_value=m))
    return m
