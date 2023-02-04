# -*- coding: utf-8 -*-
import pytest
import datetime
import hamcrest as hm
from mock import patch

from balance import mapper
from balance.paystep import (
    get_paysyses,
    PaystepNS,
    PaystepManager,
)
from balance.paystep.payments import PaymentChoice
from billing.contract_iface.constants import ContractTypeId
from balance.constants import (
    PREPAY_PAYMENT_TYPE,
    POSTPAY_PAYMENT_TYPE,
    SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
    SPECIAL_PROJECTS_COMMISSION_TYPE,
    ServiceId,
    RegionId,
    PaysysGroupIDs,
)

from balance import exc
from balance import muzzle_util as ut

from tests import object_builder as ob
from tests.balance_tests.paystep.paystep_common import (
    create_client_service_data,
    create_paysys,
    create_paysys_simple,
    create_request,
    create_person,
    create_client,
    create_firm,
    create_country,
    create_invoice,
    create_person_category,
    create_pay_policy,
    create_currency,
    create_price_tax_rate,
    create_order,
    create_product,
    create_service,
    create_manager,
    create_passport,
    create_currency_product,
    create_valid_tax,
    create_currency_rate,
    create_price,
    create_role,
    create_contract,
    USE_ADMIN_PERSONS,
    ADMIN_ACCESS,
    ISSUE_INVOICES,
    BILLING_SUPPORT,
    PAYSTEP_VIEW,
    BANK,
    CARD,
    SINGLE_ACCOUNT,
    ddict2dict,
    PermType,
    get_client_permission,
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

COMMISSION_TYPE_1 = 1
NOW = datetime.datetime.now()


def test_empty_result(session, client):
    """пустой список, если ни одного способа оплаты подобрать не удалось"""
    request_ = create_request(session, client=client)
    ns = PaystepNS(request=request_)
    assert get_paysyses(ns) == []
    assert ddict2dict(ns.paysyses_denied) == {(None, None): {}}


def test_force_paysys_not_found(session, client):
    """Если способ оплаты был передан явно и он не в списке подобранных/подобранных способов оплаты нет,
    выкидываем исключение"""
    request_ = create_request(session, client=client)
    paysys = create_paysys(session)
    with pytest.raises(exc.INVALID_PAYSYS) as exc_info:
        get_paysyses(PaystepNS(request=request_, paysys=paysys))
    assert exc_info.value.msg == 'Unavailable paysys {}'.format(paysys)


def test_force_paysys_is_available(session, firm, currency, client):
    """Если способ оплаты был передан явно и он в списке подобранных/подобранных способов оплаты,
    возвращаем"""
    request_ = create_request(session, client=client, firm_id=firm.id)
    service = request_.request_orders[0].order.service
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for payment_method_id in [CARD, BANK]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=payment_method_id, extern=1,
                                         cc='paysys_{}'.format(payment_method_id)))

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK),
                                                                                (currency.char_code, CARD)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    ns = PaystepNS(request=request_, paysys=paysys_list[0])
    assert get_paysyses(ns, None) == [paysys_list[0]]
    assert ddict2dict(ns.paysyses_denied) == {(None, None): {}}


def test_invalid_product_currency(session, client):
    """если валюта продукта в заказе не согласуется с валютой клиента, выбрасываем исключение"""
    request_ = create_request(session, client=client)
    request_.request_orders[0].order.product_currency = 'USD'
    create_client_service_data(client=request_.client,
                               service_id=request_.request_orders[0].order.service.id,
                               migrate_to_currency_dt=NOW - datetime.timedelta(hours=1))

    with pytest.raises(exc.INVALID_PRODUCT_CURRENCY) as exc_info:
        get_paysyses(PaystepNS(request=request_))
    assert exc_info.value.msg == 'invalid product currency USD. (client currency is RUB)'


@pytest.mark.parametrize('with_contract', [True, False])
def test_base(session, firm, currency, client, with_contract):
    """по договору и без, возвращаем список способо оплаты, удовлетворяющих условию"""
    request_ = create_request(session, client=client, firm_id=firm.id)
    service = request_.request_orders[0].order.service
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    ns = PaystepNS(request=request_)
    assert get_paysyses(ns, contract) == [paysys]
    assert ddict2dict(ns.paysyses_denied) == {(None, contract): {}}


def test_filter_by_ns_currency(session, firm, client):
    """Если валюта способа оплаты была передана явно, фильтруем способы оплаты по ней"""
    request_ = create_request(session, client=client)
    # firm = create_firm(session, country=create_country(session))
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    currency_1 = create_currency(session)
    currency_2 = create_currency(session)
    service = request_.request_orders[0].order.service
    paysys_list = []
    for currency in [currency_1, currency_2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(paysys_list[0].currency, BANK),
                                                                                (paysys_list[1].currency, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    assert set(get_paysyses(PaystepNS(request=request_))) == set(paysys_list)

    ns = PaystepNS(request=request_, iso_currency=currency_1.iso_code)
    assert get_paysyses(ns) == [paysys_list[0]]

    expected_denied_reason = {
        (None, None): {paysys_list[1]: 'paysys.iso_currency != ns.iso_currency ({})'.format(currency_1.char_code)}}
    assert ddict2dict(ns.paysyses_denied) == expected_denied_reason


def test_filter_by_contract_currency(session, firm, client):
    """способы оплаты фильтруем по валюте договора"""
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    currency_1 = create_currency(session)
    currency_2 = create_currency(session)
    service = request_.request_orders[0].order.service
    paysys_list = []
    for currency in [currency_1, currency_2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(paysys_list[0].currency, BANK),
                                                                                (paysys_list[1].currency, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    person = create_person(session, type=person_category.category, client=request_.client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person, currency=currency_1.num_code)

    assert get_paysyses(PaystepNS(request=request_), contract) == [paysys_list[0]]


def test_filter_by_person_region_id(session, client, currency):
    """если регион и признак резидентства плательщика были переданы явно, фильтруем способы оплаты по ним"""
    request_ = create_request(session, client=client)
    firm_1 = create_firm(session, country=create_country(session))
    firm_2 = create_firm(session, country=create_country(session))
    paysys_list = []
    for firm in [firm_1, firm_2]:
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                       is_agency=0)
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    assert set(get_paysyses(PaystepNS(request=request_,
                                      person_region_id=firm_1.country.region_id))) == set(paysys_list)
    ns = PaystepNS(request=request_,
                   person_region_id=firm_1.country.region_id,
                   person_resident=1)
    assert get_paysyses(ns) == [paysys_list[0]]
    expected_denied_reason = {(None, None): {paysys_list[1]: 'paysys country_resident does not match region {0} '
                                                             'resident 1'.format(firm_1.country.region_id, )}}
    assert ddict2dict(ns.paysyses_denied) == expected_denied_reason


def test_filter_by_legal_entity(session, firm, client,currency):
    """если статус юридического лица плательщика был передан явно, фильтруем способы оплаты по нему """
    request_ = create_request(session, client=client)
    person_category_1 = create_person_category(session, country=firm.country, ur=0, resident=1)
    person_category_2 = create_person_category(session, country=firm.country, ur=1, resident=1)
    paysys_list = []
    for person_category in [person_category_1, person_category_2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))

        create_pay_policy(session, firm_id=firm.id, legal_entity=person_category.ur,
                       paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                       is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    assert set(get_paysyses(PaystepNS(request=request_))) == set(paysys_list)
    ns = PaystepNS(request=request_, person_legal_entity=1)
    assert get_paysyses(ns) == [paysys_list[1]]
    expected_denied_reason = {(None, None): {paysys_list[0]: 'paysys category does not match filter ur = 1'}}
    assert ddict2dict(ns.paysyses_denied) == expected_denied_reason


@pytest.mark.parametrize('commission_type', [ContractTypeId.COMMISSION,
                                             ContractTypeId.WITHOUT_PARTICIPATION,
                                             ContractTypeId.OFD_WITHOUT_PARTICIPATION,
                                             ContractTypeId.WHOLESALE_AGENCY_AWARD,
                                             ContractTypeId.KZ_COMMISSION])
def test_paysys_need_contract_w_contract(session, firm, client, currency, commission_type):
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    service = request_.request_orders[0].order.service
    for cc in ['cc_ur', 'cc_kz_jp']:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc=cc))

        create_pay_policy(session, firm_id=firm.id, legal_entity=person_category.ur,
                       paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=service.id,
                       is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    person = create_person(session, type=person_category.category, client=request_.client)
    contract = create_contract(session, commission=commission_type, firm=firm.id,
                               client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    ns = PaystepNS(request=request_)
    assert get_paysyses(ns, contract) == []
    assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[0]: 'paysys needs contract',
                                                                 paysys_list[1]: 'paysys needs contract'}}


@pytest.mark.parametrize('with_contract', [True, False])
def test_filter_by_paymethod(session, firm, client, currency, with_contract):
    """если метод оплаты был передан явно, фильтруем способы оплаты по нему"""
    request_ = create_request(session, client=client)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    service = request_.request_orders[0].order.service
    for payment_method_id in [CARD, BANK]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=payment_method_id, extern=1, cc='paysys_cc'))

    create_pay_policy(session, firm_id=firm.id, legal_entity=person_category.ur,
                   paymethods_params=[(currency.char_code, BANK),
                                      (currency.char_code, CARD), ],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None

    assert set(get_paysyses(PaystepNS(request=request_), contract)) == set(paysys_list)
    ns = PaystepNS(request=request_, payment_methods=['bank'])
    assert get_paysyses(ns, contract) == [paysys_list[1]]
    expected_denied_reason = {(None, contract): {paysys_list[0]: 'paysys payment_method_cc not in [\'bank\']'}}
    assert ddict2dict(ns.paysyses_denied) == expected_denied_reason


def test_forced_firm(session, currency, client):
    """если фирма была передана явно, фильтруем способы оплаты по ней"""
    firm_1 = create_firm(session, country=create_country(session))
    firm_2 = create_firm(session, country=create_country(session))
    request_ = create_request(session, client=client, firm_id=firm_1.id)
    paysys_list = []
    for firm in [firm_1, firm_2]:
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                       is_agency=0)
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    ns = PaystepNS(request=request_)
    assert get_paysyses(ns) == [paysys_list[0]]
    expected_denied_reason = {(None, None): {paysys_list[1]: 'forced firm {}'.format(firm_1.id)}}
    assert ddict2dict(ns.paysyses_denied) == expected_denied_reason


