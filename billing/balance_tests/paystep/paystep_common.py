# -*- coding: utf-8 -*-
import datetime
import pytest
import collections

from mock import patch

from butils.decimal_unit import DecimalUnit
from balance import mapper
from balance.constants import (
    ServiceId,
    PaymentMethodIDs,
    PaysysGroupIDs,
    FirmId,
)

from balance.actions.single_account.prepare import (
    autocreate_personal_account,
    DEFAULT_SINGLE_ACCOUNT_FIRM,
    DEFAULT_ISO_CURRENCY,
)
from balance import exc
from balance import muzzle_util as ut

from tests import object_builder as ob

NOW = datetime.datetime.now()

BANK = PaymentMethodIDs.bank
CARD = PaymentMethodIDs.credit_card
YAMONEY = PaymentMethodIDs.yamoney_wallet
SINGLE_ACCOUNT = PaymentMethodIDs.single_account
WEBMONEY = 1202
PAYPAL = 1204
QIWI = 1203

ALTER_INVOICE_SUM = 'AlterInvoiceSum'
ALTER_INVOICE_DATE = 'AlterInvoiceDate'
ALTER_INVOICE_PAYSYS = 'AlterInvoicePaysys'
ALTER_INVOICE_PERSON = 'AlterInvoicePerson'
ALTER_INVOICE_CONTRACT = 'AlterInvoiceContract'
ALTER_ALL_INVOICES = 'AlterAllInvoices'
BILLING_SUPPORT = 'BillingSupport'
EDIT_CONTRACTS = 'EditContracts'
ISSUE_INVOICES = 'IssueInvoices'
PAYSTEP_VIEW = 'PaystepView'
USE_ADMIN_PERSONS = 'UseAdminPersons'
ADMIN_ACCESS = 'AdminAccess'


class PermType(object):
    wo_perm = 0
    w_perm = 1
    w_right_client = 2
    w_wrong_client = 3


def get_client_permission(session, perm_type, perm, client):
    if perm_type is PermType.wo_perm:
        return
    if perm_type is PermType.w_perm:
        return perm
    client_batch = ob.RoleClientBuilder.construct(session, client=client if perm_type is PermType.w_right_client else None). client_batch_id
    return perm, {'client_batch_id': (client_batch,)}


def create_single_personal_account_for_person(session, person, single_account_number, paysys=None):
    if paysys is None:
        paysys = create_paysys(session, payment_method_id=PaymentMethodIDs.bank, group_id=PaysysGroupIDs.default,
                               firm_id=DEFAULT_SINGLE_ACCOUNT_FIRM, iso_currency=DEFAULT_ISO_CURRENCY, currency='RUR',
                               category=person.person_category.category)
    patch_get_personal_account_paysys = patch('balance.actions.single_account.prepare.get_personal_account_paysys',
                                              return_value=paysys)

    with patch_get_personal_account_paysys:
        return autocreate_personal_account(person, single_account_number)


@pytest.fixture(name='firm')
def create_firm(session, country=None, **kwargs):
    return ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj


@pytest.fixture(name='currency')
def create_currency(session, create_rate=False, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    if create_rate:
        create_currency_rate(
            session,
            rate_dt=ut.trunc_date(NOW) - datetime.timedelta(666),
            cc=currency.char_code,
            base_cc='RUR',
            rate_src_id=1000,
            rate=100
        )

    return currency


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder.construct(session, **kwargs)


@pytest.fixture(name='service')
def create_service(session, client_only=0, **kwargs):
    return ob.ServiceBuilder.construct(session, client_only=client_only, **kwargs)


def create_manager(session, passport=None, **kwargs):
    return ob.SingleManagerBuilder(
        passport_id=passport and passport.passport_id,
        domain_login=passport and passport.login, **kwargs).build(session).obj


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj


def create_contract(session, **kwargs):
    return ob.ContractBuilder(**kwargs).build(session).obj


@pytest.fixture(name='product')
def create_product(session, create_taxes=False, create_price=False, reference_price_currency=None, **kwargs):
    product = ob.ProductBuilder(create_taxes=create_taxes,
                                create_price=create_price,
                                **kwargs).build(session).obj
    product.reference_price_currency = reference_price_currency
    return product


def create_currency_product(session, iso_currency, **kwargs):
    return ob.ProductBuilder(create_price=False, unit=create_product_unit(session, iso_currency=iso_currency),
                             create_taxes=False, **kwargs).build(session).obj


def create_product_unit(session, **kwargs):
    return ob.ProductUnitBuilder(**kwargs).build(session).obj


def create_order(session, client, product=None, service=None, **kwargs):
    return ob.OrderBuilder(client=client, service=service or create_service(session, client_only=0),
                           product=product or create_product(session), **kwargs).build(session).obj


def create_passport(session, roles, client=None, patch_session=True):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client)
    return passport


def create_client_service_data(client, service_id=ServiceId.DIRECT, **kwargs):
    client_service_data = ob.create_client_service_data(**kwargs)
    client.service_data[service_id] = client_service_data
    return client_service_data


def create_request(session, client=None, orders=None, request_client=None, quantity=1, ref_invoices=None, **kwargs):
    if not client:
        client = create_client(session)
    if orders is None:
        orders = [create_order(session, client=client, service=kwargs.pop('service', None))]
    elif not request_client:
        request_client = orders and orders[0].client or client

    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=request_client or client,
            rows=[
                ob.BasketItemBuilder(order=order, quantity=quantity)
                for order in orders
            ],
            register_rows=[
                ob.BasketRegisterRowBuilder(ref_invoice=i)
                for i in ref_invoices or []
            ],
        ),
        **kwargs
    )


