from balance.processors.oebs.person import get_firms_to_export

from tests.balance_tests.oebs.conftest import create_firm


def test_base(session, person, use_cache_cfg):
    cached_firm = create_firm(session)
    person.firms = [cached_firm]
    firm = create_firm(session, country=person.person_category.country)
    if use_cache_cfg:
        assert get_firms_to_export(person)[0] == cached_firm
    else:
        assert get_firms_to_export(person) == [firm]