@pytest.mark.parametrize('product_commission_types, is_paysys_filtered_out',
                         [((SPECIAL_PROJECTS_COMMISSION_TYPE,
                            SPECIAL_PROJECTS_COMMISSION_TYPE), False),

                          ((SPECIAL_PROJECTS_COMMISSION_TYPE,
                            COMMISSION_TYPE_1), True),

                          ((SPECIAL_PROJECTS_MARKET_COMMISSION_TYPE,
                            COMMISSION_TYPE_1), True),

                          ((COMMISSION_TYPE_1,
                            COMMISSION_TYPE_1), False),
                          ])
def test_products_commissions_wo_contract(session, firm, client, currency, service, product_commission_types,
                                          is_paysys_filtered_out):
    """при получении способов оплаты без договора возвращаем пустой список способов оплат, если
    в реквесте есть хотя бы один заказ на продукт с типом комиссии 17 или 33. Однако если все типы комиссии в
    реквесте 17, получаем способы оплаты как обычно. """
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    orders = []
    for commission_type in product_commission_types:
        product = create_product(session, commission_type=commission_type)
        orders.append(create_order(session, client, product, service))
        create_price_tax_rate(session, product, firm.country, currency)
    request_ = create_request(session, client=client, orders=orders)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    if is_paysys_filtered_out:
        ns = PaystepNS(request=request_)
        assert get_paysyses(ns) == []
        assert ddict2dict(ns.paysyses_denied) == {(None, None): {paysys: 'incompatible product commission'}}
    else:
        assert get_paysyses(PaystepNS(request=request_)) == [paysys]


@pytest.mark.parametrize('with_contract', [True, False])
def test_incompatible_offer(session, firm, client, currency, product, with_contract):
    """при получении способов оплаты без договора, проверяем правила оферт, без договора - не проверяем"""
    create_price_tax_rate(session, product, firm.country, currency)
    service = ob.Getter(mapper.Service, 98).build(session).obj
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    request_ = create_request(session, client=client, orders=[create_order(session,
                                                                           client=client,
                                                                           product=product,
                                                                           service=service)])
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    if with_contract:

        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    ns = PaystepNS(request=request_)
    result = get_paysyses(ns, contract)
    if with_contract:
        assert result == [paysys]
    else:
        assert result == []
        denied_reasons = set(ddict2dict(ns.paysyses_denied)[(None, None)].values())
        assert len(denied_reasons) == 1
        assert next(iter(denied_reasons)).startswith('incompatible offer - Offer is prohibited by rule number ')


@pytest.mark.moderation
@pytest.mark.parametrize('with_contract', [True, False])
def test_unmoderated(session, firm, client, currency, service, with_contract):
    """если реквест непромодерирован, вызываем исключение"""
    request_ = create_request(session, client=client,
                              orders=[create_order(session,
                                                   client=client,
                                                   product=create_product(session),
                                                   service=service)])
    create_price_tax_rate(session, request_.request_orders[0].product, firm.country, currency)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                  currency=currency.char_code, category=person_category.category,
                  group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    request_.request_orders[0].order.unmoderated = 1
    if with_contract:

        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        get_paysyses(PaystepNS(request=request_), contract)
    assert exc_info.value.msg == 'bad unmoderated request'


@pytest.mark.moderation
@pytest.mark.parametrize('with_contract', [True, False])
def test_unmoderated_valid_moderated_request(session, firm, client, currency, product, with_contract):
    """если реквест непромодерирован, но позволяет оплату до модерации, возвращаем только способы оплаты с признаком
    allow_unmoderated = 1"""
    request_ = create_request(session, client=client,
                              orders=[create_order(session,
                                                   client=client,
                                                   product=product,
                                                   service=ob.Getter(mapper.Service, ServiceId.DIRECT),
                                                   manager=None)])
    create_price_tax_rate(session, request_.request_orders[0].product, firm.country, currency)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for allow_unmoderated in [False, True]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                         allow_unmoderated=allow_unmoderated))
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    request_.request_orders[0].order.unmoderated = 1
    if with_contract:

        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    ns = PaystepNS(request=request_)
    assert get_paysyses(ns, contract) == [paysys_list[1]]
    assert ddict2dict(ns.paysyses_denied)[(None, contract)][paysys_list[0]] == 'need allow_unmoderated'


@pytest.mark.parametrize('with_contract', [True, False])
def test_mobile(session, firm, currency, client, with_contract):
    """признак mobile позволяет фильтровать способы оплаты по признаку mobile"""
    request_ = create_request(session, client=client, firm_id=firm.id)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for mobile in [0, 1]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                         mobile=mobile))
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    assert set(get_paysyses(PaystepNS(request=request_, mobile=False))) == set(paysys_list)
    ns = PaystepNS(request=request_, mobile=True)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    assert get_paysyses(ns, contract) == [paysys_list[1]]
    assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[0]: 'need paysys.mobile'}}


@pytest.mark.parametrize(
    'additional_perm_type, ans',
    [
        pytest.param(PermType.w_perm, True, id='w perm'),
        pytest.param(PermType.wo_perm, False, id='wo perm'),
        pytest.param(PermType.w_right_client, True, id='w right client'),
        pytest.param(PermType.w_wrong_client, False, id='w wrong client'),
    ],
)
@pytest.mark.parametrize('with_contract', [True, False])
def test_paysys_cc(session, firm, currency, client, service, with_contract, additional_perm_type, ans):
    perms = [ISSUE_INVOICES]
    additional_perm = get_client_permission(session, additional_perm_type, USE_ADMIN_PERSONS, client)
    if additional_perm:
        perms.append(additional_perm)
    role = create_role(session, *perms)
    create_passport(session, [role], patch_session=True)
    request_ = create_request(session,
                              orders=[create_order(session,
                                                   client=client,
                                                   service=service)])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for cc in ['paysys_cc', 'ex', 'ex_doc']:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc=cc,
                                         mobile=1))
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    ns = PaystepNS(request=request_, mobile=True)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None

    if ans:
        assert set(get_paysyses(ns, contract)) == set(paysys_list)
    else:
        assert get_paysyses(ns, contract) == [paysys_list[0]]
        assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[1]: 'need has_alter_perm',
                                                                     paysys_list[2]: 'need has_alter_perm'}}


