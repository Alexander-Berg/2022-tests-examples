from uuid import UUID

import pytest

from sendr_utils import json_value

from hamcrest import all_of, assert_that, contains_string, has_entries, has_key, not_

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionAction, TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV2ChallengeRequest,
    Transaction,
    TransactionActionView,
    TransactionData,
    TransactionThreeDSData,
)


@pytest.fixture
def transaction(entity_threeds_authentication_request):
    return Transaction(
        transaction_id=UUID('00000000-0000-0000-0000-000000000000'),
        checkout_order_id=UUID('00000000-0000-0000-0000-000000000000'),
        integration_id=UUID('00000000-0000-0000-0000-000000000000'),
        status=TransactionStatus.THREEDS_CHALLENGE,
        card_id='card-x1234',
        data=TransactionData(
            user_ip='192.0.2.1',
            threeds=TransactionThreeDSData(
                authentication_request=entity_threeds_authentication_request,
                challenge_request=ThreeDSV2ChallengeRequest(
                    acs_url='https://hello.test',
                    creq='123456',
                    session_data='7890',
                )
            ),
        ),
        version=1,
        reason='test',
    )


@pytest.fixture
def transaction_view(transaction):
    return TransactionActionView.from_transaction(
        transaction=transaction,
        action=TransactionAction.IFRAME,
        action_url='https://yapay.test',
    )


def test_transaction_entity_does_not_expose_data(transaction):
    """Не используй рандомные данные в тесте, иначе он может стать flacky"""
    conditions = (
        contains_string('threeds_challenge'),
        contains_string('card-x1234'),
        not_(contains_string('data')),
        not_(contains_string('https://hello.test')),
        not_(contains_string('123456')),
        not_(contains_string('7890')),
        not_(contains_string('192.0.2.1')),
    )

    assert_that(str(transaction), all_of(*conditions))
    assert_that(repr(transaction), all_of(*conditions))
    assert_that(
        json_value(transaction),
        all_of(
            has_entries(
                status='threeds_challenge',
                card_id='card-x1234',
            ),
            not_(has_key('data')),
        )
    )


def test_transaction_view_entity_does_not_expose_data(transaction_view):
    """Не используй рандомные данные в тесте, иначе он может стать flacky"""
    conditions = (
        contains_string('threeds_challenge'),
        contains_string('card-x1234'),
        contains_string('IFRAME'),
        contains_string('https://yapay.test'),
        not_(contains_string('data')),
        not_(contains_string('https://hello.test')),
        not_(contains_string('123456')),
        not_(contains_string('7890')),
        not_(contains_string('192.0.2.1')),
    )

    assert_that(str(transaction_view), all_of(*conditions))
    assert_that(repr(transaction_view), all_of(*conditions))
    assert_that(
        json_value(transaction_view),
        all_of(
            has_entries(
                status='threeds_challenge',
                card_id='card-x1234',
                action='IFRAME',
                action_url='https://yapay.test',
            ),
            not_(has_key('data')),
        )
    )
