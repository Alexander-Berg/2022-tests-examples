import uuid

import pytest

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.create import CreateDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.document.delete import DeleteDocumentAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.issue import UpdatePartnerIssueAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner_moderation import UpdatePartnerModerationAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PartnerNotFoundError
from billing.yandex_pay_admin.yandex_pay_admin.file_storage.payments_documents import PaymentsDocsFileStorage
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.contract_document import ContractDocument
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.document import Document
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import ContractDocumentType

pytest.skip(allow_module_level=True)  # TODO: remove when refactoring is complete


@pytest.mark.asyncio
async def test_updates_partner_verification(partner, storage, run_action):
    expected_verified = not partner.verified

    await run_action(verified=expected_verified)

    updated_partner = await storage.partner.get(partner.partner_id)
    assert_that(updated_partner.verified, equal_to(expected_verified))


@pytest.mark.asyncio
async def test_downloads_payments_documents(partner, storage, payments_documents_storage_mock, run_action):
    await run_action()

    payments_documents_storage_mock.assert_exited_once()
    payments_documents_storage_mock.ctx_result.download.assert_awaited_once_with('foopath')


@pytest.mark.asyncio
async def test_creates_offer_document_in_db(partner, storage, run_action):
    await run_action()

    [document] = await storage.contract_document.find_by_partner_id(partner.partner_id)
    assert_that(
        document,
        has_properties(
            {
                'partner_id': partner.partner_id,
                'type': ContractDocumentType.PCI_DSS_CERT,
            }
        ),
    )


@pytest.mark.asyncio
async def test_calls_create_document(partner, storage, create_document_mock, file_content_iter, run_action):
    await run_action()

    create_document_mock.assert_run_once_with(
        partner_id=partner.partner_id,
        path_prefix='/offer-documents',
        original_name='barname',
        content=file_content_iter,
    )


@pytest.mark.asyncio
async def test_deletes_previous_documents(partner, storage, delete_document_mock, run_action, previous_offer_document):
    previous_document = await storage.contract_document.create(previous_offer_document)

    await run_action()

    delete_document_mock.assert_run_once_with(
        document_id=previous_document.document_id,
    )


@pytest.mark.asyncio
async def test_deletes_previous_offer_documents(
    partner, storage, delete_document_mock, run_action, previous_offer_document
):
    previous_document = await storage.contract_document.create(previous_offer_document)

    await run_action()

    with pytest.raises(ContractDocument.DoesNotExist):
        await storage.contract_document.get(previous_document.contract_document_id)


@pytest.mark.asyncio
async def test_partner_not_found(partner, storage, run_action):
    with pytest.raises(PartnerNotFoundError):
        await run_action(partner_id=uuid.uuid4())


@pytest.mark.asyncio
async def test_calls_update_issue(
    run_action,
    partner,
    mock_issue_action,
):
    await run_action()

    mock_issue_action.assert_called_once_with(partner_id=partner.partner_id)


@pytest.fixture
def file_content():
    return 'content-content'


@pytest.fixture
async def document(partner, storage):
    return await storage.document.create(
        Document(
            document_id=uuid.uuid4(),
            partner_id=partner.partner_id,
            digest='digigest',
            path='bazzzpath',
        )
    )


@pytest.fixture
async def previous_document(partner, storage):
    return await storage.document.create(
        Document(
            document_id=uuid.uuid4(),
            partner_id=partner.partner_id,
            digest='digigest',
            path='barpath',
        )
    )


@pytest.fixture
def previous_offer_document(partner, storage, previous_document):
    return ContractDocument(
        contract_document_id=uuid.uuid4(),
        partner_id=partner.partner_id,
        type=ContractDocumentType.CONTRACT,
        document_id=previous_document.document_id,
    )


@pytest.fixture(autouse=True)
def create_document_mock(mock_action, document):
    return mock_action(CreateDocumentAction, return_value=document)


@pytest.fixture(autouse=True)
def delete_document_mock(mock_action):
    return mock_action(DeleteDocumentAction)


@pytest.fixture
def file_content_iter(file_content):
    async def _file_content_iter():
        yield file_content

    return _file_content_iter()


@pytest.fixture(autouse=True)
def payments_documents_storage_mock(mocker, actx_mock, file_content, file_content_iter):
    return mocker.patch.object(
        PaymentsDocsFileStorage,
        'acquire',
        actx_mock(
            return_value=mocker.Mock(
                download=mocker.AsyncMock(
                    return_value=mocker.Mock(
                        content=file_content_iter,
                    )
                )
            )
        ),
    )


@pytest.fixture
def run_action(partner):
    async def _run_action(**kwargs):
        kwargs.setdefault('partner_id', partner.partner_id)
        kwargs.setdefault('verified', partner.verified)
        kwargs.setdefault(
            'documents', [{'type': ContractDocumentType.PCI_DSS_CERT, 'path': 'foopath', 'name': 'barname'}]
        )
        return await UpdatePartnerModerationAction(**kwargs).run()

    return _run_action


@pytest.fixture(autouse=True)
def mock_issue_action(mock_action):
    return mock_action(UpdatePartnerIssueAction)
