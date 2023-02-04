# -*- coding: utf-8 -*-

import pytest
import random
import datetime as dt
from tests import object_builder as ob

from balance import mapper, muzzle_util as ut
from balance import constants as cnst
from balance.oebs_config import OebsConfig


def new_client(session, **attrs):
    res = ob.ClientBuilder(**attrs).build(session).obj
    return res


CODE_SUCCESS = 0
CODE_CLIENT_NOT_FOUND = 1003


def test_get_client_contracts_market_cpa(session, xmlrpcserver):
    pc = ob.PayOnCreditCase(session)
    now = ut.trunc_date(dt.datetime.now())
    cpa = pc.get_contract(
        dt=now + dt.timedelta(days=-10),
        finish_dt=now + dt.timedelta(days=10),
        commission=0,
        supercommission_bonus={150: 1},
        is_signed=1,
    )

    session.flush()

    at1 = pc.get_contract(
        dt=now + dt.timedelta(days=-20),
        finish_dt=now + dt.timedelta(days=20),
        commission=60,
        is_signed=1,
        attorney_agency_id=cpa.client.id,
        link_contract_id=cpa.id,
    )
    at2 = pc.get_contract(
        dt=now + dt.timedelta(days=-5),
        finish_dt=now + dt.timedelta(days=5),
        commission=60,
        is_signed=1,
        attorney_agency_id=cpa.client.id,
        link_contract_id=cpa.id
    )
    at3 = pc.get_contract(
        client=cpa.client,
        person=cpa.person,
        dt=now + dt.timedelta(days=-10),
        finish_dt=now + dt.timedelta(days=10),
        commission=60,
        is_signed=1,
        link_contract_id=cpa.id,
    )
    session.flush()

    r = xmlrpcserver.GetClientContracts(cpa.client_id)
    r = [x for x in r if x['ID'] == cpa.id][0]

    assert r['ID'] == cpa.id
    cpas = r['MARKET_CPA']
    assert len(cpas) == 3, repr(r)
    cpas1 = [c for c in cpas if c['CLIENT_ID'] == at1.client.id][0]
    cpas2 = [c for c in cpas if c['CLIENT_ID'] == at2.client.id][0]
    cpas3 = [c for c in cpas if c['CLIENT_ID'] == at3.client.id][0]

    assert cpas1['PERSON_ID'] == at1.person.id
    assert cpas2['PERSON_ID'] == at2.person.id
    assert cpas3['PERSON_ID'] == at3.person.id
    assert cpas1['CPA'][0][0].timetuple() == cpa.col0.dt.timetuple()
    assert cpas1['CPA'][1][0].timetuple() == cpa.col0.finish_dt.timetuple()
    assert cpas2['CPA'][0][0].timetuple() == at2.col0.dt.timetuple()
    assert cpas2['CPA'][1][0].timetuple() == at2.col0.finish_dt.timetuple()
    assert cpas3['CPA'][0][0].timetuple() == cpa.col0.dt.timetuple()
    assert cpas3['CPA'][1][0].timetuple() == cpa.col0.finish_dt.timetuple()


@pytest.mark.parametrize('ctype', ['SPENDABLE', 'GENERAL'])
def test_get_client_contracts_print_form_dt(ctype, session, xmlrpcserver):
    print_form_dt = dt.datetime.now()
    pc = ob.PayOnCreditCase(session)
    contract = pc.get_contract(
        is_signed=1,
        ctype=ctype,
        print_form_dt=print_form_dt,
        currency=978,
    )

    query = {
        'ClientID': contract.client_id,
        'PersonID': contract.person_id,
        'ContractType':  ctype,
        'Dt': dt.datetime.now()
    }

    resp = xmlrpcserver.GetClientContracts(query)
    assert resp[0]['PRINT_FORM_DT'] == print_form_dt