@pytest.mark.parametrize('with_contract', [True, False])
def test_skip_trust(session, firm, currency, client, with_contract, switch_new_paystep_flag):
    """по умолчанию отфильтровываем трастовые способы оплаты"""
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=PaysysGroupIDs.trust, payment_method_id=BANK, extern=1, cc='paysys_cc')
    request_ = create_request(session, client=client, firm_id=paysys.firm.id)
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=paysys.firm.id, legal_entity=0,
                   paymethods_params=[(paysys.iso_currency, paysys.payment_method_id)],
                   region_id=paysys.firm.country.region_id, service_id=service.id,
                   is_agency=0, paysys_group_id=1)
    create_price_tax_rate(session, request_.request_orders[0].order.product, paysys.firm.country,
                          paysys.currency_mapper)

    assert set(get_paysyses(PaystepNS(request=request_, skip_trust=False))) == {paysys}
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    if switch_new_paystep_flag:
        choices = PaystepManager.create(request=request_, contract=contract, skip_trust=True).payment_choices
        assert choices == []
        choices = PaystepManager.create(request=request_, contract=contract, skip_trust=False).payment_choices
        hm.assert_that(
            choices,
            hm.contains_inanyorder(
                PaymentChoice.create(contract, contract and contract.person, paysys),
            ),
        )
    else:
        ns = PaystepNS(request=request_)
        assert get_paysyses(ns, contract) == []
        assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys: 'skip_trust'}}
        assert get_paysyses(PaystepNS(request=request_, skip_trust=False), contract) == [paysys]


@pytest.mark.permissions
@pytest.mark.parametrize('with_contract', [True, False])
@pytest.mark.parametrize('additional_perms', [[ADMIN_ACCESS], []])
def test_is_agency_with_admin_access(session, firm, currency, additional_perms, with_contract, switch_new_paystep_flag):
    """без права AdminAccess агентства могут выставляться по способам оплаты с признаком for_agency = 1,
     у клиентов нет такого ограничения"""
    perms = [ISSUE_INVOICES, BILLING_SUPPORT]
    perms.extend(additional_perms)
    create_passport(session, [create_role(session, *perms)], patch_session=True)

    request_ = create_request(session, client=create_client(session, is_agency=1))
    service = request_.request_orders[0].order.service
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for for_agency in [0, 1]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                         for_agency=for_agency))

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=1)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None

    if ADMIN_ACCESS not in perms:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract,
                                            show_disabled_paysyses=True).payment_choices
            hm.assert_that(
                choices,
                hm.contains_inanyorder(
                    PaymentChoice.create(contract, contract and contract.person, paysys_list[1]),
                    hm.has_properties({
                        'contract': None,
                        'paysys': hm.has_properties({'id': paysys_list[0].id}),
                        'disable_reason': hm.has_properties({'name': 'not_agency_paysys'}),
                    }),
                ),
            )
        else:
            ns = PaystepNS(request=request_)
            assert get_paysyses(ns, contract) == [paysys_list[1]]
            assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[0]: 'not agency paysys'}}
    else:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract).payment_choices
            hm.assert_that(
                choices,
                hm.contains_inanyorder(*[
                    PaymentChoice.create(contract, contract and contract.person, paysys)
                    for paysys in paysys_list
                ]),
            )
        else:
            assert set(get_paysyses(PaystepNS(request=request_), contract)) == set(paysys_list)


@pytest.mark.permissions
@pytest.mark.parametrize('with_contract', [True, False])
@pytest.mark.parametrize('additional_perms', [[ADMIN_ACCESS], []])
def test_check_fraud_status(session, firm, client, currency, with_contract, additional_perms, switch_new_paystep_flag):
    """без права AdminAccess фродеры не могут использовать мгновенные способы оплаты"""
    perms = [ISSUE_INVOICES, PAYSTEP_VIEW]
    perms.extend(additional_perms)
    create_passport(session, [create_role(session, *perms)], patch_session=True)

    request_ = create_request(session, client=client)
    request_.client.fraud_status = mapper.ClientFraudStatus(client=request_.client)
    request_.client.fraud_status.fraud_flag = 1
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for instant in [0, 1]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                         instant=instant))
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    if ADMIN_ACCESS not in perms:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract, show_disabled_paysyses=True).payment_choices
            hm.assert_that(
                choices,
                hm.contains_inanyorder(
                    PaymentChoice.create(contract, contract and contract.person, paysys_list[0]),
                    hm.has_properties({
                        'contract': None,
                        'paysys': hm.has_properties({'id': paysys_list[1].id}),
                        'disable_reason': hm.has_properties({'name': 'denied_instant_payments_for_fraud'}),
                    }),
                ),
            )
        else:
            ns = PaystepNS(request=request_)
            assert get_paysyses(ns, contract) == [paysys_list[0]]
            assert ddict2dict(ns.paysyses_denied) == {
                (None, contract): {paysys_list[1]: 'denied instant payments for client'}}
    else:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract).payment_choices
            hm.assert_that(
                choices,
                hm.contains_inanyorder(*[
                    PaymentChoice.create(contract, contract and contract.person, paysys)
                    for paysys in paysys_list
                ]),
            )
        else:
            hm.assert_that(
                get_paysyses(PaystepNS(request=request_), contract),
                hm.contains_inanyorder(*paysys_list),
            )


class TestPaysysDoesNotMatchRegion(object):
    @pytest.mark.parametrize('with_contract', [True, False])
    @pytest.mark.parametrize(
        'additional_perm_type, ans',
        [
            pytest.param(PermType.w_perm, True, id='w perm'),
            pytest.param(PermType.wo_perm, False, id='wo perm'),
            pytest.param(PermType.w_right_client, True, id='w right client'),
            pytest.param(PermType.w_wrong_client, False, id='w wrong client'),
        ],
    )
    def test_paysys_from_netherlands(self, session, currency, client, service, with_contract, additional_perm_type, ans, switch_new_paystep_flag):
        firm = create_firm(session, country=ob.Getter(mapper.Country, RegionId.NETHERLANDS).build(session).obj)
        perms = [ISSUE_INVOICES, BILLING_SUPPORT]
        additional_perm = get_client_permission(session, additional_perm_type, USE_ADMIN_PERSONS, client)
        if additional_perm:
            perms.append(additional_perm)
        create_passport(session, [create_role(session, *perms)], patch_session=True)

        request_ = create_request(session,
                                  orders=[create_order(session,
                                                       client=client,
                                                       service=service)])

        paysys_list = []
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        _get_base_paysyses_routing_patch = patch('balance.paystep.get_base_paysyses_routing',
                                                 return_value=paysys_list[:])
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=service.id,
                       is_agency=0)
        if with_contract:
            person = create_person(session, type=person_category.category, client=request_.client)
            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        else:
            contract = None

        with _get_base_paysyses_routing_patch:
            ns = PaystepNS(request=request_)
            if with_contract or ans:
                if switch_new_paystep_flag:
                    choices = PaystepManager.create(request=request_, contract=contract).payment_choices
                    hm.assert_that(
                        choices,
                        hm.contains_inanyorder(
                            PaymentChoice.create(contract, contract and contract.person, paysys_list[0]),
                        ),
                    )
                else:
                    assert get_paysyses(ns, contract) == [paysys_list[0]]
            else:
                if switch_new_paystep_flag:
                    choices = PaystepManager.create(request=request_, contract=contract,
                                                    show_disabled_paysyses=True).payment_choices
                    hm.assert_that(
                        choices,
                        hm.contains_inanyorder(
                            hm.has_properties({
                                'contract': contract,
                                'paysys': hm.has_properties({'id': paysys_list[0].id}),
                                'disable_reason': hm.has_properties({'name': 'wrong_region_for_non_resident'}),
                            }),
                        ),
                    )
                else:
                    assert set(get_paysyses(ns, contract)) == set()
                    assert ddict2dict(ns.paysyses_denied) == {
                        (None, None): {paysys_list[0]: 'paysys does not match region'}}

    @pytest.mark.permission
    @pytest.mark.parametrize('with_contract', [True, False])
    @pytest.mark.parametrize('can_issue_initial_invoice', [True, False])
    def test_paysys_from_netherlands_can_issue_initial_invoice(self, session, currency, service, with_contract,
                                                               can_issue_initial_invoice, switch_new_paystep_flag):
        firm = create_firm(session, country=ob.Getter(mapper.Country, RegionId.NETHERLANDS).build(session).obj)
        create_passport(session, [create_role(session, ISSUE_INVOICES, BILLING_SUPPORT)], patch_session=True)

        request_ = create_request(session,
                                  orders=[create_order(session,
                                                       client=create_client(session,
                                                                            can_issue_initial_invoice=can_issue_initial_invoice),
                                                       service=service)])

        paysys_list = []
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        _get_base_paysyses_routing_patch = patch('balance.paystep.get_base_paysyses_routing',
                                                 return_value=paysys_list[:])
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=service.id,
                       is_agency=0)
        if with_contract:
            person = create_person(session, type=person_category.category, client=request_.client)
            contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                       client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                       services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
        else:
            contract = None

        with _get_base_paysyses_routing_patch:
            ns = PaystepNS(request=request_)
            if with_contract or can_issue_initial_invoice:
                if switch_new_paystep_flag:
                    choices = PaystepManager.create(request=request_, contract=contract).payment_choices
                    hm.assert_that(
                        choices,
                        hm.contains_inanyorder(
                            PaymentChoice.create(contract, contract and contract.person, paysys_list[0]),
                        ),
                    )
                else:
                   assert get_paysyses(ns, contract) == [paysys_list[0]]
            else:
                if switch_new_paystep_flag:
                    choices = PaystepManager.create(request=request_, contract=contract, show_disabled_paysyses=True).payment_choices
                    hm.assert_that(
                        choices,
                        hm.contains_inanyorder(
                            hm.has_properties({
                                'contract': contract,
                                'paysys': hm.has_properties({'id': paysys_list[0].id}),
                                'disable_reason': hm.has_properties({'name': 'wrong_region_for_non_resident'}),
                            }),
                        ),
                    )
                else:
                    assert set(get_paysyses(ns, contract)) == set()
                    assert ddict2dict(ns.paysyses_denied) == {
                        (None, None): {paysys_list[0]: 'paysys does not match region'}}


