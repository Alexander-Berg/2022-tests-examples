import pytest

from sendr_auth.entities import AuthenticationMethod

from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.form.button_cashback import ButtonCashbackHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.merchant.base import BaseMerchantHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.mixins.restrict_auth_method import RestrictAuthMethodMixin
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.public.maillist import MaillistSubscriptionHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.threeds.challenge_result import ThreeDSChallengeResultHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.threeds.method_result import ThreeDSMethodResultHandler
from billing.yandex_pay_plus.yandex_pay_plus.api.handlers.utility import PingDBHandler, PingHandler, UnistatHandler


@pytest.fixture
def internal_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, BaseInternalHandler)}


@pytest.fixture
def merchant_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, BaseMerchantHandler)}


@pytest.fixture
def no_authentication_handlers(internal_handlers, merchant_handlers):
    handlers = {
        PingHandler, PingDBHandler, UnistatHandler,
        MaillistSubscriptionHandler, ThreeDSChallengeResultHandler, ThreeDSMethodResultHandler,
        ButtonCashbackHandler
    }
    return internal_handlers | merchant_handlers | handlers


@pytest.fixture
def restricted_auth_method_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, RestrictAuthMethodMixin)}


def test_authentication_disabled(no_authentication_handlers):
    for cls in no_authentication_handlers:
        msg = (
            f'Handler {cls.__name__} not marked with `skip_authentication`, '
            f'but added to the no authentication set. Please remove it from the set '
            f'if authentication needed'
        )
        assert cls.skip_authentication is True, msg


def test_authentication_enabled(api_handlers, no_authentication_handlers):
    handlers = set(api_handlers) - no_authentication_handlers
    for cls in handlers:
        msg = (
            f'Handler {cls.__name__} marked with `skip_authentication`. '
            f'If this handler does not need authentication, please add it to the '
            f'no authentication set.'
        )
        assert not hasattr(cls, 'skip_authentication'), msg


def test_restricted_auth_method_handlers(restricted_auth_method_handlers):
    for handler in restricted_auth_method_handlers:
        assert isinstance(handler.allowed_auth_method, AuthenticationMethod)