def test_get_client_contracts(session, xmlrpcserver):
    pc = ob.PayOnCreditCase(session)
    prod = pc.get_product_hierarchy(engine_id=11)
    c1 = pc.get_contract(
        commission=0,
        is_signed=1,
        payment_type=3,
        credit_limit={
            prod[0].activity_type.id: 4700,
            prod[1].activity_type.id: 1600,
        },
        services={11, 7, 70},
    )

    prod[2]._other.price.b.tax = 1
    prod[3]._other.price.b.tax = 1
    prod[2]._other.price.b.price = 50 * 30
    prod[3]._other.price.b.price = 51 * 30
    for p in prod:
        p.build(session)

    c2 = pc.get_contract(
        commission=1,
        is_signed=1,
        payment_type=3,
        credit_limit={
            prod[0].activity_type.id: 1700,
            prod[1].activity_type.id: 3600,
        },
        services={11, 7, 70},
        person_id=c1.person.id,
        client_id=c1.client.id,
        commission_type=3,
        supercommission=2
    )

    session.flush()

    r = xmlrpcserver.GetClientContracts(c1.client_id, c1.person_id, dt.datetime.now())

    r1 = [x for x in r if x['ID'] == c1.id][0]
    r2 = [x for x in r if x['ID'] == c2.id][0]

    assert r1['CONTRACT_TYPE'] == c1.commission
    assert r2['CONTRACT_TYPE'] == c2.commission


def test_get_client_contract_filtering(session, xmlrpcserver):
    def build_contract(services, client=None, signed=False):
        from billing.contract_iface import contract_meta
        if not services:
            raise RuntimeError('no services')
        oebs_config = OebsConfig(session)
        spendable_service_type_map = oebs_config.spendable_service_type_map

        is_spend = services and set(services).issubset(spendable_service_type_map)
        contract = mapper.Contract(ctype=contract_meta.ContractTypes(type=('SPENDABLE' if is_spend else 'GENERAL')))
        contract.client = ob.ClientBuilder().build(session).obj if not client else client
        contract.person = ob.PersonBuilder(client=contract.client,
                                           type='ur'
                                           ).build(session).obj
        contract.col0.dt = dt.datetime(2017, 1, 1)
        contract.external_id = str(random.randint(10000, 999999))
        contract.col0.manager_code = 1122
        if not is_spend:
            contract.col0.commission = 0
            contract.col0.payment_type = 2
        contract.col0.currency = 643 if is_spend else 810
        if signed:
            contract.col0.is_signed = dt.datetime(2017, 1, 2)
        return contract

    c1 = build_contract(services=[cnst.ServiceId.TAXI_PROMO], signed=True)
    c2 = build_contract(services=[cnst.ServiceId.AVIA], client=c1.client, signed=True)
    c3 = build_contract(services=[cnst.ServiceId.AVIA], client=c1.client, signed=True)
    session.add(c1)
    session.add(c2)
    session.flush()
    res = xmlrpcserver.GetClientContracts({'ClientID': c1.client.id, 'ContractType': 'GENERAL'})
    assert res == xmlrpcserver.GetClientContracts(c1.client.id)
    assert len(res), 2
    c_f = lambda c, i: [x for x in c if x['ID'] == i][0]
    c_f(res, c2.id)
    c_f(res, c3.id)
    res = xmlrpcserver.GetClientContracts({'ClientID': c1.client.id, 'ContractType': 'SPENDABLE'})
    assert len(res) == 1
    c_f(res, c1.id)
    res = xmlrpcserver.GetClientContracts({'ClientID': c1.client.id,
                                           'ContractType': 'GENERAL',
                                           'ExternalID': c2.external_id
                                           })
    assert len(res) == 1
    c_f(res, c2.id)


