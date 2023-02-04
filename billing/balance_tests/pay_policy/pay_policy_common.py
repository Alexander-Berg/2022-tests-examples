# -*- coding: utf-8 -*-

import pytest

from balance.constants import PaymentMethodIDs
from balance import mapper
from tests import object_builder as ob
from balance.providers.pay_policy import PayPolicyRoutingManager

FIELDS = ['service_id', 'region_id', 'is_agency', 'is_contract', 'firm', 'category', 'payment_method_id',
          'iso_currency', 'paysys_group_id']

CATEGORY_FIELDS = ['category']

CLIENT_CATEGORY = 0
AGENCY_CATEGORY = 1

WITHOUT_CONTRACT = 0
WITH_CONTRACT = 1

RESIDENT = 1
NON_RESIDENT = 0

PAYSYS_GROUP_ID = 0

BANK = PaymentMethodIDs.bank
CARD = PaymentMethodIDs.credit_card


@pytest.fixture(name='pay_policy_manager')
def get_pay_policy_manager(session):
    return PayPolicyRoutingManager(session)


@pytest.fixture(name='person_category')
def create_person_category(session, **kwargs):
    return ob.PersonCategoryBuilder(**kwargs).build(session).obj


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def create_person(session, **kwargs):
    return ob.PersonBuilder(**kwargs).build(session).obj


def create_contract(session, **kwargs):
    return ob.ContractBuilder(**kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, **kwargs):
    return ob.FirmBuilder(**kwargs).build(session).obj


@pytest.fixture(name='service')
def create_service(session, **kwargs):
    return ob.ServiceBuilder(**kwargs).build(session).obj


def create_pay_policy(session, pay_policy_service_id=None, **kwargs):
    if not pay_policy_service_id:
        pay_policy_service_id = ob.create_pay_policy_service(session, **kwargs)
    ob.create_pay_policy_region(session, pay_policy_service_id=pay_policy_service_id, **kwargs)
    return pay_policy_service_id


def create_pay_policy_region_group(session, regions):
    region_group_id = ob.get_big_number()
    ob.create_pay_policy_region_group(session, region_group_id, regions)
    return region_group_id


def _extract_ans(rows):
    return set(map(tuple, rows))


def _extract_category(rows):
    return {row.category for row in rows}
