# coding=utf-8
import datetime
import pytest

from balance import constants as cst, mapper
from tests import object_builder as ob

def test_yandex_bank_not_assigned_general(session, xmlrpcserver):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)

    params = {
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.AUTORU): cst.ServiceId.AUTORU,
        'payment-type': cst.POSTPAY_PAYMENT_TYPE,
        'payment_term': 10,
        'commission': cst.ContractTypeId.NON_AGENCY,
    }

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert getattr(state, 'yandex_bank_enabled', None) is None
    assert getattr(state, 'yandex_bank_wallet_pay', None) is None
    assert getattr(state, 'yandex_bank_wallet_id', None) is None
    assert getattr(state, 'yandex_bank_account_id', None) is None

def test_yandex_bank_not_assigned_spendable(session, xmlrpcserver):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)

    params = {
        'type': 'SPENDABLE',
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.TOLOKA): cst.ServiceId.TOLOKA,
        'is_offer': 0,
    }

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert getattr(state, 'yandex_bank_enabled', None) is None
    assert getattr(state, 'yandex_bank_wallet_pay', None) is None
    assert getattr(state, 'yandex_bank_wallet_id', None) is None
    assert getattr(state, 'yandex_bank_account_id', None) is None

@pytest.mark.parametrize('yandex_bank_enabled', [0, 1], ids=['OFF', 'ON'])
@pytest.mark.parametrize('yandex_bank_wallet_pay', [0, 1], ids=['ACCOUNT', 'WALLET'])
def test_yandex_bank_enabled_general(session,
                         xmlrpcserver,
                         yandex_bank_enabled,
                         yandex_bank_wallet_pay):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)

    params = {
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.AUTORU): cst.ServiceId.AUTORU,
        'payment-type': cst.POSTPAY_PAYMENT_TYPE,
        'payment_term': 10,
        'commission': cst.ContractTypeId.NON_AGENCY,
    }

    params.update({'yandex_bank_enabled': yandex_bank_enabled,
                   'yandex_bank_wallet_pay': yandex_bank_wallet_pay,
                   'yandex_bank_wallet_id': 'www-333' if yandex_bank_wallet_pay else None,
                   'yandex_bank_account_id': None if yandex_bank_wallet_pay else 'www-333'})

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert state.yandex_bank_enabled == yandex_bank_enabled
    if yandex_bank_enabled:
        assert state.yandex_bank_wallet_pay == yandex_bank_wallet_pay
        if yandex_bank_wallet_pay:
            assert state.yandex_bank_wallet_id == 'www-333'
        else:
            assert state.yandex_bank_account_id == 'www-333'

@pytest.mark.parametrize('yandex_bank_enabled', [0, 1], ids=['OFF', 'ON'])
@pytest.mark.parametrize('yandex_bank_wallet_pay', [0, 1], ids=['ACCOUNT', 'WALLET'])
def test_yandex_bank_enabled_spendable(session,
                         xmlrpcserver,
                         yandex_bank_enabled,
                         yandex_bank_wallet_pay):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0)

    params = {
        'type': 'SPENDABLE',
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.TOLOKA): cst.ServiceId.TOLOKA,
        'is_offer': 0,
    }

    params.update({'yandex_bank_enabled': yandex_bank_enabled,
                   'yandex_bank_wallet_pay': yandex_bank_wallet_pay,
                   'yandex_bank_wallet_id': 'www-333' if yandex_bank_wallet_pay else None,
                   'yandex_bank_account_id': None if yandex_bank_wallet_pay else 'www-333'})

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert state.yandex_bank_enabled == yandex_bank_enabled
    if yandex_bank_enabled:
        assert state.yandex_bank_wallet_pay == yandex_bank_wallet_pay
        if yandex_bank_wallet_pay:
            assert state.yandex_bank_wallet_id == 'www-333'
        else:
            assert state.yandex_bank_account_id == 'www-333'

