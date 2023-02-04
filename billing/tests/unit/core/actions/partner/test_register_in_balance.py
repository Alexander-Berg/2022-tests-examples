import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.balance.init_client import InitClientAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.register_in_balance import (
    RegisterPartnerInBalanceAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.interactions import BalanceClient
from billing.yandex_pay_admin.yandex_pay_admin.interactions.balance import PaymentType, Person


@pytest.mark.asyncio
async def test_register(mocker, partner, user, storage, mock_init_client):
    mocker.patch.object(BalanceClient, 'create_person', mocker.AsyncMock(return_value='person-id'))
    mocker.patch.object(BalanceClient, 'create_offer', mocker.AsyncMock(return_value=('c-id', 'ext-id')))

    await RegisterPartnerInBalanceAction(uid=user.uid, partner_id=partner.partner_id).run()

    mock_init_client.assert_run_once_with(uid=user.uid, contact=partner.registration_data.contact)
    assert_that(
        await storage.partner.get(partner.partner_id),
        has_properties(
            balance_client_id='balance-client-id',
            balance_person_id='person-id',
            balance_contract_id='c-id',
            balance_ext_contract_id='ext-id',
        ),
    )


@pytest.mark.asyncio
async def test_calls_client(mocker, partner, user, yandex_pay_admin_settings):
    create_person = mocker.patch.object(BalanceClient, 'create_person', mocker.AsyncMock(return_value='person-id'))
    create_offer = mocker.patch.object(BalanceClient, 'create_offer', mocker.AsyncMock(return_value=('c-id', 'ext-id')))

    await RegisterPartnerInBalanceAction(uid=user.uid, partner_id=partner.partner_id).run()

    create_person.assert_called_once_with(
        user.uid,
        Person(
            client_id='balance-client-id',
            person_id=None,
            fname='John',
            lname='Doe',
            mname='Татьянович',
            email='email@test',
            phone='+1(000)555-0100',
            name='some partner name',
            longname='Yandex LLC',
            inn='0123 АБ',
            ogrn='ogrn',
            kpp='kpp',
            post_address='Beverly Hills, 90210',
            postcode='90210',
            legal_address='Moscow',
        ),
    )
    create_offer.assert_called_once_with(
        uid=user.uid,
        client_id='balance-client-id',
        person_id='person-id',
        currency='RUB',
        services=[yandex_pay_admin_settings.BALANCE_SERVICE],
        payment_type=PaymentType.POSTPAYMENT,
        firm_id=yandex_pay_admin_settings.BALANCE_FIRM_ID,
        payment_term_days=yandex_pay_admin_settings.BALANCE_PAYMENT_TERM_DAYS,
        integration_cc=yandex_pay_admin_settings.BALANCE_INTEGRATION,
        manager_code=yandex_pay_admin_settings.BALANCE_MANAGER_CODE,
    )


@pytest.fixture(autouse=True)
def mock_init_client(mock_action):
    return mock_action(InitClientAction, 'balance-client-id')
