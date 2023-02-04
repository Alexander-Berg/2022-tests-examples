# -*- coding: utf-8 -*-
import itertools
from contextlib import contextmanager
from datetime import datetime
from functools import partial

import pytest
from dateutil.relativedelta import relativedelta

from balance import constants as cst
from balance.constants import FirmId, RegionId, ServiceId
from balance.mapper import ContractLastAttr, ContractSignedAttr, TContractLastAttr, TContractSignedAttr, Contract, \
    CurrentContractSignedAttr
from balance.processors.oebs_api.wrappers import ContractWrapper
from billing.contract_iface import cmeta
from billing.contract_iface.contract_meta import collateral_types
from tests import object_builder as ob
from tests.balance_tests.contract_collateral_db_cache.conftest import set_collateral_attribute_links, create_collateral, \
    truncate_dt

# Use here string-like attribute with persistattr == True
TARGET_ATTRIBUTE = 'MEMO'
ORIGINAL_VALUE = "original value"
UPDATED_VALUE = "Updated value"

DT_ACCEPTED_DELTA_SECONDS = 2


def dont_flush_at_test(func):
    func.flush_at_test = False
    return func


@pytest.fixture(scope='session', autouse=True)
def verify_work_with_persistattr():
    attribute = cmeta.general.contract_attributes[TARGET_ATTRIBUTE]
    assert bool(attribute.persistattr)
    assert attribute.pytype in {'str', 'clob'}


@pytest.fixture()
def user_passport(session):
    passport = ob.create_passport(session, patch_session=True)
    return passport


def create_signed_contract(session, contract_type='GENERAL', passport=None):
    extra_params = {
        TARGET_ATTRIBUTE: ORIGINAL_VALUE,
    }
    if passport is not None:
        extra_params['passport'] = passport

    contr = ob.ContractBuilder(
        dt=datetime.now() - relativedelta(days=1),
        is_signed=datetime.now(),
        ctype=contract_type,
        **extra_params
    ).build(session).obj
    set_collateral_attribute_links(session, contr.col0)
    session.flush()
    return contr


def create_contract_with_same_dt_collaterals(session):
    contract = create_signed_contract(session)
    collateral_dt = datetime.now()
    contract.append_collateral(
        dt=collateral_dt,
        collateral_type=collateral_types['GENERAL'][1019],
        is_signed=None,
        memo='1',
    )
    contract.append_collateral(
        dt=collateral_dt,
        collateral_type=collateral_types['GENERAL'][1019],
        is_signed=None,
        memo='2',
    )
    contract.append_collateral(
        dt=collateral_dt,
        collateral_type=collateral_types['GENERAL'][1019],
        is_signed=None,
        memo='3',
    )
    return contract


@pytest.mark.parametrize(
    ('ignore_persistattr',),
    [
        (False,),
        (True,),
    ]
)
def test_latest_raw_attribute(session, ignore_persistattr, user_passport, ):
    contract = create_signed_contract(session)
    create_collateral(
        session,
        contract,
        {
            TARGET_ATTRIBUTE: UPDATED_VALUE,
            "dt": datetime.now(),
            "passport_id": user_passport.passport_id,
        },
    )

    latest_attributes = contract.get_actual(
        only_signed=False,
        db_cache_compat_mode=ignore_persistattr,
    ).get_latest_raw_attributes()

    target_attr_lst = [
        (attr, collateral)
        for (attr, collateral) in latest_attributes
        if attr.code == TARGET_ATTRIBUTE
    ]
    assert len(target_attr_lst) == 1
    raw_attr, set_collateral = target_attr_lst[0]

    latest_at_col0 = set_collateral == contract.col0
    assert latest_at_col0 == (not ignore_persistattr)


def create_signed_contract_with_collateral(
    session,
    collateral_passport,
    _xmlrpcserver,
    collateral_params,
    contract_type='GENERAL',
):
    if 'dt' not in collateral_params:
        collateral_params['dt'] = datetime.now()

    contract = create_signed_contract(session, contract_type)
    collateral_params['passport_id'] = collateral_passport.passport_id
    create_collateral(
        session,
        contract,
        collateral_params,
    )
    return contract


def create_cancelled_signed_contract(session, *args, **kwargs):
    contract = create_signed_contract(session)
    contract.col0.is_cancelled = datetime.now()
    set_collateral_attribute_links(session, contract.col0)
    session.flush()
    return contract


