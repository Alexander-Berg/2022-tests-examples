from uuid import uuid4

import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, has_length, has_properties, instance_of, match_equality, matches_regexp

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.ticket import CreateModerationTicketAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions.startrek import StartrekClient
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import TaskType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_add_task_create_ticket_async(storage, moderation):
    await CreateModerationTicketAction(origin_moderation_id=moderation.origin_moderation_id).run_async()

    filters = {
        'action_name': CreateModerationTicketAction.action_name,
        'params': lambda field: field['action_kwargs']['origin_moderation_id'].astext
        == str(moderation.origin_moderation_id),
    }
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(tasks, has_length(1))
    task = tasks[0]

    expected_ticket_task_params = {
        'max_retries': 10,
        'action_kwargs': {
            'origin_moderation_id': str(moderation.origin_moderation_id),
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


@pytest.mark.asyncio
async def test_calls_create_issue(
    partner, origin, moderation, mock_startrek_create_issue, yandex_pay_admin_settings, storage
):
    await CreateModerationTicketAction(moderation.origin_moderation_id).run()

    mock_startrek_create_issue.assert_awaited_once_with(
        queue=yandex_pay_admin_settings.STARTREK_ORIGIN_MODERATION_QUEUE,
        unique=str(moderation.origin_moderation_id),
        summary='Модерация сайта "a.test" #1',
        description=match_equality(
            all_of(
                matches_regexp(rf'{partner.partner_id}'),
                matches_regexp(rf'{origin.merchant_id}'),
                matches_regexp(rf'{origin.origin_id}'),
                matches_regexp(rf'{origin.origin}'),
                matches_regexp(rf'revision: %%{origin.revision}%%'),
                matches_regexp(r'email@test'),
            )
        ),
    )


@pytest.mark.asyncio
async def test_create_moderation(
    partner, origin, moderation, mock_startrek_create_issue, yandex_pay_admin_settings, storage
):
    created_moderation = await CreateModerationTicketAction(moderation.origin_moderation_id).run()

    assert_that(created_moderation, instance_of(OriginModeration))


@pytest.mark.asyncio
async def test_save_ticket_id(
    partner, origin, moderation, mock_startrek_create_issue, yandex_pay_admin_settings, storage
):
    await CreateModerationTicketAction(moderation.origin_moderation_id).run()

    saved_moderation = await storage.origin_moderation.get(moderation.origin_moderation_id)
    assert saved_moderation.ticket == 'TICKET-1'


@pytest.mark.asyncio
async def test_calls_create_issue__without_optionals(
    storage, partner, origin, moderation, mock_startrek_create_issue, yandex_pay_admin_settings
):
    partner.registration_data.contact.email = None
    partner = await storage.partner.save(partner)

    await CreateModerationTicketAction(moderation.origin_moderation_id).run()

    calls = mock_startrek_create_issue.call_args_list
    assert_that(calls[0].kwargs['description'], matches_regexp('Merchant email: %%null%%'))


@pytest.fixture(autouse=True)
def mock_startrek_create_issue(mocker):
    return mocker.patch.object(
        StartrekClient,
        'create_issue',
        mocker.AsyncMock(return_value=mocker.Mock(id='xxxyyy', key='TICKET-1')),
    )


@pytest.fixture
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='a.test', revision=1)
    )


@pytest.fixture(autouse=True)
async def moderation(storage, origin):
    return await storage.origin_moderation.create(
        OriginModeration(
            origin_moderation_id=uuid4(),
            origin_id=origin.origin_id,
            revision=origin.revision,
            ticket='',
        )
    )
