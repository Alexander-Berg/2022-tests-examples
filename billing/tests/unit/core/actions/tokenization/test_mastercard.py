import re
import uuid
from copy import deepcopy
from unittest.mock import AsyncMock

import pytest

from sendr_utils import alist, utcnow

from hamcrest import all_of, assert_that, equal_to, has_entries, has_length, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.tokenization.mastercard import (
    InvalidTokenStatusError, MastercardTokenizationAction
)
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import MasterCardTokenStatus, TaskType, TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.mastercard import DigitalCardData, MaskedCard
from billing.yandex_pay.yandex_pay.interactions import CardProxyClient, TrustPaysysClient
from billing.yandex_pay.yandex_pay.interactions.cardproxy import MastercardEnrollment
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysCardInfo

FAKE_PAR = 'fake_payment_account_reference'
FAKE_DESCRIPTOR = 'fake_descriptor'
FAKE_PAN = 'fake_pan'


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
async def setup_card(storage):
    return await storage.card.create(
        Card(
            trust_card_id='some_trust_id',
            owner_uid=1,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='1234',
        )
    )


@pytest.fixture
def card_in_storage(setup_card) -> Card:
    return setup_card


@pytest.fixture
def card_id_param(card_in_storage):
    return {
        'card_id': card_in_storage.card_id,
    }


@pytest.fixture
def extra_action_params(request):
    return deepcopy(getattr(request, 'param', {}))


@pytest.fixture
def tokenization_action(
    card_id_param, extra_action_params
) -> MastercardTokenizationAction:
    return MastercardTokenizationAction(**card_id_param, **extra_action_params)


@pytest.fixture
def trust_card(card_in_storage) -> TrustPaysysCardInfo:
    return TrustPaysysCardInfo(
        card_id=card_in_storage.trust_card_id,
        card_token='pci_card_token',
        holder='holder',
        expiration_year=2025,
        expiration_month=10,
    )


@pytest.fixture
def cardproxy_token_status(request):
    return getattr(request, 'param', MasterCardTokenStatus.ACTIVE)


@pytest.fixture
def mastercard_enrollment(cardproxy_token_status, time_now) -> MastercardEnrollment:
    digital_card_data = DigitalCardData(
        status=cardproxy_token_status,
        descriptor_name=FAKE_DESCRIPTOR,
        art_uri='some_uri'
    )
    masked_card = MaskedCard(
        src_digital_card_id=str(uuid.uuid4()),
        pan_bin=FAKE_PAN,
        pan_last_four='3333',
        date_of_card_created=time_now,
        digital_card_data=digital_card_data,
        payment_account_reference=FAKE_PAR,
    )
    return MastercardEnrollment(masked_card=masked_card)


@pytest.fixture(autouse=True)
def trust_paysys_mock(mocker, trust_card) -> AsyncMock:
    return mocker.patch.object(TrustPaysysClient, 'get_card', AsyncMock(return_value=trust_card))


@pytest.fixture(autouse=True)
def cardproxy_mock(mocker, mastercard_enrollment) -> AsyncMock:
    return mocker.patch.object(
        CardProxyClient,
        'mastercard_enrollment',
        AsyncMock(return_value=mastercard_enrollment),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'cardproxy_token_status',
    MasterCardTokenStatus,
    indirect=True,
)
async def test_mastercard_tokenization_should_create_enrollment(
    tokenization_action,
    storage,
    card_in_storage,
    cardproxy_token_status,
    mastercard_enrollment,
    product_logs,
):
    await tokenization_action.run()

    enrollment = await storage.enrollment.get_by_card_id_and_merchant_id(
        card_id=card_in_storage.card_id, merchant_id=None
    )

    mastercard_status = MasterCardTokenStatus(cardproxy_token_status)
    expected_tsp_token_status = TSPTokenStatus.from_mastercard_status(mastercard_status)
    expected_properties = {
        'tsp_token_id': mastercard_enrollment.masked_card.src_digital_card_id,
        'tsp_card_id': None,
        'tsp_token_status': equal_to(expected_tsp_token_status),
    }
    assert_that(enrollment, has_properties(expected_properties))

    [log] = product_logs()
    assert_that(
        log,
        has_properties(
            message='Tokenization done',
            _context=has_entries(
                tsp='mastercard',
                card={
                    'card_id': card_in_storage.card_id,
                    'pan_last4': card_in_storage.last4,
                },
                enrollment=has_entries(
                    enrollment_id=enrollment.enrollment_id,
                    merchant_id=enrollment.merchant_id,
                    tsp_token_id=enrollment.tsp_token_id,
                    tsp_card_id=enrollment.tsp_card_id,
                    tsp_token_status=enrollment.tsp_token_status,
                    expire=enrollment.expire,
                ),
                metadata=has_entries(
                    payment_account_reference=FAKE_PAR,
                    descriptor_name=FAKE_DESCRIPTOR,
                    token_last4=None,
                    src_correlation_id=None,
                    pan_bin=FAKE_PAN,
                ),
            ),
        )
    )