def create_only_faxed_collateral(session, *args, **kwargs):
    contract = ob.ContractBuilder(
        dt=datetime.now() - relativedelta(days=1),
        is_faxed=datetime.now(),
        **{
            TARGET_ATTRIBUTE: ORIGINAL_VALUE,
        }
    ).build(session).obj
    set_collateral_attribute_links(session, contract.col0)
    session.flush()
    return contract


def create_announcement_col0(session, _passport, _xmlrpcserver, announcement_type_id):
    assert announcement_type_id is not None
    contract = ob.ContractBuilder(
        dt=datetime.now() - relativedelta(days=1),
        ctype='PARTNERS',
        collateral_type_id=announcement_type_id,
        is_signed=None,
        is_faxed=None,
        **{
            TARGET_ATTRIBUTE: ORIGINAL_VALUE,
        }
    ).build(session).obj
    set_collateral_attribute_links(session, contract.col0)
    session.flush()
    assert contract.col0.class_name == 'ANNOUNCEMENT'
    assert contract.col0.is_cancelled is None
    assert contract.col0.is_faxed is None
    return contract


def create_announcement_extra_collateral(session, passport, xmlrpcserver, announcement_type_id):
    contract = create_signed_contract_with_collateral(
        session,
        passport,
        xmlrpcserver,
        {
            'collateral_type_id': announcement_type_id,
            'is_signed': None,
            'is_faxed': None,
        },
        contract_type='PARTNERS',
    )
    session.flush()
    assert contract.collaterals[-1].class_name == 'ANNOUNCEMENT'
    assert contract.collaterals[-1].is_cancelled is None
    assert contract.collaterals[-1].is_faxed is None
    return contract


def create_signed_cancelled_col0_with_signed_collateral(session, passport, xmlrpcserver, *args):
    contract = create_signed_contract_with_collateral(
        session,
        passport,
        xmlrpcserver,
        {
            'is_signed': datetime.now(),
            'memo': 'should not persist signed'
        },
    )
    contract.col0.is_cancelled = datetime.now()
    session.flush()
    latest_collateral = contract.get_collaterals()[-1]
    assert contract.col0.signed
    assert contract.col0.is_cancelled
    assert latest_collateral.signed
    assert not latest_collateral.is_cancelled
    assert latest_collateral.num is not None
    return contract


def create_not_signed_test_contract(session, *args):
    contract = ob.ContractBuilder(
        is_signed=None,
        ctype='DISTRIBUTION',
        test_mode=1,
        **{
            TARGET_ATTRIBUTE: ORIGINAL_VALUE,
        }
    ).build(session).obj
    set_collateral_attribute_links(session, contract.col0)

    session.flush()

    assert contract.col0.test_mode == 1
    assert not contract.col0.is_cancelled
    assert not contract.col0.is_signed
    assert not contract.col0.is_faxed
    return contract


def create_contract_update_field(session, *args):
    contract = ob.ContractBuilder(
        is_signed=datetime.now(),
        memo='some old value',
        services={67, 77, 70, 7},
    ).build(session).obj
    set_collateral_attribute_links(session, contract.col0)
    session.flush()

    contract.col0.memo = 'updated memo value'
    session.flush()
    return contract


def create_contract_collateral_before_col0(session, passport, *args):
    contract = create_signed_contract(session)
    collateral_params = {
        'dt': contract.col0.dt - relativedelta(days=1),
        'passport_id': passport.passport_id,
        TARGET_ATTRIBUTE: UPDATED_VALUE,
    }
    create_collateral(
        session,
        contract,
        collateral_params
    )
    return contract


def create_signed_test_contract_cancelled_col(session, passport, *args):
    contract = create_not_signed_test_contract(session, passport, *args)
    contract.col0.is_signed = datetime.now() - relativedelta(days=1)

    collateral_params = {
        'dt': datetime.now(),
        'collateral_type': contract.ctype.collateral_types.values()[0],
        'is_cancelled': datetime.now(),
        TARGET_ATTRIBUTE: UPDATED_VALUE
    }
    contract.append_collateral(**collateral_params)
    set_collateral_attribute_links(session, contract.collaterals[-1])

    return contract


