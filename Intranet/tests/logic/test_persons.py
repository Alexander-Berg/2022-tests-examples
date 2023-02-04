import pytest

from intranet.trip.src.logic.persons import PassportPersonalData


pytestmark = pytest.mark.asyncio


async def test_create_person(uow):
    uid = '123456'
    email = 'test@email.ru'

    async def complement_user_contacts_mocked(user):
        assert uid == user.uid
        assert email == user.email
        return user

    pdg = PassportPersonalData(uow)
    pdg.complement_user_contacts = complement_user_contacts_mocked
    person_id = await pdg.create_person(uid=uid, email=email)
    person = await uow.persons.get_person(person_id)

    assert person.uid == uid
    assert person.email is None  # email in DB must be an empty string
