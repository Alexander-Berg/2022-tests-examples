# coding: utf-8
from collections import defaultdict
from datetime import datetime, timedelta

from balance import balance_steps as steps
import balance.balance_db as db
import pytest
from contextlib import contextmanager

from btestlib.constants import YTSourceName, Export

SOURCE_NAME = YTSourceName.MARKET_SUBVENTIONS_SOURCE_NAME


@contextmanager
def replace_tlog_path(new_path):
    table_name = 'BO.T_COMPLETION_SOURCE'
    where = "where code='{}'".format(SOURCE_NAME)

    def update_url(url):
        return db.balance().execute(
            "update {table} set url=:url {where}".format(
                table=table_name,
                where=where
            ),
            dict(url=url)
        )

    old_path = db.balance().execute(
        "select url from {table} {where}".format(
            table=table_name,
            where=where
        ),
        single_row=True
    )['url']
    try:
        yield update_url(new_path)
    finally:
        update_url(old_path)


@pytest.mark.parametrize(
    'tlog_path',
    (
        #'//home/market/testing/mbi/billing/tlog/expenses/2021-06-22',
        '//home/market/testing/mbi/billing/tlog/expenses/2021-06-25',
        #'//home/balance-test/borograam/tlog/market_subvention/2021-06-22',
    )
)
def test_action(tlog_path):
    split = tlog_path.split('/')
    date = datetime.strptime(split[-1], '%Y-%m-%d')
    config_path = '/'.join(split[2:-1] + ['%(start_dt)s'])

    # забираем transaction_id забранных side payment
    yt_client = steps.YTSteps.create_yt_client()
    yt_data = steps.YTSteps.read_table(yt_client, tlog_path)
    transactions = [line['transaction_id'] for line in yt_data]

    # подменяем урл тлога
    with replace_tlog_path(config_path):
        # помечаем прошлый день забранным
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, date - timedelta(days=1),
                                                                     {'finished': datetime.now()})
        # забираем день
        steps.CommonPartnerSteps.create_partner_completions_resource(SOURCE_NAME, date)
        steps.CommonPartnerSteps.process_partners_completions(SOURCE_NAME, date)


    # хранилище одновременно используемых с клиентом сущностей
    client_mapper = dict()
    # список айдишников tpt
    tpt_ids = []

    # забираем side payments
    side_payments = steps.PartnerSteps.get_partner_payment_stat_with_export([1060, 1100], transactions)
    for side_payment in side_payments:
        # раскладываем в thirdparty_transaction
        steps.ExportSteps.create_export_record_and_export(
            side_payment['id'], Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT,
            service_id=side_payment['service_id'],
            with_export_record=False
        )
        # получаем tpt
        tpt_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            side_payment['id'],
            source='sidepayment'
        )
        assert len(tpt_data) == 1
        tpt_data, = tpt_data
        tpt_ids.append(tpt_data['id'])

        # запоминаем с кем используется клиент
        client_id = side_payment['client_id']
        if client_id not in client_mapper:
            client_mapper[client_id] = (tpt_data['person_id'], tpt_data['contract_id'])

    # закрываем месяц, генерируем акты
    for client_id, values in client_mapper.items():
        person, contract = values
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract, date)
        data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract)
        # act_data = steps.ActsSteps.get_all_act_data(client_id)
        # act_id = act_data[0]['id']
        # invoice_id = act_data[0]['invoice_id']
        # экспортируем в оебс

        steps.ExportSteps.export_oebs(
            client_id=client_id,
            person_id=person,
            contract_id=contract,
            # invoice_id=invoice_id,
            # act_id=act_id)
        )

    # экспортируем платежи в оебс
    for tpt_id in tpt_ids:
        steps.ExportSteps.export_oebs(
            transaction_id=tpt_id
        )

    for client_id, values in client_mapper.items():
        person, contract = values
        print 'contract: `{}`, client: `{}`, person: `{}`'.format(
            contract, client_id, person)
    for tpt_id in tpt_ids:
        print 'tpt row: `{}`'.format(tpt_id)


@pytest.mark.parametrize(
    'client_id, person, contract',
    (
            (1351061816, 15709011, 3923960),
            (1351061819, 15709012, 3923961)
    )
)
def test_reexport(client_id, person, contract):
    steps.ExportSteps.export_oebs(
        client_id=client_id,
        person_id=person,
        contract_id=contract,
        # invoice_id=invoice_id,
        # act_id=act_id)
    )

    print 'contract: `{}`, client: `{}`, person: `{}`'.format(
        contract, client_id, person)


@pytest.mark.parametrize(
    'tpt_id',
    (
            21918294430,
            21918294420,
            21918294410
    )
)
def test_reexport_tpt(tpt_id):
    steps.ExportSteps.export_oebs(
        transaction_id=tpt_id
    )
    print 'tpt row: `{}`'.format(tpt_id)
