import pytest

from intranet.trip.src.enums import (
    Citizenship,
    DocumentType,
    Gender,
)
from intranet.trip.src.lib.passport.api import BaseContactsApiClient, BaseDocumentsApiClient
from intranet.trip.src.lib.passport.models import (
    Contact,
    Document,
    NationalPassport,
    InternationalPassport,
)


pytestmark = pytest.mark.asyncio

# TODO: need actualize? https://wiki.yandex-team.ru/passport/documents/
TIME = '2022-02-24T04:55:59+03:00'

common_document_fields_response = {
    'id': '1',
    'user_id': '1',
    'user_type': 'passport',
    'doc_type': 'national',
    'create_time': TIME,
    'modification_time': TIME,
    'last_used_time': TIME,
    'version': '',
    'verification_data': '',
    'verification_status': '',
    'issued_for_user': '1',
    'self_doc': 'true',
    'default': 'false',
    'doc_number': '',
}

passport_fields_response = {
    'passport_types': 'national',
    'first_name': 'Иван',
    'last_name': 'Иванов',
    'middle_name': 'Иванович',
    'birth_place': 'г.Москва',
    'birth_date': '12.01.1971',
    'country': 'ru',
    'gender': 'male',
    'issue_date': '13.01.2021',
    'issued_by': 'Отделением ОФМС Ниичаво Республики Карелия',
    'issuer_subdivision_code': '500-312',
    'registration_region': 'Московская область',
    'registration_org': 'Отдел оуфмс Лукоморье Мытищинского района',
    'registration_org_code': '300-200',
    'registration_date': '11.07.1999',
    'registration_locality': 'Долгопрудный',
    'registration_street': 'Институтский переулок',
    'registration_house': '100500',
    'registration_housing': 'корп 2',
    'registration_apartment': '10',
}

international_fields_response = {
    'passport_types': 'international',
}

national_passport = {
    **common_document_fields_response,
    **passport_fields_response,
}

international_passport = {
    **common_document_fields_response,
    **international_fields_response,
    'doc_type': 'international',
}


create_contact_response = {
    'id': 'uid/227356512/passport/10000',
    'first_name': 'Ivan',
    'second_name': None,
    'last_name': 'Dudinov',
    'email': 'ivan.dudinov@yandex.ru',
    'phone_number': '+79261111111',
    'status': 'ok',
}

contact_fields = {
    **create_contact_response,
    'owner_service': 'passport',
}


class MockedContactsApiClient(BaseContactsApiClient):
    async def add(self, user_id: str, contact: Contact) -> str:
        return contact.id

    async def get(self, user_id: str, contact_id: str) -> Contact:
        return Contact(**contact_fields)

    async def update(self, user_id: str, contact: Contact) -> str:
        pass

    async def delete(self, user_id: str, contact_id: str):
        pass

    async def list(self, user_id: str, length: int = 5, offset: int = 0) -> list[Contact]:
        return [Contact(**contact_fields)]


class MockedDocumentsApiClient(BaseDocumentsApiClient):
    async def add(self, user_id: str, document: Document) -> str:
        return document.id

    async def update(self, document: Document):
        return document.id

    async def delete(self, document_id: str) -> str:
        return document_id

    async def get(self, document_id: str) -> Document:
        national_passport['id'] = document_id
        return NationalPassport(**national_passport)

    async def list(self, user_id: str, length: int = 5, offset: int = 0) -> list[Document]:
        return [
            NationalPassport(**national_passport),
            InternationalPassport(**international_passport),
        ]


mocked_document_client = MockedDocumentsApiClient('mock')
mocked_contact_client = MockedContactsApiClient('mock')


async def test_passport_modify_document():
    assert national_passport['id'] == await mocked_document_client.add(
        user_id='1',
        document=NationalPassport(**national_passport),
    )
    assert international_passport['id'] == await mocked_document_client.update(
        document=InternationalPassport(**international_passport),
    )
    assert national_passport['id'] == await mocked_document_client.delete(
        document_id=national_passport['id'],
    )


async def test_passport_get_document():
    response = await mocked_document_client.get('1')
    assert response.id == '1'
    document: NationalPassport = response
    assert document.first_name == passport_fields_response['first_name']
    assert document.doc_type == DocumentType.passport
    assert document.country == Citizenship.RU
    assert document.gender == Gender.male
    assert document.birth_date.strftime('%d.%m.%Y') == passport_fields_response['birth_date']


async def test_passport_list_documents():
    documents = await mocked_document_client.list('1')
    assert documents[0].doc_type == DocumentType.passport
    assert isinstance(documents[0], NationalPassport)
    assert documents[1].doc_type == DocumentType.external_passport
    assert isinstance(documents[1], InternationalPassport)


async def test_passport_add_contact():
    response = await mocked_contact_client.add(
        user_id='1',
        contact=Contact(**contact_fields),
    )
    assert response == contact_fields['id']


async def test_passport_get_contact():
    response = await mocked_contact_client.get('1', contact_fields['id'])
    assert response == Contact(**contact_fields)
