from copy import copy
from datetime import date

import pytest
from freezegun import freeze_time

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.balance.init_client import InitClientAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions import BalanceClient
from billing.yandex_pay_admin.yandex_pay_admin.interactions.balance import Client


@pytest.fixture
def client():
    return Client(name='Doe John', email='email@test', phone='+1(000)555-0100', client_id='c-id')


class BalanceClientMock:
    def __init__(self, client, mocker):
        self.find = mocker.patch.object(BalanceClient, 'find_client', mocker.AsyncMock(return_value=copy(client)))
        self.create = mocker.patch.object(BalanceClient, 'create_client', mocker.AsyncMock(return_value='c-id'))
        self.create_association = mocker.patch.object(
            BalanceClient, 'create_user_client_association', mocker.AsyncMock()
        )
        self.link = mocker.patch.object(BalanceClient, 'link_integration_to_client', mocker.AsyncMock())


@pytest.mark.asyncio
@freeze_time('2022-05-15')
async def test_register_new_client(mocker, partner, user, client, yandex_pay_admin_settings):
    mock = BalanceClientMock(None, mocker)

    client_id = await InitClientAction(uid=user.uid, contact=partner.registration_data.contact).run()

    assert_that(client_id, equal_to(client.client_id))
    mock.find.assert_called_once_with(user.uid)
    mock.create.assert_called_once_with(uid=user.uid, client=client)
    mock.create_association.assert_called_once_with(user.uid, client_id)
    mock.link.assert_called_once_with(
        user.uid,
        client_id,
        yandex_pay_admin_settings.BALANCE_INTEGRATION,
        yandex_pay_admin_settings.BALANCE_CONFIGURATION,
        date.today(),
    )


@pytest.mark.asyncio
async def test_existing_client(mocker, partner, user, client):
    mock = BalanceClientMock(client, mocker)

    client_id = await InitClientAction(uid=user.uid, contact=partner.registration_data.contact).run()

    assert_that(client_id, equal_to(client.client_id))
    mock.find.assert_called_once_with(user.uid)
    mock.create.assert_not_called()
    mock.create_association.assert_not_called()
