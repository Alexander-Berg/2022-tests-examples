from unittest.mock import ANY, Mock

import pytest

from maps_adv.warden.client.lib.client import ClientFactory, ClientWithContextManager

pytestmark = [pytest.mark.asyncio]


async def test_expected_arguments_and_parameters_when_create_object_of_client(mocker):
    mock = Mock()
    mocker.patch.object(ClientFactory, "_client_class", mock)

    ClientFactory("http://server.url").client("task_type")

    mock.assert_called_with(
        server_url="http://server.url", executor_id=ANY, task_type="task_type"
    )


async def test_returns_client_with_expect_type_for_context_method(mocker):
    mocker.patch.object(ClientFactory, "_client_class", Mock())

    context = ClientFactory("http://server.url").context("task_type")

    assert isinstance(context, ClientWithContextManager)
