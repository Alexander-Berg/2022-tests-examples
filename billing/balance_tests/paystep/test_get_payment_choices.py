# -*- coding: utf-8 -*-
import pytest
import datetime
import hamcrest as hm
import contextlib
from lxml import etree
from mock import patch
from decimal import Decimal as D

from billing.contract_iface import ContractTypeId
from billing.contract_iface.contract_meta import ContractTypes
from balance.constants import (
    PaysysGroupIDs,
    RegionId,
    POSTPAY_PAYMENT_TYPE,
    PREPAY_PAYMENT_TYPE,
    PermissionCode,
)
from balance.paystep import get_payment_choices, PCP, PaystepManager
from balance.paystep.filters import PaymentDisableReason, ContractDisableReason
from balance.mapper import Person, Service, Country, ClientFraudStatus
from balance import exc, mapper

from tests.object_builder import (
    create_pay_policy_service,
    create_pay_policy_region,
    create_pay_policy_payment_method,
    Getter,
    create_role,
    create_passport,
    FirmBuilder,
)
from tests.balance_tests.paystep.paystep_common import (
    create_request,
    create_pay_policy,
    create_firm,
    create_country,
    create_currency,
    create_person,
    create_paysys,
    create_client,
    create_order,
    create_service,
    create_manager,
    create_invoice,
    create_person_category,
    create_price_tax_rate,
    create_contract,
    pcps_to_set,
    create_passport,
    create_role,
    ALTER_INVOICE_PAYSYS,
    ALTER_INVOICE_CONTRACT,
    create_terminals_limit,
    create_trust_paymethods,
    BILLING_SUPPORT,
    ISSUE_INVOICES,
    ADMIN_ACCESS,
    BANK, CARD, YAMONEY,
    WEBMONEY, PAYPAL, QIWI,
)

pytestmark = [
    pytest.mark.paystep,
]

NOW = datetime.datetime.now()


@contextlib.contextmanager
def mock_disable_reason(reason_type, disable_reasons):
    cls_map = {
        'payment': PaymentDisableReason,
        'contract': ContractDisableReason,
    }
    base_cls = cls_map[reason_type]

    class MockDisableReason(base_cls):
        def __new__(cls, name, *a, **kw):
            if name in disable_reasons:
                return
            return super(MockDisableReason, cls).__new__(cls, *a, **kw)

    patch_path = 'balance.paystep.filters.%s' % base_cls.__name__
    with patch(patch_path, MockDisableReason):
        yield


@pytest.mark.usefixtures('switch_new_paystep_flag')
def test_get_payment_choices_basic(session, firm, currency):
    request_ = create_request(session)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    pcp_info = get_payment_choices(request_, show_disabled_paysyses=False, skip_trust=True)
    assert pcp_info.warning is None
    assert pcp_info.paysys_list == [paysys]
    assert pcp_info.request == request_
    assert pcp_info.persons_parent == request_.client
    assert pcps_to_set(session, pcp_info.pcp_list) == {(None, paysys, person_category, None)}
    assert pcp_info.lang is None


def test_get_payment_choices_sort_paysyses(session, firm, currency):
    """сортируем способы оплаты по весам методов оплаты"""
    session.config.__dict__['USE_NEW_PAYSTEP'] = False
    request_ = create_request(session)
    person = create_person(session)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for payment_method in [BANK, CARD, YAMONEY, WEBMONEY, QIWI, PAYPAL]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=payment_method, extern=1, cc='paysys_cc'))
    mocked_pcps = [PCP(contract=None, person=person, paysyses=paysys_list)]
    patch_make_pcps = patch('balance.paystep.legacy.make_pcps', return_value=mocked_pcps)
    with patch_make_pcps:
        pcp_info = get_payment_choices(request_, show_disabled_paysyses=False, skip_trust=True)
    assert [paysys.payment_method_id for paysys in pcp_info.paysys_list] == [YAMONEY, WEBMONEY, PAYPAL, QIWI, BANK,
                                                                             CARD]


def test_get_payment_choices_sorted_pcps(session, firm, currency):
    """сортирует тройки плательщик-договор-способ оплаты по плательщику, временные плательщики вперед"""
    session.config.__dict__['USE_NEW_PAYSTEP'] = False
    request_ = create_request(session)
    person_1 = create_person(session)
    person_2 = create_person(session, client=person_1.client)
    temp_person = Person(person_1.client, person_1.type, skip_category_check=True)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    mocked_pcps = []
    for person in [person_1, person_2, temp_person]:
        mocked_pcps.append(PCP(contract=None, person=person, paysyses=[paysys]))
    patch_make_pcps = patch('balance.paystep.legacy.make_pcps', return_value=mocked_pcps)
    with patch_make_pcps:
        pcp_info = get_payment_choices(request_, show_disabled_paysyses=False, skip_trust=True)
    assert [pcp.person for pcp in pcp_info.pcp_list] == sorted([person_1, person_2, temp_person],
                                                               key=lambda person: person.id)


