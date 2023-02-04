# -*- coding: utf-8 -*-

import mock
import pytest

from balance.paystep.namespaces import PaystepNS
from balance.paystep import get_creatable_person_categories

from tests.balance_tests.paystep.paystep_common import (
    create_paysys,
    create_request,
    create_client,
    create_country,
    create_firm,
    create_person_category,
    create_currency,
    create_role,
    create_passport,
    ADMIN_ACCESS,
    USE_ADMIN_PERSONS

)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


def test_get_creatable_person_empty_response(session, client):
    request = create_request(session, client=client)
    ns = (PaystepNS(request=request, only_existing_persons=False))
    assert get_creatable_person_categories(ns, paysys_list=[]) == set()


@pytest.mark.parametrize('only_existing_persons', [True, False])
def test_get_creatable_person_categories_basic(session, currency, client, firm, only_existing_persons):
    """даем список способов оплаты, получаем категорию плательщика из них"""
    request = create_request(session, client=client)
    ns = (PaystepNS(request=request, only_existing_persons=only_existing_persons))
    person_category = create_person_category(session, country=firm.country)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)
    if only_existing_persons:
        assert get_creatable_person_categories(ns, paysys_list=[paysys]) == set()
    else:
        assert get_creatable_person_categories(ns, paysys_list=[paysys]) == {person_category}


@pytest.mark.parametrize('perms', [[], [ADMIN_ACCESS]])
@pytest.mark.parametrize('is_agency', [True, False])
@pytest.mark.parametrize('for_agency_values', [[0, 1], [0, 0]])
def test_get_creatable_person_categories_for_agency(session, currency, client, firm, is_agency, perms,
                                                    for_agency_values):
    """если агенство без админских прав, и для категории плательщика есть хотя бы один способ оплаты с for_agency=1,
    возвращаем такую категорию"""
    role = create_role(session, *perms)
    create_passport(session, [role], patch_session=True)
    request_ = create_request(session, client=create_client(session, is_agency=is_agency))
    ns = (PaystepNS(request=request_, only_existing_persons=False))
    person_category = create_person_category(session, country=firm.country)
    paysys_list = []
    for for_agency in for_agency_values:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         for_agency=for_agency))
    if is_agency and ADMIN_ACCESS not in perms and not any(for_agency_values):
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == set()
        assert ns.categories_denied == {person_category: 'category not for agency'}
    else:
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == {person_category}
        assert ns.categories_denied == {}


@pytest.mark.parametrize('mobile', [True, False])
def test_get_creatable_person_categories_mobile(session, client, currency, firm, mobile):
    request_ = create_request(session, client=client)
    ns = (PaystepNS(request=request_, only_existing_persons=False, mobile=mobile))

    paysys_list = []
    person_categories_list = []
    for auto_only in [0, 1]:
        person_category = create_person_category(session, auto_only=auto_only, country=firm.country)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category))
        person_categories_list.append(person_category)

    if mobile:
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == set(person_categories_list)
    else:
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == {person_categories_list[0]}
        assert ns.categories_denied == {person_categories_list[1]: 'only for auto created persons (mobile etc)'}


def test_get_creatable_person_categories_subclient_non_resident(session, currency, firm):
    request_ = create_request(session, client=create_client(session, fullname='client_fullname',
                                                            non_resident_currency_payment='RUR'))
    ns = (PaystepNS(request=request_, only_existing_persons=False))

    person_category = create_person_category(session, country=firm.country)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category)

    assert get_creatable_person_categories(ns, paysys_list=[paysys]) == set()
    assert ns.categories_denied == {person_category: 'denied for non client_resident'}


@pytest.mark.permissions
@pytest.mark.parametrize(
    'perms', [[], [USE_ADMIN_PERSONS]]
)
def test_get_creatable_person_categories_admin_only(session, client, currency, firm, perms):
    role = create_role(session, *perms)
    create_passport(session, [role], patch_session=True)
    request_ = create_request(session, client=client)
    ns = PaystepNS(request=request_, only_existing_persons=False)

    paysys_list = []
    person_categories_list = []
    for admin_only in [0, 1]:
        person_category = create_person_category(session, admin_only=admin_only, country=firm.country)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category))
        person_categories_list.append(person_category)

    if USE_ADMIN_PERSONS not in perms:
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == {person_categories_list[0]}
        assert ns.categories_denied == {person_categories_list[1]: 'only for admin'}
    else:
        assert get_creatable_person_categories(ns, paysys_list=paysys_list) == set(person_categories_list)
        assert ns.categories_denied == {}


@pytest.mark.single_account
@mock.patch('balance.paystep.namespaces.single_account.availability.get_denied_person_categories')
def test_denied_by_single_account(get_denied_person_categories_mock, session, currency, firm):
    request = create_request(session, client=create_client(session, with_single_account=True))
    person_categories = [create_person_category(session, country=firm.country) for _ in range(2)]

    paysys_list = []
    for person_category in person_categories:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category))

    get_denied_person_categories_mock.return_value = {person_categories[0]}
    ns = (PaystepNS(request=request, only_existing_persons=False))
    assert get_creatable_person_categories(ns, paysys_list=paysys_list) == {person_categories[1]}
    assert ns.categories_denied == {
        person_categories[0]: 'denied by single personal account anti-duplicate persons rules'}


@pytest.mark.single_account
@mock.patch('balance.paystep.namespaces.single_account.availability.get_denied_person_categories')
def test_without_single_account(get_denied_person_categories_mock, client, session, currency, firm):
    request = create_request(session, client=client)
    person_categories = [create_person_category(session, country=firm.country) for _ in range(2)]

    paysys_list = []
    for person_category in person_categories:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category))

    get_denied_person_categories_mock.return_value = {person_categories[0]}
    ns = (PaystepNS(request=request, only_existing_persons=False))
    assert get_creatable_person_categories(ns, paysys_list=paysys_list) == set(person_categories)
    assert ns.categories_denied == {}
    get_denied_person_categories_mock.assert_not_called()
