import pytest
from mock import patch

from intranet.trip.src.enums import Citizenship, DocumentType
from intranet.trip.src.logic.aeroclub.documents import AeroclubDocumentsSync
from intranet.trip.src.lib.aeroclub.enums import DocumentTypestringEnum


pytestmark = pytest.mark.asyncio


db_documents_data_1 = [
    {
        'document_id': 1,
        'document_type': DocumentType.passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '123456',
    },
    {
        'document_id': 2,
        'document_type': DocumentType.passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '123456',
    },
    {
        'document_id': 3,
        'document_type': DocumentType.passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '654321',
        'provider_document_id': 103,
    },
    {
        'document_id': 4,
        'document_type': DocumentType.external_passport,
        'citizenship': Citizenship.KZ,
        'series': '12',
        'number': '12345',
        'provider_document_id': 104,
    },
    {
        'document_id': 5,
        'document_type': DocumentType.external_passport,
        'citizenship': Citizenship.KZ,
        'series': '12',
        'number': '12345',
        'provider_document_id': 105,
    },
]

aeroclub_documents_data_1 = [
    {
        'id': 103,
        'series': '1234',
        'number': '654321',
        'type': DocumentTypestringEnum.passport,
    },
    {
        'id': 104,
        'series': '12',
        'number': '12345',
        'type': DocumentTypestringEnum.national_passport,
    },
    {
        'id': 105,
        'series': '12',
        'number': '12345',
        'type': DocumentTypestringEnum.national_passport,
    },
]


db_documents_data_2 = [
    {
        'document_id': 1,
        'document_type': DocumentType.passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '123456',
        'provider_document_id': 101,
    },
]

aeroclub_documents_data_2 = []


db_documents_data_3 = [
    {
        'document_id': 1,
        'document_type': DocumentType.passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '123456',
    },
    {
        'document_id': 2,
        'document_type': DocumentType.external_passport,
        'citizenship': Citizenship.RU,
        'series': '1234',
        'number': '654321',
    },
]

aeroclub_documents_data_3 = [
    {
        'id': 101,
        'type': DocumentTypestringEnum.passport,
        'series': '1234',
        'number': '123456',
    },
]


class MockedAeroclubClient:

    def __init__(self, documents_data):
        self.documents_data = documents_data

    async def get_profile(self, *args, **kwargs):
        return {'documents': self.documents_data}

    async def add_document(self, *args, **kwargs):
        return 999

    async def delete_document(self, *args, **kwargs):
        return


async def _test_documents_sync(
    f,
    uow,
    db_documents_data,
    aeroclub_documents_data,
    to_delete_in_trip: set,
    to_delete_in_aeroclub: set,
    to_create_in_aeroclub: set,
    bind_mapping: dict,
):
    await f.create_person(person_id=1)
    for document in db_documents_data:
        await f.create_person_document(person_id=1, issued_on='2020-01-01', **document)
    person = await uow.persons.get_person(person_id=1)

    async def get_profile(self, *args, **kwargs):
        return {'documents': aeroclub_documents_data}

    documents_sync = AeroclubDocumentsSync(uow, person)
    mocked_aeroclub = MockedAeroclubClient(aeroclub_documents_data)

    with patch('intranet.trip.src.logic.aeroclub.documents.aeroclub', mocked_aeroclub):
        await documents_sync.sync()

        assert set(documents_sync._to_delete_in_trip) == to_delete_in_trip
        assert set(documents_sync._to_delete_in_aeroclub) == to_delete_in_aeroclub
        _to_create_in_aeroclub_ids = {d.document_id for d in documents_sync._to_create_in_aeroclub}
        assert _to_create_in_aeroclub_ids == to_create_in_aeroclub
        assert documents_sync._bind_mapping == bind_mapping


async def test_remove_duplicates(f, uow):
    await _test_documents_sync(
        f,
        uow,
        db_documents_data_1,
        aeroclub_documents_data_1,
        to_delete_in_trip={1, 2, 4},
        to_delete_in_aeroclub={104},
        to_create_in_aeroclub=set(),
        bind_mapping={},
    )


async def test_document_with_wrong_provider_id(f, uow):
    await _test_documents_sync(
        f,
        uow,
        db_documents_data_2,
        aeroclub_documents_data_2,
        to_delete_in_trip=set(),
        to_delete_in_aeroclub=set(),
        to_create_in_aeroclub={1},
        bind_mapping={1: 999},
    )


async def test_documents_without_provider_id(f, uow):
    await _test_documents_sync(
        f,
        uow,
        db_documents_data_3,
        aeroclub_documents_data_3,
        to_delete_in_trip=set(),
        to_delete_in_aeroclub=set(),
        to_create_in_aeroclub={2},
        bind_mapping={1: 101, 2: 999},
    )