@pytest.mark.usefixtures('switch_new_paystep_flag')
def test_get_payment_choices_raise_exception(session, firm, currency):
    request_ = create_request(session, quantity=D('100.01'))
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, first_limit=100,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    with pytest.raises(exc.PAYSYS_LIMIT_EXCEEDED):
        get_payment_choices(request_, raise_exception=True)
    pcp_info = get_payment_choices(request_, raise_exception=False)
    assert pcp_info.paysys_list == []
    pcp_info = get_payment_choices(request_)
    assert pcp_info.paysys_list == []


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
@pytest.mark.parametrize('quantity', [100, 101])
def test_get_payment_choices_wo_disabled(session, firm, currency, quantity, show_disabled_paysyses):
    request_ = create_request(session, quantity=quantity)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    first_limit = 100
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc', first_limit=first_limit)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    paysyses = get_payment_choices(request_, show_disabled_paysyses=show_disabled_paysyses, skip_trust=True)
    if first_limit < quantity:
        if show_disabled_paysyses:
            assert paysyses.paysys_list == [paysys]
            assert paysys.disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
        else:
            assert paysyses.paysys_list == []
    else:
        assert paysyses.paysys_list == [paysys]
        assert paysys.disabled_reasons == set()


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('skip_pcp_filter', [True, False])
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_skip_pcp_filter(session, firm, currency, skip_pcp_filter, show_disabled_paysyses):
    request_ = create_request(session, quantity=101)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    first_limit = 100
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc', first_limit=first_limit)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    paysyses = get_payment_choices(request_, show_disabled_paysyses=show_disabled_paysyses, skip_trust=True,
                                   skip_pcp_filter=skip_pcp_filter)
    if show_disabled_paysyses and not skip_pcp_filter:
        assert paysyses.paysys_list == [paysys]
        assert paysys.disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
    elif skip_pcp_filter:
        assert paysyses.paysys_list == [paysys]
        assert paysys.disabled_reasons == set()
    else:
        assert paysyses.paysys_list == []
        assert paysys.disabled_reasons == set()


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
@pytest.mark.parametrize('quantity', [100, 101])
def test_get_payment_choices_wo_disabled_trust_api(session, firm, currency, quantity, show_disabled_paysyses):
    request_ = create_request(session, quantity=quantity)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=PaysysGroupIDs.auto_trust)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    first_limit = 100
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                           category=person_category.category, group_id=PaysysGroupIDs.auto_trust,
                           payment_method_id=BANK, extern=1, cc='paysys_cc', first_limit=first_limit)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    trust_paymethods = create_trust_paymethods(paysys, limits_list=[100])
    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods',
                                      return_value=trust_paymethods)
    with patch_get_payment_methods:
        paysyses = get_payment_choices(request_, show_disabled_paysyses=show_disabled_paysyses, skip_trust=True,
                                       need_trust_api=True)
    if first_limit < quantity:
        if show_disabled_paysyses:
            assert paysyses.paysys_list == [paysys]
            assert paysys.disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
            assert paysys.trust_paymethods == trust_paymethods
        else:
            assert paysyses.paysys_list == []
    else:
        assert paysyses.paysys_list == [paysys]
        assert paysys.disabled_reasons == set()
        assert paysys.trust_paymethods == trust_paymethods


@pytest.mark.usefixtures('switch_new_paystep_flag')
def test_get_payment_choices_exclude_disabled_forced_trust_part(session, firm, currency):
    request_ = create_request(session, quantity=100)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=PaysysGroupIDs.auto_trust)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    first_limit = 100
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                           category=person_category.category, group_id=PaysysGroupIDs.auto_trust,
                           payment_method_id=BANK, extern=1, cc='paysys_cc', first_limit=first_limit)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    trust_paymethods = create_trust_paymethods(paysys, limits_list=[100, 99])
    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods',
                                      return_value=trust_paymethods)
    with patch_get_payment_methods:
        paysyses = get_payment_choices(request_, show_disabled_paysyses=False, skip_trust=True, need_trust_api=True)
    assert paysyses.paysys_list == [paysys]
    assert paysys.disabled_reasons == set()
    assert paysys.trust_paymethods == [trust_paymethods[0]]


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_get_payment_choices_exclude_disabled_terminals(session, firm, currency, show_disabled_paysyses):
    request_ = create_request(session, quantity=101)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0, paysys_group_id=PaysysGroupIDs.default)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                           category=person_category.category, group_id=PaysysGroupIDs.default,
                           payment_method_id=BANK, extern=1, cc='paysys_cc', first_limit=100)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency, price=1)
    trust_paymethods = create_trust_paymethods(paysys, limits_list=[100, 99])
    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods',
                                      return_value=trust_paymethods)
    terminal_limit = create_terminals_limit(session, request_.request_orders[0].order.service, currency)
    patch_get_terminals_limit = patch('balance.trust_api.actions.get_terminals_limit',
                                      return_value=terminal_limit)
    with patch_get_payment_methods, patch_get_terminals_limit:
        paysyses = get_payment_choices(request_, show_disabled_paysyses=show_disabled_paysyses, skip_trust=True,
                                       need_trust_api=True)
    if show_disabled_paysyses:
        assert paysyses.paysys_list == [paysys]
        assert paysys.disabled_reasons == {'ID_Paysys_limit_is_exceeded'}
        assert paysys.payment_limit == terminal_limit

    else:
        assert paysyses.paysys_list == []
        assert paysys.disabled_reasons == set([])


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_get_payment_choices_trust_api_wo_payment_methods(session, firm, currency, show_disabled_paysyses):
    request_ = create_request(session, quantity=101)
    order = request_.request_orders[0].order
    pp_id = create_pay_policy_service(session, order.service_id, firm.id)
    create_pay_policy_payment_method(session, pp_id, currency.iso_code, CARD, PaysysGroupIDs.auto_trust)
    create_pay_policy_payment_method(session, pp_id, currency.iso_code, CARD, PaysysGroupIDs.default)
    create_pay_policy_region(session, pp_id, firm.region_id)

    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_trust = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                                 category=person_category.category, group_id=PaysysGroupIDs.auto_trust,
                                 payment_method_id=CARD, extern=1, cc='paysys_1')
    paysys_default = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                                   category=person_category.category, group_id=PaysysGroupIDs.default,
                                   payment_method_id=CARD, extern=1, cc='paysys_2', first_limit=666)
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)

    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods', return_value=[])

    with patch_get_payment_methods:
        paysyses = get_payment_choices(
            request_,
            show_disabled_paysyses=show_disabled_paysyses,
            skip_trust=True,
            need_trust_api=True,
            need_trust_api_payment_methods=False,
        )

    assert set(paysyses.paysys_list) == {paysys_trust, paysys_default}
    assert not paysys_trust.disabled_reasons
    assert not paysys_default.disabled_reasons
    assert not paysys_trust.trust_paymethods


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('show_disabled_paysyses', [True, False])
def test_get_payment_choices_trust_api_no_max_amounts(session, firm, currency, show_disabled_paysyses):
    request_ = create_request(session, quantity=100)
    order = request_.request_orders[0].order
    pp_id = create_pay_policy_service(session, order.service_id, firm.id)
    create_pay_policy_payment_method(session, pp_id, currency.iso_code, CARD, PaysysGroupIDs.auto_trust)
    create_pay_policy_region(session, pp_id, firm.region_id)

    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, currency=currency.char_code,
                           category=person_category.category, group_id=PaysysGroupIDs.auto_trust,
                           payment_method_id=CARD, extern=1, cc='paysys_1')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)

    # в первом случае будет вообще без ключа, во втором с переданным None'ом
    trust_paymethods = create_trust_paymethods(paysys, limits_list=[None, None, 10])
    del trust_paymethods[0]['max_amount']

    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods', return_value=trust_paymethods)

    with patch_get_payment_methods:
        paysyses = get_payment_choices(
            request_,
            show_disabled_paysyses=show_disabled_paysyses,
            skip_trust=True,
            need_trust_api=True,
        )

    assert paysyses.paysys_list == [paysys]
    assert not paysys.disabled_reasons
    assert paysys.trust_paymethods == trust_paymethods[:2]


