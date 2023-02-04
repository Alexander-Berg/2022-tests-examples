# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
import hamcrest
from mock import patch

from balance.actions.invoice_create import InvoiceFactory
from balance.mapper import Person, Permission, Firm, Invoice
from balance.paystep import PaystepNS, filter_pcps, PCP
from billing.contract_iface import ContractTypeId
from balance.constants import (
    PaymentMethodIDs,
    PaysysGroupIDs,
    ConstraintTypes,
    PREPAY_PAYMENT_TYPE,
    FirmId
)

from balance.exc import PAYSYS_LIMIT_EXCEEDED

from tests.balance_tests.paystep.paystep_common import (
    create_request,
    create_client,
    create_firm,
    create_person_category,
    create_paysys,
    create_paysys_simple,
    create_person,
    create_currency,
    create_role,
    create_contract,
    create_manager,
    create_order,
    create_product,
    create_passport,
    create_service,
    create_pay_policy,
    create_country,
    create_price_tax_rate,
    create_single_personal_account_for_person,
    create_invoice,
    ALTER_INVOICE_PERSON,
    ALTER_INVOICE_CONTRACT,
    ALTER_INVOICE_PAYSYS,
    USE_ADMIN_PERSONS,
    ddict2dict,
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

BANK = PaymentMethodIDs.bank
NOW = datetime.datetime.now()


def compare_pcps(actual_pcps, expected_pcps):
    assert len(actual_pcps) == len(expected_pcps)
    for actual_pcp, expected_pcp in zip(actual_pcps, expected_pcps):
        assert actual_pcp.paysyses == expected_pcp.paysyses
        assert actual_pcp.person == expected_pcp.person
        assert actual_pcp.contract == expected_pcp.contract


def test_empty_response(session, client):
    request = create_request(session, client=client)
    ns = (PaystepNS(request=request))
    assert filter_pcps(ns, []) is None


def test_default_paysys_wo_limit(session, firm, client, currency):
    """обычный способ оплаты без лимита"""
    request = create_request(session, client=client, quantity=100)

    ns = (PaystepNS(request=request, show_disabled_paysyses=True))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                           currency=currency.char_code, category=person.person_category.category)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    filter_pcps(ns, pcp_list)
    compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])


@patch('balance.actions.invoice_create.InvoiceFactory.create', side_effect=InvoiceFactory.create)
def test_default_paysys_under_limit(invoice_create_mock, firm, client, session, currency):
    """обычный способ оплаты с лимитом, сумма временного счета не превышает лимит"""
    request = create_request(session, client=client, quantity=D('99'))
    ns = (PaystepNS(request=request, show_disabled_paysyses=False, raise_exception=False))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=99,
                           group_id=PaysysGroupIDs.default, currency=currency.char_code,
                           category=person.person_category.category)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=0)
    filter_pcps(ns, pcp_list)
    compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
    assert invoice_create_mock.call_count == 1
    assert not any(isinstance(obj, Invoice) for obj in session)


@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_show_disabled_paysyses_check(session, firm, client, currency, show_disabled_paysyses):
    """обычный способ оплаты с лимитом, сумма временного счета превышает лимит.
    с флагом show_disabled_paysyses не удаляем способ оплаты, но записываем причину отказа
    без флага - удаляем"""
    request = create_request(session, client=client, quantity=D('99.01'))
    ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=99,
                           group_id=PaysysGroupIDs.default, currency=currency.char_code,
                           category=person.person_category.category)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=None)
    filter_pcps(ns, pcp_list)
    if show_disabled_paysyses:
        compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
        assert pcp_list[0].paysyses[0].disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
    else:
        compare_pcps(pcp_list, [PCP(person, None, paysyses=[])])
        assert ddict2dict(ns.paysyses_denied) == {
            (person, None): {paysys: 'paysys limit check - balance paysys'}}


def test_w_raise_exception_flag(session, firm, client, currency):
    """обычный способ оплаты с лимитом, сумма временного счета превышает лимит.
    С флагом raise_exception бросается исключение"""
    request = create_request(session, client=client, quantity=D('99.01'))
    ns = (PaystepNS(request=request, show_disabled_paysyses=True, raise_exception=True))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=99,
                           currency=currency.char_code, category=person.person_category.category,
                           group_id=PaysysGroupIDs.default)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=None)
    with pytest.raises(PAYSYS_LIMIT_EXCEEDED) as exc_info:
        filter_pcps(ns, pcp_list)
    assert exc_info.value.msg == 'Payment amount 99.01 {currency} is bigger than the limit 99.00 {currency}'.format(
        currency=paysys.iso_currency)