@pytest.mark.parametrize('contract_params, signed, expected_history', [
    pytest.param({
        'is_signed': dt.datetime(2020, 3, 2),
        'dt': dt.datetime(2020, 3, 2),
        'finish_dt': dt.datetime(2222, 1, 1),
        'services': {650, 111},
        'payment_type': 2,
        'partner_commission_pct2': 100,
        'ctype': 'GENERAL',
        'collaterals': [
            {'num': '01', 'dt': dt.datetime(2020, 4, 1), 'is_faxed': dt.datetime(2020, 4, 1),
             'collateral_type_id': 1001, 'services': {111, 128}},
            {'num': '02', 'dt': dt.datetime(2020, 4, 1), 'is_faxed': dt.datetime(2020, 4, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 50},
            {'num': '03', 'dt': dt.datetime(2020, 5, 1), 'is_faxed': None,
             'collateral_type_id': 1001, 'services': {128}},
            {'num': '04', 'dt': dt.datetime(2020, 6, 1), 'is_signed': dt.datetime(2020, 6, 1),
             'collateral_type_id': 1001, 'services': {667, 668}},
            {'num': '05', 'dt': dt.datetime(2020, 7, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 150},
            {'num': '06', 'dt': dt.datetime(2020, 8, 1), 'is_faxed': dt.datetime(2020, 4, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 666},
        ]},
        True,
        {'SERVICES': [
            [dt.datetime(2020, 3, 2), {650, 111}],
            [dt.datetime(2020, 4, 1), {111, 128}],
            [dt.datetime(2020, 6, 1), {667, 668}],
        ],
        'partner_commission_pct2': [
            [dt.datetime(2020, 3, 2), '100'],
            [dt.datetime(2020, 4, 1), '50'],
            [dt.datetime(2020, 8, 1), '666'],
        ]},
        id='signed_contract'),
    pytest.param({
        'is_signed': None,
        'dt': dt.datetime(2020, 3, 2),
        'finish_dt': dt.datetime(2222, 1, 1),
        'services': {650, 111},
        'partner_commission_pct2': 100,
        'firm': 13,
        'payment_type': 2,
        'ctype': 'GENERAL',
        'collaterals': [
            {'num': '01', 'dt': dt.datetime(2020, 4, 1),
             'collateral_type_id': 1001, 'services': {111, 128}},
            {'num': '02', 'dt': dt.datetime(2020, 4, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 50},
            {'num': '03', 'dt': dt.datetime(2020, 5, 1),
             'collateral_type_id': 1001, 'services': {128}},
            {'num': '04', 'dt': dt.datetime(2020, 6, 1),
             'collateral_type_id': 1001, 'services': {667, 668}},
            {'num': '05', 'dt': dt.datetime(2020, 7, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 150},
            {'num': '06', 'dt': dt.datetime(2020, 8, 1),
             'collateral_type_id': 1045, 'partner_commission_pct2': 666},
        ]},
        False,
        {'SERVICES': [
            [dt.datetime(2020, 3, 2), {650, 111}],
            [dt.datetime(2020, 4, 1), {111, 128}],
            [dt.datetime(2020, 5, 1), {128}],
            [dt.datetime(2020, 6, 1), {667, 668}],
        ],
        'partner_commission_pct2': [
            [dt.datetime(2020, 3, 2), '100'],
            [dt.datetime(2020, 4, 1), '50'],
            [dt.datetime(2020, 7, 1), '150'],
            [dt.datetime(2020, 8, 1), '666'],
        ]},
        id='unsigned_contract'),
])
def test_get_client_contracts_attribute_history(contract_params, signed, expected_history, session, xmlrpcserver):
    contract = ob.ContractBuilder(**contract_params).build(session).obj
    res = xmlrpcserver.GetClientContracts({'ClientID': contract.client.id,
                                           'ContractType': 'GENERAL',
                                           'AddHistoryForAttributes': ['SERVICES', 'partner_commission_pct2'],
                                           'Signed': signed,
                                           })
    assert len(res) == 1
    attr_history = res[0]['ATTRIBUTES_HISTORY']
    assert len(attr_history) == len(expected_history)
    assert len(attr_history['SERVICES']) == len(expected_history['SERVICES'])
    assert len(attr_history['partner_commission_pct2']) == len(expected_history['partner_commission_pct2'])
    for key in ['SERVICES', 'partner_commission_pct2']:
        for i in range(len(expected_history[key])):
            # сравниваем даты
            assert expected_history[key][i][0] == attr_history[key][i][0], (key, i)
            # сравниваем значения. В реальном результате сервисы в списке, поэтому приведем его сету.
            if isinstance(attr_history[key][i][1], list):
                attr_history[key][i][1] = set(attr_history[key][i][1])
            assert expected_history[key][i][1] == attr_history[key][i][1], (key, i)


@pytest.mark.parametrize('contract_params, add_finished, on_dt, expected_result', [
    pytest.param({'finish_dt': dt.datetime(2222, 1, 1), }, False, None,
                 {'FINISH_DT': dt.datetime(2222, 1, 1),
                  'IS_ACTIVE': 1,
                  'IS_SIGNED': 1,
                  'PAYMENT_TYPE': 2},
                 id='Finished in future, no dt, no add finished'),
    pytest.param({'finish_dt': dt.datetime(2222, 1, 1), }, False, dt.datetime(2021, 3, 2),
                 {'FINISH_DT': dt.datetime(2222, 1, 1),
                  'IS_ACTIVE': 1,
                  'IS_SIGNED': 1,
                  'PAYMENT_TYPE': 2},
                 id='Finished in future, dt during active period, no add finished'),
    pytest.param({'finish_dt': dt.datetime(2222, 1, 1), }, False, dt.datetime(2223, 3, 2),
                 None,
                 id='Finished in future, dt after active period, no add finished'),
    pytest.param({'finish_dt': dt.datetime(2222, 1, 1), }, True, dt.datetime(2223, 3, 2),
                 {'FINISH_DT': dt.datetime(2222, 1, 1),
                  'IS_ACTIVE': 0,
                  'IS_SIGNED': 1,
                  'PAYMENT_TYPE': 2},
                 id='Finished in future, dt after active period, add finished'),
    pytest.param({'finish_dt': dt.datetime(2021, 3, 2), }, False, dt.datetime(2223, 3, 2),
                 None,
                 id='Finished in past, no dt, not add finished'),
    pytest.param({'finish_dt': dt.datetime(2021, 3, 2), }, True, dt.datetime(2223, 3, 2),
                 {'FINISH_DT': dt.datetime(2021, 3, 2),
                  'IS_ACTIVE': 0,
                  'IS_SIGNED': 1,
                  'PAYMENT_TYPE': 2},
                 id='Finished in past, no dt, add finished'),
])
def test_get_client_contracts_add_finished(contract_params, add_finished, on_dt, expected_result,
                                           session, xmlrpcserver):
    common_params = {
        'is_signed': dt.datetime(2020, 3, 2),
        'dt': dt.datetime(2020, 3, 2),
        'services': {650, 111},
        'payment_type': 2,
        'partner_commission_pct2': 100,
        'ctype': 'GENERAL',
    }
    params = dict(common_params, **contract_params)
    contract = ob.ContractBuilder(**params).build(session).obj
    res = xmlrpcserver.GetClientContracts({'ClientID': contract.client.id,
                                           'ContractType': 'GENERAL',
                                           'AddFinished': add_finished,
                                           'Dt': on_dt,
                                           'Signed': 1,
                                           })
    if not expected_result:
        assert len(res) == 0, res
    else:
        assert len(res) == 1
        for key, value in expected_result.items():
            assert res[0][key] == value, (key, value, res)


def test_get_client_contracts_add_finished_several_contract(session, xmlrpcserver):
    finish_dt = dt.datetime(2020, 3, 1)
    params_1 = {
        'is_signed': dt.datetime(2020, 2, 1),
        'dt': dt.datetime(2020, 2, 1),
        'finish_dt': finish_dt,
        'services': {650, 111},
        'payment_type': 2,
        'partner_commission_pct2': 100,
        'ctype': 'GENERAL',
    }
    contract_1 = ob.ContractBuilder(**params_1).build(session).obj
    params_2 = {
        'client': contract_1.client,
        'is_signed': dt.datetime(2020, 3, 1),
        'finish_dt': None,
        'dt': dt.datetime(2020, 3, 1),
        'services': {650, 111},
        'payment_type': 2,
        'partner_commission_pct2': 100,
        'ctype': 'GENERAL',
    }
    contract_2 = ob.ContractBuilder(**params_2).build(session).obj
    res = xmlrpcserver.GetClientContracts({'ClientID': contract_1.client.id,
                                           'ContractType': 'GENERAL',
                                           'AddFinished': 0,
                                           'Signed': 1,
                                           })
    assert len(res) == 1
    for key, value in {
        'IS_ACTIVE': 1,
        'IS_SIGNED': 1,
        'ID': contract_2.id
    }.iteritems():
        assert 'FINISH_DT' not in res[0]
        assert res[0][key] == value, (key, value, res)

    res = xmlrpcserver.GetClientContracts({'ClientID': contract_1.client.id,
                                           'ContractType': 'GENERAL',
                                           'AddFinished': 1,
                                           'Signed': 1,
                                           })
    assert len(res) == 2
    contract_1_data, = filter(lambda _res: _res['ID'] == contract_1.id, res)
    contract_2_data, = filter(lambda _res: _res['ID'] == contract_2.id, res)

    for key, value in {
        'FINISH_DT': finish_dt,
        'IS_ACTIVE': 0,
        'IS_SIGNED': 1,
        'ID': contract_1.id
    }.iteritems():
        assert contract_1_data[key] == value, (key, value, contract_1_data)

    for key, value in {
        'IS_ACTIVE': 1,
        'IS_SIGNED': 1,
        'ID': contract_2.id
    }.iteritems():
        assert 'FINISH_DT' not in contract_2_data
        assert contract_2_data[key] == value, (key, value, contract_2_data)
