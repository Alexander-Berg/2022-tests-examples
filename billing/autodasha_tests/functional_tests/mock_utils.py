# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import collections
import functools
from sqlalchemy.orm import exc as orm_exc


import mock

from balance import mapper
from balance import muzzle_util as ut


class MockObjectManager(object):
    def __init__(self):
        self.inst_map = collections.defaultdict(dict)

    def create_object(self, cls, key=None, **kwargs):
        obj = mock.MagicMock(**kwargs)
        key = key or kwargs['id']
        self.inst_map[cls][key] = obj
        return obj

    def add_object_key(self, cls, key, obj):
        self.inst_map[cls][key] = obj

    def _query_get_object(self, cls, *args, **kwargs):
        key = tuple(args) if args else tuple(sorted(kwargs.values()))

        if len(key) == 1:
            key, = key

        return self.inst_map[cls][key]

    def _query_one(self, cls, *args, **kwargs):
        try:
            res = self._query_get_object(cls, *args, **kwargs)
        except KeyError:
            raise orm_exc.NoResultFound
        else:
            return res

    def _query_all(self, cls, *args, **kwargs):
        try:
            res = self._query_get_object(cls, *args, **kwargs)
        except KeyError:
            return []
        else:
            return [res]

    def _query_getone(self, cls, *args, **kwargs):
        try:
            res = self._query_get_object(cls, *args, **kwargs)
        except KeyError as e:
            raise getattr(cls, 'not_found_exception', ut.NOT_FOUND)(e.message)
        else:
            return res

    def _query_filter_by(self, cls, **kwargs):
        query = self._session_query(cls)
        query.one.side_effect = functools.partial(self._query_getone, cls, **kwargs)
        return query

    def _query_filter(self, cls, *args):
        query = self._session_query(cls)
        filters = {c.left.key: c.right.value for c in args}
        query.one.side_effect = functools.partial(self._query_one, cls, **filters)
        query.all.side_effect = functools.partial(self._query_all, cls, **filters)
        return query

    def _session_query(self, cls):
        query_attrs = {
            'getone.side_effect': functools.partial(self._query_getone, cls),
            'filter_by.side_effect': functools.partial(self._query_filter_by, cls),
            'filter.side_effect': functools.partial(self._query_filter, cls),
            'options': lambda *args, **kwargs: self._session_query(cls),
            'with_for_update': lambda *args, **kwargs: self._session_query(cls)
        }
        query = mock.Mock(**query_attrs)
        return query

    def construct_session(self):
        session_attrs = {
            'query.side_effect': self._session_query,
            'mock_manager': self
        }
        session = mock.Mock(**session_attrs)
        return session


def create_client(mock_manager, id_=66666, class_id=None, **kwargs):
    client = mock_manager.create_object(mapper.Client, id=id_, class_id=class_id or id_, **kwargs)
    client.configure_mock(name='Клиент Вася Пупкин')
    return client


def create_person(mock_manager, client, id_=66666, type_='ph', is_partner=0, **kwargs):
    person = mock_manager.create_object(mapper.Person, id=id_, client=client,
                                        is_partner=is_partner, type=type_, **kwargs)
    person.configure_mock(name='Плательщик Вася Пупкин')
    return person


def create_paysys(mock_manager, firm_id=1, id_=1002, name=None, certificate=0, **kwargs):
    paysys_names_map = {
        1000: 'Яндекс.Деньги',
        1001: 'Банк для физических лиц',
        1002: 'Кредитной картой',
        1003: 'Банк для юридических лиц',
        1006: 'Сертификат',
        1102: 'Банк для физических лиц (Белоруссия)',
        2701102: 'Банк для физических лиц (Белоруссия)',
        1101: 'Банк для юридических лиц (Белоруссия)',
        2701101: 'Банк для юридических лиц (Белоруссия)'
    }

    name = name or paysys_names_map[id_]
    paysys = mock_manager.create_object(mapper.Paysys, (firm_id, name), id=id_,
                                        firm_id=firm_id, certificate=certificate, **kwargs)
    paysys.configure_mock(name=name)
    mock_manager.add_object_key(mapper.Paysys, id_, paysys)

    return paysys


