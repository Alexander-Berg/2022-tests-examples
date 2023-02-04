import pytest
from pay.lib.entities.shipping import Address, Location
from pay.lib.interactions.passport_addresses.entities import Address as PassportAddress
from pay.lib.interactions.passport_addresses.entities import Location as PassportLocation

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.address import GetAddressAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.resolve_address import ResolveAddressAction


@pytest.fixture(autouse=True)
def mock_address(mock_action):  # noqa
    return mock_action(
        GetAddressAction,
        return_value=PassportAddress(
            country='country',
            locality='locality',
            street='street',
            building='building',
            id='a-id',
            region='region',
            room='room',
            entrance='entrance',
            floor='floor',
            intercom='intercom',
            comment='comment',
            zip='zip',
            location=PassportLocation(latitude=30.15, longitude=15.30),
            type='type',
            locale='locale',
            address_line='address_line',
            district='district',
        ),
    )


@pytest.mark.asyncio
async def test_calls_get_address(mock_address, entity_auth_user):
    address = await ResolveAddressAction(user=entity_auth_user, address_id='a-id').run()

    mock_address.assert_run_once_with(
        user=entity_auth_user,
        address_id='a-id',
        lang='ru',
    )
    assert_that(
        address,
        equal_to(
            ensure_all_fields(
                Address,
                id='a-id',
                country='country',
                locality='locality',
                street='street',
                building='building',
                region='region',
                room='room',
                entrance='entrance',
                floor='floor',
                intercom='intercom',
                comment='comment',
                zip='zip',
                location=ensure_all_fields(
                    Location,
                    latitude=30.15,
                    longitude=15.30,
                ),
                locale='locale',
                address_line='address_line',
                district='district',
            )
        ),
    )