@pytest.mark.usefixtures('switch_new_paystep_flag')
@pytest.mark.parametrize('force_contractless_invoice', [0, 1])
@pytest.mark.parametrize('personal_account', [0, 1])
@pytest.mark.parametrize('is_agency', [0, 1])
def test_w_contract_contractless_persons(session, firm, currency, is_agency, personal_account,
                                         force_contractless_invoice):
    client = create_client(session, is_agency=is_agency, force_contractless_invoice=force_contractless_invoice)
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=personal_account)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing', return_value=[paysys])
    get_paysyses_patch_legacy = patch('balance.paystep.legacy.get_base_paysyses_routing', return_value=[paysys])
    with get_paysyses_patch, get_paysyses_patch_legacy:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    if personal_account or force_contractless_invoice:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, None),
                                                           (None, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, person_wo_contract)}
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, None),
                                                           (None, paysys, person_category, person_wo_contract)}


@pytest.mark.usefixtures('switch_new_paystep_flag')
def test_force_person_wo_contract_exception(session, client, firm, currency):
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=0)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing', return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True,
                                       person=person_from_contract)
    assert pcps_to_set(session, pcp_info.pcp_list) == set()
    assert pcp_info.warning == 'INVALID_PERSON'


# @pytest.mark.usefixtures('switch_new_paystep_flag')
def test_force_person_wo_contract(session, switch_new_paystep_flag, firm, client, currency):
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=0)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True,
                                       person=person_wo_contract)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(None, paysys, person_category, person_wo_contract)}

    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person_from_contract),
                                                       (None, paysys, person_category, None),
                                                       (None, paysys, person_category, person_wo_contract)}


def test_w_contract_w_force_contract(session, switch_new_paystep_flag, firm, currency, client):
    """если договор передан явно, возвращаем способы оплаты только в сочетании с договором"""
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True, contract=contract)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person)}


@pytest.mark.parametrize('is_agency', [0, 1])
@pytest.mark.parametrize('with_subclient_non_resident', [True, False])
def test_w_contract_w_subclient_non_resident(session, switch_new_paystep_flag, firm, currency, with_subclient_non_resident, is_agency):
    """c субклиентами нерезидентами можно выставляться только по договору"""
    client = create_client(session, is_agency=is_agency)
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    if with_subclient_non_resident:
        request.client.fullname = 'client_fullname'
        request.client.non_resident_currency_payment = currency.char_code
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, firm=firm.id)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    if with_subclient_non_resident:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person)}
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person),
                                                           (None, paysys, person_category, person),
                                                           (None, paysys, person_category, None)}


def test_region_netherlands_wo_contract(session, switch_new_paystep_flag, firm, currency):
    """без договора и с плательщиком нерезом нельзя выставляться в Нидерландах"""
    role = create_role(session, PermissionCode.USE_ADMIN_PERSONS)
    passport = create_passport(session, [], patch_session=True)
    client = create_client(session, is_agency=False, can_issue_initial_invoice=False)
    passport.link_to_client(client)
    country = session.query(Country).getone(RegionId.NETHERLANDS)
    firm = FirmBuilder.construct(session, country=country)
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    request.client.fullname = 'client_fullname'
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1, auto_only=0)
    client.get_creatable_person_categories = lambda: [person_category]
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code, country=firm.country,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, firm=firm.id)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, lambda ns, contract: [paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person)}


