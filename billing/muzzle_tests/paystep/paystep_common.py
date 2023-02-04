import pytest
import datetime

from balance import muzzle_util as ut
from balance.constants import PaymentMethodIDs, DIRECT_PRODUCT_RUB_ID, ServiceId, RegionId
from balance import mapper
from balance import exc
from tests import object_builder as ob

NOW = datetime.datetime.now()
VIEW_INVOICES = 'ViewInvoices'
ALTER_INVOICE_PAYSYS = 'AlterInvoicePaysys'
ALTER_INVOICE_PERSON = 'AlterInvoicePerson'
ALTER_INVOICE_CONTRACT = 'AlterInvoiceContract'
ISSUE_INVOICES = 'IssueInvoices'


def create_client(session, with_single_account=False):
    return ob.ClientBuilder(with_single_account=with_single_account).build(session).obj


def create_order(session, client, service_id=ServiceId.DIRECT):
    return ob.OrderBuilder(service_id=service_id, client=client,
                           product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID))


def create_request(session, client, order):
    basket = ob.BasketBuilder(
        client=client,
        rows=[ob.BasketItemBuilder(order=order, quantity=1)]
    )

    return ob.RequestBuilder(basket=basket).build(session).obj


def create_person(session, client, type='ph'):
    return ob.PersonBuilder(
        type=type, client=client
    ).build(session).obj


def create_invoice(session, **kwargs):
    return ob.InvoiceBuilder(**kwargs).build(session).obj


def create_paysys(session, group_id=0, payment_method_id=PaymentMethodIDs.bank, extern=1, cc='paysys_cc', category=None,
                  **kwargs):
    if category is None:
        category = create_person_category(session).category
    return ob.PaysysBuilder(group_id=group_id, payment_method_id=payment_method_id, extern=extern,
                            cc=cc, category=category, **kwargs).build(session).obj


def create_person_category(session, country=None, ur=1, resident=1, **kwargs):
    return ob.PersonCategoryBuilder(
        country=country or ob.Getter(mapper.Country, RegionId.RUSSIA),
        ur=ur,
        resident=resident,
        **kwargs
    ).build(session).obj


@pytest.fixture(name='currency')
def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


def create_tax_policy(session, **kwargs):
    return ob.TaxPolicyBuilder(taxes=[], **kwargs).build(session).obj


def create_tax_policy_pct(session, **kwargs):
    return ob.TaxPolicyPctBuilder(**kwargs).build(session).obj


def create_tax(session, product_id, **kwargs):
    return ob.TaxBuilder(product_id=product_id, **kwargs).build(session).obj


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj


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


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, country=None, **kwargs):
    return ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj


def create_passport(session, *roles, **kwargs):
    return ob.create_passport(session, *roles, **kwargs)


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)
