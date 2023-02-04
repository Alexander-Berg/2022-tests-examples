import pytest

from mock import patch

from intranet.trip.src.api.schemas import PersonDocumentCreate
from intranet.trip.src.enums import (
    DocumentType,
    Citizenship,
)
from intranet.trip.src.logic.persons import PersonalDataGateway, get_personal_data_gateway

pytestmark = pytest.mark.asyncio


def mock_translit(text, *args, **kwargs):
    return f'translit {text}'


first_name = 'First'
last_name = 'Last'
first_name_doc = 'Имя'
last_name_doc = 'Фамилия'


async def create_person_document(
    uow,
    pdg: PersonalDataGateway,
    should_be_smth: bool,
    document_type: DocumentType,
    citizenship: Citizenship,
):
    person = await uow.persons.get_person(person_id=1)
    with patch('intranet.trip.src.logic.persons.ICAORussianLanguagePack') as icao_lang_pack_mock:
        icao_lang_pack_mock.return_value.translit = mock_translit
        await pdg.create_person_document(
            person=person,
            document_create=PersonDocumentCreate(
                person_id=1,
                document_id=1,
                document_type=document_type,
                citizenship=citizenship,
                first_name=first_name_doc if should_be_smth else first_name,
                last_name=last_name_doc if should_be_smth else first_name,
                series='1111',
                number='123123',
            )
        )


@pytest.mark.parametrize('citizenship, document_type, should_be_transliterated', (
    (Citizenship.RU, DocumentType.passport, True),
    (Citizenship.RU, DocumentType.other, False),
    (Citizenship.OTHER, DocumentType.other, False),
))
async def test_transliterate_name_by_create_person_document(
        f,
        client,
        uow,
        citizenship,
        document_type,
        should_be_transliterated,
):
    await f.create_person(
        person_id=1,
        first_name=first_name,
        last_name=last_name,
        first_name_ac=first_name,
        last_name_ac=last_name,
        first_name_ac_en=None,
        last_name_ac_en=None,
    )
    pdg: PersonalDataGateway = await get_personal_data_gateway(uow)
    person = await pdg.get_person(1)

    assert person.first_name_ac == first_name
    assert person.last_name_ac == last_name

    await create_person_document(uow, pdg, should_be_transliterated, document_type, citizenship)
    person_after_adding_document = await uow.persons.get_person(1)

    if should_be_transliterated:
        assert person_after_adding_document.first_name_ac_en == f'translit {first_name_doc}'
        assert person_after_adding_document.last_name_ac_en == f'translit {last_name_doc}'
    else:
        assert person_after_adding_document.first_name_ac_en is None
        assert person_after_adding_document.last_name_ac_en is None


@pytest.mark.parametrize('citizenship, document_type, should_be_name_changed', (
    (Citizenship.RU, DocumentType.passport, True),
    (Citizenship.RU, DocumentType.other, False),
    (Citizenship.OTHER, DocumentType.other, False),
))
async def test_change_name_by_create_person_document(
        f,
        client,
        uow,
        citizenship,
        document_type,
        should_be_name_changed,
):
    await f.create_person(
        person_id=1,
        first_name_ac=first_name,
        last_name_ac=last_name,
    )
    pdg: PersonalDataGateway = await get_personal_data_gateway(uow)
    await create_person_document(uow, pdg, should_be_name_changed, document_type, citizenship)
    person_after_adding_document = await uow.persons.get_person(1)

    if should_be_name_changed:
        assert person_after_adding_document.first_name_ac == first_name_doc
        assert person_after_adding_document.last_name_ac == last_name_doc
    else:
        assert person_after_adding_document.first_name_ac == first_name
        assert person_after_adding_document.last_name_ac == last_name


def _get_error_fields(exc_info) -> set[str]:
    """
    Пример str(exc_info.value):
    '2 validation errors for PersonDocumentCreate
    series
      The passport series must contain 4 digits (type=value_error)
    number
      The passport number must contain 6 digits (type=value_error)'

    вернет соответственно {'series', 'number'}
    """
    return {
        error_field for error_field in str(exc_info.value).split('\n')[1::2]
    }


@pytest.mark.parametrize('citizenship, document_type, first, last, series, number, error_fields', (
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', None, None, {'series', 'number'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', None, '1231', {'series', 'number'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', None, '123123', {'series'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', '1212', '23', {'number'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', '12', '123123', {'series'}),
    (Citizenship.RU, DocumentType.passport, 'Name', 'Фамилия', '1234', '123123', {'first_name'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Lastname', '1234', '123123', {'last_name'}),
    (Citizenship.RU, DocumentType.passport, 'Имя', '-Фамилия-', '1234', '123123', {'last_name'}),
    (Citizenship.RU, DocumentType.external_passport, 'Имя', 'Lastname', 'AA', '12', {'first_name'}),
    (Citizenship.RU, DocumentType.external_passport, 'Name', 'Фамилия', 'AA', '12', {'last_name'}),
    (Citizenship.RU, DocumentType.external_passport, 'Name', 'Lastname', 'AA11', '12', {'series'}),
    (Citizenship.UA, DocumentType.external_passport, 'Имя', 'Lastname', 'AA', '12', {'first_name'}),
    (Citizenship.UA, DocumentType.external_passport, 'Name', 'Фамилия', 'AA', '12', {'last_name'}),
))
async def test_validator_create_invalid_document(
        f,
        citizenship,
        document_type,
        first,
        last,
        series,
        number,
        error_fields,
):
    with pytest.raises(ValueError) as exc_info:
        PersonDocumentCreate(
            document_type=document_type,
            citizenship=citizenship,
            series=series,
            number=number,
            first_name=first,
            last_name=last,
            expires_on='2030-01-01',
        )
    assert _get_error_fields(exc_info) == error_fields


@pytest.mark.parametrize('citizenship, document_type, first, last, middle, series, number', (
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', None, '1234', '123123'),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', '', '1234', '123123'),
    (Citizenship.RU, DocumentType.passport, 'Имя', 'Фамилия', 'Отчество', '1234', '123123'),
    (Citizenship.RU, DocumentType.external_passport, 'Name', 'Lastname', None, 'AA', '12'),
    (Citizenship.RU, DocumentType.external_passport, 'Name', 'Lastname', '', 'AA', '12'),
    (Citizenship.RU, DocumentType.external_passport, 'Name', 'Lastname', 'Middle', 'AA', '12'),
))
async def test_validator_create_valid_document(
        f,
        citizenship,
        document_type,
        first,
        last,
        middle,
        series,
        number,
):
    PersonDocumentCreate(
        document_type=document_type,
        citizenship=citizenship,
        series=series,
        number=number,
        first_name=first,
        last_name=last,
        middle_name=middle,
        expires_on='2030-01-01',
    )