@pytest.mark.parametrize('force_contractless_invoice', [0, 1])
@pytest.mark.parametrize('contract_needed_values', [[0, 1], [0, 0]])
@pytest.mark.parametrize('is_suspended', [NOW, None])
@pytest.mark.parametrize('allowed_agency_without_contract_values', [[0, 1], [1, 1]])
def test_w_contract_w_agency(session, firm, currency, allowed_agency_without_contract_values, is_suspended,
                             contract_needed_values, force_contractless_invoice, switch_new_paystep_flag):
    """Для агенств:
    - если хотя бы у одного сервиса есть признак contract_needed=1, позволяем выставлять только по договору
    - если ни у одного сервиса нет признака contract_needed=1, позволяем выставлять по договору и без
    - если у агенства есть приостановленный договор и все сервисы обладают признаком allowed_agency_without_contract
    или хотя бы 1 сервис - признаком contract_needed, не даем выставляться никак, не по договору, не без
    - если у агенства есть приостановленный договор и не все сервисы обладают признаком allowed_agency_without_contract
    и нет ни одного сервиса с признаком contract_needed=1, даем выставляться без договора
    """
    session.config.__dict__['SINGLE_ACCOUNT_MIN_CLIENT_DT'] = session.now() - datetime.timedelta(days=1)
    client = create_client(session, is_agency=1, force_contractless_invoice=force_contractless_invoice,
                           creation_dt=session.now(), with_single_account=True)
    services = []
    for allowed_agency_without_contract, contract_needed in zip(allowed_agency_without_contract_values,
                                                                contract_needed_values):
        services.append(create_service(session, allowed_agency_without_contract=allowed_agency_without_contract,
                                       contract_needed_agency=contract_needed))
    orders = []
    for service in services:
        order = create_order(session, client=client, service=service)
        orders.append(order)
        create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services={service.id for service in services}, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, is_suspended=is_suspended)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_paysyses', return_value=[paysys])

    # мокаем 2 фильтра, потому что они не дают проверить логику, которая нам нужна
    with mock_disable_reason('payment', ['multiple_services_wo_single_account']), \
            mock_disable_reason('contract', ['multiple_services_w_contract']), \
            get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)

    if is_suspended:
        if all(allowed_agency_without_contract_values) \
                or any(contract_needed_values) \
                and not force_contractless_invoice:
            assert result == set()
        else:
            assert result == {(None, paysys, person_category, None),
                              (None, paysys, person_category, person),
                              (None, paysys, person_category, person_wo_contract)}

    elif any(contract_needed_values) and not force_contractless_invoice:
        assert result == {(contract, paysys, person_category, person)}

    else:
        assert result == {(contract, paysys, person_category, person),
                          (None, paysys, person_category, None),
                          (None, paysys, person_category, person),
                          (None, paysys, person_category, person_wo_contract)}


@pytest.mark.parametrize('allowed_agency_without_contract_values', [[0, 1], [1, 1]])
def test_w_contract_w_agency_need_deal_passport(session, firm, currency, allowed_agency_without_contract_values,
                                                switch_new_paystep_flag):
    client = create_client(session, is_agency=1, force_contractless_invoice=0)
    services = []
    for allowed_agency_without_contract in allowed_agency_without_contract_values:
        services.append(create_service(session, allowed_agency_without_contract=allowed_agency_without_contract,
                                       contract_needed_agency=0))
    orders = []
    for service in services:
        order = create_order(session, client=client, service=service)
        orders.append(order)
        create_price_tax_rate(session, order.product, firm.country, currency, price=1, resident=0)
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=1, resident=0)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services={service.id for service in services}, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=0, is_suspended=NOW)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_paysyses', return_value=[paysys])

    with mock_disable_reason('payment', ['multiple_services_wo_single_account']), \
            get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)
    if all(allowed_agency_without_contract_values):
        assert result == set()
    else:
        assert result == {(None, paysys, person_category, None),
                          (None, paysys, person_category, person),
                          (None, paysys, person_category, person_wo_contract)}


@pytest.mark.parametrize('force_contractless_invoice', [0, 1])
@pytest.mark.parametrize('contract_needed_values', [[0, 1], [0, 0]])
@pytest.mark.parametrize('allowed_agency_without_contract_values', [[0, 1], [1, 1]])
def test_wo_contract_w_agency(session, firm, currency, allowed_agency_without_contract_values, contract_needed_values,
                              force_contractless_invoice):
    client = create_client(session, is_agency=1, force_contractless_invoice=force_contractless_invoice)
    services = []
    for allowed_agency_without_contract, contract_needed in zip(allowed_agency_without_contract_values,
                                                                contract_needed_values):
        services.append(create_service(session, allowed_agency_without_contract=allowed_agency_without_contract,
                                       contract_needed_agency=contract_needed))
    orders = []
    for service in services:
        orders.append(create_order(session, client=client, service=service))
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    get_paysyses_patch = patch('balance.paystep.legacy.get_paysyses', return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)

    if not all(allowed_agency_without_contract_values) \
            and any(contract_needed_values) \
            and not force_contractless_invoice:
        assert result == set()
    else:
        assert {(None, (paysys,), person_category, None),
                (None, (paysys,), person_category, person_wo_contract)}


@pytest.mark.parametrize('contract_needed_client_values', [[0, 1], [0, 0]])
@pytest.mark.parametrize('is_suspended', [NOW, None])
def test_w_contract_w_client(session, firm, client, currency, is_suspended, contract_needed_client_values,
                             switch_new_paystep_flag):
    services = []
    for contract_needed_client in contract_needed_client_values:
        services.append(create_service(session, contract_needed_client=contract_needed_client))
    orders = []
    for service in services:
        order = create_order(session, client=client, service=service)
        orders.append(order)
        create_price_tax_rate(session, order.product, firm.country, currency, price=1, resident=1)
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    person = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services={service.id for service in services}, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, is_suspended=is_suspended)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_paysyses', return_value=[paysys])
    with mock_disable_reason('payment', ['multiple_services_wo_single_account']), \
            mock_disable_reason('contract', ['multiple_services_w_contract']), \
            get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)

    if is_suspended:
        assert result == {(None, paysys, person_category, None),
                          (None, paysys, person_category, person),
                          (None, paysys, person_category, person_wo_contract)}
    else:
        if any(contract_needed_client_values):
            assert result == {(contract, paysys, person_category, person)}
        else:
            assert result == {(contract, paysys, person_category, person),
                              (None, paysys, person_category, None),
                              (None, paysys, person_category, person),
                              (None, paysys, person_category, person_wo_contract)}