def create_signed_contract_for_future(session, *args):
    contract = create_signed_contract(session)
    contract.col0.dt = contract.col0.is_signed + relativedelta(days=3)
    return contract


def pass_func(*args, **kwargs):
    pass


@contextmanager
def non_flushable_session(session):
    original_flush_func = session.flush
    session.flush = pass_func

    yield session

    session.flush = original_flush_func


def create_contract_not_flushed_collateral(session, *args):
    with non_flushable_session(session) as s:
        contract = create_signed_contract(s)
    return contract


def create_contract_for_passport_hook(session, passport, *args):
    contract = create_signed_contract(session, passport=passport)
    with non_flushable_session(session) as s:
        non_flushed_passport = ob.create_passport(s, passport_id=123, patch_session=False)
        contract.append_collateral(
            dt=datetime.now(),
            collateral_type=collateral_types['GENERAL'][1019],
            is_signed=None,
            memo='not yet in the db',
            passport=non_flushed_passport
        )
    return contract


def create_contract_update_collateral_other_person(session, passport, *args):
    contract = create_signed_contract(session, passport=passport)
    session.flush()

    other_passport = ob.create_passport(session, passport_id=123, patch_session=True)
    contract.col0.memo = 'Updated by other person'
    return contract


def create_contract_with_currency(session, currency_code):
    contract = ob.ContractBuilder(
        dt=datetime.now() - relativedelta(days=1),
        is_signed=datetime.now(),
        ctype='GENERAL',
        currency=currency_code,
    ).build(session).obj
    set_collateral_attribute_links(session, contract.col0)

    return contract


def create_contract_with_old_rub(session, *args):
    contract = create_contract_with_currency(session, currency_code=810)
    return contract


def create_contract_new_rub(session, *args):
    contract = create_contract_with_currency(session, currency_code=643)
    return contract


def create_contract_with_str(session, currency_str):
    contract = create_contract_with_currency(session, currency_code=810)
    for raw_attr in contract.col0.attributes:
        if raw_attr.code.upper() == 'CURRENCY':
            raw_attr.value_num = None
            raw_attr.value_str = currency_str
    return contract


def create_contract_with_rub_str(session, *args):
    contract = create_contract_with_str(session, 'RUB')
    return contract


def create_contract_new_numbered_collateral(session, passport, *args):
    contract = create_signed_contract(session, 'GENERAL', passport)
    session.flush()

    with non_flushable_session(session) as s:
        collateral = create_collateral(
            s,
            contract,
            {
                'num': 1,
                'dt': contract.col0.dt,
                'passport_id': passport.passport_id,
                TARGET_ATTRIBUTE: UPDATED_VALUE,
            }
        )
    assert collateral.id is None
    assert collateral.num == 1
    return contract


@dont_flush_at_test
def create_xmlrpc_offer_contract(session, passport, xmlrpcserver, *args):
    # Тест написан по наблюдению бага из продакшена.
    # Более лаконичная версия: create_contract_new_numbered_collateral и test_new_numbered_collateral_ordering
    # Суть бага:
    # xmlrpcserver.CreateCollateral создает колатерал без id, но с colnum, из-за чего нарушалась сортировка колатералов
    client = ob.ClientBuilder.construct(
        session,
        region_id=cst.RegionId.RUSSIA,
        full_repayment=1,
        suspect=1,
    )
    person = ob.PersonBuilder.construct(
        session,
        client=client,
        is_partner=1,  # точно нужно
        type='ur'  # точно нужно
    )
    manager = ob.SingleManagerBuilder.construct(
        session,
        manager_type=1,
        is_sales=1,
        firm_id=FirmId.TAXI,
        office_id=1,
        passport_id=passport.passport_id
    )

    params = {
        'currency': 'RUR',
        'country': RegionId.RUSSIA,
        'manager_uid': manager.passport_id,
        'ctype': 'SPENDABLE',
        'services': [ServiceId.TAXI_CORP, ServiceId.TAXI_CORP_PARTNERS],
        'firm_id': FirmId.TAXI,
        'payment_type': 1,
        'client_id': client.id,
        'person_id': person.id,
        'nds': 0,
    }
    contract_info = xmlrpcserver.CreateOffer(
        session.oper_id,
        params,
    )

    xmlrpcserver.CreateCollateral(
        session.oper_id,
        contract_info['ID'],
        7080,  # Spendable -> 'изменение способа вывода денег'
        {
            'WITHDRAW_ON_DEMAND': 1,
            'PRINT_FORM_TYPE': 3,
        }
    )

    contract = session.query(Contract).get(contract_info['ID'])
    return contract


