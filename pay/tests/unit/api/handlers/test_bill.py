from datetime import date, datetime, timezone
from uuid import UUID

import pytest
from freezegun import freeze_time

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.bill.state import BillsState, GetBillsStateAction
from pay.bill_payments.bill_payments.core.actions.user.get import UserState
from pay.bill_payments.bill_payments.interactions.kazna.entities import DepartmentType
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus


@pytest.fixture(autouse=True)
def mock_authentication(mocker, auth_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))


@pytest.fixture
def uri():
    return '/api/v1/bills'


@pytest.fixture
def full_filled_bill():
    return Bill(
        bill_id=UUID('cb3b1b72-0f36-4c70-a573-0d9144dcb461'),
        uid=228,
        supplier_bill_id='supplier_bill_id',
        document_id=UUID('1c2490f9-24a1-4f7e-a2a7-48968c05de08'),
        status=BillStatus.NEW,
        amount=55,
        amount_to_pay=25,
        bill_date=datetime(year=2021, month=10, day=10, tzinfo=timezone.utc),
        purpose='purpose',
        kbk='kbk',
        oktmo='oktmo',
        inn='inn',
        kpp='kpp',
        bik='bik',
        account_number='account_number',
        payee_name='payee_name',
        payer_name='payer_name',
        div_id=33,
        treasure_branch='treasure_branch',
        dep_type=DepartmentType.GIBDD,
        legal_act='legal_act',
        offense_name='offense_name',
        offense_place='offense_place',
        offense_date=datetime(year=2020, month=10, day=10, tzinfo=timezone.utc),
        discount_size='50',
        discount_date=date(year=2021, month=12, day=10),
        paid_amount=30,
        paid_date=datetime(year=2006, month=10, day=10, tzinfo=timezone.utc),
    )


@pytest.fixture
def bill_with_optional_fields():
    return Bill(
        bill_id=UUID('0a0315ae-1cd4-4981-8459-e508c275a470'),
        uid=1,
        supplier_bill_id='number_one',
        document_id=UUID('1c2490f9-24a1-4f7e-a2a7-48968c05de08'),
        status=BillStatus.NEW,
        amount=1,
        amount_to_pay=1,
        bill_date=datetime(year=2001, month=1, day=1, tzinfo=timezone.utc),
    )


@pytest.fixture
def expected_full_filled_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'state': 'completed',
            'bills': [
                {
                    'bill_id': 'cb3b1b72-0f36-4c70-a573-0d9144dcb461',
                    'supplier_bill_id': 'supplier_bill_id',
                    'document_id': '1c2490f9-24a1-4f7e-a2a7-48968c05de08',
                    'status': 'new',
                    'amount': 55,
                    'amount_to_pay': 25,
                    'bill_date': '2021-10-10T00:00:00+00:00',
                    'purpose': 'purpose',
                    'kbk': 'kbk',
                    'oktmo': 'oktmo',
                    'inn': 'inn',
                    'kpp': 'kpp',
                    'bik': 'bik',
                    'account_number': 'account_number',
                    'payee_name': 'payee_name',
                    'payer_name': 'payer_name',
                    'div_id': 33,
                    'treasure_branch': 'treasure_branch',
                    'dep_type': 'GIBDD',
                    'legal_act': 'legal_act',
                    'offense_name': 'offense_name',
                    'offense_place': 'offense_place',
                    'offense_date': '2020-10-10T00:00:00+00:00',
                    'payment_deadline': '2021-12-19T00:00:00+00:00',
                    'discount_size': '50',
                    'discount_date': '2021-12-10',
                    'discounted_amount': 13,
                    'fee_amount': 2000,
                    'paid_amount': 30,
                    'paid_date': '2006-10-10T00:00:00+00:00',
                }
            ],
        },
    }


@pytest.fixture
def expected_with_optional_fields_response_body():
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'state': 'completed',
            'bills': [
                {
                    'bill_id': '0a0315ae-1cd4-4981-8459-e508c275a470',
                    'supplier_bill_id': 'number_one',
                    'document_id': '1c2490f9-24a1-4f7e-a2a7-48968c05de08',
                    'status': 'new',
                    'amount': 1,
                    'amount_to_pay': 1,
                    'discounted_amount': 1,
                    'fee_amount': 2000,
                    'bill_date': '2001-01-01T00:00:00+00:00',
                    'payment_deadline': '2001-03-12T00:00:00+00:00',
                }
            ],
        },
    }


@pytest.mark.asyncio
@freeze_time('2021-12-01')
async def test_should_return_full_filled_bill(
    app, uri, mock_action, full_filled_bill, expected_full_filled_response_body
):
    mock_action(
        GetBillsStateAction,
        BillsState(
            state=UserState.COMPLETED,
            bills=[full_filled_bill],
        ),
    )

    resp = await app.get(uri)
    json_body = await resp.json()

    assert_that(resp.status, equal_to(200))
    assert_that(
        json_body,
        equal_to(expected_full_filled_response_body),
    )


@pytest.mark.asyncio
async def test_should_work_correctly_with_optional_fields(
    app, uri, mock_action, bill_with_optional_fields, expected_with_optional_fields_response_body
):
    mock_action(
        GetBillsStateAction,
        BillsState(
            state=UserState.COMPLETED,
            bills=[bill_with_optional_fields],
        ),
    )

    resp = await app.get(uri)
    json_body = await resp.json()

    assert_that(resp.status, equal_to(200))
    assert_that(
        json_body,
        equal_to(expected_with_optional_fields_response_body),
    )


@pytest.mark.asyncio
async def test_should_call_action_with_expected_args(app, uri, mock_action, auth_user):
    action_mocker = mock_action(
        GetBillsStateAction,
        BillsState(
            state=UserState.COMPLETED,
            bills=[],
        ),
    )

    await app.get(uri)

    action_mocker.assert_called_once_with(auth_user.uid)
