# -*- coding: utf-8 -*-

import pytest
from tests import object_builder as ob
from balance.actions.invoice_turnon import InvoiceTurnOn
from balance.constants import PaymentMethodIDs

BANK = PaymentMethodIDs.bank
ADDITIONAL_FUNCTIONS = 'AdditionalFunctions'
BILLING_SUPPORT = 'BillingSupport'
CLIENT_FRAUD_STATUS_EDIT = 'ClientFraudStatusEdit'

PARTNERS = 'PARTNERS'
GEOCONTEXT = 'GEOCONTEXT'
DISTRIBUTION = 'DISTRIBUTION'
AFISHA = 'AFISHA'
PREFERRED_DEAL = 'PREFERRED_DEAL'
SPENDABLE = 'SPENDABLE'
ACQUIRING = 'ACQUIRING'
GENERAL = 'GENERAL'
USE_ADMIN_PERSONS = 'UseAdminPersons'


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


def create_person(session, **kwargs):
    return ob.PersonBuilder(**kwargs).build(session).obj


@pytest.fixture(name='client')
def create_client(session, is_agency=0, **kwargs):
    return ob.ClientBuilder(is_agency=is_agency, **kwargs).build(session).obj


@pytest.fixture(name='product')
def create_product(session, create_taxes=False, create_price=False, reference_price_currency=None, **kwargs):
    product = ob.ProductBuilder(create_taxes=create_taxes,
                                create_price=create_price,
                                **kwargs).build(session).obj
    product.reference_price_currency = reference_price_currency
    return product


def create_order(session, client, product=None, service=None, **kwargs):
    return ob.OrderBuilder(client=client, service=service or create_service(session, client_only=0),
                           product=product or create_product(session), **kwargs).build(session).obj


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='service')
def create_service(session, client_only=0, **kwargs):
    return ob.ServiceBuilder(client_only=client_only, **kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, country=None, **kwargs):
    return ob.FirmBuilder(country=country or create_country(session), **kwargs).build(session).obj


def create_person_category(session, **kwargs):
    return ob.PersonCategoryBuilder(**kwargs).build(session).obj


def create_pay_policy(session, pay_policy_service_id=None, legal_entity=0, category=None, is_atypical=0,
                      paymethods_params=None, region_id=None, **kwargs):
    if not pay_policy_service_id:
        pay_policy_service_id = ob.create_pay_policy_service(
            session, legal_entity=legal_entity, category=category, is_atypical=is_atypical,
            paymethods_params=paymethods_params, **kwargs
        )
    ob.create_pay_policy_region(session, pay_policy_service_id=pay_policy_service_id, region_id=region_id, **kwargs)
    return pay_policy_service_id


def create_passport(session, roles, client=None, patch_session=True):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client)
    return passport


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


def _extract_categories(categories):
    return {pc.category for pc in categories}


def create_paid_invoice(session, order_client, service_id=None, orders=None, person=None):
    if not orders:
        orders = [ob.OrderBuilder(client=order_client, service_id=service_id)]
    invoice = ob.InvoiceBuilder(person=person,
                                request=ob.RequestBuilder(
                                    basket=ob.BasketBuilder(
                                        client=order_client,
                                        rows=[ob.BasketItemBuilder(order=order,
                                                                   quantity=1) for order in orders]))).build(
        session).obj
    InvoiceTurnOn(invoice, manual=True).do()
    return invoice


def create_contract(session, client, contract_type):
    return ob.ContractBuilder(client=client, ctype=contract_type).build(session).obj
