import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.wallet.beta import CheckBetaIsAllowedAction
from billing.yandex_pay.yandex_pay.core.entities.user import User


@pytest.fixture
def user():
    return User(123)


@pytest.fixture(autouse=True)
def mock_authentication(user, mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))


@pytest.mark.asyncio
async def test_should_return_action_result_and_call_with_expected_args(app, mock_action, user):
    action_mocker = mock_action(CheckBetaIsAllowedAction, True)

    r = await app.get(
        '/api/mobile/v1/wallet/app/beta/allowed'
    )

    assert_that(r.status, equal_to(200))
    json_body = await r.json()
    assert_that(
        json_body,
        equal_to(
            {
                'data': {'allowed': True},
                'status': 'success',
                'code': 200,
            }
        )
    )
    action_mocker.assert_called_once_with(
        user=User(uid=123, tvm_ticket=None, login_id=None, auth_method=None, is_yandexoid=False))