@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_auto_trust_limit_exceeded(session, firm, client, currency, show_disabled_paysyses):
    """трастовый способ оплаты с методами оплаты из траста, сумма временного счета превышает лимит.
    с флагом show_disabled_paysyses не удаляем способ оплаты, но записываем причину отказа
    без флага - удаляем"""
    request = create_request(session, client=client, quantity=100)
    ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
    person_category = create_person_category(session, country=firm.country)
    person = create_person(session, client=request.client, type=person_category.category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                           currency=currency.char_code, category=person_category.category,
                           group_id=PaysysGroupIDs.auto_trust)
    paysys.trust_paymethods = [{'max_amount': 99}]
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods',
                                             return_value=paysys.trust_paymethods)
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=None)
    with patch_get_paysys_payment_methods:
        filter_pcps(ns, pcp_list)
    assert paysys.trust_paymethods == [{'max_amount': 99}]
    if show_disabled_paysyses:
        compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
        assert pcp_list[0].paysyses[0].disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
    else:
        compare_pcps(pcp_list, [PCP(person, None, paysyses=[])])
        assert ddict2dict(ns.paysyses_denied) == {
            (person, None): {paysys: 'paysys limit check - trust api paysys'}}


def test_auto_trust_filter_paymethod(session, firm, client, currency):
    """трастовый способ оплаты с методами оплаты, среди которых есть те, что меньше суммы временного счета, и те, что
    больше. В таком случае не отфильтровываем способ оплаты, но отфильтровываем неподходящие трастовые методы оплаты"""
    request = create_request(session, client=client, quantity=100)
    ns = (PaystepNS(request=request, show_disabled_paysyses=False, raise_exception=False))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                           currency=currency.char_code, category=person.person_category.category,
                           group_id=PaysysGroupIDs.auto_trust)
    paysys.trust_paymethods = [{'max_amount': 99},
                               {'max_amount': 100}]
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=None)
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods',
                                             return_value=paysys.trust_paymethods)
    with patch_get_paysys_payment_methods:
        filter_pcps(ns, pcp_list)
    compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
    assert pcp_list[0].paysyses[0].disabled_reasons == set([])
    assert paysys.trust_paymethods == [{'max_amount': 100}]
    assert ddict2dict(ns.paysyses_denied) == {(person, None): {}}


def test_auto_trust_no_max_amount(session, firm, client, currency):
    """
    трастовый способ оплаты у которого в ответе траста вообще нет max_amount'а
    в этом случае мы для него лимит не проверяем.
    """
    request = create_request(session, client=client, quantity=100)
    ns = (PaystepNS(request=request, show_disabled_paysyses=False, raise_exception=False))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                           currency=currency.char_code, category=person.person_category.category,
                           group_id=PaysysGroupIDs.auto_trust)
    paysys.trust_paymethods = [{}, {'max_amount': 10}]
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    create_pay_policy(session, firm_id=firm.id, legal_entity=None, paymethods_params=[(paysys.currency, BANK)],
                   region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                   is_agency=None)
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods',
                                             return_value=paysys.trust_paymethods)
    with patch_get_paysys_payment_methods:
        filter_pcps(ns, pcp_list)
    compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
    assert pcp_list[0].paysyses[0].disabled_reasons == set([])
    assert paysys.trust_paymethods == [{}]
    assert ddict2dict(ns.paysyses_denied) == {(person, None): {}}


def test_auto_trust_no_trust_paymethods(session, firm, client, currency):
    """
    трастовый пейсис но без закешированных методов оплаты вообще - случай анонимных платежей.
    В таком случае лимиты тоже не проверяем и отдаём его как есть.
    """
    request = create_request(session, client=client, quantity=100)

    ns = (PaystepNS(request=request, show_disabled_paysyses=False, raise_exception=False))
    person = create_person(session, client=request.client,
                           type=create_person_category(session, country=firm.country).category)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                           currency=currency.char_code, category=person.person_category.category,
                           group_id=PaysysGroupIDs.auto_trust)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    pcp_list = [PCP(person, None, paysyses=[paysys])]
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods', return_value=[])
    with patch_get_paysys_payment_methods:
        filter_pcps(ns, pcp_list)
    compare_pcps(pcp_list, [PCP(person, None, paysyses=[paysys])])
    assert pcp_list[0].paysyses[0].disabled_reasons == set([])
    assert paysys.trust_paymethods == []
    assert ddict2dict(ns.paysyses_denied) == {(person, None): {}}


