import re
from uuid import UUID

import pytest
from aiohttp import MultipartWriter

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_entries, has_length, has_properties, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.ticket import CreateModerationTicketAction
from billing.yandex_pay_admin.yandex_pay_admin.core.entities.enums import PayBackendType
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.documents import YandexPayAdminDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import TaskType

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.mark.asyncio
async def test_returned(app, partner, origin, layout, disable_tvm_checking):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']

    r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/moderations')

    assert_that(r.status, equal_to(200))
    assert_that(
        await r.json(),
        equal_to(
            {
                'status': 'success',
                'code': 200,
                'data': match_equality(
                    has_entries(
                        origin_id=origin_id,
                    )
                ),
            }
        ),
    )


@pytest.mark.asyncio
async def test_creates_moderation(app, partner, origin, layout, storage, disable_tvm_checking):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']

    r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/moderations')
    assert r.status < 300

    moderation_id = (await r.json())['data']['origin_moderation_id']
    assert_that(
        await storage.origin_moderation.get(moderation_id),
        has_properties(
            origin_id=UUID(origin_id),
            ticket=None,
        ),
    )


@pytest.mark.asyncio
async def test_add_task_create_issue(
    app,
    partner,
    origin,
    layout,
    disable_tvm_checking,
    storage,
    yandex_pay_admin_settings,
):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']

    r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/moderations')
    assert r.status < 300
    data = await r.json()
    origin_moderation_id = data['data']['origin_moderation_id']

    filters = {
        'action_name': CreateModerationTicketAction.action_name,
        'params': lambda field: field['action_kwargs']['origin_moderation_id'].astext == origin_moderation_id,
    }
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(tasks, has_length(1))
    task = tasks[0]

    expected_ticket_task_params = {
        'max_retries': 10,
        'action_kwargs': {
            'origin_moderation_id': origin_moderation_id,
        },
    }

    assert_that(
        task,
        has_properties(
            params=expected_ticket_task_params,
            task_type=TaskType.RUN_ACTION,
            action_name=CreateModerationTicketAction.action_name,
        ),
    )


@pytest.fixture
async def origin(app, rands, partner, role, merchant, disable_tvm_checking):
    r = await app.post(
        f'/api/web/v1/partners/{partner.partner_id}/merchants/{merchant.merchant_id}/origins',
        json={"origin": rands()},
    )
    assert r.status < 300
    return (await r.json())['data']


@pytest.fixture
async def layout(app, partner, origin, disable_tvm_checking):
    partner_id = partner.partner_id
    origin_id = origin['origin_id']
    with MultipartWriter("form-data") as mpwriter:
        file_part = mpwriter.append('filecontent', {'content-type': 'image/png'})
        file_part.set_content_disposition('form-data', filename='file.png', name='file')

        type_part = mpwriter.append('checkout')
        type_part.set_content_disposition('form-data', name='type')
        r = await app.post(f'/api/web/v1/partners/{partner_id}/origins/{origin_id}/layouts', data=mpwriter)
        assert r.status < 300
        return (await r.json())['data']


@pytest.fixture(autouse=True)
def mock_startrek_create(aioresponses_mocker):
    payload = {
        'id': 'xxxyyy',
        'key': 'TICKET-1',
        'status': {
            'id': '1',
            'key': 'open',
        },
    }
    return aioresponses_mocker.post(re.compile('.*/v2/issues'), payload=payload)


@pytest.fixture(autouse=True)
def mock_file_storage(mocker, actx_mock):
    return mocker.patch.object(
        YandexPayAdminDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(
                upload_stream=actx_mock(return_value=mocker.Mock(write=mocker.AsyncMock())),
            )
        ),
    )


@pytest.fixture(autouse=True)
def mock_pay_backends(mock_pay_backend_put_merchant, mock_pay_plus_backend_put_merchant):
    mock_pay_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_backend_put_merchant(PayBackendType.SANDBOX)
    mock_pay_plus_backend_put_merchant(PayBackendType.PRODUCTION)
    mock_pay_plus_backend_put_merchant(PayBackendType.SANDBOX)