@pytest.mark.parametrize('contract_needed_client_values', [[0, 1], [0, 0]])
def test_wo_contract_w_client(session, firm, client, currency, contract_needed_client_values, switch_new_paystep_flag):
    services = []
    for contract_needed_client in contract_needed_client_values:
        services.append(create_service(session, contract_needed_client=contract_needed_client))
    orders = []
    for service in services:
        order = create_order(session, client=client, service=service)
        orders.append(order)
        create_price_tax_rate(session, order.product, firm.country, currency, price=1, resident=1)
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_paysyses', return_value=[paysys])
    with mock_disable_reason('payment', ['multiple_services_wo_single_account']), \
            get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)

    assert result == {(None, paysys, person_category, None),
                      (None, paysys, person_category, person_wo_contract)}


@pytest.mark.parametrize('is_agency', [0, 1])
def test_need_contract_w_market_cpa(session, switch_new_paystep_flag, is_agency, firm, currency):
    client = create_client(session, is_agency=is_agency)
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, supercommission_bonus={150})
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person)}


@pytest.mark.parametrize('with_contract', [True, False])
@pytest.mark.parametrize('region_id', [None, RegionId.UNITED_STATES])
@pytest.mark.parametrize('is_agency', [0, 1])
@pytest.mark.parametrize('is_market_in_services', [True, False])
def test_need_contract_from_usa_market(session, firm, currency, is_agency, region_id, is_market_in_services,
                                       with_contract):
    session.config.__dict__['USE_NEW_PAYSTEP'] = False
    client = create_client(session, is_agency=is_agency, region_id=region_id)
    orders = []
    services = [create_service(session, contract_needed_client=0),
                Getter(Service, 11 if is_market_in_services else 7).build(session).obj]
    for service in services:
        orders.append(create_order(session, client=client, service=service))
    request = create_request(session, orders=orders)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    get_paysyses_patch = patch('balance.paystep.legacy.get_paysyses', return_value=[paysys])
    if with_contract:
        person = create_person(session, client=request.client, type=person_category.category)
        contract = create_contract(session, is_signed=NOW, dt=NOW, person=person, currency=currency.num_code,
                                   services={service.id for service in services}, payment_type=POSTPAY_PAYMENT_TYPE,
                                   client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                                   personal_account=1)
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
        result = pcps_to_set(session, pcp_info.pcp_list)
    if with_contract:
        if is_agency \
                or (region_id == RegionId.UNITED_STATES and is_market_in_services) \
                or (not is_agency and is_market_in_services):
            assert result == {(contract, paysys, person_category, person)}
        else:
            assert result == {(contract, paysys, person_category, person),
                              (None, paysys, person_category, None),
                              (None, paysys, person_category, person),
                              (None, paysys, person_category, person_wo_contract)}
    else:
        if is_agency or (is_market_in_services and region_id):
            assert result == set()
        else:
            assert result == {(None, paysys, person_category, None),
                              (None, paysys, person_category, person_wo_contract)}


@pytest.mark.parametrize('is_suspended', [None, NOW])
@pytest.mark.parametrize('is_deactivated', [0, 1])
@pytest.mark.parametrize('is_agency', [0, 1])
def test_w_contract_valid_contract_deactivated_suspended(session, switch_new_paystep_flag, firm, currency, is_agency, is_deactivated,
                                                         is_suspended):
    client = create_client(session, is_agency=is_agency, force_contractless_invoice=0)
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                               personal_account=1, is_deactivated=is_deactivated, is_suspended=is_suspended)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    if is_suspended and not is_deactivated:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(None, paysys, person_category, None),
                                                           (None, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, person_wo_contract)}
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, None),
                                                           (None, paysys, person_category, person_from_contract),
                                                           (None, paysys, person_category, person_wo_contract)}


def test_w_contract_force_contract(session, switch_new_paystep_flag, firm, client, currency):
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contracts = []
    for _ in range(2):
        contracts.append(
            create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                            services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                            client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                            personal_account=1))
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True, contract=contracts[0])
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contracts[0], paysys, person_category, person_from_contract)}

    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True)
    assert pcps_to_set(session, pcp_info.pcp_list) == {(contracts[0], paysys, person_category, person_from_contract),
                                                       (contracts[1], paysys, person_category, person_from_contract),
                                                       (None, paysys, person_category, None),
                                                       (None, paysys, person_category, person_from_contract),
                                                       (None, paysys, person_category, person_wo_contract)}


@pytest.mark.usefixtures('switch_new_paystep_flag')
def test_w_contract_invalid_force_contract(session, firm, client, currency):
    order = create_order(session, client=client, service=create_service(session, contract_needed_client=0))
    request = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    person_wo_contract = create_person(session, client=request.client, type=person_category.category)
    contracts = []
    for is_suspended in [NOW, None]:
        contracts.append(
            create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                            services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                            client=request.client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                            personal_account=1, is_suspended=is_suspended))
    get_paysyses_patch = patch('balance.paystep.payments.get_base_paysyses_routing', return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True, contract=contracts[0])
    assert pcps_to_set(session, pcp_info.pcp_list) == set()


@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_w_contract_force_person_not_from_contract(session, switch_new_paystep_flag, client, firm, currency, commission):
    agency = create_client(session, is_agency=1)
    order = create_order(session, client=client, agency=agency, service=create_service(session, contract_needed_client=0))
    request = create_request(session, request_client=agency, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    subclient_person = create_person(session, client=client, type=person_category.category)
    agency_person = create_person(session, client=agency, type=person_category.category)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=commission,
                               personal_account=1)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True, contract=contract,
                                       person=subclient_person)
    if commission in [ContractTypeId.WITHOUT_PARTICIPATION,
                      ContractTypeId.OFD_WITHOUT_PARTICIPATION]:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, subclient_person)}
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == set()