class TestCanAlterInvoicePcp:
    @pytest.mark.parametrize('with_manager', [True, False])
    def test_another_person_w_manager(self, session, currency, service, client, firm, with_manager):
        """пользователь без всяких хитрых прав и не менеджер не может менять плательщика в счетах.
        менеджер может при наличии права AlterInvoicePerson с разбиением по менеджерам"""
        role = create_role(session, (Permission(code='AlterInvoicePerson'), {"manager": 1}))
        passport = create_passport(session, [(role, None)], patch_session=True)
        if with_manager:
            manager = create_manager(session, passport=passport)

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service,
                                                      manager=manager if with_manager else None)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice = create_invoice(session, request=request, paysys=paysys, person=person)
        another_person = create_person(session, client=request.client, type=person_category.category)
        pcp_list = [PCP(another_person, None, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if with_manager:
            compare_pcps(pcp_list, [PCP(another_person, None, paysyses=[paysys])])
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(another_person, None): {}}
        else:
            compare_pcps(pcp_list, [PCP(another_person, None, paysyses=[])])
            assert ddict2dict(ns.paysyses_denied) == {
                (another_person, None): {paysys: 'Altering invoice\'s PCP is forbidden'}}

    @pytest.mark.parametrize('with_manager', [True, False])
    def test_another_contract_w_manager(self, session, firm, client, currency, service, with_manager):
        role = create_role(session, (Permission(code='AlterInvoiceContract'), {"manager": 1}))
        passport = create_passport(session, [(role, None)], patch_session=True)
        if with_manager:
            manager = create_manager(session, passport=passport)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service,
                                                      manager=manager if with_manager else None)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice_contract, another_contract = [create_contract(session, commission=ContractTypeId.NON_AGENCY,
                                                              firm=firm.id, client=request.client,
                                                              payment_type=PREPAY_PAYMENT_TYPE,
                                                              services={request.request_orders[0].order.service.id},
                                                              is_signed=NOW, person=person,
                                                              currency=currency.num_code) for _ in range(2)]
        invoice = create_invoice(session, request=request, paysys=paysys, person=person, contract=invoice_contract)
        pcp_list = [PCP(person, invoice_contract, paysyses=[paysys]),
                    PCP(person, None, paysyses=[paysys]),
                    PCP(person, another_contract, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if with_manager:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[paysys]),
                                    PCP(person, another_contract, paysyses=[paysys])])
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {},
                                                      (person, another_contract): {}}
        else:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[]),
                                    PCP(person, another_contract, paysyses=[])])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {paysys: 'Altering invoice\'s PCP is forbidden'},
                                                      (person, another_contract): {
                                                          paysys: 'Altering invoice\'s PCP is forbidden'}}

    @pytest.mark.parametrize('with_invoice', [True, False])
    def test_another_person_w_invoice(self, session, client, service, currency, firm, with_invoice):
        role = create_role(session)
        create_passport(session, [(role, None)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice = create_invoice(session, request=request, paysys=paysys, person=person)
        another_person = create_person(session, client=request.client, type=person_category.category)
        pcp_list = [PCP(another_person, None, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice if with_invoice else None, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if with_invoice:
            assert pcp_list[0].paysyses == []
            assert ddict2dict(ns.paysyses_denied) == {
                (another_person, None): {paysys: 'Altering invoice\'s PCP is forbidden'}}
        else:
            assert pcp_list[0].paysyses == [paysys]
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(another_person, None): {}}

    @pytest.mark.parametrize('with_invoice', [True, False])
    def test_another_contract_w_invoice(self, session, client, service, currency, firm, with_invoice):
        role = create_role(session, ())
        create_passport(session, [(role, None)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice_contract, another_contract = [create_contract(session, commission=ContractTypeId.NON_AGENCY,
                                                              firm=firm.id, client=request.client,
                                                              payment_type=PREPAY_PAYMENT_TYPE,
                                                              services={request.request_orders[0].order.service.id},
                                                              is_signed=NOW, person=person,
                                                              currency=currency.num_code) for _ in range(2)]
        invoice = create_invoice(session, request=request, paysys=paysys, person=person, contract=invoice_contract)
        pcp_list = [PCP(person, invoice_contract, paysyses=[paysys]),
                    PCP(person, None, paysyses=[paysys]),
                    PCP(person, another_contract, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice if with_invoice else None, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if with_invoice:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[]),
                                    PCP(person, another_contract, paysyses=[])])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {paysys: 'Altering invoice\'s PCP is forbidden'},
                                                      (person, another_contract): {
                                                          paysys: 'Altering invoice\'s PCP is forbidden'}}
        else:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[paysys]),
                                    PCP(person, another_contract, paysyses=[paysys])])
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {},
                                                      (person, another_contract): {}}

    @pytest.mark.parametrize('perm', [ALTER_INVOICE_PERSON, None])
    def test_another_person_w_perms(self, session, currency, client, service, firm, perm):
        role = create_role(session, perm)
        create_passport(session, [(role, None)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],

                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice = create_invoice(session, request=request, paysys=paysys, person=person)
        another_person = create_person(session, client=request.client, type=person_category.category)
        pcp_list = [PCP(another_person, None, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if perm == ALTER_INVOICE_PERSON:
            assert pcp_list[0].paysyses == [paysys]
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(another_person, None): {}}
        else:
            assert pcp_list[0].paysyses == []
            assert ddict2dict(ns.paysyses_denied) == {
                (another_person, None): {paysys: 'Altering invoice\'s PCP is forbidden'}}

    @pytest.mark.parametrize('perm', [ALTER_INVOICE_CONTRACT, None])
    def test_another_contract_w_perms(self, session, client, service, currency, firm, perm):
        role = create_role(session, perm)
        create_passport(session, [(role, None)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category=person_category.category)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service)])
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = create_person(session, client=request.client, type=person_category.category)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice_contract, another_contract = [create_contract(session, commission=ContractTypeId.NON_AGENCY,
                                                              firm=firm.id, client=request.client,
                                                              payment_type=PREPAY_PAYMENT_TYPE,
                                                              services={request.request_orders[0].order.service.id},
                                                              is_signed=NOW, person=person,
                                                              currency=currency.num_code) for _ in range(2)]
        invoice = create_invoice(session, request=request, paysys=paysys, person=person, contract=invoice_contract)
        pcp_list = [PCP(person, invoice_contract, paysyses=[paysys]),
                    PCP(person, None, paysyses=[paysys]),
                    PCP(person, another_contract, paysyses=[paysys])]
        ns = (PaystepNS(request=request, invoice=invoice, show_disabled_paysyses=False))
        filter_pcps(ns, pcp_list)
        if perm == ALTER_INVOICE_CONTRACT:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[paysys]),
                                    PCP(person, another_contract, paysyses=[paysys])])
            assert pcp_list[0].paysyses[0].disabled_reasons == set([])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {},
                                                      (person, another_contract): {}}

        else:
            compare_pcps(pcp_list, [PCP(person, invoice_contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[]),
                                    PCP(person, another_contract, paysyses=[])])
            assert ddict2dict(ns.paysyses_denied) == {(person, invoice_contract): {},
                                                      (person, None): {paysys: 'Altering invoice\'s PCP is forbidden'},
                                                      (person, another_contract): {
                                                          paysys: 'Altering invoice\'s PCP is forbidden'}}

    @pytest.mark.permissions
    def test_alter_paysys_perm_firm(self, session, client, currency):
        """даже с правом AlterInvoicePaysys нельзя сменить способ оплаты на другой с фирмой, не совпадающей
         с фирмой счета"""
        country = create_country(session)
        person_category = create_person_category(session, country, ur=0)
        firm1 = create_firm(session, country=country)
        firm2 = create_firm(session, country=country)
        paysys_list = []
        for firm in [firm1, firm1, firm2]:
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                             group_id=PaysysGroupIDs.default, currency=currency.char_code,
                                             category=person_category.category, instant=1))
        invoice_paysys, same_firm_paysys, other_firm_paysys = paysys_list
        person = create_person(session, type=person_category.category)
        request = create_request(session, client, firm_id=firm1.id)
        order = request.request_orders[0].order
        create_price_tax_rate(session, order.product, country, currency)
        invoice = create_invoice(session, request=request, paysys=invoice_paysys, person=person)

        role = create_role(session, (ALTER_INVOICE_PAYSYS, {ConstraintTypes.firm_id: None}))
        create_passport(session, [(role, firm1.id)], patch_session=True)

        ns = PaystepNS(request=request, invoice=invoice)
        pcp = PCP(person, None, paysyses=[invoice_paysys, same_firm_paysys, other_firm_paysys])
        filter_pcps(ns, [pcp])

        assert pcp.paysyses == [invoice_paysys, same_firm_paysys]
        assert ddict2dict(ns.paysyses_denied) == {
            (person, None): {other_firm_paysys: "Altering invoice's PCP is forbidden"}}

    @pytest.mark.permissions
    def test_alter_person_owner(self, session, currency, client, firm):
        person_category = create_person_category(session, firm.country, ur=0)
        paysys1 = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                group_id=PaysysGroupIDs.default, currency=currency.char_code,
                                category=person_category.category, instant=1)
        paysys2 = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                group_id=PaysysGroupIDs.default, currency=currency.char_code,
                                category=person_category.category, instant=1)

        person1 = create_person(session, type=person_category.category)
        person2 = create_person(session, type=person_category.category)
        request = create_request(session, client, firm_id=firm.id)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        invoice = create_invoice(session, request=request, paysys=paysys1, person=person1)
        passport = create_passport(session, [create_role(session)], client=client, patch_session=True)
        session.flush()

        ns = PaystepNS(request=request, invoice=invoice)
        pcp1 = PCP(person1, None, paysyses=[paysys1, paysys2])
        pcp2 = PCP(person2, None, paysyses=[paysys1, paysys2])
        filter_pcps(ns, [pcp1, pcp2])

        assert pcp1.paysyses == [paysys1, paysys2]
        assert pcp2.paysyses == []
        assert ddict2dict(ns.paysyses_denied) == {
            (person1, None): {},
            (person2, None): {
                paysys1: "Altering invoice's PCP is forbidden",
                paysys2: "Altering invoice's PCP is forbidden",
            }
        }


