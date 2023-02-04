import logging
from typing import List

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties, is_

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.auth_methods import (
    FilterApplicableAuthMethodsAction, ListAvailableAuthMethodsAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import ExpirationDate, UserCard
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMethod
from billing.yandex_pay.yandex_pay.core.exceptions import CoreDifferentPSPOrMerchantInPaymentMethodsError
from billing.yandex_pay.yandex_pay.utils.normalize_banks import IssuerBank


@pytest.fixture
def user_card():
    return UserCard(
        card_id='some_id',
        owner_uid=10,
        card_network=CardNetwork.MASTERCARD,
        last4='4122',
        allowed_auth_methods=[AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY],
        created=utcnow(),
        last_paid=utcnow(),
        issuer_bank=IssuerBank.ALFABANK,
        expiration_date=ExpirationDate(
            month=1,
            year=2099,
        ),
        trust_card_id='some_id',
    )


class TestFilterApplicableAuthMethodsAction:
    @pytest.fixture
    def payment_methods(self):
        return [
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='unused',
                gateway_merchant_id='unused',
                allowed_auth_methods=[AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            )
        ]

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'card_allowed_auth_methods,payment_allowed_auth_methods,selected_auth_methods',
        [
            ([AuthMethod.PAN_ONLY], [AuthMethod.PAN_ONLY], {AuthMethod.PAN_ONLY}),
            ([AuthMethod.PAN_ONLY], [AuthMethod.CLOUD_TOKEN], set()),
            (
                [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
                [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
                {AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN},
            ),
            (
                [AuthMethod.PAN_ONLY],
                [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
                {AuthMethod.PAN_ONLY},
            ),
            (
                [AuthMethod.CLOUD_TOKEN],
                [AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
                {AuthMethod.CLOUD_TOKEN},
            ),
        ]
    )
    async def test_filter_auth_methods_for_card(
        self,
        card_allowed_auth_methods,
        payment_allowed_auth_methods,
        selected_auth_methods,
        user_card,
        payment_methods
    ):
        user_card.allowed_auth_methods = card_allowed_auth_methods
        payment_methods[0].allowed_auth_methods = payment_allowed_auth_methods

        applicable_auth_methods = await FilterApplicableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(selected_auth_methods, equal_to(applicable_auth_methods))

    @pytest.mark.asyncio
    async def test_error_when_different_gateways(self, user_card):
        payment_methods = [
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='a',
                gateway_merchant_id='unused',
                allowed_auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='b',
                gateway_merchant_id='unused',
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            )
        ]

        with pytest.raises(CoreDifferentPSPOrMerchantInPaymentMethodsError):
            await FilterApplicableAuthMethodsAction(
                payment_methods=payment_methods,
                user_card=user_card,
            ).run()

    @pytest.mark.asyncio
    async def test_error_when_different_gateway_merchants(self, user_card):
        payment_methods = [
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='a',
                gateway_merchant_id='1',
                allowed_auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='a',
                gateway_merchant_id='2',
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            )
        ]

        with pytest.raises(CoreDifferentPSPOrMerchantInPaymentMethodsError):
            await FilterApplicableAuthMethodsAction(
                payment_methods=payment_methods,
                user_card=user_card,
            ).run()

    @pytest.mark.asyncio
    async def test_call_logged(self, user_card, payment_methods, dummy_logs):
        user_card.allowed_auth_methods = [AuthMethod.PAN_ONLY]
        await FilterApplicableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Auth methods per card network retrieved',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user_card.owner_uid,
                        user_card=user_card,
                        payment_methods=payment_methods,
                        allowed_auth_methods_per_card_network={
                            CardNetwork.MASTERCARD: {AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN},
                        }
                    )
                ),
                has_properties(
                    message='Auth methods allowed for the card network retrieved',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user_card.owner_uid,
                        user_card=user_card,
                        payment_methods=payment_methods,
                        card_network=user_card.card_network,
                        allowed_auth_methods={AuthMethod.PAN_ONLY},
                    )
                ),
            ),
        )


class TestListAvailableAuthMethodsAction:
    @pytest.fixture
    def psp_id(self) -> str:
        return 'some_external_id'

    @pytest.fixture
    def make_payment_method(self, psp_id):
        def _inner(auth_methods: List[AuthMethod], allowed_card_networks: List[CardNetwork]):
            return PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway=psp_id,
                gateway_merchant_id='unused',
                allowed_auth_methods=auth_methods,
                allowed_card_networks=allowed_card_networks,
            )
        return _inner

    @pytest.fixture
    def payment_methods(self, make_payment_method) -> List[PaymentMethod]:
        return [
            make_payment_method(
                auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            make_payment_method(
                auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            make_payment_method(
                auth_methods=[AuthMethod.PAN_ONLY],
                allowed_card_networks=[CardNetwork.VISA],
            ),
            make_payment_method(
                auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.VISA],
            ),
        ]

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'auth_methods',
        [
            (AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN),
            (AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY),
        ]
    )
    async def test_priority_with_default_settings(
        self,
        auth_methods,
        yandex_pay_settings,
        payment_methods,
        user_card,
    ):
        yandex_pay_settings.DEFAULT_PAYMENT_METHODS = auth_methods

        result = await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(result, equal_to(list(auth_methods)))
        # ensure we didn't modify the settings
        assert_that(yandex_pay_settings.DEFAULT_PAYMENT_METHODS, is_(auth_methods))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'auth_methods',
        (
            (AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN),
            (AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY),
        )
    )
    async def test_priority_with_psp_settings(
        self,
        auth_methods,
        yandex_pay_settings,
        payment_methods,
        user_card,
        psp_id,
    ):
        yandex_pay_settings.PSP_PAYMENT_METHODS = {
            psp_id: {
                'methods': auth_methods,
                'token_systems': (CardNetwork.MASTERCARD, CardNetwork.VISA),
            },
        }

        result = await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(result, equal_to(list(auth_methods)))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'card_network,other_card_system',
        [
            (CardNetwork.VISA, CardNetwork.MASTERCARD),
            (CardNetwork.MASTERCARD, CardNetwork.VISA),
        ]
    )
    async def test_token_system_excluded_for_psp(
        self,
        card_network,
        other_card_system,
        yandex_pay_settings,
        payment_methods,
        user_card,
        psp_id,
    ):
        methods = (AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY)
        yandex_pay_settings.PSP_PAYMENT_METHODS = {
            psp_id: {
                'methods': methods,
                'token_systems': (other_card_system,),
            },
        }
        user_card.card_network = card_network

        result = await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(result, equal_to([AuthMethod.PAN_ONLY]))
        # ensure we didn't modify the settings
        assert_that(
            yandex_pay_settings.PSP_PAYMENT_METHODS[psp_id]['methods'],
            is_(methods),
        )

    @pytest.mark.asyncio
    async def test_should_return_pan_if_no_token_systems_allowed(
        self,
        yandex_pay_settings,
        payment_methods,
        psp_id,
        user_card,
    ):
        methods = (AuthMethod.CLOUD_TOKEN, AuthMethod.PAN_ONLY)
        yandex_pay_settings.PSP_PAYMENT_METHODS = {
            psp_id: {
                'methods': methods,
                'token_systems': (),
            },
        }

        result = await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(result, equal_to([AuthMethod.PAN_ONLY]))
        # ensure we didn't modify the settings
        assert_that(
            yandex_pay_settings.PSP_PAYMENT_METHODS[psp_id]['methods'],
            is_(methods),
        )

    @pytest.mark.asyncio
    async def test_should_not_raise_if_cloud_token_disabled_and_no_token_systems_allowed(
        self,
        yandex_pay_settings,
        payment_methods,
        psp_id,
        user_card,
    ):
        methods = (AuthMethod.PAN_ONLY,)
        yandex_pay_settings.PSP_PAYMENT_METHODS = {
            psp_id: {
                'methods': methods,
                'token_systems': (),
            },
        }

        result = await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        assert_that(result, equal_to([AuthMethod.PAN_ONLY]))
        # ensure we didn't modify the settings
        assert_that(
            yandex_pay_settings.PSP_PAYMENT_METHODS[psp_id]['methods'],
            is_(methods),
        )

    @pytest.mark.asyncio
    async def test_call_logged(
        self, user_card, payment_methods, psp_id, yandex_pay_settings, dummy_logs
    ):
        yandex_pay_settings.PSP_PAYMENT_METHODS = {
            psp_id: {
                'methods': (AuthMethod.PAN_ONLY,),
                'token_systems': (),
            },
        }

        await ListAvailableAuthMethodsAction(
            payment_methods=payment_methods,
            user_card=user_card,
        ).run()

        logs = dummy_logs()
        assert_that(
            logs,
            has_items(
                has_properties(
                    message='Auth preference list evaluated',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user_card.owner_uid,
                        user_card=user_card,
                        payment_methods=payment_methods,
                        default_payment_methods=yandex_pay_settings.DEFAULT_PAYMENT_METHODS,
                        psp_id=psp_id,
                        psp_payment_methods=yandex_pay_settings.PSP_PAYMENT_METHODS,
                        auth_preference_list=[AuthMethod.PAN_ONLY],
                    )
                ),
                has_properties(
                    message='Auth methods list evaluated',
                    levelno=logging.INFO,
                    _context=has_entries(
                        uid=user_card.owner_uid,
                        user_card=user_card,
                        payment_methods=payment_methods,
                        auth_methods=[AuthMethod.PAN_ONLY],
                    )
                ),
            ),
        )
