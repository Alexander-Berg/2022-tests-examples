# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime
import pytest
import mock

from balance import mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from balance.constants import *
from tests import object_builder as ob

from tests.balance_tests.rev_partners.common import (
    gen_contract,
    gen_actotron_rows_for_contract,
    compose_get_contract_func,
    gen_tpts_for_contract,
    generate_acts,
)


NS = 'oplata'
SID = ServiceId.MESSENGER
MIGRATE_DT = datetime.datetime(2021, 2, 1)
CLOSE_DT = datetime.datetime(2021, 4, 1)


def create_migration(session, namespace, filter_, object_id, from_dt, dry_run):
    migration = mapper.MigrationNewBilling(namespace=namespace, filter=filter_, object_id=object_id,
                                        from_dt=from_dt, dry_run=dry_run)
    session.merge(migration)
    session.flush()
    return migration


def set_configs(session, from_scheme, extra_args=None):
    session.config.__dict__['SERVICE_NEW_BILLING_NAMESPACE_MAP'] = [[SID, NS]]
    mc_cfg = {str(SID): {'from_scheme': from_scheme}}
    if extra_args:
        mc_cfg[str(SID)].update({'extra_completions_args': extra_args})
    session.config.__dict__['BILLING_30_MONTH_CLOSE_SERVICES'] = mc_cfg


def create_actotron_rows(contract, product):
    gen_actotron_rows_for_contract(contract, [dict(mdh_product_id=product.mdh_id,
                                                          tariffer_service_id=SID,
                                                          act_sum=666, act_effective_nds_pct=20,
                                                          act_start_dt=MIGRATE_DT,
                                                          act_finish_dt=ut.add_months_to_date(MIGRATE_DT, 1) - datetime.timedelta(1),
                                                          currency='RUB')])


def create_all_migrations(session, contract, migrated):
    if migrated:
        create_migration(session, NS, 'Client', contract.client_id, MIGRATE_DT, 0)
        create_migration(session, NS, 'Firm', contract.firm.id, ut.add_months_to_date(MIGRATE_DT, 1), 0)


@pytest.mark.parametrize('migrated,from_scheme,extra_args',
                         [(True, 'none', None),
                          (True, 'thirdparty', None),
                          (False, 'thirdparty', None),
                          (True, 'thirdparty', {'filter_paysys_types': ['one', 'two']}),
                          (True, 'acts', None),
                          (False, 'acts', None),
                          ]
)
def test_actotron_migration_flow(session, migrated, from_scheme, extra_args):
    con_func = compose_get_contract_func(SID)
    contract = gen_contract(
        session, postpay=True, personal_account=True, con_func=con_func
    )
    create_all_migrations(session, contract, migrated)
    set_configs(session, from_scheme, extra_args)
    product = ob.ProductBuilder(engine_id=SID, firm_id=contract.firm.id).build(session)
    create_actotron_rows(contract, product)

    with mock.patch('balance.reverse_partners.normalized_thirdparty_completions_new') as mocked_tc,\
         mock.patch('balance.reverse_partners.get_acted_qty', return_value=D(666)) as mocked_aq:
        a_m = mapper.ActMonth(for_month=CLOSE_DT)
        rpc = rp.ReversePartnerCalc(contract, [SID], a_m)
        rpc.process_and_enqueue_act()
        if from_scheme == 'thirdparty':
            mocked_tc.assert_called_once_with(contract,
                                              MIGRATE_DT if migrated else ut.add_months_to_date(CLOSE_DT, 1),
                                              service_id=SID,
                                              **(extra_args or {})
                                              )
        elif from_scheme == 'none':
            mocked_tc.assert_not_called()
        elif from_scheme == 'acts':
            mocked_tc.assert_not_called()
            if migrated:
                assert mocked_aq.call_count, 2
                mocked_aq.assert_any_call(rp.get_order(SID, contract, product), MIGRATE_DT)


@pytest.mark.parametrize('migrated', (True, False))
def test_combined_act(session, migrated):
    con_func = compose_get_contract_func(SID)
    contract = gen_contract(
        session, postpay=True, personal_account=True, con_func=con_func
    )
    create_all_migrations(session, contract, migrated)
    set_configs(session, 'thirdparty')
    product = ob.ProductBuilder(engine_id=SID, firm_id=contract.firm.id).build(session)
    create_actotron_rows(contract, product)


    gen_tpts_for_contract(contract, [dict(service_id=SID, 
                                          product_id=product.id,
                                          yandex_reward=D('6.66'),
                                          dt=MIGRATE_DT - datetime.timedelta(days=1),
                                          ),
                                     ]
                          )

    a_m = mapper.ActMonth(for_month=CLOSE_DT)
    rpc = rp.ReversePartnerCalc(contract, [SID], a_m)
    (dps, invoices) = rpc.process_and_enqueue_act()
    acts = generate_acts(contract, a_m, dps=dps, invoices=invoices)
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == (D("6.66") if not migrated else D('672.66'))