@pytest.mark.parametrize('commission', [ContractTypeId.NON_AGENCY,
                                        ContractTypeId.WITHOUT_PARTICIPATION,
                                        ContractTypeId.OFD_WITHOUT_PARTICIPATION])
def test_w_contract_wo_participation(session, switch_new_paystep_flag, firm, client, currency, commission):
    agency = create_client(session, is_agency=1)
    order = create_order(session, client=client, agency=agency, service=create_service(session, contract_needed_client=0))
    request = create_request(session, request_client=agency, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)
    subclient_person = create_person(session, client=client, type=person_category.category)
    agency_person = create_person(session, client=agency, type=person_category.category)
    person_from_contract = create_person(session, client=request.client, type=person_category.category)
    contract = create_contract(session, is_signed=NOW, dt=NOW, person=person_from_contract, currency=currency.num_code,
                               services=request.request_orders[0].order.service.id, payment_type=POSTPAY_PAYMENT_TYPE,
                               client=request.client, ctype='GENERAL', commission=commission,
                               personal_account=1)
    patch_path = 'balance.paystep.payments.get_base_paysyses_routing' if switch_new_paystep_flag else 'balance.paystep.legacy.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])
    with get_paysyses_patch:
        pcp_info = get_payment_choices(request, show_disabled_paysyses=False, skip_trust=True, contract=contract)
    if commission in [ContractTypeId.WITHOUT_PARTICIPATION,
                      ContractTypeId.OFD_WITHOUT_PARTICIPATION]:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, subclient_person)}
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person_from_contract)}


OFFER_TEXT = """<?xml version="1.0" encoding="utf-8"?>
<cat lang="ru">
  <msg id="3">
    <value>Offer ru text</value>
  </msg>
</cat>
"""


def parse_file(self, lang):
    assert lang == 'ru'
    tree = etree.fromstring(OFFER_TEXT.encode('utf-8'))
    self._values_dict = {}
    self._iterate_to_dict(tree)


@pytest.mark.parametrize(
    'w_patch_offers, offer_id, res_offer_id, disable_reason',
    [
        pytest.param(False, 333, 333, None, id='wo offers'),
        pytest.param(True, 3, 3, None, id='existing'),
        pytest.param(True, 0, None, None, id='offer wo text'),
        pytest.param(True, 333, None, 'offer_not_found', id='wo offer text'),
        pytest.param(True, None, None, 'incompatible_offer', id='wo offer number'),
    ],
)
def test_offer(session, client, firm, currency, w_patch_offers, offer_id, res_offer_id, disable_reason):
    session.config.__dict__['USE_NEW_PAYSTEP'] = True

    role = create_role(session, PermissionCode.ADMIN_ACCESS, PermissionCode.BILLING_SUPPORT,
                       PermissionCode.ISSUE_INVOICES, PermissionCode.PAYSTEP_VIEW)
    create_passport(session, [role], patch_session=True)

    order = create_order(session, client=client)
    request = create_request(session, request_client=client, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, order.product, firm.country, currency, price=1)

    patch_path = 'balance.paystep.payments.get_base_paysyses_routing'
    get_paysyses_patch = patch(patch_path, return_value=[paysys])

    offers_patch_path = 'balance.multilang_support._CommonManager._parse'
    if w_patch_offers:
        offers_patch = patch(offers_patch_path, parse_file)
    else:
        offers_patch = patch(offers_patch_path, side_effect=Exception)

    offer_rules_path = 'balance.offer.process_rules'
    offer_rules_patch = patch(offer_rules_path, return_value=(offer_id, 666))

    with offers_patch, get_paysyses_patch, offer_rules_patch:
        paystep_manager = PaystepManager.create(
            request=request,
            skip_trust=True,
            show_disabled_paysyses=True,
            lang='ru',
        )
        choices = paystep_manager.payment_choices
    hm.assert_that(
        choices,
        hm.contains(
            hm.has_properties(
                offer_id=res_offer_id,
                disable_reason=(
                    hm.has_properties(name=disable_reason)
                    if disable_reason
                    else None
                ),
            ),
        ),
    )


@pytest.mark.parametrize(
    'desired_contract_id, is_offer',
    [
        (-1, True),  # Заданная оферта
        (1, False),  # Заданный контракт
        (None, False),  # Незаданный контракт
        (None, True),  # Незаданная оферта
    ]
)
def test_request_w_desired_contract_id(session, client, firm, currency, switch_new_paystep_flag,
                                       desired_contract_id, is_offer):
    services = []
    for contract_needed_client in [1, 0]:
        services.append(create_service(session, contract_needed_client=contract_needed_client))

    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, client=client, person_category=person_category)

    if not is_offer:
        contract = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                   payment_type=POSTPAY_PAYMENT_TYPE, services={service.id for service in services},
                                   person=person, client=client, ctype='GENERAL', commission=ContractTypeId.NON_AGENCY,
                                   personal_account=1, firm=firm.id)
    else:
        contract = None

    order = create_order(session, client=client, service=services[0])

    c_id = desired_contract_id if desired_contract_id in (None, -1) else contract.id
    request = create_request(session, client=client, orders=[order], invoice_desired_contract_id=c_id, firm_id=firm.id)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)

    pcp_info = get_payment_choices(request, contract=contract, skip_trust=True)

    assert pcp_info.warning is None
    assert pcp_info.paysys_list == [paysys]
    assert pcp_info.request == request
    assert pcp_info.persons_parent == request.client
    assert pcp_info.lang is None

    if is_offer:
        assert pcps_to_set(session, pcp_info.pcp_list) == {
            (None, paysys, person_category, None),
            (None, paysys, person_category, person),
        }
    else:
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract, paysys, person_category, person)}


