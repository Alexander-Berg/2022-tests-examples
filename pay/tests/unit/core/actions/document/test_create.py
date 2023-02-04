from uuid import UUID

import pytest

from sendr_auth import User as AuthUser

from hamcrest import assert_that, equal_to, instance_of, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.bill.search import SearchFinesAction
from pay.bill_payments.bill_payments.core.actions.document.create import CreateDocumentAction
from pay.bill_payments.bill_payments.core.actions.user.ensure import EnsureUserAction
from pay.bill_payments.bill_payments.core.exceptions import TooManyDocumentsError
from pay.bill_payments.bill_payments.interactions.kazna.entities import DocumentCode
from pay.bill_payments.bill_payments.storage.entities.document import Document


@pytest.mark.asyncio
async def test_returns_created(params):
    document = await CreateDocumentAction(**params).run()

    assert_that(
        document,
        equal_to(
            Document(
                document_id=match_equality(instance_of(UUID)),
                uid=777000,
                code=DocumentCode.DRIVER_LICENSE,
                title='MyTitle',
                value='driverlicenseno',
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_returns_created__without_optionals(params):
    del params['title']
    document = await CreateDocumentAction(**params).run()

    assert_that(
        document,
        equal_to(
            Document(
                document_id=match_equality(instance_of(UUID)),
                uid=777000,
                code=DocumentCode.DRIVER_LICENSE,
                title=None,
                value='driverlicenseno',
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_creates_in_db(storage, params):
    returned_document = await CreateDocumentAction(**params).run()

    expected_document = await storage.document.get(returned_document.document_id)
    assert_that(returned_document, equal_to(expected_document))


@pytest.mark.asyncio
async def test_ensures_user(storage, params, ensure_user_spy):
    await CreateDocumentAction(**params).run()

    ensure_user_spy.assert_called_once()


@pytest.mark.asyncio
async def test_schedules_search(storage, params, mocker):
    search_fines_mock = mocker.patch.object(SearchFinesAction, 'schedule', mocker.AsyncMock(return_value=2))

    await CreateDocumentAction(**params).run()

    search_fines_mock.assert_called_once()


@pytest.mark.asyncio
async def test_too_many_documents(storage, params, bill_payments_settings):
    bill_payments_settings.API_DOCUMENTS_LIMIT = 2
    params['value'] = 'license1'
    await CreateDocumentAction(**params).run()
    params['value'] = 'license2'
    await CreateDocumentAction(**params).run()

    with pytest.raises(TooManyDocumentsError) as exc_info:
        params['value'] = 'license3'
        await CreateDocumentAction(**params).run()

    assert_that(exc_info.value.params['limit'], equal_to(2))


@pytest.mark.asyncio
@pytest.mark.skip(
    reason=("Не помешала бы дедупликация документов" "Но не хочется её продумывать до интеграции с персонализацией")
)
async def test_duplicate_documents():
    raise NotImplementedError


@pytest.fixture
def params():
    return dict(
        user=AuthUser(uid=777000),
        code=DocumentCode.DRIVER_LICENSE,
        value='driverlicenseno',
        title='MyTitle',
    )


@pytest.fixture
def ensure_user_spy(mocker):
    return mocker.spy(EnsureUserAction, 'run')
