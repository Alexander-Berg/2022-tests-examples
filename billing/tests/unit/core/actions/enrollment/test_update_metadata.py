import logging
import uuid
from copy import deepcopy
from dataclasses import asdict
from uuid import uuid4

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, has_entries, has_length, has_properties, has_property

from billing.yandex_pay.yandex_pay.core.actions.enrollment.update_metadata import UpdateEnrollmentMetadataAction
from billing.yandex_pay.yandex_pay.core.actions.image.mastercard import UpdateMasterCardCardImageAction
from billing.yandex_pay.yandex_pay.core.actions.image.visa import UpdateVisaCardImageAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.interactions.visa.entities.content import Content
from billing.yandex_pay.yandex_pay.interactions.visa.entities.metadata import CardData, CardMetaData


@pytest.fixture
def time_now():
    return utcnow()


@pytest.fixture
def fake_metadata():
    return {
        'metadata': 'fake',
        'contains': {
            'list': [1, None],
            'tuple': (2, 3),
            'uuid': uuid.UUID(int=0),
            'enum': TSPType.MASTERCARD,
        },
    }


@pytest.fixture
def serialized_metadata(fake_metadata):
    return UpdateEnrollmentMetadataAction.serialize_kwargs(fake_metadata)


@pytest.fixture
def expected_metadata(serialized_metadata):
    expected_metadata_ = deepcopy(serialized_metadata)
    expected_metadata_['contains']['tuple'] = [2, 3]
    return expected_metadata_


@pytest.fixture
def tsp_type(request):
    status = getattr(request, 'param', TSPType.MASTERCARD)
    return getattr(status, 'value', status)


@pytest.fixture
async def card(storage, tsp_type, time_now, randn):
    return await storage.card.create(
        Card(
            trust_card_id=str(uuid4()),
            owner_uid=randn(),
            tsp=tsp_type,
            expire=time_now,
            last4='0000',
            card_id=uuid4(),
        )
    )


@pytest.fixture
async def enrollment(storage, card):
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_card_id=None,
            tsp_token_id=str(uuid4()),
            tsp_token_status=TSPTokenStatus.ACTIVE,
            card_last4=card.last4,
        )
    )


@pytest.fixture
def card_art_guid():
    return 'img-guid'


@pytest.fixture
def visa_card_metadata(card_art_guid):
    return CardMetaData(
        background_color='0xBBAADD',
        card_data=[
            CardData(
                guid='wrong-guid',
                content_type='notDigitalCardArt',
                content=[
                    Content(
                        mime_type='image/png',
                        width=100,
                        height=100,
                        encoded_data=None
                    ),
                ]),
            CardData(
                guid=card_art_guid,
                content_type='digitalCardArt',
                content=[
                    Content(
                        mime_type='image/png',
                        width=100,
                        height=100,
                        encoded_data=None
                    ),
                ]),
        ]
    )


@pytest.mark.parametrize('tsp_type', list(TSPType), indirect=True)
@pytest.mark.asyncio
async def test_update_metadata_logs_to_product_log(
    card, enrollment, serialized_metadata, expected_metadata, time_now, product_logs
):
    await UpdateEnrollmentMetadataAction(
        raw_tsp_metadata=serialized_metadata,
        enrollment_id=enrollment.enrollment_id,
        event_timestamp=time_now,
    ).run()

    [log] = product_logs()
    assert_that(
        log,
        has_properties(
            message='TSP token metadata updated',
            _context=has_entries(
                tsp=card.tsp,
                event_timestamp=time_now,
                uid=card.owner_uid,
                card={'card_id': card.card_id, 'pan_last4': card.last4},
                enrollment={
                    'enrollment_id': enrollment.enrollment_id,
                    'merchant_id': enrollment.merchant_id,
                    'tsp_token_id': enrollment.tsp_token_id,
                    'tsp_card_id': enrollment.tsp_card_id,
                    'tsp_token_status': enrollment.tsp_token_status,
                    'expire': enrollment.expire,
                },
                metadata={
                    'raw_tsp_metadata': serialized_metadata,
                }
            )
        )
    )


@pytest.mark.asyncio
async def test_try_update_missing_enrollment(
    time_now, serialized_metadata, dummy_logger, caplog
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
    await UpdateEnrollmentMetadataAction(
        raw_tsp_metadata=serialized_metadata,
        enrollment_id=uuid.uuid4(),
        event_timestamp=time_now,
    ).run()

    [log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Enrollment not found',
            levelno=logging.WARNING,
        )
    )


@pytest.mark.parametrize(
    'tsp_type',
    [TSPType.MASTERCARD],
    indirect=True,
)
@pytest.mark.asyncio
async def test_update_metadata_triggers_image_task_for_mastercard(
    card, storage, enrollment, serialized_metadata, time_now
):
    fake_image_url = 'https://example.test/fake.png'
    serialized_metadata['masked_card'] = {
        'digital_card_data': {'art_uri': fake_image_url}
    }
    await UpdateEnrollmentMetadataAction(
        raw_tsp_metadata=serialized_metadata,
        enrollment_id=enrollment.enrollment_id,
        event_timestamp=time_now,
    ).run()

    filters = {
        'task_type': 'run_action',
        'action_name': UpdateMasterCardCardImageAction.action_name,
        'state': TaskState.PENDING,
    }
    image_update_tasks = await alist(storage.task.find(filters=filters))
    assert_that(image_update_tasks, has_length(1))
    assert_that(
        image_update_tasks[0],
        has_property(
            'params',
            has_entries(
                action_kwargs=has_entries(
                    card_id=str(card.card_id), download_from_url=fake_image_url
                )
            )
        )
    )


@pytest.mark.parametrize(
    'tsp_type',
    [TSPType.VISA],
    indirect=True,
)
@pytest.mark.asyncio
async def test_update_metadata_triggers_image_task_for_visa(
    card,
    visa_card_metadata,
    storage,
    enrollment,
    card_art_guid,
):
    serialized_metadata = UpdateEnrollmentMetadataAction.serialize_kwargs(
        asdict(visa_card_metadata)
    )

    await UpdateEnrollmentMetadataAction(
        raw_tsp_metadata=serialized_metadata,
        enrollment_id=enrollment.enrollment_id,
    ).run()

    filters = {
        'task_type': 'run_action',
        'action_name': UpdateVisaCardImageAction.action_name,
        'state': TaskState.PENDING,
    }
    image_update_tasks = await alist(storage.task.find(filters=filters))
    assert_that(image_update_tasks, has_length(1))
    assert_that(
        image_update_tasks[0],
        has_property(
            'params',
            has_entries(
                action_kwargs=has_entries(
                    card_id=str(card.card_id), guid=card_art_guid
                )
            )
        )
    )


@pytest.mark.parametrize(
    'tsp_type',
    [TSPType.VISA],
    indirect=True,
)
@pytest.mark.asyncio
async def test_update_metadata_without_card_data(
    card,
    visa_card_metadata,
    storage,
    enrollment,
    card_art_guid,
):
    await UpdateEnrollmentMetadataAction(
        raw_tsp_metadata={'card_metadata': {'background_color': '0x000000'}},
        enrollment_id=enrollment.enrollment_id,
    ).run()

    filters = {
        'task_type': 'run_action',
        'action_name': UpdateVisaCardImageAction.action_name,
        'state': TaskState.PENDING,
    }
    image_update_tasks = await alist(storage.task.find(filters=filters))
    assert_that(image_update_tasks, has_length(0))
