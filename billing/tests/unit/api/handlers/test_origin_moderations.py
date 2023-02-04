from datetime import datetime, timezone
from uuid import UUID, uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.create import CreateOriginModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration

PARTNER_ID = uuid4()
ORIGIN_ID = uuid4()
FILE_CONTENT = b'1' * 1000


@pytest.mark.asyncio
async def test_success(
    app,
    disable_tvm_checking,
    mock_action,
):
    mock_action(
        CreateOriginModerationAction,
        OriginModeration(
            origin_moderation_id=UUID('e6d6e59b-f21b-4b5a-8ccb-febc28e9d660'),
            origin_id=UUID('87a5c726-de4a-4eed-a532-f4a9323857d2'),
            ticket='TICKET-1',
            ignored=False,
            approved=True,
            resolved=True,
            revision=1,
            reason={'some': 'thing'},
            created=datetime(2021, 11, 30, 10, 30, 50, tzinfo=timezone.utc),
            updated=datetime(2021, 12, 30, 10, 30, 50, tzinfo=timezone.utc),
        ),
    )

    response = await app.post(f'/api/web/v1/partners/{PARTNER_ID}/origins/{ORIGIN_ID}/moderations')

    assert_that(response.status, equal_to(200))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': {
                    'origin_moderation_id': 'e6d6e59b-f21b-4b5a-8ccb-febc28e9d660',
                    'origin_id': '87a5c726-de4a-4eed-a532-f4a9323857d2',
                    'approved': True,
                    'resolved': True,
                    'created': '2021-11-30T10:30:50+00:00',
                    'updated': '2021-12-30T10:30:50+00:00',
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_create_moderation(
    app,
    disable_tvm_checking,
    mock_action,
    user,
):
    mock_create_moderation = mock_action(CreateOriginModerationAction)

    await app.post(f'/api/web/v1/partners/{PARTNER_ID}/origins/{ORIGIN_ID}/moderations')

    mock_create_moderation.assert_run_once_with(
        partner_id=PARTNER_ID,
        origin_id=ORIGIN_ID,
        user=user,
    )


@pytest.fixture(autouse=True)
def mock_create_moderation(mock_action):
    return
