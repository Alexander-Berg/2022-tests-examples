from tests import object_builder as ob

NDS_PCT = 45
NSP_PCT = 12


def create_currency_product(session, iso_currency, **kwargs):
    return ob.ProductBuilder(create_price=False, unit=create_product_unit(session, iso_currency=iso_currency),
                             create_taxes=False, **kwargs).build(session).obj


def create_non_currency_product(session, reference_price_currency=None, **kwargs):
    return ob.ProductBuilder(create_price=False, unit=create_product_unit(session, iso_currency=None),
                             reference_price_currency=reference_price_currency, create_taxes=False, **kwargs).build(session).obj


def create_valid_tax(session, product, currency, country, dt, nds_pct=NDS_PCT, nsp_pct=NSP_PCT):
    tax_policy = create_tax_policy(session, resident=1, country=country)
    tax = create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
                     hidden=0, dt=dt, policy=tax_policy, iso_currency=currency.iso_code)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=dt, nds_pct=nds_pct,
                                           nsp_pct=nsp_pct)
    return tax, tax_policy, tax_policy_pct


def create_product_unit(session, **kwargs):
    return ob.ProductUnitBuilder(**kwargs).build(session).obj


def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    return currency


def create_price(session, **kwargs):
    return ob.SimplePriceBuilder(**kwargs).build(session).obj


def create_tax_policy_pct(session, **kwargs):
    return ob.TaxPolicyPctBuilder(**kwargs).build(session).obj


def create_country(session, **kwargs):
    return ob.CountryBuilder(**kwargs).build(session).obj


def create_tax(session, product_id, **kwargs):
    return ob.TaxBuilder(product_id=product_id, **kwargs).build(session).obj


def create_product(session, **kwargs):
    return ob.ProductBuilder(create_taxes=False, reference_price_currency=None, create_price=False, **kwargs).build(
        session).obj


def create_tax_policy(session, **kwargs):
    return ob.TaxPolicyBuilder(taxes=[], **kwargs).build(session).obj
