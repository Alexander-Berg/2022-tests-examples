import pytest

from sendr_interactions.clients.blackbox import BlackBoxInvalidSessionError, OauthResult

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.agent.authenticate import AuthenticateAgentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import AgentAuthenticationError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import BlackBoxClient


@pytest.mark.asyncio
async def test_success(agent, mocker):
    blackbox = mocker.patch.object(
        BlackBoxClient,
        'get_oauth_token_info',
        mocker.AsyncMock(return_value=OauthResult(agent.oauth_client_id)),
    )

    agent_id = await AuthenticateAgentAction(authorization_header='OAuth token', user_ip='0.0.0.0').run()

    assert_that(agent_id, equal_to(agent.agent_id))
    blackbox.assert_called_once_with('token', user_ip='0.0.0.0')


@pytest.mark.asyncio
async def test_blackbox_exception(mocker):
    mocker.patch.object(
        BlackBoxClient,
        'get_oauth_token_info',
        mocker.AsyncMock(side_effect=BlackBoxInvalidSessionError()),
    )

    with pytest.raises(AgentAuthenticationError) as exc_info:
        await AuthenticateAgentAction(authorization_header='OAuth token', user_ip='0.0.0.0').run()

    assert_that(exc_info.value.message, equal_to('INVALID_OAUTH_TOKEN'))


@pytest.mark.asyncio
async def test_missing_token(agent, mocker):
    with pytest.raises(AgentAuthenticationError) as exc_info:
        await AuthenticateAgentAction(authorization_header=None, user_ip='0.0.0.0').run()

    assert_that(exc_info.value.message, equal_to('MISSING_OAUTH_TOKEN'))


@pytest.mark.asyncio
async def test_agent_not_found(agent, mocker):
    mocker.patch.object(BlackBoxClient, 'get_oauth_token_info', mocker.AsyncMock(return_value=OauthResult('xxx')))

    with pytest.raises(AgentAuthenticationError) as exc_info:
        await AuthenticateAgentAction(authorization_header='OAuth token', user_ip='0.0.0.0').run()

    assert_that(exc_info.value.message, equal_to('UNKNOWN_OAUTH_CLIENT_ID'))
