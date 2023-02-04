import pytest
from aiohttp import hdrs
from aiohttp_cors import CorsViewMixin

from billing.yandex_pay.yandex_pay.api.handlers.events.base import BaseLoggingNotificationHandler
from billing.yandex_pay.yandex_pay.api.handlers.events.events_handler import EventsHandler
from billing.yandex_pay.yandex_pay.api.handlers.events.mastercard import MasterCardCardNotificationHandler
from billing.yandex_pay.yandex_pay.api.handlers.events.visa import (
    VisaCardMetadataUpdateNotificationHandler, VisaNotificationHandlerBase, VisaTokenStatusUpdateNotificationHandler
)
from billing.yandex_pay.yandex_pay.api.handlers.form.mobile.base import BaseMobileAPIMixin
from billing.yandex_pay.yandex_pay.api.handlers.form.mobile.wallet.encrypt_app_id import EncryptAppIdHandler
from billing.yandex_pay.yandex_pay.api.handlers.form.web.base import BaseWebAPIMixin
from billing.yandex_pay.yandex_pay.api.handlers.form.web.csrf import CSRFTokenHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.base import BaseInternalHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.card import UserCardHandler, UserCardsHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.checkout import CheckoutPaymentTokenHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.merchant import MerchantHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.psp import PSPHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.split import SplitCallbackHandler
from billing.yandex_pay.yandex_pay.api.handlers.internal.trust import TrustPaymentTokenHandler
from billing.yandex_pay.yandex_pay.api.handlers.jwk import JsonWebKeysHandler
from billing.yandex_pay.yandex_pay.api.handlers.mixins.psp_auth import PSPAuthMixin
from billing.yandex_pay.yandex_pay.api.handlers.psp.base import BasePSPAPIHandler
from billing.yandex_pay.yandex_pay.api.handlers.psp.keys import KeysHandler
from billing.yandex_pay.yandex_pay.api.handlers.utility import PingDBHandler, PingHandler
from billing.yandex_pay.yandex_pay.api.handlers.wallet.thales import ThalesPushNotificationHandler


def get_declared_public_methods(cls) -> dict:
    methods = dict()
    d = cls.__dict__
    for key in d:
        if not key.startswith('_') and callable(d[key]):
            methods[key] = d[key]

    return methods


@pytest.fixture
def no_authentication_handlers() -> set:
    return {
        BaseInternalHandler,
        BaseLoggingNotificationHandler,
        BasePSPAPIHandler,
        CSRFTokenHandler,
        CheckoutPaymentTokenHandler,
        EncryptAppIdHandler,
        EventsHandler,
        JsonWebKeysHandler,
        KeysHandler,
        MasterCardCardNotificationHandler,
        MerchantHandler,
        PingDBHandler,
        PingHandler,
        PSPHandler,
        SplitCallbackHandler,
        ThalesPushNotificationHandler,  # implemented inside class
        TrustPaymentTokenHandler,
        UserCardsHandler,
        UserCardHandler,
        VisaCardMetadataUpdateNotificationHandler,
        VisaNotificationHandlerBase,
        VisaTokenStatusUpdateNotificationHandler,
    }


@pytest.fixture
def web_api_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, BaseWebAPIMixin)}


@pytest.fixture
def mobile_api_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, BaseMobileAPIMixin)}


@pytest.fixture
def psp_api_handlers(api_handlers):
    return {handler_cls for handler_cls in api_handlers if issubclass(handler_cls, BasePSPAPIHandler)}


@pytest.mark.parametrize('handlers', ([web_api_handlers], [mobile_api_handlers]))
def test_web_handlers_should_support_authentication(handlers,
                                                    no_authentication_handlers):
    for cls in handlers:
        if cls not in no_authentication_handlers:
            assert not hasattr(cls, 'skip_authentication'), \
                f'{cls.__name__} marked as `skip_authentication`. ' \
                'If this handler does not need authentication, please add it to the no authentication set'
        else:
            assert hasattr(cls, 'skip_authentication'), \
                f'{cls.__name__} not marked as `skip_authentication`. ' \
                'But this handler added to the no authentication set, ' \
                'please remove it from the set if authentication needed'


def test_should_support_anti_forgery_token_for_web_handlers_with_modification_methods(web_api_handlers):
    modification_method_prefixes = tuple(
        http_method.lower() for http_method in (hdrs.METH_ALL - {hdrs.METH_GET, hdrs.METH_HEAD, hdrs.METH_OPTIONS})
    )

    for cls in web_api_handlers:
        methods = get_declared_public_methods(cls)
        has_modification_methods = False
        for key in methods:
            if key.startswith(modification_method_prefixes):
                has_modification_methods = True
                break

        if has_modification_methods:
            assert not hasattr(cls, 'skip_csrf'), \
                f'{cls.__name__} should support anti forgery token because it has modification methods. '


def test_psp_handlers_should_support_authentication(psp_api_handlers,
                                                    no_authentication_handlers):
    for cls in psp_api_handlers:
        if cls not in no_authentication_handlers:
            assert issubclass(cls, PSPAuthMixin), \
                f'{cls.__name__}.should support psp authentication. ' \
                'If this handler does not need authentication, please add it to the no authentication set'
        else:
            assert not issubclass(cls, PSPAuthMixin), \
                f'{cls.__name__}.should not support psp authentication ' \
                'But this handler added to the no authentication set, ' \
                'please remove it from the set if authentication needed'


def test_cors_should_be_set_up_exactly_for_web_api(api_handlers,
                                                   web_api_handlers,
                                                   no_authentication_handlers):
    for cls in api_handlers:
        if cls in web_api_handlers:
            assert issubclass(cls, CorsViewMixin), \
                f'{cls.__name__}.should support CORS OPTIONS request.'
        else:
            assert not issubclass(cls, CorsViewMixin), \
                f'{cls.__name__}.should not support CORS OPTIONS request.'


def test_orphaned_handlers_should_be_explicitly_marked_as_auth_free(
    api_handlers,
    no_authentication_handlers,
):
    for cls in api_handlers:
        if issubclass(cls, (BaseWebAPIMixin, BaseMobileAPIMixin, BasePSPAPIHandler)):
            continue
        assert cls in no_authentication_handlers, \
            f'{cls.__name__} is not explicitly marked as auth-free, ' \
            'please add it in the no authentication set'
