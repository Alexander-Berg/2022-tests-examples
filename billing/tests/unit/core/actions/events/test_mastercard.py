import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_length, has_properties

from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_enrollment import UpdateEnrollmentAction
from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.events.mastercard import CreateNotificationTaskAction
from billing.yandex_pay.yandex_pay.core.entities.enums import MasterCardTokenStatus, TaskType
from billing.yandex_pay.yandex_pay.core.entities.events.mastercard import DigitalCardUpdateNotification
from billing.yandex_pay.yandex_pay.core.entities.mastercard import DigitalCardData, MaskedCard


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
def fake_notification(time_now):
    digital_card_data = DigitalCardData(
        status=MasterCardTokenStatus.ACTIVE,
        descriptor_name='fake_descriptor_name',
        art_uri='fake_uri',
    )
    masked_card = MaskedCard(
        src_digital_card_id='fake_card_id',
        pan_bin='fake_bin',
        pan_last_four='1111',
        date_of_card_created=time_now,
        digital_card_data=digital_card_data,
    )
    return DigitalCardUpdateNotification(
        event_timestamp=time_now,
        masked_card=masked_card
    )


@pytest.mark.asyncio
async def test_task_created(storage, fake_notification, time_now):
    time_now_str = time_now.isoformat(sep=' ')
    action = CreateNotificationTaskAction(notification=fake_notification)
    await action.run()

    created_tasks = await alist(storage.task.find())
    assert_that(created_tasks, has_length(2))

    expected_status_task_params = {
        'max_retries': UpdateEnrollmentAction.max_retries,
        'action_kwargs': {
            'tsp': 'mastercard',
            'tsp_token_id': fake_notification.masked_card.src_digital_card_id,
            'tsp_token_status': 'active',
            'event_timestamp': time_now_str,
            'reason': None,
            'card_last4': fake_notification.masked_card.pan_last_four,
            'expire': fake_notification.masked_card.get_pan_expiration_date(),
        }
    }
    expected_metadata_task_params = {
        'max_retries': UpdateEnrollmentMetadataAction.max_retries,
        'action_kwargs': {
            'tsp': 'mastercard',
            'tsp_token_id': fake_notification.masked_card.src_digital_card_id,
            'event_timestamp': time_now_str,
            'raw_tsp_metadata': {
                'masked_card': {
                    'pan_bin': 'fake_bin',
                    'pan_last_four': '1111',
                    'digital_card_data': {
                        'art_uri': 'fake_uri',
                        'descriptor_name': 'fake_descriptor_name'
                    },
                    'date_of_card_created': time_now_str,
                }
            }
        }
    }
    assert_that(
        created_tasks,
        contains_inanyorder(
            has_properties(
                params=expected_status_task_params,
                task_type=TaskType.RUN_ACTION,
                action_name=UpdateEnrollmentAction.action_name
            ),
            has_properties(
                params=expected_metadata_task_params,
                task_type=TaskType.RUN_ACTION,
                action_name=UpdateEnrollmentMetadataAction.action_name
            ),
        )
    )


def test_action_non_transactional(fake_notification):
    action = CreateNotificationTaskAction(notification=fake_notification)
    assert_that(action.transact, equal_to(False))
