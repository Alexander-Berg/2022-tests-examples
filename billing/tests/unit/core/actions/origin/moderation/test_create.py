from datetime import timedelta
from uuid import uuid4

import pytest

from sendr_pytest.matchers import close_to_datetime
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_length, match_equality

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.authorize import AuthorizeOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.check import (
    CheckHasNoApprovedModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.create import (
    CoreCreateOriginModerationAction,
    CreateOriginModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.ticket import CreateModerationTicketAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import OriginHasNoLayoutsError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin_moderation import OriginModeration


@pytest.mark.asyncio
async def test_returned(partner, origin, layout, mocker, user):
    finalization_cadence = 60
    mocker.patch.object(CoreCreateOriginModerationAction, 'finalization_cadence_sec', finalization_cadence)

    returned = await CreateOriginModerationAction(
        user=user, partner_id=partner.partner_id, origin_id=origin.origin_id
    ).run()

    delta = timedelta(seconds=20)
    expected_finalize_at = close_to_datetime(utcnow() + timedelta(seconds=finalization_cadence), delta=delta)
    assert_that(
        returned,
        equal_to(
            OriginModeration(
                origin_moderation_id=returned.origin_moderation_id,
                origin_id=origin.origin_id,
                revision=777,
                ignored=False,
                resolved=False,
                approved=False,
                created=match_equality(close_to_datetime(utcnow(), delta=delta)),
                updated=match_equality(close_to_datetime(utcnow(), delta=delta)),
                finalize_at=match_equality(expected_finalize_at),
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_authorize_origin(storage, partner, mock_authorize_origin, origin, layout, user):
    await CreateOriginModerationAction(user=user, partner_id=partner.partner_id, origin_id=origin.origin_id).run()

    mock_authorize_origin.assert_run_once_with(partner_id=partner.partner_id, origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_calls_check_has_no_approved_moderation(
    storage, partner, mock_check_has_no_approved_moderation, origin, layout, user
):
    await CreateOriginModerationAction(user=user, partner_id=partner.partner_id, origin_id=origin.origin_id).run()

    mock_check_has_no_approved_moderation.assert_run_once_with(origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_add_task_create_ticket(storage, partner, origin, layout, user):
    await CreateOriginModerationAction(user=user, partner_id=partner.partner_id, origin_id=origin.origin_id).run()

    filters = {'action_name': CreateModerationTicketAction.action_name}
    tasks = await alist(storage.task.find(filters=filters))
    assert_that(tasks, has_length(1))


@pytest.mark.asyncio
async def test_raises_when_origin_has_no_layouts(storage, partner, origin, user):
    with pytest.raises(OriginHasNoLayoutsError):
        await CreateOriginModerationAction(user=user, partner_id=partner.partner_id, origin_id=origin.origin_id).run()


@pytest.mark.asyncio
async def test_calls_authorize_role(user, mock_authorize_role, merchant, origin, layout):
    await CreateOriginModerationAction(user=user, partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

    mock_authorize_role.assert_run_once_with(partner_id=merchant.partner_id, user=user)


@pytest.fixture
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(merchant_id=merchant.merchant_id, origin_id=uuid4(), origin='https://a.test', revision=777)
    )


@pytest.fixture
async def document(storage, partner, origin):
    return await storage.document.create(
        Document(partner_id=partner.partner_id, document_id=uuid4(), path='pa/th', digest='')
    )


@pytest.fixture
async def layout(storage, partner, origin, document):
    return await storage.layout.create(
        Layout(
            layout_id=uuid4(),
            origin_id=origin.origin_id,
            document_id=document.document_id,
            type=LayoutType.CHECKOUT,
        )
    )


@pytest.fixture(autouse=True)
def mock_authorize_origin(mock_action):
    return mock_action(AuthorizeOriginAction)


@pytest.fixture(autouse=True)
def mock_authorize_role(mock_action):
    return mock_action(AuthorizeRoleAction)


@pytest.fixture(autouse=True)
def mock_check_has_no_approved_moderation(mock_action):
    return mock_action(CheckHasNoApprovedModerationAction)
