import re
import uuid
from datetime import timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties, is_

from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_enrollment import UpdateEnrollmentAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType, VisaTokenStatusUpdateReason
from billing.yandex_pay.yandex_pay.core.exceptions import CoreDataTooOldError, CoreEnrollmentNotFoundError

PREDEFINED_CARD_ID = 'aaf024bb-2f0e-4cad-9010-40328ffcae9a'


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
async def card(storage, time_now):
    return await storage.card.create(
        Card(
            trust_card_id='trust_card_id',
            owner_uid=123,
            tsp=TSPType.MASTERCARD,
            expire=time_now,
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_ID),
        )
    )


@pytest.fixture
async def enrollment(storage, card):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id='tsp-token-id',
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        )
    )


@pytest.mark.asyncio
async def test_update_enrollment(enrollment, storage, time_now):
    expire = time_now + timedelta(days=1)
    await UpdateEnrollmentAction(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.SUSPENDED,
        tsp_token_id=enrollment.tsp_token_id,
        event_timestamp=time_now,
        expire=expire,
    ).run()

    enrollment = await storage.enrollment.get(enrollment.enrollment_id)
    assert_that(
        enrollment,
        has_properties(
            tsp_token_status=TSPTokenStatus.SUSPENDED,
            tsp_event_processed=time_now,
            expire=expire,
        )
    )


@pytest.mark.asyncio
async def test_update_enrollment_logs_to_product_log(card, enrollment, storage, time_now, product_logs):
    expire = time_now + timedelta(days=1)

    extra_params = {'some': {'extra': 'params'}}

    await UpdateEnrollmentAction(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.SUSPENDED,
        tsp_token_id=enrollment.tsp_token_id,
        event_timestamp=time_now,
        reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
        extra_params=extra_params,
        expire=expire,
    ).run()

    updated_enrollment = await storage.enrollment.get(enrollment.enrollment_id)

    [log] = product_logs()
    assert_that(
        log,
        has_properties(
            message='TSP token status changed',
            _context=has_entries(
                tsp=TSPType.MASTERCARD,
                event_timestamp=time_now,
                uid=card.owner_uid,
                card={'card_id': card.card_id, 'pan_last4': card.last4},
                enrollment={
                    'enrollment_id': updated_enrollment.enrollment_id,
                    'merchant_id': updated_enrollment.merchant_id,
                    'tsp_token_id': updated_enrollment.tsp_token_id,
                    'tsp_card_id': updated_enrollment.tsp_card_id,
                    'tsp_token_status': updated_enrollment.tsp_token_status,
                    'previous_tsp_token_status': enrollment.tsp_token_status,
                    'expire': expire,
                },
                reason=VisaTokenStatusUpdateReason.CUSTOMER_CONFIRMED,
                extra_params=extra_params,
            )
        )
    )


@pytest.mark.asyncio
async def test_try_update_missing_enrollment(time_now):
    action = UpdateEnrollmentAction(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.SUSPENDED,
        tsp_token_id='non-existing',
        event_timestamp=time_now,
    )

    with pytest.raises(CoreEnrollmentNotFoundError):
        await action.run()


@pytest.mark.asyncio
async def test_update_event_too_old(storage, enrollment, time_now):
    enrollment.tsp_event_processed = time_now
    await storage.enrollment.save(enrollment)

    action = UpdateEnrollmentAction(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.SUSPENDED,
        tsp_token_id=enrollment.tsp_token_id,
        event_timestamp=time_now,
    )

    pattern = (
        f'Event timestamp [{time_now.isoformat()}] must be greater than '
        f'the timestamp of the last processed event [{time_now.isoformat()}]'
    )
    with pytest.raises(CoreDataTooOldError, match=re.escape(pattern)):
        await action.run()


def test_serialize_kwargs(time_now):
    expire = time_now + timedelta(days=1)
    action = UpdateEnrollmentAction(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.INACTIVE,
        tsp_token_id='tsp_token_id',
        event_timestamp=time_now,
        expire=expire,
    )

    expected_serialized_kwargs = {
        'tsp': 'mastercard',
        'tsp_token_status': 'inactive',
        'tsp_token_id': 'tsp_token_id',
        'event_timestamp': time_now.isoformat(sep=' '),
        'expire': expire.isoformat(sep=' '),
    }
    assert_that(
        action.serialize_kwargs(action._init_kwargs),
        equal_to(expected_serialized_kwargs)
    )


def test_deserialize_kwargs(time_now):
    expire = time_now + timedelta(days=1)
    raw_params = {
        'tsp': 'mastercard',
        'tsp_token_status': 'inactive',
        'tsp_token_id': 'tsp_token_id',
        'event_timestamp': time_now.isoformat(sep=' '),
        'expire': expire.isoformat(sep=' '),
    }

    expected_deserialized_kwargs = dict(
        tsp=TSPType.MASTERCARD,
        tsp_token_status=TSPTokenStatus.INACTIVE,
        tsp_token_id='tsp_token_id',
        event_timestamp=time_now,
        expire=expire,
    )
    assert_that(
        UpdateEnrollmentAction.deserialize_kwargs(raw_params),
        equal_to(expected_deserialized_kwargs),
    )


@pytest.mark.parametrize('exception,expected', [(CoreEnrollmentNotFoundError(), True)])
def test_should_retry_exception(exception, expected):
    assert_that(UpdateEnrollmentAction.should_retry_exception(exception), is_(expected))
