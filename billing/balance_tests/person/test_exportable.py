import pytest
from tests import object_builder as ob
from tests.balance_tests.person.person_common import create_firm, create_country, create_person, create_person_category, export_w_oebs_api


def test_use_config_when_create(session, country, export_w_oebs_api):
    create_firm(session, w_firm_export=True, country=country)
    person_category = create_person_category(session, country=country)
    person = create_person(session, type=person_category.category)
    person.id = None
    session.clear_cache()
    assert person.exportable == {'OEBS'}


@pytest.mark.parametrize('export_w_oebs_api', [
    {},
    {'Person': {'pct': 100}}])
def test_use_config_when_update(session, country, export_w_oebs_api):
    create_firm(session, w_firm_export=True, country=country)
    person_category = create_person_category(session, country=country)
    person = create_person(session, type=person_category.category)
    assert person.exportable == {'OEBS'}
    person = create_person(session, type=person_category.category, person_id=person.id, phone=ob.get_big_number())
    session.clear_cache()
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = export_w_oebs_api
    if export_w_oebs_api:
        assert person.exportable == {'OEBS_API'}
    else:
        assert person.exportable == {'OEBS'}


@pytest.mark.parametrize('w_forbidden_firms', [True, False])
def test_use_config_forbidden_firms(session, country, w_forbidden_firms):
    firm_1 = create_firm(session, w_firm_export=True, country=country)
    firm_2 = create_firm(session, w_firm_export=True, country=country)
    person_category = create_person_category(session, country=country)
    person = create_person(session, type=person_category.category)
    person.cache_firm(firm_2)
    person.cache_firm(firm_1)
    session.flush()
    assert person.exportable == {'OEBS'}
    firm_3 = create_firm(session, w_firm_export=True, country=country)
    session.clear_cache()
    forbidden_firm_ids = [firm_2.id if w_forbidden_firms else firm_3.id]
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': {'pct': 100,
                                                                               'forbidden_firm_ids': forbidden_firm_ids}}
    if w_forbidden_firms:
        assert person.exportable == {'OEBS'}
    else:
        assert person.exportable == {'OEBS_API'}


def test_use_config_when_create_not_exportable(session, country, export_w_oebs_api):
    create_firm(session, w_firm_export=False, country=country)
    person_category = create_person_category(session, country=country)
    person = create_person(session, type=person_category.category)
    assert person.exportable == set()
