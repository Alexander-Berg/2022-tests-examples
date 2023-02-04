from uuid import uuid4

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.delete import DeleteDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout.delete import DeleteLayoutAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.authorize import AuthorizeOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.revision import BumpOriginRevisionAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.mark.asyncio
async def test_deletes_layout(storage, merchant, origin, layout):
    await DeleteLayoutAction(partner_id=merchant.partner_id, layout_id=layout.layout_id).run()

    with pytest.raises(Layout.DoesNotExist):
        await storage.layout.get(layout.layout_id)


@pytest.mark.asyncio
async def test_calls_authorize_origin(merchant, origin, layout, mock_authorize_origin):
    await DeleteLayoutAction(partner_id=merchant.partner_id, layout_id=layout.layout_id).run()

    mock_authorize_origin.assert_run_once_with(partner_id=merchant.partner_id, origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_calls_delete_document(merchant, origin, layout, mock_delete_document):
    await DeleteLayoutAction(partner_id=merchant.partner_id, layout_id=layout.layout_id).run()

    mock_delete_document.assert_run_once_with(document_id=layout.document_id)


@pytest.mark.asyncio
async def test_calls_bump_revision(merchant, origin, layout, mock_bump_revision):
    await DeleteLayoutAction(partner_id=merchant.partner_id, layout_id=layout.layout_id).run()

    mock_bump_revision.assert_run_once_with(origin_id=origin.origin_id)


@pytest.fixture(autouse=True)
async def mock_authorize_origin(mock_action):
    return mock_action(AuthorizeOriginAction)


@pytest.fixture(autouse=True)
async def mock_delete_document(mock_action):
    return mock_action(DeleteDocumentAction)


@pytest.fixture(autouse=True)
async def mock_bump_revision(mock_action):
    return mock_action(BumpOriginRevisionAction)


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(Origin(origin_id=uuid4(), origin='a.test', merchant_id=merchant.merchant_id))


@pytest.fixture(autouse=True)
async def layout(storage, partner, origin, document):
    return await storage.layout.create(
        Layout(
            layout_id=uuid4(),
            origin_id=origin.origin_id,
            type=LayoutType.CHECKOUT,
            document_id=document.document_id,
        )
    )


@pytest.fixture(autouse=True)
async def document(storage, partner):
    return await storage.document.create(
        Document(document_id=uuid4(), partner_id=partner.partner_id, path='/', digest='')
    )
