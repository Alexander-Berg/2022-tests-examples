from copy import deepcopy
from uuid import uuid4

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_entries, has_length, has_properties

from billing.yandex_pay.yandex_pay.core.actions.enrollment.delete import MarkEnrollmentAsDeletedAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_enrollment import UpdateEnrollmentAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.image.mastercard import UpdateMasterCardCardImageAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import MasterCardTokenStatus, TSPTokenStatus, TSPType


@pytest.fixture
def tsp_token_id():
    return str(uuid4())


@pytest.fixture
def card_update_notification(tsp_token_id):
    return {
        "eventTimeStamp": "1613662929000",
        "maskedCard": {
            "srcDigitalCardId": tsp_token_id,
            "srcPaymentCardId": None,
            "panBin": "545503",
            "panLastFour": "6828",
            "tokenBinRange": "123456",
            "tokenLastFour": "0123",
            "digitalCardData": {
                "status": MasterCardTokenStatus.ACTIVE.value,
                "descriptorName": "Example Bank Product Configuration",
                "artUri": "https://example.test/fake.png",
                "artHeight": None,
                "artWidth": None,
                "pendingEvents": None
            },
            "panExpirationMonth": "04",
            "panExpirationYear": "2023",
            "paymentCardDescriptor": "MasterCard",
            "paymentCardType": "CREDIT",
            "digitalCardFeatures": None,
            "countryCode": "933",
            "maskedBillingAddress": None,
            "dcf": None,
            "serviceId": "COF_CP_GOO_1",
            "paymentAccountReference": "500150F5DE22SND132Y6PR32AR5HB",
            "dateOfCardCreated": "2020-02-24T11:32:32.060Z",
            "dateOfCardLastUsed": "2020-02-24T11:32:32.060Z"
        }
    }


@pytest.fixture
async def card(storage, randn):
    card = await storage.card.create(
        Card(
            trust_card_id='trust-card-id',
            owner_uid=randn(),
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
            card_id=uuid4(),
        )
    )
    yield card

    await storage.card.delete(card)


@pytest.fixture
async def card_enrollment(storage, card, tsp_token_id):
    enrollment = await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.INACTIVE,
            tsp_card_id=None,
            tsp_token_id=tsp_token_id,
            card_last4=card.last4,
        )
    )
    yield enrollment

    try:
        enrollment_metadata = await storage.enrollment_metadata.get(enrollment.enrollment_id)
    except Exception:
        pass
    else:
        await storage.enrollment_metadata.delete(enrollment_metadata)

    await storage.enrollment.delete(enrollment)


@pytest.fixture
def mastercard_notifications(card_update_notification):
    second_notification = deepcopy(card_update_notification)
    second_notification['eventTimeStamp'] = '1613662929001'
    second_notification['maskedCard']['digitalCardData']['status'] = MasterCardTokenStatus.CANCELLED.value
    return {
        "digitalCardUpdateNotifications": [
            second_notification,
            card_update_notification
        ]
    }


@pytest.fixture(autouse=True)
async def old_tasks(storage):
    old_tasks_ids = {
        task.task_id
        async for task in storage.task.find()
    }
    yield old_tasks_ids

    filters = {'task_id': lambda field: ~field.in_(old_tasks_ids)}
    async for task in storage.task.find(filters=filters):
        await storage.task.delete(task)