@pytest.mark.parametrize('yandex_bank_enabled', [0, 1], ids=['OFF', 'ON'])
@pytest.mark.parametrize('yandex_bank_wallet_pay', [0, 1], ids=['ACCOUNT', 'WALLET'])
def test_yandex_bank_enabled_general_collateral(session,
                         xmlrpcserver,
                         yandex_bank_enabled,
                         yandex_bank_wallet_pay):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0) - datetime.timedelta(days=1)

    params = {
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed': 1,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.AUTORU): cst.ServiceId.AUTORU,
        'payment-type': cst.POSTPAY_PAYMENT_TYPE,
        'payment_term': 10,
        'commission': cst.ContractTypeId.NON_AGENCY,
    }

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert getattr(state, 'yandex_bank_enabled', None) is None
    assert getattr(state, 'yandex_bank_wallet_pay', None) is None
    assert getattr(state, 'yandex_bank_wallet_id', None) is None
    assert getattr(state, 'yandex_bank_account_id', None) is None

    params = {
        'DT': dt + datetime.timedelta(days=1),
        'IS_SIGNED': dt + datetime.timedelta(days=1),
        'yandex_bank_enabled': yandex_bank_enabled,
        'yandex_bank_wallet_pay': yandex_bank_wallet_pay,
        'yandex_bank_wallet_id': 'www-333' if yandex_bank_wallet_pay else None,
        'yandex_bank_account_id': None if yandex_bank_wallet_pay else 'www-333'
    }

    xmlrpcserver.CreateCollateral(session.oper_id, res['ID'], 1097, params)
    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.current_signed()

    assert state.yandex_bank_enabled == yandex_bank_enabled
    if yandex_bank_enabled:
        assert state.yandex_bank_wallet_pay == yandex_bank_wallet_pay
        if yandex_bank_wallet_pay:
            assert state.yandex_bank_wallet_id == 'www-333'
        else:
            assert state.yandex_bank_account_id == 'www-333'

@pytest.mark.parametrize('yandex_bank_enabled', [0, 1], ids=['OFF', 'ON'])
@pytest.mark.parametrize('yandex_bank_wallet_pay', [0, 1], ids=['ACCOUNT', 'WALLET'])
def test_yandex_bank_enabled_spendable_collateral(session,
                         xmlrpcserver,
                         yandex_bank_enabled,
                         yandex_bank_wallet_pay):
    client = ob.ClientBuilder(region_id=cst.RegionId.RUSSIA).build(session).obj
    person = ob.PersonBuilder(client=client).build(session).obj
    manager = ob.SingleManagerBuilder(manager_type=1).build(session).obj
    dt = session.now().replace(microsecond=0) - datetime.timedelta(days=1)

    params = {
        'type': 'SPENDABLE',
        'client-id': client.id,
        'person-id': person.id,
        'manager_code': manager.manager_code,
        'dt': dt,
        'is-signed': 1,
        'is-signed-dt': dt.isoformat(),
        'finish-dt': dt + datetime.timedelta(days=10),
        'firm': cst.FirmId.TAXI,
        'services': u'1',
        ('services-%s' % cst.ServiceId.TOLOKA): cst.ServiceId.TOLOKA,
        'is_offer': 0,
    }

    res = xmlrpcserver.CreateContract(session.oper_id, params)

    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.col0

    assert getattr(state, 'yandex_bank_enabled', None) is None
    assert getattr(state, 'yandex_bank_wallet_pay', None) is None
    assert getattr(state, 'yandex_bank_wallet_id', None) is None
    assert getattr(state, 'yandex_bank_account_id', None) is None

    params = {
        'DT': dt + datetime.timedelta(days=1),
        'IS_SIGNED': dt + datetime.timedelta(days=1),
        'yandex_bank_enabled': yandex_bank_enabled,
        'yandex_bank_wallet_pay': yandex_bank_wallet_pay,
        'yandex_bank_wallet_id': 'www-333' if yandex_bank_wallet_pay else None,
        'yandex_bank_account_id': None if yandex_bank_wallet_pay else 'www-333'
    }

    xmlrpcserver.CreateCollateral(session.oper_id, res['ID'], 7110, params)
    contract = session.query(mapper.Contract).getone(res['ID'])
    state = contract.current_signed()

    assert state.yandex_bank_enabled == yandex_bank_enabled
    if yandex_bank_enabled:
        assert state.yandex_bank_wallet_pay == yandex_bank_wallet_pay
        if yandex_bank_wallet_pay:
            assert state.yandex_bank_wallet_id == 'www-333'
        else:
            assert state.yandex_bank_account_id == 'www-333'