class TestNonVerifiedYtPerson:

    @pytest.mark.parametrize('can_issue_initial_invoice', [True, False])
    @pytest.mark.parametrize('perm', [USE_ADMIN_PERSONS, None])
    @pytest.mark.parametrize('with_paid_list', [True, False])
    def test_base(self, session, currency, perm, country, service, can_issue_initial_invoice, with_paid_list):
        role = create_role(session, perm)
        create_passport(session, [role], patch_session=True)
        firm = session.query(Firm).getone(FirmId.YANDEX_OOO)
        request = create_request(session,
                                 orders=[create_order(session,
                                                      client=create_client(session,
                                                                           can_issue_initial_invoice=can_issue_initial_invoice),
                                                      service=service)])

        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, group_id=PaysysGroupIDs.default,
                               currency=currency.char_code, category='yt')
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        person = Person(request.client, 'yt', skip_category_check=True)
        session.add(person)
        temp_person = Person(request.client, 'yt', skip_category_check=True)
        session.expunge(temp_person)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY,
                                   firm=firm.id, client=request.client,
                                   payment_type=PREPAY_PAYMENT_TYPE,
                                   services={request.request_orders[0].order.service.id},
                                   is_signed=NOW, person=person,
                                   currency=currency.num_code)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, resident=0)
        if with_paid_list:
            invoice = create_invoice(session, request=request, paysys=paysys, person=person, client=request.client)
            invoice.receipt_sum_1c = 1
            session.flush()
        ns = (PaystepNS(request=request, show_disabled_paysyses=False))
        pcp_list = [PCP(person, contract, paysyses=[paysys]),
                    PCP(person, None, paysyses=[paysys]),
                    PCP(temp_person, None, paysyses=[paysys])]
        if with_paid_list or perm == USE_ADMIN_PERSONS or can_issue_initial_invoice:

            filter_pcps(ns, pcp_list)
            compare_pcps(pcp_list, [PCP(person, contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[paysys]),
                                    PCP(temp_person, None, paysyses=[paysys])
                                    ])
            assert ddict2dict(ns.paysyses_denied) == {(person, contract): {},
                                                      (person, None): {},
                                                      (temp_person, None): {}}
        else:
            filter_pcps(ns, pcp_list)
            compare_pcps(pcp_list, [PCP(person, contract, paysyses=[paysys]),
                                    PCP(person, None, paysyses=[]),
                                    PCP(temp_person, None, paysyses=[paysys])
                                    ])
            assert ddict2dict(ns.paysyses_denied) == {(person, contract): {},
                                                      (person, None): {paysys: 'empty non verified yt person'},
                                                      (temp_person, None): {}}


