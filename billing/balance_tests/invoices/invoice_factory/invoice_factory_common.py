# -*- coding: utf-8 -*-
import datetime
import pytest

from tests import object_builder as ob
from balance import mapper, exc
from balance.constants import PaymentMethodIDs

NOW = datetime.datetime.now()
YESTERDAY = NOW - datetime.timedelta(days=1)

GENERAL = 'GENERAL'
ACQUIRING = 'ACQUIRING'
AFISHA = 'AFISHA'
DISTRIBUTION = 'DISTRIBUTION'
GEOCONTEXT = 'GEOCONTEXT'
PARTNERS = 'PARTNERS'
PREFERRED_DEAL = 'PREFERRED_DEAL'
SPENDABLE = 'SPENDABLE'


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='agency')
def create_agency(session):
    return ob.ClientBuilder(is_agency=1).build(session).obj


def create_subclient_non_resident(session, currency):
    client = create_client(session)
    client.fullname = 'client_fullname'
    client.non_resident_currency_payment = currency.char_code
    return client


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


@pytest.fixture(name='paysys')
def create_paysys(session, group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1, cc='paysys_cc', category=None,
                  country=None, **kwargs):
    if category is None:
        category = create_person_category(session, country=country or create_country(session)).category
    return ob.PaysysBuilder(group_id=group_id, payment_method_id=payment_method_id, extern=extern,
                            cc=cc, category=category, **kwargs).build(session).obj


# This function does the same that method ob.ContractBuilder.construct()
def create_contract(session, **kwargs):
    return ob.ContractBuilder(**kwargs).build(session).obj


@pytest.fixture(name='product')
def create_product(session, create_taxes=False, create_price=False, reference_price_currency=None, **kwargs):
    product = ob.ProductBuilder(create_taxes=create_taxes,
                                create_price=create_price,
                                **kwargs).build(session).obj
    product.reference_price_currency = reference_price_currency
    return product


def create_order(session, client, product=None, service_id=None, agency=None, **kwargs):
    product = product or create_product(session)
    if service_id:
        service = ob.Getter(mapper.Service, service_id)
    else:
        service = kwargs.pop('service', None) or create_service(session)
    return ob.OrderBuilder(
        product=product,
        service=service,
        client=client,
        agency=agency,
        **kwargs
    ).build(session).obj


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='person_category')
def create_person_category(session, country=None, **kwargs):
    return ob.PersonCategoryBuilder(country=country or create_country(session), **kwargs).build(session).obj


@pytest.fixture(name='person')
def create_person(session, client, type=None):
    if not type:
        type = create_person_category(session).category
    return ob.PersonBuilder(client=client, type=type).build(session).obj


@pytest.fixture(name='currency')
def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


@pytest.fixture(name='request_obj')
def create_request(session, client, orders=None, **kwargs):
    if orders is None:
        orders = [create_order(session, client)]

    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=1)
            for o in orders
        ],
        dt=kwargs.pop('dt', None)
    )
    return ob.RequestBuilder(basket=basket, **kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, country=None, **kwargs):
    return ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj


@pytest.fixture(name='manager')
def create_manager(session, passport=None, **kwargs):
    return ob.SingleManagerBuilder(
        passport_id=passport and passport.passport_id,
        domain_login=passport and passport.login, **kwargs).build(session).obj


@pytest.fixture(name='service')
def create_service(session, client_only=0, **kwargs):
    return ob.ServiceBuilder(client_only=client_only, **kwargs).build(session).obj


def create_contract_commission2discount_type_map(session, commission_type_id, discount_type_ids):
    return ob.create_comsn_type_discount_types(session, commission_type_id, discount_type_ids)


def create_contract_commission_type(session):
    return ob.create_contract_commission_type(session)


def create_service2discount_type_map(session, service_id, discount_type_ids):
    for discount_type in discount_type_ids:
        service_discount_type = mapper.ServiceDiscountType(service_id=service_id, discount_type_id=discount_type)
        session.add(service_discount_type)
        session.flush()


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
    _, _, tax_policy_pct = create_valid_tax(session, product=product, resident=resident,
                                            currency=currency, country=country, dt=NOW)
    create_price(session, currency_code=currency.char_code, product_id=product.id,
                 hidden=0, dt=NOW, tax_policy_pct=tax_policy_pct, price=price)
    try:
        product.currency_rate_by_date(currency.char_code, NOW)
    except exc.INVALID_MISSED_CURRENCY_RATE:
        create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                             base_cc='RUR', rate_src_id=1000, rate=100)


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj
