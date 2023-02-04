# coding=utf-8

import json
import os

import pytest
import datetime
from hamcrest import equal_to, contains_string, is_in, not_
from decimal import Decimal as D

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils as butils
from check import retrying, utils, db
from check import steps as check_steps
from check.defaults import DATA_DIR
from check.defaults import Services, Products
from check.utils import need_data_regeneration
from btestlib.constants import OEBSOperationType
from dateutil.relativedelta import relativedelta
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT_CLONE
from balance.balance_steps.new_taxi_steps import TaxiSteps

CHECK_DEFAULTS = {
    'iob_market':
        {'service_id': Services.market,
         'product_id': Products.market,
         'paysys_id': 1003,
         'person_category': 'ur',
         'person_additional_params': None,
         'firm_id': 111},
}

check_list = CHECK_DEFAULTS.keys()


@retrying.retry(stop_max_attempt_number=5, wait_exponential_multiplier=1 * 1000)
def export(inv_id):
    steps.ExportSteps.export_oebs(invoice_id=inv_id)


def create_client_and_person(check_code):

    client_id = check_steps.create_client()
    person_id = check_steps.create_person(
        client_id, person_category=CHECK_DEFAULTS[check_code]['person_category'],
        additional_params=CHECK_DEFAULTS[check_code].get('person_additional_params', None)
    )
    agency_id = None
    steps.ExportSteps.export_oebs(client_id=client_id)
    return client_id, person_id, agency_id


def create_invoice(check_code, orders_map):
    client_id, person_id, agency_id = create_client_and_person(check_code)

    invoice_map = check_steps.create_invoice_map(orders_map, client_id, person_id)
    export(invoice_map['id'])
    return invoice_map


def create_correction_netting(invoice_eid, amount, dt, netting_amount):
    TaxiSteps.create_cash_payment_fact(invoice_eid, netting_amount, dt, OEBSOperationType.INSERT_NETTING)
    return TaxiSteps.create_cash_payment_fact(invoice_eid, -amount, dt, OEBSOperationType.CORRECTION_NETTING)


def insert_netting_into_oebs(amount, receipt_date, receipt_number, operation_type=u'INSERT_NETTING',
                             source_table=u'XXAP_AGENT_PAYMENT_BATCHES', cash_receipt_number='X-3186776'):

    def get_cach_id():
        return api.test_balance().ExecuteSQL(
            'oebs_qa', 'select XXAR_CASH_PAYMENT_FACT_SEQ.NEXTVAL as val from dual')[0]['val']

    cash_fact_id = get_cach_id()
    while api.test_balance().ExecuteSQL(
            'oebs_qa',
            'select * from xxfin.xxar_cash_payment_fact where xxar_cash_fact_id= {}'.format(cash_fact_id)) not in (
            None, []):
        cash_fact_id = get_cach_id()

    query = u'''insert into xxfin.xxar_cash_payment_fact
            (amount, receipt_number, receipt_date, last_updated_by, last_update_date, last_update_login, created_by,
            creation_date, xxar_cash_fact_id, cash_receipt_number, operation_type, source_table, source_id)
            values
            ({amount}, 'ЛСТ-{receipt_number}-1', to_date('{receipt_date}','DD.MM.YY HH24:MI:SS'), 4612,
            to_date('{receipt_date}','DD.MM.YY HH24:MI:SS'), 101387046, 4612,
            to_date('{receipt_date}','DD.MM.YY HH24:MI:SS'), {cash_id}, '{cash_receipt_number}', '{operation_type}',
            '{source_table}', 428211)'''.format(amount=str(amount),
                                                receipt_date=receipt_date.strftime('%d.%m.%y %H:%M:%S'),
                                                receipt_number=str(receipt_number.split('-')[1]),
                                                operation_type=operation_type,
                                                source_table=source_table, cash_id=cash_fact_id,
                                                cash_receipt_number=cash_receipt_number)
    api.test_balance().ExecuteSQL('oebs_qa', query)


def create_netting(netting_delta=0, cash_receipt_number='X-3186776'):
    context = TAXI_RU_CONTEXT_CLONE
    CONTRACT_START_DT = butils.Date.first_day_of_month(datetime.datetime.now() - relativedelta(months=1))
    NETTING_DT = butils.Date.nullify_time_of_date(datetime.datetime.now())
    NETTING_PCT = D('100')

    correction_netting_amount = D('50.3')
    insert_netting_amount = D('120.6')

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params={
                                                                                           'netting': 1,
                                                                                           'netting_pct': NETTING_PCT,
                                                                                           'start_dt': CONTRACT_START_DT
                                                                                       })

    invoice_id, invoice_eid = TaxiSteps.get_commission_personal_account_by_client_id(contract_id)
    if not netting_delta:
        create_correction_netting(invoice_eid, correction_netting_amount, NETTING_DT,
                                                       insert_netting_amount)
        TaxiSteps.process_payment(invoice_id, True)
    steps.ExportSteps.export_oebs(person_id=person_id)
    steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, invoice_id=invoice_id)
    insert_netting_into_oebs(
        insert_netting_amount + netting_delta, NETTING_DT, invoice_eid, cash_receipt_number=cash_receipt_number)
    insert_netting_into_oebs(correction_netting_amount, NETTING_DT, invoice_eid, 'CORRECTION_NETTING',
                             'XXAP_AGENT_REWARD_DATA', cash_receipt_number=cash_receipt_number)
    return invoice_id, invoice_eid