@pytest.mark.permissions
@pytest.mark.parametrize('with_contract', [True, False])
@pytest.mark.parametrize('additional_perms', [[ADMIN_ACCESS], []])
def test_deny_cc(session, firm, currency, with_contract, additional_perms, switch_new_paystep_flag):
    """без права AdminAccess если клиенту запрещена оплаты кредитными картами, фильтруем способы оплаты по признаку
    credit_card"""
    perms = [ISSUE_INVOICES]
    perms.extend(additional_perms)
    role = create_role(session, *perms)
    create_passport(session, [role], patch_session=True)
    request_ = create_request(session, client=create_client(session, deny_cc=1))
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for credit_card in [0, 1]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc',
                                         credit_card=credit_card))
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    if ADMIN_ACCESS not in perms:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract, show_disabled_paysyses=True).payment_choices
            hm.assert_that(
                choices,
                hm.assert_that(
                    choices,
                    hm.contains_inanyorder(
                        PaymentChoice.create(contract, contract and contract.person, paysys_list[0]),
                    ),
                )
            )
        else:
            ns = PaystepNS(request=request_)
            assert get_paysyses(ns, contract) == [paysys_list[0]]
            assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[1]: 'denied credit cards for client'}}
    else:
        if switch_new_paystep_flag:
            choices = PaystepManager.create(request=request_, contract=contract).payment_choices
            hm.assert_that(
                choices,
                hm.contains_inanyorder(*[
                    PaymentChoice.create(contract, contract and contract.person, paysys)
                    for paysys in paysys_list
                ]),
            )
        else:
            assert get_paysyses(PaystepNS(request=request_), contract) == paysys_list


def test_filter_iso_currency(session, firm, client, switch_new_paystep_flag):
    """для валютных продуктов, если с каким-нибудь payment_method_id можно платить в нескольких валютах,
    включая валюту продукта, фильтруем способы оплаты по payment_method_id и валюте продукта"""
    currency_1 = create_currency(session)
    currency_2 = create_currency(session)
    product = create_currency_product(session, iso_currency=currency_1.iso_code)
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for currency in [currency_1, currency_2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(paysys_list[0].currency, BANK),
                                                                                (paysys_list[1].currency, BANK)],
                   region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                   is_agency=0)
    ns = PaystepNS(request=request_)
    if switch_new_paystep_flag:
        choices = PaystepManager.create(request=request_, show_disabled_paysyses=True).payment_choices
        hm.assert_that(
            choices,
            hm.contains_inanyorder(
                PaymentChoice.create(None, None, paysys_list[0]),
                hm.has_properties({
                    'contract': None,
                    'paysys': hm.has_properties({'id': paysys_list[1].id}),
                    'disable_reason': hm.has_properties({'name': 'filter_product_iso_currency'}),
                }),
            ),
        )
    else:
        assert get_paysyses(ns) == [paysys_list[0]]
        expected_denied_reason = 'paysys.iso_currency != ns.filter_iso_currency ({})'.format(product.product_currency)
        assert ddict2dict(ns.paysyses_denied) == {(None, None): {paysys_list[1]: expected_denied_reason}}


def test_filter_iso_currency_uzs(session, client, firm, switch_new_paystep_flag):
    """В некоторых валютах, в которой никогда не будет мультивалютности,
    можно выставляться даже мультивалютным клиентам."""
    currency_1 = create_currency(session)
    currency_2 = ob.Getter(mapper.Currency, 'UZS').build(session).obj
    product = create_currency_product(session, iso_currency=currency_1.iso_code)
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    for currency in [currency_1, currency_2]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(paysys_list[0].currency, BANK),
                                                                                (paysys_list[1].currency, BANK)],
                   region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                   is_agency=0)
    if switch_new_paystep_flag:
        choices = PaystepManager.create(request=request_).payment_choices
        hm.assert_that(
            choices,
            hm.contains_inanyorder(*[
                PaymentChoice.create(None, None, paysys)
                for paysys in paysys_list
            ]),
        )
    else:
        assert set(get_paysyses(PaystepNS(request=request_))) == set(paysys_list)


def test_strict_mode_payment_methods(session, client, firm, switch_new_paystep_flag):
    """если для какого-то payment_method`а нет возможности оплаты в валюте продукта, то разрешено
     платить в любой валюте"""
    currency_1 = create_currency(session)
    currency_2 = create_currency(session)
    product = create_currency_product(session, iso_currency=currency_1.iso_code)
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency_2.iso_code,
                           currency=currency_2.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency_2)

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency_2.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                   is_agency=0)
    if switch_new_paystep_flag:
        choices = PaystepManager.create(request=request_).payment_choices
        hm.assert_that(
            choices,
            hm.contains_inanyorder(
                PaymentChoice.create(None, None, paysys),
            ),
        )
    else:
        assert get_paysyses(PaystepNS(request=request_)) == [paysys]


def test_strict_mode_payment_methods_w_contract(session, client, firm, switch_new_paystep_flag):
    """если для какого-то payment_method`а нет возможности оплаты в валюте продукта, то разрешено
     платить в любой валюте.
     при это формирование списка запрещенных метоов оплаты должно быть изолировано в рамках договора."""
    currency_1 = create_currency(session)
    currency_2 = create_currency(session)
    product = create_currency_product(session, iso_currency=currency_1.iso_code)
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    service = order.service
    balance_service = service.balance_service
    balance_service.contract_needed_client = 0
    session.flush()

    person_category_ph = create_person_category(session, country=firm.country, ur=0, resident=1)
    person_category_ur = create_person_category(session, country=firm.country, ur=1, resident=1)

    paysys1 = create_paysys(session, firm=firm, iso_currency=currency_1.iso_code,
                           currency=currency_1.char_code, category=person_category_ph.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    paysys2 = create_paysys(session, firm=firm, iso_currency=currency_2.iso_code,
                           currency=currency_2.char_code, category=person_category_ur.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

    person_ph = create_person(session, type=person_category_ph.category, client=request_.client)
    person_ur = create_person(session, type=person_category_ur.category, client=request_.client)

    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service.id}, is_signed=NOW, person=person_ur, currency=currency_2.num_code)

    create_price_tax_rate(session, product, firm.country, currency_1)
    create_price_tax_rate(session, product, firm.country, currency_2)
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency_1.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id, is_contract=0,
                   is_agency=0)
    create_pay_policy(session, firm_id=firm.id, legal_entity=1, paymethods_params=[(currency_2.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id, is_contract=1,
                   is_agency=0)

    if switch_new_paystep_flag:
        choices = PaystepManager.create(request=request_).payment_choices
        hm.assert_that(
            choices,
            hm.contains_inanyorder(
                PaymentChoice.create(None, person_ph, paysys1),
                PaymentChoice.create(None, None, paysys1),
                PaymentChoice.create(contract, person_ur, paysys2),
            ),
        )
    else:
        assert get_paysyses(PaystepNS(request=request_)) == [paysys1]
        assert get_paysyses(PaystepNS(request=request_), contract=contract) == [paysys2]


@pytest.mark.parametrize('with_contract', [True, False])
def test_no_product_price_on_date(session, firm, client, product, currency, with_contract):
    """если для валюты способа оплаты не известна цена продукта, отфильтровываем такой способ оплаты"""
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)
    _, _, tax_policy_pct = create_valid_tax(session, product=product,
                                            currency=currency, country=firm.country, dt=NOW)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    assert get_paysyses(PaystepNS(request=request_), contract) == []
    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW, tax_policy_pct=tax_policy_pct)
    assert get_paysyses(PaystepNS(request=request_), contract) == [paysys]


@pytest.mark.parametrize('with_contract', [True, False])
def test_no_product_tax_on_date(session, firm, currency, client, product, with_contract):
    """если в регионе категории плательщика из способа оплаты не известен налог,
     отфильтровываем такой способ оплаты"""
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)

    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    assert get_paysyses(PaystepNS(request=request_), contract) == []
    _, _, tax_policy_pct = create_valid_tax(session, product=product,
                                            currency=currency, country=firm.country, dt=NOW)
    assert get_paysyses(PaystepNS(request=request_), contract) == [paysys]


@pytest.mark.parametrize('with_contract', [True, False])
def test_invalid_missed_currency_rate(session, firm, currency, client, product, with_contract):
    """если нет курса валюты к рубли, отфильтровываем такой способ оплаты"""
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0)

    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW)

    _, _, tax_policy_pct = create_valid_tax(session, product=product,
                                            currency=currency, country=firm.country, dt=NOW)
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    assert get_paysyses(PaystepNS(request=request_), contract) == []
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    assert get_paysyses(PaystepNS(request=request_), contract) == [paysys]


@pytest.mark.parametrize('with_contract', [True, False])
def test_need_trust_api_payment_methods(session, firm, with_contract, currency, client, product):
    """по умолчанию не возвращаем способы оплаты для траста как процессинга. с флагом need_trust_api
    пытаемся получить trust_paymethods, если их нет, отфильтровываем такой способ оплаты"""
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys_list = []
    service = request_.request_orders[0].order.service
    for group_id in [PaysysGroupIDs.default, PaysysGroupIDs.auto_trust]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         group_id=group_id, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=service.id,
                       is_agency=0, paysys_group_id=group_id)

    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    ns = PaystepNS(request=request_,
                   need_trust_api=False,
                   need_trust_api_payment_methods=True)
    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods', return_value=[])
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None

    with patch_get_payment_methods:
        assert get_paysyses(ns, contract) == [paysys_list[0]]
    assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[1]: 'skip trust_api paysyses'}}
    ns.need_trust_api = True
    with patch_get_payment_methods:
        assert get_paysyses(ns, contract) == [paysys_list[0]]
    assert ddict2dict(ns.paysyses_denied) == {(None, contract): {paysys_list[1]: 'trust payments for this '
                                                                                 'paysys not available'}}


@pytest.mark.parametrize('with_contract', [True, False])
def test_need_trust_api_payment_methods_w_auto_trust(session, firm, with_contract, client, currency, product):
    """c флагом need_trust_api_payment_methods для способов оплаты для траста как процессинга получаем
    trust_paymethods, с флагом need_trust_api и/или need_trust_api_payment_methods, если trust_paymethods есть,
     оставляем такой способ оплаты"""
    order = create_order(session, client=client, product=product)
    request_ = create_request(session, orders=[order])
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=PaysysGroupIDs.auto_trust, payment_method_id=BANK, extern=1, cc='paysys_cc')
    service = request_.request_orders[0].order.service
    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                   region_id=firm.country.region_id, service_id=service.id,
                   is_agency=0, paysys_group_id=PaysysGroupIDs.auto_trust)

    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    trust_paymethods = [{'firm_id': paysys.firm.id,
                         'payment_method': paysys.payment_method.cc,
                         'currency': paysys.currency_mapper.char_code}]
    if with_contract:
        person = create_person(session, type=person_category.category, client=request_.client)
        contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                                   client=request_.client, payment_type=PREPAY_PAYMENT_TYPE,
                                   services={service.id}, is_signed=NOW, person=person, currency=currency.num_code)
    else:
        contract = None
    patch_get_payment_methods = patch('balance.trust_api.actions.get_payment_methods', return_value=trust_paymethods)
    with patch_get_payment_methods:
        assert set(get_paysyses(PaystepNS(request=request_,
                                          need_trust_api=True,
                                          need_trust_api_payment_methods=True), contract)) == {paysys}
    assert paysys.trust_paymethods == trust_paymethods


