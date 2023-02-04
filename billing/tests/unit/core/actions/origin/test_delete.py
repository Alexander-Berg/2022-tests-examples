from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout.delete import DeleteLayoutAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin import delete as origin_delete_module
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.authorize import AuthorizeOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.delete import (
    DeleteOriginAction,
    delete_origin_layouts,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.check import (
    CheckHasNoApprovedModerationAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.moderation.delete import (
    DeleteOriginModerationsAction,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(origin_id=uuid4(), origin='https://a.test', merchant_id=merchant.merchant_id)
    )


@pytest.mark.asyncio
async def test_delete_layouts(storage, merchant, origin, mock_action):
    document = await storage.document.create(
        Document(
            document_id=uuid4(),
            partner_id=merchant.partner_id,
            path='/',
            digest='',
        )
    )
    layout = await storage.layout.create(
        Layout(
            layout_id=uuid4(),
            origin_id=origin.origin_id,
            document_id=document.document_id,
            type=LayoutType.CHECKOUT,
        )
    )
    mock = mock_action(DeleteLayoutAction)

    await delete_origin_layouts(storage, merchant.partner_id, origin)

    mock.assert_run_once_with(partner_id=merchant.partner_id, layout_id=layout.layout_id)


class TestDeleteOriginAction:
    @pytest.mark.asyncio
    async def test_deletes_origin(self, storage, merchant, origin):
        await DeleteOriginAction(partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

        with pytest.raises(Origin.DoesNotExist):
            await storage.origin.get(origin.origin_id)

    @pytest.mark.asyncio
    async def test_calls_authorize_origin(self, merchant, origin, mock_authorize_origin):
        await DeleteOriginAction(partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

        mock_authorize_origin.assert_run_once_with(partner_id=merchant.partner_id, origin_id=origin.origin_id)

    @pytest.mark.asyncio
    async def test_deletes_layout(self, storage, merchant, origin, mock_delete_layouts):
        await DeleteOriginAction(partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

        mock_delete_layouts.assert_awaited_once_with(storage, merchant.partner_id, origin)

    @pytest.mark.asyncio
    async def test_calls_check_has_no_approved_moderation(
        self, mocker, storage, merchant, origin, mock_check_has_no_approved_moderation
    ):
        await DeleteOriginAction(partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

        mock_check_has_no_approved_moderation.assert_run_once_with(origin_id=origin.origin_id)

    @pytest.mark.asyncio
    async def test_calls_delete_moderations(self, mocker, storage, merchant, origin, mock_delete_moderations):
        await DeleteOriginAction(partner_id=merchant.partner_id, origin_id=origin.origin_id).run()

        mock_delete_moderations.assert_run_once_with(origin_id=origin.origin_id)

    @pytest.fixture(autouse=True)
    async def mock_authorize_origin(self, mock_action):
        return mock_action(AuthorizeOriginAction)

    @pytest.fixture(autouse=True)
    def mock_check_has_no_approved_moderation(self, mock_action):
        return mock_action(CheckHasNoApprovedModerationAction)

    @pytest.fixture(autouse=True)
    def mock_delete_moderations(self, mock_action):
        return mock_action(DeleteOriginModerationsAction)

    @pytest.fixture(autouse=True)
    def mock_delete_layouts(self, mocker):
        return mocker.patch.object(origin_delete_module, 'delete_origin_layouts')