def new_section(description):
    print(description)
    return description


def get_invoice_taxi(invoices_list):
    return invoices_list.pop()


def generate_new_cache(invoices):
    for check_code in check_list:
        invoices[check_code] = {}
        check_defaults = CHECK_DEFAULTS[check_code]
        print (u'Генерируем данные для сверки ' + str(check_code))
        create_invoice_ = lambda: create_invoice(check_code,
                                                 {1: {'paysys_id': check_defaults.get('paysys_id'),
                                                      'service_id': check_defaults.get('service_id'),
                                                      'product_id': check_defaults.get('product_id'),
                                                      'shipment_info': {'Bucks': 30}}})

        # ----------------------------------------------------------------------------

        # ----------------------------------------------------------------------------
        # if check_code in ['iob_market']:
        #     invoice_map = create_invoice_()
        #     description = new_section('CHECK-2201')
        #     inv_id, _, _ = check_steps.create_invoice_for_check_endbuyer(check_defaults['service_id'],
        #                                                                  check_defaults['product_id'],
        #                                                                  check_defaults['paysys_id'],
        #                                                                  check_defaults[
        #                                                                      'person_additional_params'],
        #                                                                  firm=check_defaults['firm_id'])
        #     external_id = balance().execute('select external_id from bo.t_invoice where id= :inv_id',
        #                                     {'inv_id': inv_id})[0]['external_id']
        #     invoices[check_code][description] = {'id': inv_id, 'eid': external_id, 'expected': 0,
        #                                          'invoice': invoice_map}
            # ----------------------------------------------------------------------------
    return invoices


@pytest.fixture(scope="module")
def fixtures():
    invoices_cache = None
    file_path = os.path.join(DATA_DIR, 'iob_data.txt')
    if os.path.exists(file_path):
        with open(file_path, 'r') as fh:
            invoices_cache = json.load(fh)
    invoices = {}
    # invoices_cache = None
    if need_data_regeneration(invoices_cache, type_='invoice'):
        invoices_cache = generate_new_cache(invoices)
        utils.create_data_file('iob_data.txt', json.dumps(check_steps.make_serializable(invoices_cache)))

    # Create list of all act external_ids
    for check_code in check_list:
        # last_check_run_times[check_code] = check.db.get_max_cmp_dt(check_code)
        objects = []
        for description in invoices_cache[check_code]:
            objects.append(invoices_cache[check_code][description]['eid'].encode('utf8'))
        cmp_id = utils.run_check_new(check_code, str(','.join(objects)))
        invoices_cache[check_code]['cmp_id'] = cmp_id
        invoices_cache[check_code]['diff_invoices'] = get_diff_orders(
            cmp_id=cmp_id,
            check_code_name=check_code if check_code in ['iob_auto', 'iob_sw', 'iob_tr', 'iob_ua',
                                                         'iob_us'] else 'iob')

    return invoices_cache


def get_diff_orders(cmp_id, check_code_name):
    query = """
    select eid, state
    from cmp.{0}_cmp_data
    where cmp_id = {1}
""".format(check_code_name, cmp_id)
    return {
        row['eid']: row['state']
        for row in api.test_balance().ExecuteSQL('cmp', query)
        }

@pytest.mark.xfail(reason=u'пробема при выгрузке договоров')
@pytest.mark.parametrize("type_", ['iob_market'])
@pytest.mark.parametrize(
    "description",
    [
        'CHECK-2201'
    ],
    ids=lambda x: x
)
def test_iob(fixtures, description, type_):
    print(description)
    fixture = fixtures[type_][description]
    check_diff(fixture['eid'], fixtures[type_]['diff_invoices'],
               fixture['expected'])


def test_CHECK_2585_netting_wod_wd():
    invoice_eids = []

    _, invoice_eid_wod = create_netting()
    invoice_eids.append(invoice_eid_wod.encode('utf-8'))

    _, invoice_eid_wd = create_netting(100, cash_receipt_number='X-3186779')
    invoice_eids.append(invoice_eid_wd.encode('utf-8'))

    eids = ','.join(invoice_eids)
    cmp_id = utils.run_check_new('iob_taxi', eids)

    cmp_data = db.get_cmp_diff(cmp_id, 'iob')

    result = [(row['eid'], row['state']) for row in cmp_data]

    butils.check_that(result, not_(contains_string(invoice_eid_wod)))
    butils.check_that((invoice_eid_wd, 4), is_in(result))




def check_diff(act_eid, diff_orders, expected_state):
    if act_eid in diff_orders.keys():
        state = diff_orders[act_eid]
    else:
        state = 0

    print('external_id - ' + act_eid)
    print('state = ' + str(state) + ';   expected - ' + str(expected_state))
    butils.check_that(state, equal_to(expected_state))

