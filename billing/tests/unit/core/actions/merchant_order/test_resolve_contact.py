import pytest
from pay.lib.entities.order import Contact
from pay.lib.interactions.passport_addresses.entities import Contact as PassportContact

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.contact import GetContactAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.resolve_contact import ResolveContactAction


@pytest.fixture(autouse=True)
def mock_contact(mock_action):  # noqa
    return mock_action(
        GetContactAction,
        return_value=PassportContact(
            id='c-id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone_number='phone_number',
        ),
    )


@pytest.mark.asyncio
async def test_calls_get_contact(mock_contact, entity_auth_user):
    address = await ResolveContactAction(user=entity_auth_user, contact_id='c-id').run()

    mock_contact.assert_run_once_with(user=entity_auth_user, contact_id='c-id')
    assert_that(
        address,
        equal_to(
            ensure_all_fields(
                Contact,
                id='c-id',
                first_name='first_name',
                second_name='second_name',
                last_name='last_name',
                email='email',
                phone='phone_number',
            )
        ),
    )
