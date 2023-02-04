import pytest

from intranet.trip.src.api.schemas import ExtPersonCreate, ExtPersonUpdate
from intranet.trip.src.enums import ExtPersonStatus, ServiceType
from intranet.trip.src.logic.ext_persons import (
    create_ext_person,
    regenerate_secret,
    update_ext_person,
    ExtPersonException,
)


pytestmark = pytest.mark.asyncio


async def test_create_ext_person(f, uow):
    await f.create_person(person_id=1)
    ext_person_create = ExtPersonCreate(
        name='Alias',
        email='email@email.com',
    )
    result = await create_ext_person(
        uow=uow,
        person_id=1,
        ext_person_create=ext_person_create,
    )
    ext_person_id = result['ext_person_id']
    ext_person = await uow.ext_persons.get_by_id(ext_person_id=ext_person_id)
    assert ext_person.name == 'Alias'
    assert ext_person.email == 'email@email.com'
    assert ext_person.status == ExtPersonStatus.pending
    assert ext_person.external_uid
    assert ext_person.secret


async def test_regenerate_secret(f, uow):
    old_secret = 'old_secret'
    await f.create_person(person_id=1)
    await f.create_ext_person(
        person_id=1,
        ext_person_id=1,
        secret=old_secret,
        status=ExtPersonStatus.completed,
    )
    await regenerate_secret(
        uow=uow,
        person_id=1,
        ext_person_id=1,
    )
    ext_person = await uow.ext_persons.get_by_id(ext_person_id=1)
    assert ext_person.status == ExtPersonStatus.pending
    assert ext_person.secret != old_secret


ext_person_update_data = {
    'gender': 'male',
    'date_of_birth': '1990-01-01',
    'phone_number': '+79991111111',
    'first_name': 'Имя',
    'last_name': 'Фамилия',
    'first_name_en': 'First',
    'last_name_en': 'Second',
    'secret': 'secret',
    'email': 'email@email.com',
    'documents': [
        {
            'document_type': 'passport',
            'citizenship': 'RU',
            'series': '1234',
            'number': '456789',
            'issued_on': '1990-03-03',
            'first_name': 'Имя',
            'last_name': 'Фамилия',
        },
        {
            'document_type': 'external_passport',
            'citizenship': 'RU',
            'series': '12',
            'number': '567823',
            'issued_on': '1991-03-03',
            'expires_on': '1999-03-03',
            'first_name': 'First',
            'last_name': 'Second',
        },
    ],
    'bonus_cards': [
        {
            'number': '123',
            'service_provider_type': 'avia',
            'service_provider_code': 'SU',
        },
    ],
}


async def test_update_ext_person(f, uow):
    await f.create_service_provider(
        service_type='avia',
        code='SU',
        name='Аэрофлот',
        name_en='Aeroflot',
    )
    await f.create_person(person_id=1)
    await f.create_ext_person(
        person_id=1,
        ext_person_id=1,
        secret='secret',
        status=ExtPersonStatus.pending,
    )
    ext_person_update = ExtPersonUpdate(**ext_person_update_data)
    await update_ext_person(
        uow=uow,
        ext_person_id=1,
        ext_person_update=ext_person_update,
    )
    ext_person = await uow.ext_persons.get_by_id(ext_person_id=1)
    assert ext_person.status == ExtPersonStatus.completed
    assert ext_person.secret != ext_person_update.secret
    assert len(ext_person.documents) == 2
    assert len(ext_person.bonus_cards) == 1
    bonus_card = ext_person.bonus_cards[0]
    assert bonus_card.service_provider_type == ServiceType.avia
    assert bonus_card.service_provider_code == 'SU'


async def test_update_ext_person_wrong_secret(f, uow):
    await f.create_person(person_id=1)
    await f.create_ext_person(
        person_id=1,
        ext_person_id=1,
        secret='secret',
        status=ExtPersonStatus.pending,
    )
    data = dict(ext_person_update_data)
    data['secret'] = 'wrong_secret'
    ext_person_update = ExtPersonUpdate(**data)
    with pytest.raises(ExtPersonException) as exc_info:
        await update_ext_person(
            uow=uow,
            ext_person_id=1,
            ext_person_update=ext_person_update,
        )
    assert str(exc_info.value) == 'Wrong secret'
