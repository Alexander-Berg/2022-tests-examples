# -*- coding: utf-8 -*-

import pytest

from balance.paystep import user_can_issue_invoice, PaystepNS
from tests.balance_tests.paystep.paystep_common import (
    create_request,
    create_paysys,
    create_client,
    create_firm,
    create_country,
    create_person_category,
    create_order,
    create_role,
    create_currency,
    create_service,
    create_manager,
    create_passport,
    NOW,
    ISSUE_INVOICES,
    BANK
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


def test_wo_oper_id(session, firm, currency, client, service):
    """сессия без паспорта  -> можно выставлять счета с любым способом оплаты"""
    del session.oper_id
    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    assert user_can_issue_invoice(ns, paysys) is True


def test_request_is_owned_by_passport_client(session, firm, currency, service, client):
    """Реквест принадлежит клиенту из паспорта  -> пользователь может выставлять счета"""
    create_passport(session, [(create_role(session), None)], patch_session=True, client=client)
    assert session.oper_id
    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    assert user_can_issue_invoice(ns, paysys) is True


def test_passport_w_role_wo_perm(session, firm, currency, client, service):
    """сессия c паспортом без клиента, у паспорта нет роли с правом IssueInvoices -> пользователь не может выставлять
     счета"""
    passport = create_passport(session, [(create_role(session), None)], patch_session=True)
    assert session.oper_id
    assert passport.client_id is None

    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    assert user_can_issue_invoice(ns, paysys) is False


@pytest.mark.parametrize('firm_in_perm', [None, 'paysys_firm', 'another_firm'])
def test_depend_on_firm_in_perm(session, firm, currency, client, service, firm_in_perm):
    """У паспорта роль с правом IssueInvoices без ограничения по фирме,
    фирма в праве не указана или совпадает с фирмой способа оплаты -> пользователь может выставлять счета

    фирма не совпадает с фирмой способа оплаты -> пользователь не может выставлять счета с этим способом оплаты"""
    if firm_in_perm == 'paysys_firm':
        perm_firm_id = firm.id
    elif firm_in_perm == 'another_firm':
        perm_firm_id = create_firm(session).id
    else:
        perm_firm_id = None
    permissions = (ISSUE_INVOICES, {'firm_id': perm_firm_id and [perm_firm_id]})
    passport = create_passport(session, [(create_role(session, permissions), None)], patch_session=True)
    assert session.oper_id
    assert passport.client_id is None

    request = create_request(session, client=client, orders=[create_order(session, client=client,
                                                                          service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    if perm_firm_id and perm_firm_id != paysys.firm.id:
        assert user_can_issue_invoice(ns, paysys) is False
    else:
        assert user_can_issue_invoice(ns, paysys) is True


@pytest.mark.parametrize('firm_in_role', [None, 'paysys_firm', 'another_firm'])
def test_depend_on_firm_in_role(session, firm, currency, client, service, firm_in_role):
    """У паспорта роль с правом IssueInvoices без ограничения по фирме или с ограничением по фирме способа оплаты,
     фирма в праве не указана -> пользователь может выставлять счета"""
    if firm_in_role == 'paysys_firm':
        role_firm_id = firm.id
    elif firm_in_role == 'another_firm':
        role_firm_id = create_firm(session).id
    else:
        role_firm_id = None
    permissions = (ISSUE_INVOICES, {'firm_id': None})
    passport = create_passport(session, [(create_role(session, permissions), role_firm_id)],
                               patch_session=True)
    assert session.oper_id
    assert passport.client_id is None

    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    if role_firm_id and role_firm_id != paysys.firm.id:
        assert user_can_issue_invoice(ns, paysys) is False
    else:
        assert user_can_issue_invoice(ns, paysys) is True


@pytest.mark.parametrize('firm_in_req', [None, 'from_passport_firm', 'another_firm'])
def test_depend_on_firm_in_request(session, firm, currency, client, service, firm_in_req):
    """У паспорта роль с правом IssueInvoices без ограничения по фирме или с ограничением по фирме способа оплаты,
     фирма в праве не указана, фирма в реквесте не указана или совпадает с фирмой способа оплаты -> пользователь
     может выставлять счета"""
    if firm_in_req == 'from_passport_firm':
        req_firm_id = firm.id
    elif firm_in_req == 'another_firm':
        req_firm_id = create_firm(session).id
    else:
        req_firm_id = None
    permissions = (ISSUE_INVOICES, {'firm_id': None})
    create_passport(session, [(create_role(session, permissions), firm.id)], patch_session=True)

    request = create_request(session, client=client, firm_id=req_firm_id,
                             orders=[create_order(session,
                                                  client=client,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    if req_firm_id and req_firm_id != firm.id:
        assert user_can_issue_invoice(ns, paysys) is False
    else:
        assert user_can_issue_invoice(ns, paysys) is True


@pytest.mark.parametrize('firm_in_perm', [None, 'paysys_firm', 'another_firm'])
@pytest.mark.parametrize('w_manager_in_order', [True, False])
def test_w_manager_perm(session, firm, currency, client, service, w_manager_in_order, firm_in_perm):
    if firm_in_perm == 'paysys_firm':
        perm_firm_id = firm.id
    elif firm_in_perm == 'another_firm':
        perm_firm_id = create_firm(session).id
    else:
        perm_firm_id = None
    permissions = (ISSUE_INVOICES, {'firm_id': perm_firm_id and [perm_firm_id], 'manager': 1})
    passport = create_passport(session, [(create_role(session, permissions), None)], patch_session=True)
    manager = create_manager(session, passport)
    assert session.oper_id
    assert passport.client_id is None

    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client, manager=manager if w_manager_in_order else None,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    ns = PaystepNS(request=request)
    if w_manager_in_order and firm_in_perm != 'another_firm':
        assert user_can_issue_invoice(ns, paysys) is True
    else:
        assert user_can_issue_invoice(ns, paysys) is False


@pytest.mark.parametrize('w_manager_in_order', [True, False])
def test_w_manager_perm_wo_manager_perm(session, firm, currency, service, client, w_manager_in_order):
    firm1 = create_firm(session)
    permissions = (ISSUE_INVOICES, {'firm_id': [firm1.id], 'manager': 1})
    role1 = create_role(session, permissions)

    firm2 = create_firm(session)
    permissions = (ISSUE_INVOICES, {'firm_id': [firm2.id]})
    role2 = create_role(session, permissions)

    passport = create_passport(session, [(role1, None), (role2, None)], patch_session=True)
    manager = create_manager(session, passport)
    assert session.oper_id
    assert passport.client_id is None

    request = create_request(session, client=client,
                             orders=[create_order(session,
                                                  client=client, manager=manager if w_manager_in_order else None,
                                                  service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for firm in [firm1, firm2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
    ns = PaystepNS(request=request)
    assert user_can_issue_invoice(ns, paysys_list[0]) is w_manager_in_order
    assert user_can_issue_invoice(ns, paysys_list[1]) is True
