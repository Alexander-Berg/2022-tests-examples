import pytest


pytestmark = pytest.mark.asyncio


persons = [
    {'company_id': 1, 'holding_id': 2, 'first_name': 'Иванов', 'login': 'user1', },
    {'company_id': 1, 'holding_id': 2, 'first_name': 'Петров', 'login': 'user2', },
    {'company_id': 1, 'holding_id': 2, 'first_name': 'Ивановский', 'login': 'user3', },
    {'company_id': 2, 'holding_id': 3, 'first_name': 'Иванов', 'login': 'user4', },
    {'company_id': 2, 'holding_id': 3, 'first_name': 'Петрович', 'login': 'user5', },
    {'company_id': 5, 'holding_id': 2, 'first_name': 'Худавердян', 'login': 'user6', },
]


async def _create_companies(f):
    for holding_id in {(p['holding_id']) for p in persons}:
        await f.create_holding(holding_id)
    for company_id, holding_id in {(p['company_id'], p['holding_id']) for p in persons}:
        await f.create_company(company_id=company_id, holding_id=holding_id)


async def _create_persons(f):
    for i in range(len(persons)):
        await f.create_person(
            person_id=i + 1,
            company_id=persons[i]['company_id'],
            first_name=persons[i]['first_name'],
        )


async def test_search_persons(f, uow):
    await _create_companies(f)
    await _create_persons(f)

    persons = await uow.persons.search_persons(
        text='иВаНов',
        limit=10,
        holding_id=2,
    )
    assert len(persons) == 2

    persons = await uow.persons.search_persons(
        text='Петров',
        limit=10,
        holding_id=None,
    )
    assert len(persons) == 2

    persons = await uow.persons.search_persons(
        text='Ху',
        limit=10,
        holding_id=3,
    )
    assert len(persons) == 0