@pytest.mark.parametrize('desired_contract_id', [-1, 1, None])
def test_filters_desired_contract(session, client, firm, currency, switch_new_paystep_flag,
                                  desired_contract_id):
    service = create_service(session, contract_needed_client=0)

    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, client=client, person_category=person_category)

    contract_common = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                      payment_type=POSTPAY_PAYMENT_TYPE, services={service.id},
                                      person=person, client=client, ctype='GENERAL',
                                      commission=ContractTypeId.NON_AGENCY, personal_account=1, firm=firm.id)

    contract_desired = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                       payment_type=POSTPAY_PAYMENT_TYPE, services={service.id},
                                       person=person, client=client, ctype='GENERAL',
                                       commission=ContractTypeId.NON_AGENCY, personal_account=1, firm=firm.id)

    order = create_order(session, client=client, service=service)

    c_id = desired_contract_id if desired_contract_id in (None, -1) else contract_desired.id
    request = create_request(session, client=client, orders=[order], invoice_desired_contract_id=c_id, firm_id=firm.id)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request.request_orders[0].order.service.id,
                      is_agency=0)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)

    pcp_info = get_payment_choices(request, contract=None, skip_trust=True)

    assert pcp_info.warning is None
    assert pcp_info.paysys_list == [paysys]
    assert pcp_info.request == request
    assert pcp_info.persons_parent == request.client
    assert pcp_info.lang is None

    if desired_contract_id == -1:
        # Оферта
        assert pcps_to_set(session, pcp_info.pcp_list) == {
            (None, paysys, person_category, None),
            (None, paysys, person_category, person),
        }
    elif desired_contract_id is None:
        # Все, что доступно
        assert pcps_to_set(session, pcp_info.pcp_list) == {
            (None, paysys, person_category, None),
            (None, paysys, person_category, person),
            (contract_common, paysys, person_category, person),
            (contract_desired, paysys, person_category, person),
        }
    else:
        # Заданный контракт
        assert pcps_to_set(session, pcp_info.pcp_list) == {(contract_desired, paysys, person_category, person)}


def test_choice_disable_reason_for_offer_mismatch_is_hidden(session, client, firm, currency):
    service = create_service(session, contract_needed_client=0)

    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    person = create_person(session, client=client, person_category=person_category)

    other_contract = create_contract(session, is_signed=NOW, dt=NOW, currency=currency.num_code,
                                     payment_type=POSTPAY_PAYMENT_TYPE, services={service.id},
                                     person=person, client=client, ctype='GENERAL',
                                     commission=ContractTypeId.NON_AGENCY, personal_account=1, firm=firm.id)

    order = create_order(session, client=client, service=service)

    request = create_request(session, client=client, orders=[order], invoice_desired_contract_id=123, firm_id=firm.id)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=service.id,
                      is_agency=0)
    create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                  currency=currency.char_code, category=person_category.category,
                  group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency, price=1)

    choices = PaystepManager.create(request=request, contract=None, show_disabled_paysyses=True).payment_choices

    # Should contain only disabled choice for mismatched contract, not for offer
    assert len(choices) == 1
    hm.assert_that(
        choices[0],
        hm.has_properties({
            "contract": hm.has_properties({"id": other_contract.id}),
            "disable_reason": hm.has_properties({"name": "request_desired_contract_mismatch"}),
        })
    )


@pytest.mark.parametrize(
    'desired_contract_id, invoice_contract_id',
    [
        (-1, 123),  # Request по оферте, Invoice по контракту
        (123, 321),  # Request и Invoice на разные контракты
    ]
)
def test_warns_if_contracts_dont_match(session, client, firm, currency, switch_new_paystep_flag,
                                       desired_contract_id, invoice_contract_id):
    request = create_request(session, invoice_desired_contract_id=desired_contract_id)
    if invoice_contract_id is not None:
        contract = mapper.Contract(ContractTypes(type='GENERAL'), id=invoice_contract_id)
    else:
        contract = None

    pcp_info = get_payment_choices(request, contract=contract, skip_trust=True)

    assert pcp_info.warning == 'PAYSTEP_REQUEST_INVOICE_CONTRACT_MISMATCH'