@pytest.mark.asyncio
async def test_mastercard_tokenization_should_update_enrollment_metadata(
    tokenization_action,
    storage,
    card_in_storage,
    time_now,
    mocker
):
    time_now_str = time_now.isoformat(sep=' ')
    mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.tokenization.mastercard.utcnow',
        return_value=time_now
    )

    await tokenization_action.run()

    enrollment = await storage.enrollment.get_by_card_id_and_merchant_id(
        card_id=card_in_storage.card_id, merchant_id=None
    )

    created_tasks = await alist(storage.task.find())
    assert_that(created_tasks, has_length(1))
    metadata_update_task = created_tasks[0]

    expected_metadata_task_params = {
        'max_retries': 10,
        'action_kwargs': {
            'event_timestamp': time_now_str,
            'enrollment_id': str(enrollment.enrollment_id),
            'raw_tsp_metadata': {
                'masked_card': {
                    'pan_bin': FAKE_PAN,
                    'pan_last_four': '3333',
                    'digital_card_data': {
                        'art_uri': 'some_uri',
                        'descriptor_name': FAKE_DESCRIPTOR,
                    },
                    'date_of_card_created': time_now_str,
                    'payment_account_reference': FAKE_PAR,
                },
            }
        }
    }
    assert_that(
        metadata_update_task,
        has_properties(
            params=expected_metadata_task_params,
            task_type=TaskType.RUN_ACTION,
            action_name=UpdateEnrollmentMetadataAction.action_name
        )
    )


@pytest.mark.asyncio
async def test_mastercard_tokenization_should_call_clients_with_expected_params(
    tokenization_action,
    card_in_storage,
    trust_card,
    trust_paysys_mock,
    cardproxy_mock,
):
    await tokenization_action.run()

    trust_paysys_mock.assert_awaited_once_with(trust_card_id=card_in_storage.trust_card_id)
    cardproxy_mock.assert_awaited_once_with(
        uid=card_in_storage.owner_uid,
        pci_card_token=trust_card.card_token,
        expiration_month=trust_card.expiration_month,
        expiration_year=trust_card.expiration_year,
        holder_name=trust_card.holder,
    )


INVALID_TOKEN_STATUS = 'invalid_token_status'


@pytest.mark.parametrize('cardproxy_token_status', [INVALID_TOKEN_STATUS], indirect=True)
@pytest.mark.asyncio
async def test_mastercard_tokenization_fails_if_cardproxy_returns_invalid_status(
    tokenization_action, card_in_storage, caplog
):
    error = f'CardProxy MasterCard returned invalid token status: {INVALID_TOKEN_STATUS}'

    with pytest.raises(InvalidTokenStatusError, match=re.escape(error)):
        await tokenization_action.run()

    assert error in caplog.text


@pytest.mark.asyncio
async def test_can_re_tokenize_card_if_enrollment_already_exist(
    tokenization_action,
    storage,
    card_in_storage,
    mastercard_enrollment,
):
    exist_enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card_in_storage.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id='some_old_token',
            tsp_token_status=TSPTokenStatus.DELETED,
            expire=card_in_storage.expire,
            card_last4=card_in_storage.last4,
        )
    )
    await tokenization_action.run()

    enrollments_after_update = await alist(storage.enrollment.find(filters={'card_id': card_in_storage.card_id}))
    assert_that(len(enrollments_after_update), equal_to(1))

    updated_enrollment = enrollments_after_update[0]

    assert_that(
        updated_enrollment,
        all_of(
            has_property('tsp_token_id', mastercard_enrollment.masked_card.src_digital_card_id),
            has_property('tsp_token_status', equal_to(TSPTokenStatus.ACTIVE)),
            has_property('enrollment_id', equal_to(exist_enrollment.enrollment_id))
        )
    )


def test_action_should_run_in_transaction():
    assert_that(MastercardTokenizationAction.transact, equal_to(True))
