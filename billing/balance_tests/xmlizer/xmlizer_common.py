import datetime
import pytest
from balance import muzzle_util as ut
from balance import exc
from balance import core
import tests.object_builder as ob

NOW = datetime.datetime.now()


def create_request(session, client=None, orders=None, request_client=None, quantity=1, **kwargs):
    if not client:
        client = create_client(session)
    if not orders:
        orders = [create_order(session, client=client)]
    elif not request_client:
        request_client = orders[0].client
    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            rows=[ob.BasketItemBuilder(order=order, quantity=quantity) for order in orders],
            client=request_client or client), **kwargs).build(session).obj


def create_invoice(session, **kwargs):
    return ob.InvoiceBuilder(**kwargs).build(session).obj


def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def create_person(session, **kwargs):
    return ob.PersonBuilder(**kwargs).build(session).obj


def create_contract(session, **kwargs):
    return ob.ContractBuilder(**kwargs).build(session).obj


def create_order(session, client, product=None, service=None, **kwargs):
    return ob.OrderBuilder(client=client, service=service or create_service(session),
                           product=product or create_product(session), **kwargs).build(session).obj


def create_passport(session):
    return ob.PassportBuilder().build(session).obj


def create_service(session, **kwargs):
    return ob.ServiceBuilder(name='service_name', cc='service_cc').build(session).obj


def create_product(session, create_taxes=False, create_price=False, reference_price_currency=None, **kwargs):
    product = ob.ProductBuilder(create_taxes=create_taxes,
                                create_price=create_price,
                                **kwargs).build(session).obj
    product.reference_price_currency = reference_price_currency
    return product


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


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


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


@pytest.fixture(name='currency')
def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


@pytest.fixture
def core_obj(session):
    return core.Core(session=session)