@pytest.mark.single_account
def test_multiple_services_w_contract(session, firm, currency, product):
    """получение способов оплаты по мультисервисным реквестам в сочетании с договором, вернет пустой список"""
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    client = create_client(session, single_account_number=ob.get_big_number())
    create_price_tax_rate(session, product, firm.country, currency)

    service1 = create_service(session)
    service2 = create_service(session)
    order1 = create_order(session, client, product, service1)
    order2 = create_order(session, client, product, service2)
    request = create_request(session, orders=[order1, order2])
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           payment_method_id=BANK, extern=1, cc='paysys_cc')

    create_pay_policy(session, region_id=firm.country.region_id, service_id=service1.id, firm_id=firm.id, paymethods_params=[(currency.char_code, BANK)])
    create_pay_policy(session, region_id=firm.country.region_id, service_id=service2.id, firm_id=firm.id, paymethods_params=[(currency.char_code, BANK)])
    person = create_person(session, type=person_category.category, client=request.client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={service1.id, service2.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                            return_value=None)

    patcher_service = patch('balance.actions.single_account.availability.get_deny_reason_for_service',
                            return_value=None)
    patcher_product = patch('balance.actions.single_account.availability.get_deny_reason_for_product',
                            return_value=None)
    with patcher_payment, patcher_service, patcher_product:
        ns = PaystepNS(request=request)
        assert get_paysyses(ns, contract) == []
        assert ns.paysyses_denied == {(None, contract): {
            paysys: 'Multiple services allowed only for single account which isn\'t compatible with contract'}}


def test_multiple_services_instant(session, switch_new_paystep_flag, firm, currency, product):
    """получение способов оплаты по мультисервисным реквестам в сочетании с договором, вернет пустой список"""
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    client = create_client(session, single_account_number=ob.get_big_number())
    create_price_tax_rate(session, product, firm.country, currency)

    service1 = create_service(session)
    service2 = create_service(session)
    order1 = create_order(session, client, product, service1)
    order2 = create_order(session, client, product, service2)
    request = create_request(session, orders=[order1, order2])

    paysys_bank = create_paysys_simple(session, BANK, currency.iso_code, person_category.category, firm.id, instant=0)
    paysys_card = create_paysys_simple(session, CARD, currency.iso_code, person_category.category, firm.id, instant=1)

    paymethods_params = [
        (currency.char_code, BANK),
        (currency.char_code, CARD),
    ]
    create_pay_policy(session, region_id=firm.country.region_id, service_id=service1.id, firm_id=firm.id, paymethods_params=paymethods_params)
    create_pay_policy(session, region_id=firm.country.region_id, service_id=service2.id, firm_id=firm.id, paymethods_params=paymethods_params)

    with patch('balance.paystep.legacy.can_use_single_account', return_value=True):
        ns = PaystepNS(request=request)
        assert get_paysyses(ns, None) == [paysys_bank]
        msg = 'Multiple services allowed only for bank payments at the moment'
        assert ns.paysyses_denied == {(None, None): {paysys_card: msg}}


@pytest.mark.single_account
def test_forbidden_single_paysys_w_contract(session, firm, client, product, currency, service):
    """при получении способов оплаты с договором, отфильтровываем ЕЛС способы оплаты"""
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    client = create_client(session, single_account_number=ob.get_big_number())
    create_price_tax_rate(session, product, firm.country, currency)

    order = create_order(session, client, product, service=service)
    request = create_request(session, orders=[order])
    paysys_list = []
    for payment_method_id in [BANK, SINGLE_ACCOUNT]:
        paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                         currency=currency.char_code, category=person_category.category,
                                         payment_method_id=payment_method_id, extern=1, cc='paysys_cc'))

    create_pay_policy(session, region_id=firm.country.region_id, service_id=order.service.id, firm_id=firm.id,
                                              paymethods_params=[(currency.char_code, BANK),
                                                                 (currency.char_code, SINGLE_ACCOUNT)])
    person = create_person(session, type=person_category.category, client=request.client)
    contract = create_contract(session, commission=ContractTypeId.NON_AGENCY, firm=firm.id,
                               client=request.client, payment_type=PREPAY_PAYMENT_TYPE,
                               services={order.service.id}, is_signed=NOW, person=person,
                               currency=currency.num_code)
    patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                            return_value=None)

    patcher_service = patch('balance.actions.single_account.availability.get_deny_reason_for_service',
                            return_value=None)
    patcher_product = patch('balance.actions.single_account.availability.get_deny_reason_for_product',
                            return_value=None)
    with patcher_payment, patcher_service, patcher_product:
        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == paysys_list
        assert get_paysyses(ns, contract) == [paysys_list[0]]
        assert ns.paysyses_denied == {
            (None, contract): {paysys_list[1]: 'single account not allowed for any contracts'},
            (None, None): {}}


@pytest.mark.single_account
def test_multiple_services_forbidden_single_account(session, firm, currency, client, product):
    """если в реквесте больше одного сервиса, проверяем способы оплаты на возможность использования вместе с ЕЛС"""
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    create_price_tax_rate(session, product, firm.country, currency)

    service1 = create_service(session)
    service2 = create_service(session)
    order1 = create_order(session, client, product, service1)
    order2 = create_order(session, client, product, service2)
    request = create_request(session, orders=[order1, order2])

    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           payment_method_id=BANK, extern=1, cc='paysys_cc')

    create_pay_policy(session, region_id=firm.country.region_id, service_id=service1.id, firm_id=firm.id, paymethods_params=[(currency.char_code, BANK)])
    create_pay_policy(session, region_id=firm.country.region_id, service_id=service2.id, firm_id=firm.id, paymethods_params=[(currency.char_code, BANK)])

    patcher_request = patch('balance.actions.single_account.availability.get_deny_reason_for_request',
                            return_value=None)
    patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                            return_value="Go to pen")
    with patcher_request, patcher_payment as sa_mock:
        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == []
        assert ns.paysyses_denied == {(None, None): {
            paysys: 'Multiple services allowed only for single account which is denied for that paysys'
        }}
    assert sa_mock.call_count == 1


@pytest.mark.single_account
def test_multiple_services_partial_single_account(session, currency, client, product, country):
    person_category = create_person_category(session, country=country, ur=0, resident=1)
    create_price_tax_rate(session, product, country, currency)

    service1 = create_service(session)
    service2 = create_service(session)
    order1 = create_order(session, client, product, service1)
    order2 = create_order(session, client, product, service2)
    request = create_request(session, orders=[order1, order2])

    firm1 = create_firm(session, country=country)
    firm2 = create_firm(session, country=country)

    paysyses = []
    for firm in [firm1, firm2]:
        paysyses.append(
            create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                          currency=currency.char_code, category=person_category.category,
                          payment_method_id=BANK, extern=1, cc='paysys_cc')
        )

        create_pay_policy(session, region_id=firm.country.region_id, service_id=service1.id, firm_id=firm.id,
                          paymethods_params=[(currency.char_code, BANK)])
        create_pay_policy(session, region_id=firm.country.region_id, service_id=service2.id, firm_id=firm.id,
                          paymethods_params=[(currency.char_code, BANK)])

    paysys1, paysys2 = paysyses

    def mock_deny_reason(person_category, firm):
        return 'Nope' if firm == firm2 else None

    patcher_request = patch('balance.actions.single_account.availability.get_deny_reason_for_request',
                            return_value=None)
    patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                            mock_deny_reason)
    with patcher_request, patcher_payment:
        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == [paysys1]
        assert ns.paysyses_denied == {(None, None): {
            paysys2: 'Multiple services allowed only for single account which is denied for that paysys'
        }}


