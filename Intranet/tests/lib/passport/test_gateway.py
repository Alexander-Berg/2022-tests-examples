import pytest

from intranet.trip.src.lib.passport.gateway import PassportApiGateway
from intranet.trip.src.lib.passport.models import (
    Contact,
    NationalPassport,
    InternationalPassport,
)
from .test_api import (
    MockedDocumentsApiClient,
    MockedContactsApiClient,
    common_document_fields_response,
    contact_fields,
    international_passport,
    national_passport,
)


pytestmark = pytest.mark.asyncio


mocked_gateway = PassportApiGateway(
    documents=MockedDocumentsApiClient(''),
    contacts=MockedContactsApiClient(''),
)


async def test_add_document():
    document_id = await mocked_gateway.documents.add(
        user_id='1',
        document=NationalPassport(**national_passport),
    )
    assert document_id == common_document_fields_response['id']


async def test_update_document():
    assert await mocked_gateway.documents.update(
        document=NationalPassport(**national_passport),
    )


async def test_delete_document():
    assert await mocked_gateway.documents.delete(document_id='1')


async def test_get_document():
    document = await mocked_gateway.documents.get(document_id='1')
    assert document == NationalPassport(**national_passport)


async def test_get_document_list():
    documents = await mocked_gateway.documents.list(user_id='1')
    assert documents[0] == NationalPassport(**national_passport)
    assert documents[1] == InternationalPassport(**international_passport)


async def test_add_contact():
    contact_id = await mocked_gateway.contacts.add(
        user_id='1',
        contact=Contact(**contact_fields),
    )
    assert contact_id == contact_fields['id']


async def test_get_contact():
    contact = await mocked_gateway.contacts.get(user_id='1', contact_id='1')
    assert contact == Contact(**contact_fields)


async def test_get_contact_list():
    contacts = await mocked_gateway.contacts.list(user_id='1')
    assert contacts[0] == Contact(**contact_fields)
