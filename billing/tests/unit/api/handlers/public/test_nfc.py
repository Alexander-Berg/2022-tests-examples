from decimal import Decimal

import pytest

from sendr_auth.entities import AuthenticationMethod

from hamcrest import assert_that, equal_to, is_

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.mixins.restrict_auth_method import RestrictAuthMethodMixin
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.public.nfc import GetNFCInstallRewardHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.public_app import YandexPayPlusPublicApplication
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.get_install_reward import GetNFCInstallRewardAmountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.nfc.init_install_reward import InitNFCInstallRewardAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User


@pytest.fixture
def application(db_engine) -> YandexPayPlusPublicApplication:
    # overrides the application fixture from common_conftest.py
    return YandexPayPlusPublicApplication(db_engine=db_engine)


@pytest.fixture
def fake_user(randn):
    return User(randn(), auth_method=AuthenticationMethod.OAUTH)


@pytest.fixture
def mock_authentication(mocker, fake_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=fake_user))


@pytest.fixture
def device_id(rands):
    return rands()


class TestGetNFCInstallReward:
    URL = '/api/public/v1/nfc/rewards/get-install-reward'
    AMOUNT = Decimal('200.0')

    @pytest.fixture(autouse=True)
    def mock_get_reward(self, mock_action):
        return mock_action(GetNFCInstallRewardAmountAction, self.AMOUNT)

    def test_auth_method_restricted_to_token(self):
        assert_that(issubclass(GetNFCInstallRewardHandler, RestrictAuthMethodMixin), is_(True))
        assert_that(GetNFCInstallRewardHandler.allowed_auth_method, equal_to(AuthenticationMethod.OAUTH))

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_authentication')
    async def test_success(self, app, device_id, mock_get_reward, fake_user):
        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': str(self.AMOUNT)}}, 'status': 'success', 'code': 200})
        )
        mock_get_reward.assert_called_once_with(user=fake_user, device_id=device_id)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_authentication')
    async def test_device_id_required(self, app, mock_get_reward, fake_user):
        r = await app.post(self.URL)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'data': {
                        'params': {'device_id': ['Missing data for required field.']},
                        'message': 'BAD_FORMAT',
                    },
                    'status': 'fail',
                    'code': 400,
                }
            )
        )
        mock_get_reward.assert_not_called()


class TestInitNFCInstallReward:
    URL = '/api/public/v1/nfc/rewards/init-install-reward'
    AMOUNT = Decimal('300.0')

    @pytest.fixture(autouse=True)
    def mock_init_reward(self, mock_action):
        return mock_action(InitNFCInstallRewardAction, self.AMOUNT)

    def test_auth_method_restricted_to_token(self):
        assert_that(issubclass(GetNFCInstallRewardHandler, RestrictAuthMethodMixin), is_(True))
        assert_that(GetNFCInstallRewardHandler.allowed_auth_method, equal_to(AuthenticationMethod.OAUTH))

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_authentication')
    async def test_success(self, app, device_id, mock_init_reward, fake_user):
        r = await app.post(self.URL, json={'device_id': device_id}, raise_for_status=True)

        assert_that(
            await r.json(),
            equal_to({'data': {'reward': {'amount': str(self.AMOUNT)}}, 'status': 'success', 'code': 200})
        )
        mock_init_reward.assert_called_once_with(user=fake_user, device_id=device_id)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('mock_authentication')
    async def test_device_id_required(self, app, mock_init_reward, fake_user):
        r = await app.post(self.URL)

        assert_that(r.status, equal_to(400))
        assert_that(
            await r.json(),
            equal_to(
                {
                    'data': {
                        'params': {'device_id': ['Missing data for required field.']},
                        'message': 'BAD_FORMAT',
                    },
                    'status': 'fail',
                    'code': 400,
                }
            )
        )
        mock_init_reward.assert_not_called()