class TestInvoiceAlteringByUser(object):
    @pytest.mark.usefixtures('switch_new_paystep_flag')
    @pytest.mark.parametrize(
        'is_owner, perms',
        [
            pytest.param(True, [None], id='owner'),
            pytest.param(False, [ALTER_INVOICE_PAYSYS], id='perm'),
            pytest.param(False, [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT], id='support'),
        ]
    )
    @pytest.mark.parametrize('with_contract', [False, False], ids=['w_contract', 'wo_contract'])
    def test_change_nds_in_invoice(self, session, client, service, currency, firm, with_contract, is_owner, perms):
        """Ни владельцы счета, ни обладатели права AlterInvoicePaysys не могут сменить способ оплаты в счете, если у них
         отличается nds"""
        order = create_order(session, client=client, service=service)
        request = create_request(session, orders=[order])
        create_price_tax_rate(session, order.product, firm.country, currency, price=1)

        service = request.request_orders[0].order.service

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list = []
        for nds in [0, 1]:
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code, nds=nds,
                                             currency=currency.char_code, category=person_category.category, instant=1,
                                             group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))

        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                          region_id=firm.country.region_id, service_id=service.id,
                          is_agency=0)
        person = create_person(session, type=person_category.category, client=request.client)
        if with_contract:

            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        else:
            contract = None
        invoice = create_invoice(session, request=request, paysys=paysys_list[0], person=person, contract=contract)
        role = create_role(session, *perms)
        create_passport(session, [(role, firm.id)], client=invoice.client if is_owner else None, patch_session=True)
        pcp_info = get_payment_choices(request, invoice, contract=contract)
        if contract:
            assert pcps_to_set(session, pcp_info.pcp_list) == {
                (contract, paysys_list[0], person_category, person)}
        else:
            assert pcps_to_set(session, pcp_info.pcp_list) == {
                (None, paysys_list[0], person_category, person)}

    @pytest.mark.usefixtures('switch_new_paystep_flag')
    @pytest.mark.parametrize(
        'is_owner, perms',
        [
            pytest.param(True, [None], id='owner'),
            pytest.param(False, [ALTER_INVOICE_PAYSYS, ISSUE_INVOICES], id='perm'),
            pytest.param(False, [ALTER_INVOICE_PAYSYS, BILLING_SUPPORT, ISSUE_INVOICES], id='support'),
        ]
    )
    def test_change_currency_in_invoice(self, firm, client, service, session, is_owner, perms):
        order = create_order(session, client=client, service=service)
        request = create_request(session, orders=[order])

        service = request.request_orders[0].order.service
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list = []
        paymethods_params = []
        for _ in range(2):
            currency = create_currency(session)
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                             currency=currency.char_code, category=person_category.category,
                                             instant=1,
                                             group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
            create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
            paymethods_params.append((currency.char_code, BANK))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=paymethods_params,
                          region_id=firm.country.region_id, service_id=service.id,
                          is_agency=0)
        person = create_person(session, type=person_category.category, client=request.client)
        invoice = create_invoice(session, request=request, paysys=paysys_list[0], person=person)
        role = create_role(session, *perms)
        create_passport(session, [(role, firm.id)], client=invoice.client if is_owner else None, patch_session=True)

        pcp_info = get_payment_choices(request)
        assert pcps_to_set(session, pcp_info.pcp_list) == {(None, frozenset(paysys_list), person_category, person),
                                                           (None, frozenset(paysys_list), person_category, None)}

        pcp_info = get_payment_choices(request, invoice)
        assert pcps_to_set(session, pcp_info.pcp_list) == {(None, paysys_list[0], person_category, person)}


class TestInstantPaysys(object):
    @pytest.mark.usefixtures('switch_new_paystep_flag')
    @pytest.mark.parametrize('invoice_paysys_instant', [1, 0], ids=['instant_invoice', 'non_instant_invoice'])
    @pytest.mark.parametrize('is_owner, additional_perms', [(True, []),
                                                            (False, [ALTER_INVOICE_PAYSYS])])
    @pytest.mark.parametrize('with_contract', [True, False])
    def test_change_paysys(self, session, with_contract, firm, currency, client, service, is_owner, additional_perms,
                           invoice_paysys_instant):
        order = create_order(session, client=client, service=service)
        request = create_request(session, orders=[order])

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list = []
        for instant in [0, 1, invoice_paysys_instant]:
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code, nds=1,
                                             currency=currency.char_code, category=person_category.category,
                                             instant=instant, group_id=0, payment_method_id=BANK, extern=1,
                                             cc='paysys_cc'))

        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                          region_id=firm.country.region_id, service_id=service.id,
                          is_agency=0)
        person = create_person(session, type=person_category.category, client=request.client)

        if with_contract:

            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        else:

            contract = None
        invoice = create_invoice(session, request=request, paysys=paysys_list[-1], person=person, contract=contract)
        perms = [ISSUE_INVOICES]
        perms.extend(additional_perms)
        role = create_role(session, *perms)
        create_passport(session, [(role, firm.id)], client=invoice.client if is_owner else None, patch_session=True)

        pcp_info = get_payment_choices(request, invoice)
        if is_owner:
            if invoice_paysys_instant:
                assert pcps_to_set(session, pcp_info.pcp_list) == {
                    (contract, frozenset(paysys_list[1:]), person_category, person)}
            else:
                # не ошибка, для счета с не мгновенным способом оплаты нельзя перейти на страницу изменения
                assert pcps_to_set(session, pcp_info.pcp_list) == {
                    (contract, paysys_list[-1], person_category, person)}
        else:
            assert pcps_to_set(session, pcp_info.pcp_list) == {
                (contract, frozenset(paysys_list), person_category, person)}

    @pytest.mark.usefixtures('switch_new_paystep_flag')
    @pytest.mark.permission
    @pytest.mark.parametrize('with_contract', [True, False])
    @pytest.mark.parametrize('additional_perms', [[], [ADMIN_ACCESS]])
    def test_check_fraud_status(self, session, firm, client, with_contract, currency, additional_perms):
        """без права AdminAccess фродеры не могут использовать мгновенные способы оплаты"""

        request = create_request(session, client=client)
        request.client.fraud_status = ClientFraudStatus(client=request.client)
        request.client.fraud_status.fraud_flag = 1
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list = []
        for instant in [0, 1]:
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                             currency=currency.char_code, category=person_category.category,
                                             group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                             instant=instant))
        service = request.request_orders[0].order.service
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                          region_id=firm.country.region_id, service_id=service.id,
                          is_agency=0)
        create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
        person = create_person(session, type=person_category.category, client=request.client)
        if with_contract:

            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        else:
            contract = None
        perms = [ISSUE_INVOICES]
        perms.extend(additional_perms)
        role = create_role(session, *perms)
        create_passport(session, [(role, firm.id)], client=None, patch_session=True)
        pcp_info = get_payment_choices(request)

        expected_paysyses = frozenset(paysys_list) if ADMIN_ACCESS in perms else paysys_list[0]
        if with_contract:
            assert pcps_to_set(session, pcp_info.pcp_list) == {
                (contract, expected_paysyses, person_category, person)}
        else:
            assert pcps_to_set(session, pcp_info.pcp_list) == {
                (contract, expected_paysyses, person_category, person),
                (contract, expected_paysyses, person_category, None),
            }