@dont_flush_at_test
def verify_persistence_independently_of_order(session, *args):
    contract = create_signed_contract(session)
    signed_dt = contract.col0.dt - relativedelta(days=10)
    col1 = create_collateral(
        session,
        contract,
        {
            'dt': contract.col0.dt,
            TARGET_ATTRIBUTE: '1'
        },
    )
    col2 = create_collateral(
        session,
        contract,
        {
            'dt': contract.col0.dt,
            TARGET_ATTRIBUTE: '2',
        },
    )
    contract.col0.is_signed = None
    col1.is_signed = None
    col2.is_signed = None
    session.flush()

    def sign_and_get_attr(col):
        col.is_signed = signed_dt
        session.flush()
        memo_at_col = (
            session.query(CurrentContractSignedAttr)
                .filter_by(contract_id=contract.id, code=TARGET_ATTRIBUTE)
                .all()
        )[0]
        session.refresh(memo_at_col)
        return memo_at_col

    memo_at_col0 = sign_and_get_attr(contract.col0).value_str
    memo_at_col2 = sign_and_get_attr(col2).value_str
    memo_at_col1 = sign_and_get_attr(col1).value_str

    assert memo_at_col0 == contract.col0.memo
    assert memo_at_col2 == col2.memo
    assert memo_at_col1 == col2.memo

    return contract


@pytest.mark.parametrize(
    ('target_view', 'target_table', 'ignored_view_fields'),
    [
        (ContractLastAttr, TContractLastAttr, {'collateral_dt', 'update_dt'},),
        (ContractSignedAttr, CurrentContractSignedAttr, {'attribute_batch_id', 'update_dt'},),
    ]
)
@pytest.mark.parametrize(
    ('contract_func',),
    [
        (verify_persistence_independently_of_order,),
        (create_contract_new_numbered_collateral,),
        (create_xmlrpc_offer_contract,),
        (create_contract_with_rub_str,),
        (create_contract_with_old_rub,),
        (create_contract_new_rub,),
        (create_contract_update_collateral_other_person,),
        (create_contract_for_passport_hook,),
        (create_contract_not_flushed_collateral,),
        (create_signed_contract_for_future,),
        (create_signed_test_contract_cancelled_col,),
        (create_contract_collateral_before_col0,),
        (create_contract_update_field,),
        (create_not_signed_test_contract,),
        (create_signed_cancelled_col0_with_signed_collateral,),
        (create_cancelled_signed_contract,),
        (create_only_faxed_collateral,),
        (
            partial(
                create_announcement_col0,
                announcement_type_id=2100,
            ),
        ),
        (
            partial(
                create_announcement_col0,
                announcement_type_id=2090,
            ),
        ),
        (
            partial(
                create_announcement_extra_collateral,
                announcement_type_id=2100,
            ),
        ),
        (
            partial(
                create_announcement_extra_collateral,
                announcement_type_id=2090,
            ),
        ),
        (
            partial(
                create_signed_contract_with_collateral,
                collateral_params={
                    TARGET_ATTRIBUTE: UPDATED_VALUE,
                    'num': None,
                    'is_cancelled': datetime.now(),
                },
            ),
        ),
        (
            partial(
                create_signed_contract_with_collateral,
                collateral_params={TARGET_ATTRIBUTE: UPDATED_VALUE},
            ),
        ),
        (
            partial(
                create_signed_contract_with_collateral,
                collateral_params={
                    TARGET_ATTRIBUTE: UPDATED_VALUE,
                    'is_signed': datetime.now(),
                },
            ),
        ),
        (
            partial(
                create_signed_contract_with_collateral,
                collateral_params={TARGET_ATTRIBUTE: u'\u3042\xe4'},
            ),
        ),
        (
            partial(
                create_signed_contract_with_collateral,
                collateral_params={'SERVICES': {67, 77, 70, 7}},
            ),
        ),
    ],
)
def test_immediate_attribute_view_table_correspondence(
    session,
    contract_func,
    target_view,
    target_table,
    ignored_view_fields,
    user_passport,
    xmlrpcserver,
):
    VIEW_FIELDS = {
        'contract_id',
        'id',
        'collateral_id',
        'dt',
        'code',
        'key_num',
        'value_str',
        'value_num',
        'update_dt',
        'value_dt',
        'passport_id',
        'collateral_dt',
        'attribute_batch_id',
    }

    comparison_fields = VIEW_FIELDS.difference(ignored_view_fields)

    VIEW_FIELD_RENAMING = {
        'id': 'contract_attribute_id'
    }

    contract = contract_func(session, user_passport, xmlrpcserver)
    if getattr(contract_func, 'flush_at_test', True):
        session.flush()

    view_data = session.query(target_view).filter_by(contract_id=contract.id).all()
    table_data = session.query(target_table).filter_by(contract_id=contract.id).all()

    assert len(view_data) == len(table_data)

    table_data_named = {
        (raw_attr.code, raw_attr.key_num): raw_attr
        for raw_attr in table_data
    }

    for view_raw_attr in view_data:

        attribute_key = (view_raw_attr.code, view_raw_attr.key_num)
        assert attribute_key in table_data_named, \
            "Attribute %s is not present in the table %s" % (
                attribute_key,
                target_table.__table__,
            )

        table_raw_attr = table_data_named[attribute_key]

        for raw_attr_field_name in comparison_fields:
            renamed_field_name = VIEW_FIELD_RENAMING.get(raw_attr_field_name, raw_attr_field_name)

            assert hasattr(table_raw_attr, renamed_field_name), \
                "Table %s lacks column %s" % (target_table.__table__, renamed_field_name)

            view_field_value = getattr(view_raw_attr, raw_attr_field_name)
            table_field_value = getattr(
                table_raw_attr,
                renamed_field_name,
            )
            assert view_field_value == table_field_value, \
                "Field: %s.%s" % (view_raw_attr.code, renamed_field_name)