class TestSingleAccountChecks:
    @pytest.mark.parametrize('show_disabled_paysyses', [True, False])
    def test_single_account_temporary_person(self, session, currency, firm, show_disabled_paysyses):
        """Если способ оплаты - елс, не выдаем этот способ в сочетании с временными плательщиками"""
        request = create_request(session, client=create_client(session, with_single_account=True), quantity=100)
        ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
        person = create_person(session, client=request.client,
                               type=create_person_category(session, country=firm.country).category)

        temp_person = Person(request.client, person.person_category.category, skip_category_check=True)
        session.expunge(temp_person)
        paysyses = []
        for payment_method_id in [PaymentMethodIDs.single_account, PaymentMethodIDs.credit_card]:
            paysyses.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                                          currency=currency.char_code, category=person.person_category.category,
                                          group_id=PaysysGroupIDs.default, nds_pct=1,
                                          payment_method_id=payment_method_id))

        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        personal_account = create_single_personal_account_for_person(session, person,
                                                                     request.client.single_account_number,
                                                                     paysys=paysyses[0])
        personal_account.create_receipt(100)
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        pcp_list = [PCP(temp_person, None, paysyses=paysyses[:]),
                    PCP(person, None, paysyses=paysyses[:])]
        filter_pcps(ns, pcp_list)
        compare_pcps(pcp_list, [PCP(temp_person, None, paysyses=[paysyses[1]]),
                                PCP(person, None, paysyses=paysyses)])
        assert ddict2dict(ns.paysyses_denied) == {
            (temp_person, None): {paysyses[0]: 'single account not available'},
            (person, None): {}}

    @pytest.mark.parametrize('show_disabled_paysyses', [True, False])
    def test_single_account_person_wo_els(self, session, currency, firm, show_disabled_paysyses):
        """Если у плательщика нет ЕЛС, не выводим способ оплаты ЕЛС"""
        request = create_request(session, client=create_client(session, with_single_account=True), quantity=100)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
        person_category = create_person_category(session, country=firm.country)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                               currency=currency.char_code, category=person_category.category,
                               group_id=PaysysGroupIDs.default, nds_pct=1,
                               payment_method_id=PaymentMethodIDs.single_account)
        persons = [create_person(session, client=request.client, type=person_category.category) for _ in range(2)]
        personal_account = create_single_personal_account_for_person(session, persons[0],
                                                                     request.client.single_account_number,
                                                                     paysys=paysys)
        personal_account.create_receipt(100)

        pcp_list = [PCP(persons[0], None, paysyses=[paysys]),
                    PCP(persons[1], None, paysyses=[paysys])]
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        filter_pcps(ns, pcp_list)
        compare_pcps(pcp_list, [PCP(persons[0], None, paysyses=[paysys]),
                                PCP(persons[1], None, paysyses=[])])
        assert ddict2dict(ns.paysyses_denied) == {
            (persons[0], None): {},
            (persons[1], None): {paysys: 'single account not available'}}

    @pytest.mark.parametrize('show_disabled_paysyses', [True, False])
    def test_single_account_wo_free_funds(self, session, currency, firm, show_disabled_paysyses):
        """Если на ЕЛС нет свободных средств, не выдаем этот способ оплаты"""
        request = create_request(session, client=create_client(session, with_single_account=True), quantity=100)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
        person_category = create_person_category(session, country=firm.country)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                               currency=currency.char_code, category=person_category.category,
                               group_id=PaysysGroupIDs.default, nds_pct=1,
                               payment_method_id=PaymentMethodIDs.single_account)
        persons = [create_person(session, client=request.client, type=person_category.category) for _ in range(2)]
        invoices = [create_single_personal_account_for_person(session, person, request.client.single_account_number,
                                                              paysys=paysys) for person in persons]
        invoices[0].create_receipt(100)
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        pcp_list = [PCP(persons[0], None, paysyses=[paysys]),
                    PCP(persons[1], None, paysyses=[paysys])]
        filter_pcps(ns, pcp_list)
        compare_pcps(pcp_list, [PCP(persons[0], None, paysyses=[paysys]),
                                PCP(persons[1], None, paysyses=[])])
        assert ddict2dict(ns.paysyses_denied) == {
            (persons[0], None): {},
            (persons[1], None): {paysys: 'single account not available'}}

    @pytest.mark.parametrize('show_disabled_paysyses', [True, False])
    def test_single_account_paysys_client_without_els(self, session, currency, client, firm, show_disabled_paysyses):
        """Не выводим способ оплаты елс, если у клиента нет елс"""
        request = create_request(session, client=client, quantity=100)
        ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
        person = create_person(session, client=request.client,
                               type=create_person_category(session, country=firm.country).category)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                               currency=currency.char_code, category=person.person_category.category, nds_pct=1,
                               group_id=PaysysGroupIDs.default, payment_method_id=PaymentMethodIDs.single_account)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        pcp_list = [PCP(person, None, paysyses=[paysys])]
        filter_pcps(ns, pcp_list)
        compare_pcps(pcp_list, [PCP(person, None, paysyses=[])])
        assert ddict2dict(ns.paysyses_denied) == {
            (person, None): {paysys: 'single account not available'}}

    @pytest.mark.parametrize('show_disabled_paysyses', [True, False])
    def test_single_account_free_funds_under_sum(self, session, currency, firm, show_disabled_paysyses):
        """Если сумма свободных средств на ЛС меньше, чем сумма временного счета, отфильтровываем способ оплаты ЕЛС
        с флагом show_disabled_paysyses не удаляем способ оплаты, но записываем причину отказа в single_account_data_dict
        без флага - удаляем"""
        request = create_request(session, client=create_client(session, with_single_account=True), quantity=100)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        ns = (PaystepNS(request=request, show_disabled_paysyses=show_disabled_paysyses, raise_exception=False))
        person_category = create_person_category(session, country=firm.country)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=1554,
                               currency=currency.char_code, category=person_category.category,
                               group_id=PaysysGroupIDs.default, nds_pct=1,
                               payment_method_id=PaymentMethodIDs.single_account)
        persons = [create_person(session, client=request.client, type=person_category.category) for _ in range(2)]
        invoices = [create_single_personal_account_for_person(session, person, request.client.single_account_number,
                                                              paysys=paysys) for person in persons]

        invoices[0].create_receipt(100)
        invoices[1].create_receipt(99)
        create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                       is_agency=0)
        pcp_list = [PCP(persons[0], None, paysyses=[paysys]),
                    PCP(persons[1], None, paysyses=[paysys])]
        filter_pcps(ns, pcp_list)
        if show_disabled_paysyses:
            compare_pcps(pcp_list, [PCP(persons[0], None, paysyses=[paysys]),
                                    PCP(persons[1], None, paysyses=[paysys])])
            assert ns.single_account_data_dict == {
                (persons[0].id, paysys.firm_id, paysys.iso_currency): {'firm_id': firm.id,
                                                                       'paysys_id': paysys.id,
                                                                       'person_id': persons[0].id,
                                                                       'iso_currency': currency.iso_code,
                                                                       'unused_funds': D('100')},
                (persons[1].id, paysys.firm_id, paysys.iso_currency): {
                    'disabled_reason': 'ID_Single_account_sum_less_than_invoice_sum',
                    'firm_id': firm.id,
                    'paysys_id': paysys.id,
                    'person_id': persons[1].id,
                    'iso_currency': currency.iso_code,
                    'unused_funds': D('99')}}
            assert ddict2dict(ns.paysyses_denied) == {(persons[0], None): {},
                                                      (persons[1], None): {}}
        else:
            compare_pcps(pcp_list, [PCP(persons[0], None, paysyses=[paysys]),
                                    PCP(persons[1], None, paysyses=[])])
            assert ddict2dict(ns.paysyses_denied) == {
                (persons[0], None): {},
                (persons[1], None): {
                    paysys: 'paysys limit check - not enough funds on single account'}}


