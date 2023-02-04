import pytest


pytestmark = pytest.mark.asyncio


async def _create_companies(f):
    companies = [
        {'company_id': 1, 'name': 'Яндекс', 'holding_id': 1},
        {'company_id': 2, 'name': 'Яндекс Технологии', 'holding_id': 1},
        {'company_id': 3, 'name': 'AeroClub', 'holding_id': 2},
    ]
    for company_info in companies:
        await f.create_holding(holding_id=company_info['holding_id'])
        await f.create_company(**company_info)

    domains = [
        {'company_id': 1, 'domain': 'yandex-team.ru'},
        {'company_id': 1, 'domain': 'yandex.ru'},
        {'company_id': 2, 'domain': 'yandex-team.ru'},
        {'company_id': 2, 'domain': 'yandex.ru'},
        {'company_id': 3, 'domain': 'time.aero'},
    ]
    for domain_info in domains:
        await f.add_company_domain(**domain_info)


async def test_get_company_by_domain(uow, f):
    await _create_companies(f)

    companies = await uow.companies.get_companies_by_domain('yandex-team.ru')
    assert len(companies) == 2
    companies = await uow.companies.get_companies_by_domain('yandex.ru')
    assert len(companies) == 2

    companies = await uow.companies.get_companies_by_domain('time.aero')
    assert len(companies) == 1
    assert companies[0].company_id == 3
    assert companies[0].name == 'AeroClub'
