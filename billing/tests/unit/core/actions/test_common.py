from typing import Collection, Type

import pytest

from billing.yandex_pay.yandex_pay.core.actions.base import BaseAsyncDBAction, BaseDBAction
from billing.yandex_pay.yandex_pay.core.actions.card.get import GetUserCardByPayCardIdAction
from billing.yandex_pay.yandex_pay.core.actions.card.list import GetUserCardsAction
from billing.yandex_pay.yandex_pay.core.actions.merchant.update_order import UpdateMerchantOrderAction
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.validate_sheet import ValidatePaymentSheetAction
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.checkout import (
    CreateInternalCheckoutPaymentTokenAction
)
from billing.yandex_pay.yandex_pay.core.actions.payment_token.internal.trust import (
    CreateInternalTrustPaymentTokenAction
)
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.update_order_status import (
    YandexPayPlusUpdateOrderStatusAction
)
from billing.yandex_pay.yandex_pay.core.actions.psp.auth import AuthPSPRequestAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization_acceptance.get import GetTokenizationAcceptanceAction
from billing.yandex_pay.yandex_pay.core.actions.validate_merchant import ValidateMerchantAction


@pytest.fixture
def actions_with_replica_read() -> Collection[Type[BaseDBAction]]:
    return [
        AuthPSPRequestAction,
        CreateInternalCheckoutPaymentTokenAction,
        CreateInternalTrustPaymentTokenAction,
        GetTokenizationAcceptanceAction,
        GetUserCardByPayCardIdAction,
        GetUserCardsAction,
        UpdateMerchantOrderAction,
        ValidateMerchantAction,
        ValidatePaymentSheetAction,
        YandexPayPlusCreateOrderAction,
        YandexPayPlusUpdateOrderStatusAction,
    ]


@pytest.fixture
def not_allowed_replica_read_actions(actions_with_replica_read, get_subclasses) -> Collection[Type[BaseDBAction]]:
    db_actions = set(get_subclasses(BaseDBAction)) | set(get_subclasses(BaseAsyncDBAction))
    not_allowed_db_actions = db_actions - set(actions_with_replica_read)
    return not_allowed_db_actions


def test_replica_read_field(actions_with_replica_read):
    for action in actions_with_replica_read:
        assert action.allow_replica_read


def test_not_allowed_replica_read_field(not_allowed_replica_read_actions):
    for action in not_allowed_replica_read_actions:
        assert not action.allow_replica_read
