import logging
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from uuid import UUID, uuid4

import pytest
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal
from pay.lib.interactions.split.entities import (
    YandexSplitPayment, YandexSplitPaymentPlan, YandexSplitPaymentPlanDetails, YandexSplitPaymentPlanStatus,
    YandexSplitPaymentStatus
)

from hamcrest import assert_that, contains, equal_to, has_entries, has_item, has_properties

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.validate_sheet import ValidatePaymentSheetAction
from billing.yandex_pay.yandex_pay.core.actions.split.get_payment_plans import GetPaymentPlansAction
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMerchant, PaymentMethod, PaymentSheet
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions.split import YandexSplitClient


@pytest.fixture
def split_response():
    user_id = 'dd9b8d73-f2db-593d-5722-2d6edbdb42ba'
    class_name = 'regular_instalment_plan'
    dt = datetime(2021, 12, 9, 9, 27, 45, tzinfo=timezone.utc)
    return [
        YandexSplitPaymentPlan(
            id='c3b60686-b791-65d9-c069-817c20bcde9d',
            user_id=user_id,
            class_name=class_name,
            constructor='test',
            status=YandexSplitPaymentPlanStatus.DRAFT,
            sum=Decimal('2052.00'),
            details=YandexSplitPaymentPlanDetails(
                deposit=Decimal('0'),
                payments=[
                    YandexSplitPayment(
                        amount=Decimal('1026.00'),
                        status=YandexSplitPaymentStatus.COMING,
                        datetime=dt + timedelta(minutes=delta),
                    )
                    for delta in (0, 10)
                ]
            )
        ),
        YandexSplitPaymentPlan(
            id='898330cd-d091-4387-e035-65bb43db4762',
            user_id=user_id,
            class_name=class_name,
            constructor='fast_loan_transfer_sums',
            status=YandexSplitPaymentPlanStatus.DRAFT,
            sum=Decimal('2052.00'),
            details=YandexSplitPaymentPlanDetails(
                deposit=Decimal('1000'),
                payments=[
                    YandexSplitPayment(
                        amount=Decimal('526.00'),
                        status=YandexSplitPaymentStatus.COMING,
                        datetime=dt + timedelta(minutes=delta),
                    )
                    for delta in (0, 1)
                ]
            )
        )
    ]


@pytest.fixture(autouse=True)
def mock_split(mocker, split_response):
    response = mocker.AsyncMock(return_value=split_response)
    return mocker.patch.object(YandexSplitClient, 'get_payment_plans', response)


@pytest.fixture(autouse=True)
def mock_validate(mock_action):
    return mock_action(ValidatePaymentSheetAction)


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def login_id(rands):
    return rands()


@pytest.fixture
def user(uid, login_id):
    return User(uid, None, login_id)


@pytest.fixture
def amount():
    return Decimal('2052.00')


@pytest.fixture
def sheet(amount):
    return PaymentSheet(
        merchant=PaymentMerchant(
            id=UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3'),
            name='merchant-name',
            url='http://site.test',
        ),
        version=2,
        currency_code='xts',
        country_code='ru',
        payment_methods=[
            PaymentMethod(method_type=PaymentMethodType.SPLIT)
        ],
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=amount,
            ),
        ),
    )


@pytest.mark.asyncio
async def test_returned(user, sheet, split_response):
    result = await GetPaymentPlansAction(user=user, sheet=sheet).run()

    assert_that(result, equal_to([split_response[0]]))


@pytest.mark.asyncio
async def test_split_client_called(user, sheet, uid, login_id, amount, mock_split, yandex_pay_settings):
    await GetPaymentPlansAction(user=user, sheet=sheet).run()

    mock_split.assert_called_once_with(
        uid=uid,
        currency='xts',
        amount=amount,
        login_id=login_id,
        merchant_id=yandex_pay_settings.SPLIT_INTERNAL_MERCHANT_ID,
    )


@pytest.mark.asyncio
async def test_validate_sheet_called(user, sheet, mock_validate):
    await GetPaymentPlansAction(user=user, sheet=sheet).run()

    mock_validate.assert_called_once_with(
        sheet=sheet,
        validate_origin=False,
    )


@pytest.mark.asyncio
async def test_call_logged(user, sheet, dummy_logs, uid, split_response):
    await GetPaymentPlansAction(user=user, sheet=sheet).run()

    logs = dummy_logs()
    assert_that(
        logs,
        contains(
            has_properties(
                message='SPLIT_PLANS_REQUESTED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    sheet=sheet,
                )
            ),
            has_properties(
                message='SPLIT_PLANS_RECEIVED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    sheet=sheet,
                    plans=split_response,
                )
            ),
        ),
    )


@pytest.mark.asyncio
async def test_split_disabled(user, sheet, dummy_logs, uid, yandex_pay_settings):
    yandex_pay_settings.SPLIT_PAYMENTS_ENABLED = False

    result = await GetPaymentPlansAction(user=user, sheet=sheet).run()
    assert_that(result, equal_to([]))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='SPLIT_GLOBALLY_DISABLED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    sheet=sheet,
                )
            )
        ),
    )


@pytest.mark.asyncio
async def test_merchant_not_in_whitelist(user, sheet, dummy_logs, uid, mocker):
    mock_whitelist = {uuid4()}
    mocker.patch.object(GetPaymentPlansAction, 'merchant_id_whitelist', mock_whitelist)

    result = await GetPaymentPlansAction(user=user, sheet=sheet).run()
    assert_that(result, equal_to([]))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_NOT_IN_SPLIT_WHITELIST',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=uid,
                    sheet=sheet,
                    merchant_id=sheet.merchant.id,
                    merchant_id_whitelist=mock_whitelist,
                )
            )
        ),
    )
