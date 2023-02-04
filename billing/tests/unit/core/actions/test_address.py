from dataclasses import replace

import pytest
from pay.lib.interactions.passport_addresses.entities import Address, Service

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.address import GetUserAddressesAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.passport_addresses import PassportAddressesClient


@pytest.fixture
def user():
    return User(uid=1, tvm_ticket='ticket')


def _make_address(**kwargs):
    return Address(country='C', locality='L', street='S', building='B', address_line='A', **kwargs)


@pytest.mark.asyncio
async def test_remove_duplicates(user, mocker):
    mocker.patch.object(
        PassportAddressesClient,
        'list',
        mocker.AsyncMock(return_value=[
            _make_address(id='1', type='home', owner_service=Service.MAPS),
            _make_address(id='2', owner_service=Service.MARKET),
            _make_address(id='3', room='', owner_service=Service.TAXI),

            _make_address(id='4', room='1', type='work', owner_service=Service.MAPS),
            _make_address(id='5', room='1', type='home', owner_service=Service.TAXI),
            _make_address(id='6', room='1', type='home', owner_service=Service.MAPS),

            _make_address(id='8', room='2'),
            _make_address(id='9', room='2'),
        ]),
    )

    result = await GetUserAddressesAction(user=user, lang='').run()

    assert_that(
        result,
        equal_to([
            _make_address(id='2', owner_service=Service.MARKET),
            _make_address(id='6', room='1', type='home', owner_service=Service.MAPS),
            _make_address(id='9', room='2'),
        ]),
    )


@pytest.mark.asyncio
async def test_filter_out_addresses_with_empty_building(user, mocker):
    mocker.patch.object(
        PassportAddressesClient,
        'list',
        mocker.AsyncMock(return_value=[
            replace(_make_address(id='1'), building='B'),
            replace(_make_address(id='2'), building=''),
            replace(_make_address(id='3'), building=' '),
        ]),
    )

    result = await GetUserAddressesAction(user=user, lang='').run()

    assert_that(
        result,
        equal_to([
            replace(_make_address(id='1'), building='B'),
        ]),
    )