def create_paysys(session, group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1, cc='paysys_cc', category=None,
                  country=None, **kwargs):
    if category is None:
        category = create_person_category(session, country=country or create_country(session)).category
    return ob.PaysysBuilder(group_id=group_id, payment_method_id=payment_method_id, extern=extern,
                            cc=cc, category=category, **kwargs).build(session).obj


def create_paysys_simple(session,
                         payment_method_id=PaymentMethodIDs.bank,
                         iso_currency='RUB',
                         category='ur',
                         firm_id=FirmId.YANDEX_OOO,
                         group_id=PaysysGroupIDs.default,
                         cc='paysys_cc',
                         extern=1,
                         **kwargs):

    firm = session.query(mapper.Firm).get(firm_id)

    return ob.PaysysBuilder(
        payment_method_id=payment_method_id,
        iso_currency=iso_currency,
        currency=mapper.fix_crate_base_cc(iso_currency),
        category=category,
        firm_id=firm_id,
        region_id=firm.region_id,
        group_id=group_id,
        cc=cc,
        extern=extern,
        **kwargs
    ).build(session).obj


def create_person(session, **kwargs):
    return ob.PersonBuilder(**kwargs).build(session).obj


def create_invoice(session, **kwargs):
    return ob.InvoiceBuilder(**kwargs).build(session).obj


def create_person_category(session, country, ur=1, resident=1, **kwargs):
    return ob.PersonCategoryBuilder(
        country=country,
        ur=ur,
        resident=resident,
        **kwargs
    ).build(session).obj


def create_pay_policy(session, pay_policy_service_id=None, **kwargs):
    if not pay_policy_service_id:
        pay_policy_service_id = ob.create_pay_policy_service(session, **kwargs)
    ob.create_pay_policy_region(session, pay_policy_service_id=pay_policy_service_id, **kwargs)
    return pay_policy_service_id


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


def create_tax(session, product_id, **kwargs):
    return ob.TaxBuilder(product_id=product_id, **kwargs).build(session).obj


def create_tax_policy(session, **kwargs):
    return ob.TaxPolicyBuilder(taxes=[], **kwargs).build(session).obj


def create_tax_policy_pct(session, **kwargs):
    return ob.TaxPolicyPctBuilder(**kwargs).build(session).obj


def create_valid_tax(session, product, currency, country, dt, nds_pct=22, nsp_pct=2, resident=1):
    tax_policy = create_tax_policy(session, resident=resident, country=country)
    tax = create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
                     hidden=0, dt=dt, policy=tax_policy, iso_currency=currency.iso_code)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=dt, nds_pct=nds_pct,
                                           nsp_pct=nsp_pct)
    return tax, tax_policy, tax_policy_pct


def create_price_tax_rate(session, product, country, currency, price=1, resident=1):
    _, tax_policy, tax_policy_pct = create_valid_tax(
        session,
        product=product,
        resident=resident,
        currency=currency,
        country=country,
        dt=NOW
    )
    tax_policy.default_tax = 1
    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW, tax_policy_pct=tax_policy_pct, price=price)
    try:
        product.currency_rate_by_date(currency.char_code, NOW)
    except exc.INVALID_MISSED_CURRENCY_RATE:
        create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                             base_cc='RUR', rate_src_id=1000, rate=100)


def pcps_to_set(session, pcps):
    result = set()
    for pcp in pcps:
        assert pcp.person
        person = pcp.person if pcp.person in session else None
        if len(pcp.paysyses) == 1:
            paysyses = pcp.paysyses[0]
        else:
            paysyses = frozenset(pcp.paysyses)
        result.add((pcp.contract, paysyses, pcp.person.person_category, person))
    return result


def ddict2dict(d):
    for k, v in d.items():
        if isinstance(v, dict):
            d[k] = ddict2dict(v)
        if isinstance(v, list):
            d[k] = sorted(v)
    return dict(d)


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


def not_existing_contract_id(session):
    return session.execute('select bo.s_contract_id.nextval from dual').scalar()


def create_trust_paymethods(paysys, limits_list):
    trust_paymethods = [{'firm_id': paysys.firm.id,
                         'payment_method': paysys.payment_method.cc,
                         'currency': paysys.currency_mapper.char_code,
                         'max_amount': limit} for limit in limits_list]
    return trust_paymethods


def create_terminals_limit(session, service, currency):
    session.config.__dict__['PAYSYS_TERMINAL_LIMITS_PAYMETHODS'] = [[service.id,
                                                                     PaymentMethodIDs.bank]]
    terminal_limit = DecimalUnit('100', currency.char_code)
    return terminal_limit


def rec_dd():
    return collections.defaultdict(rec_dd)


def group_paysyses_by_params(paysyses):
    result = rec_dd()
    for ps in paysyses:
        if result[ps.group_id][ps.firm_id][ps.category][ps.currency]:
            result[ps.group_id][ps.firm_id][ps.category][ps.currency].append(ps.payment_method.cc)
        else:
            result[ps.group_id][ps.firm_id][ps.category][ps.currency] = [ps.payment_method.cc]
    return ddict2dict(result)


def extract_pcps(pcp_list):
    return {
        (pcp.person.id, pcp.person.type, pcp.contract, tuple(extract_paysyses(pcp.paysyses)))
        for pcp in pcp_list
    }


def extract_paysyses(paysys_list):
    return [
        (ps.payment_method.cc, ps.currency, ps.firm_id, ps.category, ps.group_id)
        for ps in paysys_list
    ]
