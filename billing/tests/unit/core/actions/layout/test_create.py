import io
from uuid import uuid4

import pytest
from aiohttp.web_request import FileField

from sendr_utils import alist

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.create import CreateDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout import create as layout_create_module
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.layout.create import CreateLayoutAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.authorize import AuthorizeOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.revision import BumpOriginRevisionAction
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import LayoutType
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.layout import Layout
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin


@pytest.mark.asyncio
async def test_returned(storage, partner, origin, file_field, document):
    returned = await CreateLayoutAction(
        partner_id=partner.partner_id,
        origin_id=origin.origin_id,
        type=LayoutType.CHECKOUT,
        file_field=file_field,
    ).run()

    assert_that(
        returned,
        equal_to(
            Layout(
                layout_id=returned.layout_id,
                origin_id=origin.origin_id,
                type=LayoutType.CHECKOUT,
                document_id=document.document_id,
                document=document,
                created=document.created,
                updated=document.updated,
            )
        ),
    )


@pytest.mark.asyncio
async def test_calls_create_document(mocker, storage, partner, origin, file_field, mock_create_document):
    mock = mocker.patch.object(layout_create_module, 'get_file_content_iter')

    await CreateLayoutAction(
        partner_id=partner.partner_id,
        origin_id=origin.origin_id,
        type=LayoutType.CHECKOUT,
        file_field=file_field,
    ).run()

    mock_create_document.assert_run_once_with(
        partner_id=partner.partner_id,
        path_prefix='/origin-layouts',
        original_name=file_field.filename,
        content=mock(),
    )


@pytest.mark.asyncio
async def test_calls_create_document_with_file(mocker, storage, partner, origin, file_field, mock_create_document):
    mocker.patch.object(layout_create_module, 'FILE_CHUNK_SIZE', 256)
    file_data = b'1' * (256 * 3)
    file_field = FileField(
        name='file-field-name',
        filename='file-field-filename',
        file=io.BytesIO(file_data),
        content_type='content-type',
        headers={'hea': 'ders'},
    )

    await CreateLayoutAction(
        partner_id=partner.partner_id,
        origin_id=origin.origin_id,
        type=LayoutType.CHECKOUT,
        file_field=file_field,
    ).run()

    content = mock_create_document.call_args_list[0].kwargs['content']
    assert_that(
        await alist(content),
        equal_to([b'1' * 256] * 3),
    )


@pytest.mark.asyncio
async def test_calls_authorize_origin(storage, partner, origin, file_field, mock_authorize_origin):
    await CreateLayoutAction(
        partner_id=partner.partner_id,
        origin_id=origin.origin_id,
        type=LayoutType.CHECKOUT,
        file_field=file_field,
    ).run()

    mock_authorize_origin.assert_run_once_with(partner_id=partner.partner_id, origin_id=origin.origin_id)


@pytest.mark.asyncio
async def test_calls_bump_revision(partner, origin, file_field, mock_bump_revision):
    await CreateLayoutAction(
        partner_id=partner.partner_id,
        origin_id=origin.origin_id,
        type=LayoutType.CHECKOUT,
        file_field=file_field,
    ).run()

    mock_bump_revision.assert_run_once_with(origin_id=origin.origin_id)


@pytest.fixture(autouse=True)
def file_field(mocker):
    return FileField(
        name='file-field-name',
        filename='file-field-filename',
        file=mocker.Mock(),
        content_type='content-type',
        headers={'hea': 'ders'},
    )


@pytest.fixture(autouse=True)
async def document(storage, partner):
    return await storage.document.create(
        Document(document_id=uuid4(), path='/', digest='', partner_id=partner.partner_id)
    )


@pytest.fixture(autouse=True)
async def origin(storage, merchant):
    return await storage.origin.create(
        Origin(
            origin_id=uuid4(),
            origin='a.test',
            merchant_id=merchant.merchant_id,
        )
    )


@pytest.fixture(autouse=True)
def mock_create_document(mock_action, document):
    return mock_action(CreateDocumentAction, return_value=document)


@pytest.fixture(autouse=True)
async def mock_authorize_origin(mock_action):
    return mock_action(AuthorizeOriginAction)


@pytest.fixture(autouse=True)
async def mock_bump_revision(mock_action):
    return mock_action(BumpOriginRevisionAction)