@pytest.mark.asyncio
async def test_tasks_created_in_correct_order(
    run_action, app, storage, card_enrollment, mastercard_notifications, tsp_token_id, old_tasks
):
    response = await app.post(
        'events/mastercard/notifications/card', json=mastercard_notifications
    )
    assert_that(response.status, equal_to(204))
    assert_that(await response.text(), equal_to(''))
    assert_that(int(response.headers.get('Content-Length', 0)), equal_to(0))

    filters = {
        'task_type': 'run_action',
        'action_name': lambda field: field.in_(
            [
                UpdateEnrollmentAction.action_name,
                MarkEnrollmentAsDeletedAction.action_name,
            ],
        ),
        'task_id': lambda field: ~field.in_(old_tasks),
    }
    status_update_tasks = await alist(
        storage.task.find(filters=filters, order=('task_id',))
    )

    assert_that(status_update_tasks, has_length(2))
    task1, task2 = status_update_tasks

    expected_task1_properties = {
        'params': {
            "max_retries": UpdateEnrollmentAction.max_retries,
            "action_kwargs": {
                "tsp": "mastercard",
                "event_timestamp": "2021-02-18 15:42:09+00:00",
                "reason": None,
                "tsp_token_id": tsp_token_id,
                "tsp_token_status": TSPTokenStatus.ACTIVE.value,
                'card_last4': '6828',
                'expire': '2023-04-30 00:00:00+00:00'
            }
        },
        'state': TaskState.PENDING,
        'action_name': UpdateEnrollmentAction.action_name,
    }
    assert_that(task1, has_properties(expected_task1_properties))

    expected_task2_properties = {
        'params': {
            "max_retries": MarkEnrollmentAsDeletedAction.max_retries,
            "action_kwargs": {
                "tsp": "mastercard",
                "event_timestamp": "2021-02-18 15:42:09.001000+00:00",
                "reason": None,
                "tsp_token_id": tsp_token_id,
            }
        },
        'state': TaskState.PENDING,
        'action_name': MarkEnrollmentAsDeletedAction.action_name,
    }
    assert_that(task2, has_properties(expected_task2_properties))

    # manually triggering tasks without using ActionWorker
    await run_action(action_cls=UpdateEnrollmentAction, action_kwargs=task1.params['action_kwargs'])
    enrollment = await storage.enrollment.get(card_enrollment.enrollment_id)
    assert_that(enrollment, has_properties(tsp_token_status=TSPTokenStatus.ACTIVE))

    # enrollment metadata update task
    filters = {
        'task_type': 'run_action',
        'action_name': UpdateEnrollmentMetadataAction.action_name,
        'task_id': lambda field: ~field.in_(old_tasks)
    }
    metadata_update_tasks = await alist(storage.task.find(filters=filters))
    assert_that(metadata_update_tasks, has_length(1))

    metadata_task1 = metadata_update_tasks[0]
    expected_metadata_task1_properties = {
        'params': {
            "max_retries": 10,
            "action_kwargs": {
                "event_timestamp": "2021-02-18 15:42:09+00:00",
                "tsp": "mastercard",
                "tsp_token_id": tsp_token_id,
                "raw_tsp_metadata": {
                    "masked_card": {
                        "pan_bin": "545503",
                        "service_id": "COF_CP_GOO_1",
                        "pan_last_four": "6828",
                        "token_last_four": "0123",
                        "digital_card_data": {
                            "art_uri": "https://example.test/fake.png",
                            "descriptor_name": "Example Bank Product Configuration",
                        },
                        "payment_card_type": "CREDIT",
                        "pan_expiration_year": "2023",
                        "date_of_card_created": "2020-02-24 11:32:32.060000+00:00",
                        "pan_expiration_month": "04",
                        "date_of_card_last_used": "2020-02-24 11:32:32.060000+00:00",
                        "payment_card_descriptor": "MasterCard",
                        "payment_account_reference": "500150F5DE22SND132Y6PR32AR5HB",
                    }
                }
            }
        },
        'state': TaskState.PENDING
    }
    assert_that(metadata_task1, has_properties(expected_metadata_task1_properties))

    # manually triggering tasks without using ActionWorker
    await run_action(action_cls=UpdateEnrollmentMetadataAction, action_kwargs=metadata_task1.params['action_kwargs'])

    await run_action(action_cls=MarkEnrollmentAsDeletedAction, action_kwargs=task2.params['action_kwargs'])
    enrollment = await storage.enrollment.get(card_enrollment.enrollment_id)
    assert enrollment.tsp_token_status == TSPTokenStatus.DELETED

    # image update task
    filters = {
        'task_type': 'run_action',
        'action_name': UpdateMasterCardCardImageAction.action_name,
        'task_id': lambda field: ~field.in_(old_tasks),
    }
    image_update_tasks = await alist(storage.task.find(filters=filters))
    assert_that(image_update_tasks, has_length(1))

    image_update_task = image_update_tasks[0]
    assert_that(
        image_update_task,
        has_properties(
            params=has_entries(
                action_kwargs=has_entries(
                    card_id=str(card_enrollment.card_id),
                    download_from_url='https://example.test/fake.png',
                )
            ),
            state=TaskState.PENDING,
        )
    )