def combine_duplicates(attrs):
    key_func = lambda attr: (attr['code'], attr['key_num'], attr['from_dt'], attr['to_dt'])
    data = sorted(attrs, key=key_func)
    duplicates = {}
    for key, group in itertools.groupby(data, key_func):
        g_tpl = tuple(group)
        if len(g_tpl) > 1:
            duplicates[key] = g_tpl
    return duplicates


def test_signed_contract_multiple_collateral_changes_same_day(session):
    contract = create_contract_with_same_dt_collaterals(session)
    today = truncate_dt(datetime.today())
    collateral_start = today + relativedelta(days=1)
    faxed_dt = today - relativedelta(days=1)
    for collateral_num, collateral in enumerate(contract.collaterals):
        collateral.dt = collateral_start
        collateral.is_faxed = faxed_dt
        collateral.memo = str(collateral_num)

    signed_attrs_now = contract._get_insert_signed_attrs(datetime.now())
    duplicates = combine_duplicates(signed_attrs_now)
    assert len(duplicates) == 0, "Duplicated keys are:\n{}".format(
        '\n'.join(map(str, duplicates.keys()))
    )

    memo_attrs = [
        raw_attr
        for raw_attr in signed_attrs_now
        if raw_attr['code'] == 'MEMO'
    ]

    assert len(memo_attrs) == 1
    memo_attr = memo_attrs[0]

    assert memo_attr['from_dt'] == contract.collaterals[-1].dt
    assert memo_attr['to_dt'] == datetime.max
    assert memo_attr['value_str'] == contract.collaterals[-1].memo

    # If flush now, then the error, if present,
    # will be caught and rolled back by try block inside "on_before_flush"


def test_preserve_collateral_order_same_dt(session):
    contract = create_contract_with_same_dt_collaterals(session)
    session.flush()

    collaterals_info = ContractWrapper(contract).get_info()['collaterals']
    collaterals_order = [
        col.get('colnum', 0)
        for col in collaterals_info
    ]
    assert all([x < y for x, y in zip(collaterals_order, collaterals_order[1:])])


def test_old_test_mode_logic(session, user_passport):
    contract = create_signed_test_contract_cancelled_col(session, user_passport)
    contract.collaterals[-1].is_cancelled = None
    session.flush()

    collateral_history = list(contract.collateral_history_generator(
        check_signed=True,
        on_dt=datetime.now(),
        db_cache_compat_mode=False
    ))

    assert len(collateral_history) == 1
    assert collateral_history[0] == contract.col0