@pytest.mark.permissions
class TestPermissions(object):
    def test_nobody(self, session, firm, currency, client):
        """сессия без паспорта  -> не фильтруем способы оплаты по фирме"""
        del session.oper_id

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=order.service_id,
                       is_agency=0)
        create_price_tax_rate(session, order.product, firm.country, currency)

        ns = PaystepNS(request=request_)
        assert get_paysyses(ns) == [paysys]
        assert ddict2dict(ns.paysyses_denied) == {(None, None): {}}

    def test_request_is_owned_by_passport_client(self, session, currency, client, firm):
        """Реквест принадлежит клиенту из паспорта  -> не фильтруем способы оплаты по фирме"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=order.service_id,
                       is_agency=0)
        create_price_tax_rate(session, order.product, firm.country, currency)

        create_passport(session, [create_role(session)], patch_session=True, client=client)

        ns = PaystepNS(request=request_)
        assert get_paysyses(ns) == [paysys]
        assert ddict2dict(ns.paysyses_denied) == {(None, None): {}}

    def test_passport_wo_perms(self, session, currency, firm, client):
        """В паспорте не указан клиент  -> ничего не возвращаем"""
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order

        create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                       region_id=firm.country.region_id, service_id=order.service_id,
                       is_agency=0)
        create_price_tax_rate(session, order.product, firm.country, currency)

        passport = create_passport(session, [(create_role(session), None)], patch_session=True)
        assert session.oper_id
        assert passport.client_id is None

        ns = PaystepNS(request=request_)
        assert get_paysyses(ns) == []
        assert ddict2dict(ns.paysyses_denied) == {
            (None, None): {paysys: 'User doesn\'t have permissions to issue invoice'}}

    @pytest.mark.parametrize('firm_in_perm', [None, 'paysys_firm', 'another_firm'])
    def test_depend_on_firm_in_perm(self, session, currency, client, firm_in_perm, firm):
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
        create_passport(session, [(create_role(session, permissions), None)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc_1')
        create_pay_policy(
            session,
            region_id=firm.country.region_id,
            service_id=order.service_id,
            firm_id=firm.id,
            paymethods_params=[(currency.char_code, BANK)]
        )

        create_price_tax_rate(session, order.product, firm.country, currency)

        ns = PaystepNS(request=request_)
        if perm_firm_id and perm_firm_id != paysys.firm.id:
            assert get_paysyses(ns) == []
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {paysys: "User doesn't have permissions to issue invoice"}
            }
        else:
            assert get_paysyses(ns) == [paysys]
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {}
            }

    @pytest.mark.parametrize('firm_in_role', [None, 'paysys_firm', 'another_firm'])
    def test_depend_on_firm_in_role(self, session, currency, firm, client, firm_in_role):
        """У паспорта роль с правом IssueInvoices без ограничения по фирме или с ограничением по фирме способа оплаты,
         фирма в праве не указана -> пользователь может выставлять счета

        фирма в роли не совпадает с фирмой способа оплаты -> пользователь не может выставлять счета с этим
         способом оплаты"""
        if firm_in_role == 'paysys_firm':
            role_firm_id = firm.id
        elif firm_in_role == 'another_firm':
            role_firm_id = create_firm(session).id
        else:
            role_firm_id = None
        permissions = (ISSUE_INVOICES, {'firm_id': None})
        create_passport(session, [(create_role(session, permissions), role_firm_id)], patch_session=True)
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc_1')
        create_pay_policy(
            session,
            region_id=firm.country.region_id,
            service_id=order.service_id,
            firm_id=firm.id,
            paymethods_params=[(currency.char_code, BANK)]
        )

        create_price_tax_rate(session, order.product, firm.country, currency)

        ns = PaystepNS(request=request_)
        if role_firm_id and role_firm_id != paysys.firm.id:
            assert get_paysyses(ns) == []
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {paysys: "User doesn't have permissions to issue invoice"}
            }
        else:
            assert get_paysyses(ns) == [paysys]
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {}
            }

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_depend_on_client_id(self, session, currency, firm, client, match_client):
        client_batch = ob.RoleClientBuilder.construct(session, client=client if match_client else None).client_batch_id
        role = ob.create_role(session, (ISSUE_INVOICES, {'client_batch_id': None}))
        ob.set_roles(
            session,
            session.passport,
            (role, {'client_batch_id': client_batch}),
        )

        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        request_ = create_request(session, client=client)
        order = request_.request_orders[0].order
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc_1')
        create_pay_policy(
            session,
            region_id=firm.country.region_id,
            service_id=order.service_id,
            firm_id=firm.id,
            paymethods_params=[(currency.char_code, BANK)]
        )

        create_price_tax_rate(session, order.product, firm.country, currency)

        ns = PaystepNS(request=request_)
        if not match_client:
            assert get_paysyses(ns) == []
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {paysys: "User doesn't have permissions to issue invoice"}
            }
        else:
            assert get_paysyses(ns) == [paysys]
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {}
            }

    @pytest.mark.parametrize('firm_in_req', [None, 'from_passport_firm', 'another_firm'])
    def test_depend_on_firm_in_req(self, session, currency, firm, client, service, firm_in_req):
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
        person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
        request = create_request(session, client=client, firm_id=req_firm_id,
                                 orders=[create_order(session,
                                                      client=client,
                                                      service=service)])
        order = request.request_orders[0].order
        paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                               currency=currency.char_code, category=person_category.category,
                               group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc_1')
        create_pay_policy(
            session,
            region_id=firm.country.region_id,
            service_id=order.service_id,
            firm_id=firm.id,
            paymethods_params=[(currency.char_code, BANK)]
        )

        create_price_tax_rate(session, order.product, firm.country, currency)

        ns = PaystepNS(request=request)
        if req_firm_id and req_firm_id != firm.id:
            assert get_paysyses(ns) == []
        else:
            assert get_paysyses(ns) == [paysys]
            assert ddict2dict(ns.paysyses_denied) == {
                (None, None): {}
            }

    @pytest.mark.parametrize('w_manager_in_order', [True, False])
    def test_w_manager_perm_wo_manager_perm(self, session, currency, country, client, service, w_manager_in_order):
        """У паспорта роль с правом IssueInvoices без ограничения по фирме или с ограничением по фирме способа оплаты,
         фирма в праве не указана, фирма в реквесте не указана или совпадает с фирмой способа оплаты -> пользователь
         может выставлять счета"""
        firm1 = create_firm(session)
        permissions = (ISSUE_INVOICES, {'firm_id': [firm1.id], 'manager': 1})
        role1 = create_role(session, permissions), None

        firm2 = create_firm(session)
        permissions = (ISSUE_INVOICES, {'firm_id': [firm2.id]})
        role2 = create_role(session, permissions), None

        passport = create_passport(session, [role1, role2], patch_session=True)
        manager = create_manager(session, passport)
        person_category = create_person_category(session, country=country, ur=0, resident=1)
        request = create_request(session, client=client,
                                 orders=[create_order(session, manager=manager if w_manager_in_order else None,
                                                      client=client,
                                                      service=service)])
        order = request.request_orders[0].order
        paysys_list = []
        for firm in [firm1, firm2]:
            paysys_list.append(create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                                             currency=currency.char_code, category=person_category.category,
                                             group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc'))
        patch_path = 'balance.paystep.legacy.get_base_paysyses_routing'
        _get_base_paysyses_routing_patch = patch(patch_path, return_value=paysys_list[:])

        create_price_tax_rate(session, order.product, country, currency)

        ns = PaystepNS(request=request)
        with _get_base_paysyses_routing_patch:
            if w_manager_in_order:
                assert set(get_paysyses(ns)) == set(paysys_list)
            else:
                assert get_paysyses(ns) == [paysys_list[1]]
                assert ddict2dict(ns.paysyses_denied) == {
                    (None, None): {paysys_list[0]: "User doesn't have permissions to issue invoice"}
                }


@pytest.mark.charge_note_register
class TestChargeNoteRegister(object):
    @pytest.fixture(autouse=True)
    def mock_config(self, session):
        session.config.__dict__['ALLOW_CHARGE_NOTE_REGISTER_WO_SINGLE_ACCOUNT'] = True

    @pytest.mark.parametrize(
        'single_account, is_allowed',
        [
            pytest.param(True, True, id='single_account'),
            pytest.param(False, False, id='normal'),
        ]
    )
    def test_allowed(self, session, client, service, firm, currency, single_account, is_allowed):
        session.config.__dict__['ALLOW_CHARGE_NOTE_REGISTER_WO_SINGLE_ACCOUNT'] = False

        country = firm.country
        person_category = create_person_category(session, country)

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
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        contract = create_contract(
            session,
            commission=ContractTypeId.NON_AGENCY,
            firm=firm.id,
            client=client,
            payment_type=PREPAY_PAYMENT_TYPE,
            services={service.id},
            is_signed=NOW,
            person=person,
            currency=currency.num_code
        )

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            contract=contract,
            request=create_request(session, client=client, orders=[order], firm_id=firm.id),
        )

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=[ref_invoice]
        )

        if single_account:
            sa_check_res = None
        else:
            sa_check_res = "qwerk"

        msg = "Can't issue charge note register without single account"
        if is_allowed:
            allowed_paysyses = [paysys]
            offer_denies = {}
        else:
            allowed_paysyses = []
            offer_denies = {paysys: msg}

        ns = PaystepNS(request=request)

        patcher_request = patch('balance.actions.single_account.availability.get_deny_reason_for_request',
                                return_value=sa_check_res)
        patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                                return_value=sa_check_res)
        with patcher_request, patcher_payment:
            assert get_paysyses(ns) == allowed_paysyses
            assert get_paysyses(ns, contract) == []

        assert ns.paysyses_denied == {
            (None, None): offer_denies,
            (None, contract): {paysys: msg}
        }

    def test_currency_filter(self, session, client, service, firm):
        person_category = create_person_category(session, firm.country)

        paysys1, paysys2 = [
            create_paysys_simple(
                session,
                category=person_category.category,
                firm_id=firm.id,
                iso_currency=create_currency(session, create_rate=True).iso_code
            )
            for _ in range(2)
        ]
        currency1, currency2 = paysys1.currency_mapper, paysys2.currency_mapper

        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[
                (paysys1.currency, paysys1.payment_method_id),
                (paysys2.currency, paysys2.payment_method_id),
            ]
        )

        product = create_product(session)
        for currency in [currency1, currency2]:
            create_price_tax_rate(session, product, firm.country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        ref_invoice = create_invoice(
            session,
            paysys=paysys1,
            person=person,
            request=create_request(session, client=client, orders=[order]),
        )

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == [paysys1]
        msg = "paysys iso_currency doesn't match currency in register invoices: %s" % currency1.iso_code
        assert ns.paysyses_denied == {(None, None): {paysys2: msg}}

    def test_firm_filter(self, session, client, service, country, currency):
        person_category = create_person_category(session, country)

        paysys1, paysys2 = [
            create_paysys_simple(
                session,
                category=person_category.category,
                firm_id=create_firm(session, country).id,
                iso_currency=currency.iso_code
            )
            for _ in range(2)
        ]
        firm1, firm2 = paysys1.firm, paysys2.firm

        for paysys in [paysys1, paysys2]:
            create_pay_policy(
                session,
                region_id=country.region_id,
                service_id=service.id,
                firm_id=paysys.firm_id,
                paymethods_params=[(paysys.currency, paysys.payment_method_id)]
            )

        product = create_product(session)
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        ref_invoice = create_invoice(
            session,
            paysys=paysys1,
            person=person,
            request=create_request(session, client=client, orders=[order], firm_id=firm1.id),
        )

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == [paysys1]
        msg = "paysys firm doesn't match firm in register invoices: %s" % firm1.id
        assert ns.paysyses_denied == {(None, None): {paysys2: msg}}

    def test_contract_filter(self, session, client, service, firm, currency):
        country = firm.country
        person_category = create_person_category(session, country)

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
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        contract1, contract2 = [
            create_contract(
                session,
                commission=ContractTypeId.NON_AGENCY,
                firm=firm.id,
                client=client,
                payment_type=PREPAY_PAYMENT_TYPE,
                services={service.id},
                is_signed=NOW,
                person=person,
                currency=currency.num_code
            )
            for _ in range(2)
        ]

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            contract=contract1,
            request=create_request(session, client=client, orders=[order], firm_id=firm.id),
        )

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)
        assert get_paysyses(ns, contract1) == [paysys]
        assert get_paysyses(ns, contract2) == []
        msg = "contract doesn't match contract in register invoices: %s" % contract1
        assert ns.paysyses_denied == {
            (None, contract1): {},
            (None, contract2): {paysys: msg}
        }

    def test_multiple_currencies(self, session, client, service, firm):
        person_category = create_person_category(session, firm.country)

        paysyses = [
            create_paysys_simple(
                session,
                category=person_category.category,
                firm_id=firm.id,
                iso_currency=create_currency(session, create_rate=True).iso_code
            )
            for _ in range(2)
        ]

        create_pay_policy(
            session,
            region_id=firm.region_id,
            service_id=service.id,
            firm_id=firm.id,
            paymethods_params=[
                (paysys.currency, paysys.payment_method_id)
                for paysys in paysyses
            ]
        )

        product = create_product(session)
        for paysys in paysyses:
            create_price_tax_rate(session, product, firm.country, paysys.currency_mapper)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        ref_invoices = [
            create_invoice(
                session,
                paysys=paysys,
                person=person,
                request=create_request(session, client=client, orders=[order]),
            )
            for paysys in paysyses
        ]

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=ref_invoices
        )

        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == []
        msg = "paysys iso_currency doesn't match currency in register invoices: %s,%s" \
              % tuple(sorted(ps.iso_currency for ps in paysyses))
        assert ns.paysyses_denied == {(None, None): {ps: msg for ps in paysyses}}

    def test_multiple_firms(self, session, client, service, country, currency):
        person_category = create_person_category(session, country)

        paysyses = [
            create_paysys_simple(
                session,
                category=person_category.category,
                firm_id=create_firm(session, country).id,
                iso_currency=currency.iso_code
            )
            for _ in range(2)
        ]

        for paysys in paysyses:
            create_pay_policy(
                session,
                region_id=country.region_id,
                service_id=service.id,
                firm_id=paysys.firm_id,
                paymethods_params=[(paysys.currency, paysys.payment_method_id)]
            )

        product = create_product(session)
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        ref_invoices = [
            create_invoice(
                session,
                paysys=paysys,
                person=person,
                request=create_request(session, client=client, orders=[order], firm_id=paysys.firm_id),
            )
            for paysys in paysyses
        ]

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=ref_invoices
        )

        ns = PaystepNS(request=request)
        assert get_paysyses(ns) == []
        msg = "paysys firm doesn't match firm in register invoices: %s" % ','.join(sorted({str(ps.firm_id) for ps in paysyses}))
        hm.assert_that(
            ns.paysyses_denied,
            hm.has_entries({
                (None, None): hm.has_entries({
                    ps: msg
                    for ps in paysyses
                })
            }),
        )

    def test_multiple_contracts(self, session, service, firm, currency):
        country = firm.country
        person_category = create_person_category(session, country)

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
        create_price_tax_rate(session, product, country, currency)

        client = create_client(session)
        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        contracts = [
            create_contract(
                session,
                commission=ContractTypeId.NON_AGENCY,
                firm=firm.id,
                client=client,
                payment_type=PREPAY_PAYMENT_TYPE,
                services={service.id},
                is_signed=NOW,
                person=person,
                currency=currency.num_code
            )
            for _ in range(2)
        ]
        ref_invoices = [
            create_invoice(
                session,
                paysys=paysys,
                person=person,
                contract=contract,
                request=create_request(session, client=client, orders=[order], firm_id=firm.id),
            )
            for contract in contracts
        ]

        request = create_request(
            session,
            client=client,
            orders=[],
            ref_invoices=ref_invoices
        )

        ns = PaystepNS(request=request)

        msg = "contract doesn't match contract in register invoices: %s,%s" % tuple(sorted(contracts))
        assert get_paysyses(ns, contracts[0]) == []
        assert get_paysyses(ns, contracts[1]) == []
        assert ns.paysyses_denied == {
            (None, contracts[0]): {paysys: msg},
            (None, contracts[1]): {paysys: msg},
        }

    @pytest.mark.parametrize(
        'personal_account_fictive, is_allowed',
        [
            pytest.param(0, True, id='personal_account'),
            pytest.param(1, False, id='personal_account_fictive')
        ]
    )
    def test_orders_contract(self, session, service, client, firm, currency, personal_account_fictive, is_allowed):
        country = firm.country
        person_category = create_person_category(session, country)

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
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        contract = create_contract(
            session,
            commission=ContractTypeId.NON_AGENCY,
            firm=firm.id,
            client=client,
            payment_type=POSTPAY_PAYMENT_TYPE,
            personal_account=1,
            personal_account_fictive=personal_account_fictive,
            services={service.id},
            is_signed=NOW,
            person=person,
            currency=currency.num_code
        )

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            request=create_request(session, client=client, orders=[order], firm_id=firm.id),
        )
        ref_invoice.assign_contract(contract)

        request = create_request(
            session,
            client=client,
            orders=[order],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)
        if is_allowed:
            assert get_paysyses(ns, contract) == [paysys]
            assert ns.paysyses_denied == {(None, contract): {}}
        else:
            assert get_paysyses(ns, contract) == []
            msg = "Can't pay invoices and orders with contract without old personal account"
            assert ns.paysyses_denied == {(None, contract): {paysys: msg}}

    @pytest.mark.parametrize(
        'single_account, is_allowed',
        [
            pytest.param(True, True, id='single_account'),
            pytest.param(False, False, id='normal'),
        ]
    )
    def test_orders_offer(self, session, service, firm, client, currency, single_account, is_allowed):
        country = firm.country
        person_category = create_person_category(session, country)

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
        create_price_tax_rate(session, product, country, currency)

        person = create_person(session, type=person_category.category, client=client)
        order = create_order(session, client, product, service)

        ref_invoice = create_invoice(
            session,
            paysys=paysys,
            person=person,
            request=create_request(session, client=client, orders=[order], firm_id=firm.id),
        )

        request = create_request(
            session,
            client=client,
            orders=[order],
            ref_invoices=[ref_invoice]
        )

        ns = PaystepNS(request=request)

        if single_account:
            sa_check_res = None
        else:
            sa_check_res = "qwerk"
        patcher_request = patch('balance.actions.single_account.availability.get_deny_reason_for_request',
                                return_value=sa_check_res)
        patcher_payment = patch('balance.actions.single_account.availability.get_deny_reason_for_payment_params',
                                return_value=sa_check_res)
        with patcher_request, patcher_payment:
            res_paysyses = get_paysyses(ns, None)

        if is_allowed:
            assert res_paysyses == [paysys]
            assert ns.paysyses_denied == {(None, None): {}}
        else:
            assert res_paysyses == []
            msg = "Can pay invoices and orders for offer only with single_account"
            assert ns.paysyses_denied == {(None, None): {paysys: msg}}