def create_invoice(mock_manager, client=None, external_id='Б-66666-6', paysys_id=1000, type='prepayment',
                   firm_id=1, paysys=None, contract=None, person=None, **kwargs):
    client = client or create_client(mock_manager)
    person = person or create_person(mock_manager, client)
    paysys = paysys or create_paysys(mock_manager, firm_id, paysys_id)
    mapper.Invoice.__mapper__.get_property('acts')
    return mock_manager.create_object(mapper.Invoice, external_id,
                                      external_id=external_id,
                                      client=client, client_id=client.id,
                                      person=person, person_id=person.id,
                                      paysys=paysys, paysys_id=paysys.id,
                                      firm_id=firm_id, type=type,
                                      contract=contract, contract_id=contract and contract.id,
                                      **kwargs)


def create_act(mock_manager, invoice, external_id='123456789', **kwargs):
    return mock_manager.create_object(mapper.Act, external_id, external_id=external_id,
                                      invoice=invoice, **kwargs)


def create_order(mock_manager, service_id=7, service_order_id=666666, client=None, agency=None):
    return mock_manager.create_object(mapper.Order, (service_id, service_order_id),
                                      service_id=service_id, service_order_id=service_order_id,
                                      client=client, agency=agency)


def create_service(mock_manager, id=7, name='Сервис', **kwargs):
    return mock_manager.create_object(mapper.Service, id=id, name=name)


def create_manager(mock_manager, manager_code=666, login='manager', name='Иванов Иван Иваныч'):
    manager = mock_manager.create_object(mapper.Manager, manager_code, login=login, domain_login=login)
    manager.configure_mock(name=name)
    mock_manager.add_object_key(mapper.Manager, name, manager)
    return manager


def create_contract(mock_manager, type_='GENERAL', external_id='66666/66', **kwargs):
    contract = mock_manager.create_object(mapper.Contract, external_id, external_id=external_id, type=type_, **kwargs)
    return contract


def create_contract_collateral(mock_manager, id=666, collateral_type_id=None, num='',
                               **kwargs):
    col = mock_manager.create_object(mapper.ContractCollateral, id=id,
                                     collateral_type_id=collateral_type_id, num=num,
                                     **kwargs)
    return col


def create_contract_attribute(mock_manager, id=1, collateral_id=666, code='CODE',
                              **kwargs):
    attr = mock_manager.create_object(mapper.AttributeValue, id=id,
                                      collateral_id=collateral_id, code=code, **kwargs)
    return attr


def create_passport(mock_manager, login='superuniquelogin2017pro',
                    passport_id=2130000022965343, client=None, client_id=None):
    p = mock_manager.create_object(mapper.Passport, login, login=login, passport_id=passport_id,
                                   client=client, client_id=client_id)
    mock_manager.add_object_key(mapper.Passport, passport_id, p)
    mock_manager.add_object_key(mapper.Passport, client_id, p)
    return p


def create_product(mock_manager, id_=666666, **kwargs):
    pr = mock_manager.create_object(mapper.Product, id=id_, **kwargs)
    mock_manager.add_object_key(mapper.Product, id_, pr)
    return pr


def create_firm(mock_manager, id=666, title='ООО Яндекс.Сексшоп'):
    return mock_manager.create_object(mapper.Firm, id=id, title=title)


def create_trust_payment(mock_manager, trust_payment_id='5х5х5х5х5х5х5х5х5х5х5х5х', id_=666, success=True):
    success_params = {
        'payment_dt': 1 if success else 0,
        'cancel_dt': 0
    }
    p = mock_manager.create_object(
        mapper.Payment, trust_payment_id, trust_payment_id=trust_payment_id, id=id_,
        **success_params
    )
    mock_manager.add_object_key(mapper.Payment, id_, p)
    return p


def create_simple_payment(mock_manager, payment_id, invoice=None):
    p = mock_manager.create_object(
        mapper.Payment, payment_id, invoice=invoice
    )
    return p


def create_promocode(mock_manager, code):
    pc = mock_manager.create_object(mapper.PromoCode, code)
    return pc
