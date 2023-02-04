# -*- coding: utf-8 -*-
import pytest
import datetime

from billing.contract_iface.cmeta.helpers import attrdict, attribute
from billing.contract_iface.cmeta.helpers import CollateralTypes as CollateralTypesBase, CollateralAttrs
from billing.contract_iface.contract_meta import collateral_types
from billing.contract_iface.contract_json import JSONContract

from balance.mapper import Service, get_contract_json

from tests import object_builder as ob

ACQUIRING = 'ACQUIRING'
AFISHA = 'AFISHA'
DISTRIBUTION = 'DISTRIBUTION'
GENERAL = 'GENERAL'
GEOCONTEXT = 'GEOCONTEXT'
PARTNERS = 'PARTNERS'
PREFERRED_DEAL = 'PREFERRED_DEAL'
SPENDABLE = 'SPENDABLE'

NOW = datetime.datetime.now()


@pytest.fixture(name='person_category')
def create_person_category(session, country=None, **kwargs):
    return ob.PersonCategoryBuilder(country=country or create_country(session), **kwargs).build(session).obj


@pytest.fixture(name='country')
def create_country(session):
    return ob.CountryBuilder().build(session).obj


@pytest.fixture(name='person')
def create_person(session, client, type=None):
    if not type:
        type = create_person_category(session).category
    return ob.PersonBuilder(client=client, type=type).build(session).obj


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def create_contract(session, **kwargs):
    contract_params = {'is_signed': None,
                       'finish_dt': None,
                       'dt': NOW}
    contract_params.update(kwargs)
    return ob.ContractBuilder(**contract_params).build(session, ).obj


def create_jc(contract):
    json_body = get_contract_json(contract)
    return JSONContract.loads(json_body)


def set_same_external_id(contracts):
    external_id = contracts[0].create_new_eid()
    for contract in contracts:
        contract.external_id = external_id


def create_request(session, client=None, orders=None, quantity=1, **kwargs):
    if not client:
        client = create_client(session)
    if not orders:
        orders = [create_order(session, client=client)]

    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            rows=[ob.BasketItemBuilder(order=order, quantity=quantity) for order in orders],
            client=client), **kwargs).build(session).obj


def set_unique_external_id(contract):
    contract.external_id = contract.create_new_eid()


def create_contract_type(session, descr=None):
    test_contract_type = 'TEST_CONTRACT_TYPE' + str(ob.get_big_number())
    session.execute('''INSERT INTO T_CONTRACT_TYPES VALUES (:contract_type, :descr)''',
                    {'contract_type': test_contract_type, 'descr': descr})
    return test_contract_type


def create_attr(contract_type, name, persistattr=0, pytype='int'):
    contract_attrs = attrdict('code')
    contract_attrs[name.upper()] = attribute(contract_type=contract_type, pytype=pytype, htmltype='refselect',
                                             source='firms', caption=u'Тестовый атрибут', headattr=1, position=26,
                                             grp=1, persistattr=persistattr)
    return contract_attrs


def create_collateral_type(contract_type):
    collateral_types[contract_type] = attrdict('id')
    collateral_types[contract_type][1] = CollateralTypesBase(caption=u'Тестовое допсоглашение',
                                                             contract_type=contract_type,
                                                             attributes=[CollateralAttrs(attribute_code='TEST_ATTR')])


def create_order(session, client, service=None, **kwargs):
    if service is None:
        service = ob.Getter(Service, 7).build(session).obj
    product = ob.ProductBuilder().build(session).obj
    return ob.OrderBuilder(client=client, service=service,
                           product=product, **kwargs).build(session).obj