def test_contract_preserve_collateral_order(session, user_passport):
    """
    Test that collateral_history_generator creates a copy of collaterals and sort it
    instead of changing collaterals order in contract obj
    """
    contract = create_contract_collateral_before_col0(session, user_passport)

    assert contract.col0.num is None


def test_new_numbered_collateral_ordering(session, user_passport):
    contract = create_contract_new_numbered_collateral(session, user_passport)

    collateral_order = list(contract.collateral_history_generator(
        check_signed=False,
        on_dt=datetime.now(),
        db_cache_compat_mode=True,
    ))

    assert collateral_order == contract.collaterals


@pytest.mark.parametrize(
    ('contract_func', 'at_dt', 'expected_count'),
    [
        (create_signed_contract_for_future, datetime.now(), 0),
        (create_signed_contract_for_future, datetime.now() + relativedelta(days=4), 6),
    ]
)
def test_contract_expected_attr_count_at_dt(
    session,
    contract_func,
    at_dt,
    expected_count,
    user_passport,
    xmlrpcserver,
):
    contract = contract_func(session, user_passport, xmlrpcserver)
    session.flush()

    signed_attributes_at_time = session.query(TContractSignedAttr).filter(
        TContractSignedAttr.contract_id == contract.id,
        TContractSignedAttr.from_dt <= at_dt,
        at_dt < TContractSignedAttr.to_dt,
    ).all()

    assert len(signed_attributes_at_time) == expected_count


@pytest.fixture()
def signed_contract_today_collateral(session):
    """
    Контракт создан вчера, колатерал начинает действовать сегодня,
    атрибуты заполнены вчера.
    """
    contract = create_signed_contract(session)
    create_collateral(
        session,
        contract,
        {
            'dt': truncate_dt(datetime.now()),
            'is_signed': truncate_dt(datetime.now()),
            TARGET_ATTRIBUTE: UPDATED_VALUE,
        },
    )

    contract._on_before_flush_attributes(
        from_dt=contract.col0.dt,
    )
    return contract


def test_signed_attributes_fill(session, signed_contract_today_collateral):
    contract = signed_contract_today_collateral
    assert len(contract.collaterals) == 2, 'Test expects only 2 collaterals present'
    col0 = contract.col0
    col1 = contract.collaterals[1]

    signed_attributes = (
        session.query(TContractSignedAttr)
        .filter_by(contract_id=contract.id)
        .all()
    )
    assert len(signed_attributes) == sum(map(lambda col: len(col.attributes), contract.collaterals))

    col0_attrs = {
        (attr.code, attr.key_num)
        for attr in col0.attributes
    }

    col1_attrs = {
        (attr.code, attr.key_num)
        for attr in col1.attributes
    }

    only_col0_attrs = col0_attrs - col1_attrs

    for attr in signed_attributes:
        attr_key = (attr.code, attr.key_num)
        if attr_key in only_col0_attrs:
            assert attr.from_dt == truncate_dt(col0.dt)
            assert attr.to_dt == truncate_dt(datetime.max)
        elif attr.collateral_id == col0.id:
            assert attr.from_dt == truncate_dt(col0.dt)
            assert attr.to_dt == truncate_dt(col1.dt)
        elif attr.collateral_id == col1.id:
            assert attr.from_dt == truncate_dt(col1.dt)
            assert attr.to_dt == truncate_dt(datetime.max)
        else:
            assert False, 'Did not expect to go here'


def test_contract_collateral_view_unique(session, signed_contract_today_collateral):
    """
    Проверяет, что в день начала колатерала существует только
    одна запись про атрибут и эта запись из нового колатерала
    """
    contract = signed_contract_today_collateral
    current_attributes = (
        session.query(CurrentContractSignedAttr)
            .filter_by(contract_id=contract.id)
            .all()
    )

    target_attribute_lst = [
        attr
        for attr in current_attributes
        if attr.code == TARGET_ATTRIBUTE
    ]
    assert len(target_attribute_lst) == 1

    target_attribute = target_attribute_lst[0]
    assert target_attribute.collateral_id == contract.collaterals[-1].id
