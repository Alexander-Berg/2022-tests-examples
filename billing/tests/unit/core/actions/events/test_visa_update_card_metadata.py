from datetime import timedelta
from uuid import uuid4

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_entries, has_properties

from billing.yandex_pay.yandex_pay.core.actions.events.visa import VisaProcessUpdateCardMetaDataNotificationAction
from billing.yandex_pay.yandex_pay.core.actions.visa_metadata import VisaUpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.exceptions import CoreEnrollmentNotFoundError
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime


@pytest.fixture
def pan_enrollment_id():
    return str(uuid4())


@pytest.fixture
def card_metadata_update_notification(pan_enrollment_id):
    return {
        'date': utcnow(),
        'vPanEnrollmentID': pan_enrollment_id
    }


@pytest.fixture
async def card(storage, randn):
    card = await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=randn(),
            tsp=TSPType.VISA,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )
    yield card
    await storage.card.delete(card)


@pytest.fixture
async def card2(storage, randn):
    card = await storage.card.create(
        Card(
            trust_card_id='trust-card-id2',
            owner_uid=randn(),
            tsp=TSPType.VISA,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )
    yield card
    await storage.card.delete(card)


@pytest.fixture
async def card_enrollment(storage, card, pan_enrollment_id) -> Enrollment:
    merchant = await storage.merchant.create(
        Merchant(
            merchant_id=uuid4(),
            name='the-name',
        )
    )

    enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=merchant.merchant_id,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=pan_enrollment_id,
            tsp_token_id=str(uuid4()),
            card_last4=card.last4,
        )
    )
    yield enrollment
    await storage.enrollment.delete(enrollment)


@pytest.fixture
async def card_enrollment2(storage, card2, pan_enrollment_id) -> Enrollment:
    merchant = await storage.merchant.create(
        Merchant(
            merchant_id=uuid4(),
            name='the-name2',
        )
    )

    enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card2.card_id,
            merchant_id=merchant.merchant_id,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=pan_enrollment_id,
            tsp_token_id=str(uuid4()),
            card_last4=card2.last4,
        )
    )
    yield enrollment
    await storage.enrollment.delete(enrollment)


@pytest.fixture(autouse=True)
def patch_action_context(mocker, storage):
    mocker.patch.object(
        VisaProcessUpdateCardMetaDataNotificationAction,
        'context',
        storage=storage
    )
    mocker.patch.object(
        VisaUpdateEnrollmentMetadataAction,
        'context',
        storage=storage
    )


@pytest.fixture
def mock_visa_update_metadata_action(mocker):
    mock_run = mocker.AsyncMock()
    mock_action_cls = mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.'
        'events.visa.VisaUpdateEnrollmentMetadataAction'
    )
    mock_action_cls.return_value.run_async = mock_run
    return mock_action_cls


@pytest.mark.asyncio
async def test_card_metadata_update_should_work(
    card_enrollment,
    pan_enrollment_id,
    mock_visa_update_metadata_action,
):
    await VisaProcessUpdateCardMetaDataNotificationAction(
        pan_enrollment_id=pan_enrollment_id,
    ).run()

    mock_visa_update_metadata_action.assert_called_once_with(
        enrollment_id=card_enrollment.enrollment_id,
        pan_enrollment_id=pan_enrollment_id,
    )
    mock_visa_update_metadata_action.return_value.run_async.assert_awaited_once_with()


@pytest.mark.asyncio
async def test_card_metadata_update_should_work_with_few_enrollments(
    card_enrollment,
    card_enrollment2,
    pan_enrollment_id,
    mock_visa_update_metadata_action,
):

    await VisaProcessUpdateCardMetaDataNotificationAction(
        pan_enrollment_id=pan_enrollment_id,
    ).run()

    mock_visa_update_metadata_action.assert_any_call(
        enrollment_id=card_enrollment.enrollment_id,
        pan_enrollment_id=pan_enrollment_id,
    )
    mock_visa_update_metadata_action.assert_any_call(
        enrollment_id=card_enrollment2.enrollment_id,
        pan_enrollment_id=pan_enrollment_id,
    )


@pytest.mark.asyncio
async def test_card_metadata_should_raise_if_enrollment_not_found(
    mock_visa_update_metadata_action,
    pan_enrollment_id,
):
    with pytest.raises(CoreEnrollmentNotFoundError):
        await VisaProcessUpdateCardMetaDataNotificationAction(
            pan_enrollment_id=pan_enrollment_id,
        ).run()


@pytest.mark.asyncio
async def test_async_run_of_action_is_delayed(pan_enrollment_id, storage):
    await VisaProcessUpdateCardMetaDataNotificationAction(
        pan_enrollment_id=pan_enrollment_id
    ).run_async()

    filters = {'action_name': VisaProcessUpdateCardMetaDataNotificationAction.action_name}
    [task] = await alist(storage.task.find(filters=filters))

    assert_that(
        task,
        has_properties(
            run_at=close_to_datetime(
                utcnow() + VisaProcessUpdateCardMetaDataNotificationAction.task_run_delay,
                timedelta(seconds=10),
            ),
            params=has_entries(
                action_kwargs={'pan_enrollment_id': pan_enrollment_id},
            )
        )
    )
