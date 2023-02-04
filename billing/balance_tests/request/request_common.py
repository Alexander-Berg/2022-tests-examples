# -*- coding: utf-8 -*-

import pytest
import datetime

from medium.medium_logic import Logic

from balance.constants import PaymentMethodIDs, Enum
from balance import exc, mapper
from balance import muzzle_util as ut

import tests.object_builder as ob

QTY = 1
NOW = datetime.datetime.now()
BANK = PaymentMethodIDs.bank

PERMISSION = 'Permission'
ISSUE_INVOICES = 'IssueInvoices'
VIEW_ALL_INVOICES = 'ViewAllInvoices'
VIEW_INVOICES = 'ViewInvoices'
YAMONEY_SUPPORT_ACCESS = 'YaMoneySupportAccess'


class CheckType(Enum):
    object = 'object'
    query = 'query'


@pytest.fixture
def logic():
    return Logic()


def create_person(session, **kwargs):
    return ob.PersonBuilder(**kwargs).build(session).obj


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Request.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Request)
                .filter(mapper.Request.id == obj.id,
                        filters)
                .exists()
        )


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='currency')
def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


def create_pay_policy(session, pay_policy_service_id=None, **kwargs):
    if not pay_policy_service_id:
        pay_policy_service_id = ob.create_pay_policy_service(session, **kwargs)
    ob.create_pay_policy_region(session, pay_policy_service_id=pay_policy_service_id, **kwargs)
    return pay_policy_service_id


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


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


def create_tax(session, product_id, **kwargs):
    return ob.TaxBuilder(product_id=product_id, **kwargs).build(session).obj


def create_price_tax_rate(session, product, country, currency, price=1, resident=1):
    _, _, tax_policy_pct = create_valid_tax(session, product=product, resident=resident,
                                            currency=currency, country=country, dt=NOW)
    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW, tax_policy_pct=tax_policy_pct, price=price)
    try:
        product.currency_rate_by_date(currency.char_code, NOW)
    except exc.INVALID_MISSED_CURRENCY_RATE:
        create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                             base_cc='RUR', rate_src_id=1000, rate=100)


@pytest.fixture(name='service')
def create_service(session, **kwargs):
    return ob.ServiceBuilder(**kwargs).build(session).obj


def create_person_category(session, **kwargs):
    return ob.PersonCategoryBuilder(**kwargs).build(session).obj


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, country=None, **kwargs):
    return ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


def create_product(session, create_taxes=False, create_price=False, reference_price_currency=None, **kwargs):
    product = ob.ProductBuilder(create_taxes=create_taxes,
                                create_price=create_price,
                                **kwargs).build(session).obj
    product.reference_price_currency = reference_price_currency
    return product


@pytest.fixture(name='order')
def create_order(session, client, product=None, service=None, service_id=None, **kwargs):
    if service_id:
        service = ob.Getter(mapper.Service, service_id)
    return ob.OrderBuilder(client=client, service=service or create_service(session),
                           product=product or create_product(session), **kwargs).build(session).obj


@pytest.fixture(name='request_')
def create_request(session, client=None, orders=None, **kwargs):
    if not client:
        client = create_client(session)
    if not orders:
        orders = [create_order(session, client=client)]
    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            rows=[ob.BasketItemBuilder(order=order, quantity=QTY) for order in orders],
            client=client), **kwargs).build(session).obj


def create_paysys(session, group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1, cc='paysys_cc', category=None,
                  **kwargs):
    if category is None:
        category = create_person_category(session).category
    return ob.PaysysBuilder(group_id=group_id, payment_method_id=payment_method_id, extern=extern,
                            cc=cc, category=category, **kwargs).build(session).obj


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


@pytest.fixture(name='agency')
def create_agency(session, **kwargs):
    return ob.ClientBuilder(is_agency=1, **kwargs).build(session).obj


def create_passport(session, *roles, **kwargs):
    return ob.create_passport(session, *roles, **kwargs)


def create_manager(session, passport=None):
    return ob.SingleManagerBuilder(
        passport_id=passport and passport.passport_id,
        domain_login=passport and passport.login).build(session).obj


def create_contract(session, **kwargs):
    contract_params = {'is_signed': None,
                       'finish_dt': None,
                       'dt': NOW}
    contract_params.update(kwargs)
    return ob.ContractBuilder(**contract_params).build(session).obj