@pytest.mark.charge_note_register
class TestChargeNoteRegister(object):
    def test_person_filter(self, session, client, service, firm, currency):
        person_category = create_person_category(session, firm.country)

        paysys = create_paysys_simple(
            session,
            category=person_category.category,
            firm_id=firm.id,
            iso_currency=currency.iso_code
        )
        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[(paysys.currency, paysys.payment_method_id)]
        )

        product = create_product(session)
        create_price_tax_rate(session, product, firm.country, currency)
        order = create_order(session, client, product, service)

        persons = [
            create_person(session, type=person_category.category, client=client)
            for _ in range(2)
        ]

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=persons[0],
            request=create_request(session, client=client, orders=[order]),
        )

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)
        pcp_list = [
            PCP(person, None, [paysys])
            for person in persons
        ]
        filter_pcps(ns, pcp_list)

        hamcrest.assert_that(
            pcp_list,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    person=persons[0],
                    contract=None,
                    paysyses=[paysys]
                ),
                hamcrest.has_properties(
                    person=persons[1],
                    contract=None,
                    paysyses=[]
                )
            )
        )
        msg = "pcp.person doesn't match person in register invoices: %s" % persons[0]
        assert ns.paysyses_denied == {
            (persons[0], None): {},
            (persons[1], None): {paysys: msg}
        }

    def test_multiple_persons(self, session, client, service, firm, currency):
        person_category = create_person_category(session, firm.country)

        paysys = create_paysys_simple(
            session,
            category=person_category.category,
            firm_id=firm.id,
            iso_currency=currency.iso_code
        )
        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[(paysys.currency, paysys.payment_method_id)]
        )

        product = create_product(session)
        create_price_tax_rate(session, product, firm.country, currency)
        order = create_order(session, client, product, service)

        persons = [
            create_person(session, type=person_category.category, client=client)
            for _ in range(2)
        ]

        ref_invoices = [
            create_invoice(
                session,
                paysys=paysys,
                person=person,
                request=create_request(session, client=client, orders=[order]),
            )
            for person in persons
        ]

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=ref_invoices
        )

        ns = PaystepNS(request=request)
        pcp_list = [
            PCP(person, None, [paysys])
            for person in persons
        ]
        filter_pcps(ns, pcp_list)

        hamcrest.assert_that(
            pcp_list,
            hamcrest.contains_inanyorder(*[
                hamcrest.has_properties(
                    person=person,
                    contract=None,
                    paysyses=[]
                )
                for person in persons
            ])
        )
        msg = "pcp.person doesn't match person in register invoices: %s" % ','.join(map(str, sorted(persons)))
        assert ns.paysyses_denied == {
            (person, None): {paysys: msg}
            for person in persons
        }

    @pytest.mark.parametrize(
        'invoice_sum, orders_sum',
        [
            pytest.param(110, 0, id='invoice'),
            pytest.param(50, 60, id='invoice_order'),
        ]
    )
    def test_paysys_limit(self, session, client, service, firm, currency, invoice_sum, orders_sum):
        person_category = create_person_category(session, firm.country)

        paysys = create_paysys_simple(
            session,
            category=person_category.category,
            firm_id=firm.id,
            iso_currency=currency.iso_code,
            first_limit=120,
            nds_pct=666
        )
        paysys_lim = create_paysys_simple(
            session,
            category=person_category.category,
            firm_id=firm.id,
            iso_currency=currency.iso_code,
            first_limit=100,
            nds_pct=666,
        )
        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[(paysys.currency, paysys.payment_method_id)]
        )

        product = create_product(session)
        create_price_tax_rate(session, product, firm.country, currency)
        order = create_order(session, client, product, service)

        person = create_person(session, type=person_category.category, client=client)

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            request=create_request(session, client=client, orders=[order], quantity=invoice_sum),
        )

        if orders_sum:
            order_params = dict(orders=[order], quantity=orders_sum)
        else:
            order_params = dict(orders=[])
        request = create_request(
            session,
            client=client,
            ref_invoices=[ref_invoice],
            **order_params
        )

        ns = PaystepNS(request=request)
        pcp = PCP(person, None, [paysys, paysys_lim])
        filter_pcps(ns, [pcp])

        assert pcp.paysyses == [paysys]
        assert ns.paysyses_denied == {
            (person, None): {paysys_lim: 'paysys limit check - balance paysys'},
        }

    @pytest.mark.parametrize(
        'limit, invoice_sum, is_ok',
        [
            (100, 90, True),
            (100, 101, False),
        ]
    )
    def test_invalid_paysys_nds_without_orders(self, session, client, service, firm, currency,
                                               limit, invoice_sum, is_ok):
        person_category = create_person_category(session, firm.country, resident=1)

        paysys = create_paysys_simple(
            session,
            category=person_category.category,
            firm_id=firm.id,
            iso_currency=currency.iso_code,
            first_limit=limit,
            nds_pct=0
        )
        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[(paysys.currency, paysys.payment_method_id)]
        )

        product = create_product(session)
        create_price_tax_rate(session, product, firm.country, currency, resident=1)
        order = create_order(session, client, product, service)

        person = create_person(session, type=person_category.category, client=client)

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            request=create_request(session, client=client, orders=[order], quantity=invoice_sum),
        )

        request = create_request(
            session,
            client=client,
            ref_invoices=[ref_invoice],
            orders=[]
        )

        ns = PaystepNS(request=request)
        pcp = PCP(person, None, [paysys])
        filter_pcps(ns, [pcp])

        if is_ok:
            assert pcp.paysyses == [paysys]
            assert ns.paysyses_denied == {(person, None): {}}
        else:
            assert pcp.paysyses == []
            assert ns.paysyses_denied == {(person, None): {paysys: 'paysys limit check - balance paysys'}}
