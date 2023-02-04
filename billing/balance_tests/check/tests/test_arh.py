# coding: utf-8
__author__ = 'chihiro'

from datetime import datetime, timedelta
from decimal import Decimal
import json

import pytest
from dateutil.relativedelta import relativedelta
from startrek_client import Startrek
from hamcrest import contains_string, equal_to

from balance import balance_api as api
from balance import balance_db as db
from btestlib import utils as b_utils
from check import shared_steps, utils
from check.shared import CheckSharedBefore
from check.defaults import STARTREK_PARAMS

BEGIN = (datetime.now() - timedelta(days=2)).strftime('%d.%m.%y %H:%M:%S')
END = (datetime.now() - timedelta(days=1)).strftime('%d.%m.%y %H:%M:%S')
CONTRACT_NAME = '17-309/11'
LINES_REWARD_AMOUNT = Decimal('0')
HEADER_REWARD_AMOUNT = Decimal('11')
PAY_AMOUNT = Decimal('4')
DIFFS_COUNT = 9

TEXT_1 = u"""производился ли ручной расчет вознаграждения для следующих договоров"""

TEXT_2 = u"""причины, по которым отсутствуют данные по вознаграждению для следующих договоров"""

TEXT_3 = u"""производился ли ручной расчет вознаграждения для следующих договоров"""


def setup_module(module):
    query = "select value_json from t_config where item = 'arh_auto_analysis_responsible'"
    arh_auto_analysis_responsible = json.loads(api.test_balance().ExecuteSQL('cmp', query)[0]['value_json'])
    query = "select value_json from t_config where item = 'arh_manual_exclusions'"
    arh_manual_exclusions = json.loads(api.test_balance().ExecuteSQL('cmp', query)[0]['value_json'])

    if arh_auto_analysis_responsible['0'] != ['azurkin', 'prolidol']:
        arh_auto_analysis_responsible['0'] = ['azurkin', 'prolidol']
        arh_auto_analysis_responsible['64553'] = ['chihiro']
        arh_auto_analysis_responsible['64554'] = ['aikawa']
        arh_auto_analysis_responsible = json.dumps(arh_auto_analysis_responsible)
        query = "update t_config set value_json = '{}' where item = 'arh_auto_analysis_responsible'".format(
            arh_auto_analysis_responsible)
        api.test_balance().ExecuteSQL('cmp', query)

    if '573011/11' not in arh_manual_exclusions:
        arh_manual_exclusions.append('573011/11')
        arh_manual_exclusions = json.dumps(arh_manual_exclusions)
        query = "update t_config set value_json = '{}' where item = 'arh_manual_exclusions'".format(
            arh_manual_exclusions)
        api.test_balance().ExecuteSQL('cmp', query)


def insert_into_header_comissioners(header_id, contract_name, nach_amount, perech_amount, begin, end,
                                    calc_confirm_status='N', org_id=121, delta=1):
    new_id = db.oebs().execute('select max(id) as new_id from apps.xxar_header_comissioners')[0]['new_id'] + delta
    nach_amount = get_amount(header_id, nach_amount)
    perech_amount = get_amount(header_id, perech_amount)
    db.oebs().execute("""
                      insert
                      into apps.xxar_header_comissioners
                      (id, period_from, person_id, person_name, contract_id, contract_name, send_status, receive_status,
                      seen_status, receive_doc_status, paid_status, creation_date, created_by, last_update_date,
                      last_updated_by, last_update_login, period_start_date, period_end_date, period_to, summary,
                      party_id, check_status, receive_akt_status, receive_schet_status, receive_sf_status, description,
                      keep_no_status, accept_status, num_report, status_pay_not, currency_code, comiss_vat_type,
                      send_original_status, send_original_date, calc_confirm_status, nach_amount, perech_amount,
                      documents, org_id, assign_customer_id, avans_amount)
                      values
                      ({new_id}, 'Июл-07', 15290, 'АртПром', {header_id}, '{contract_name}', 'N', 'N', 'Y', 'Y',
                      null, to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'), 1212,
                      to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'), 1190, null,
                      to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'), to_date('{end_dt}','DD.MM.YY HH24:MI:SS'),
                      'Июл-07', 'N', 55749, 'N', 'Y', 'Y', 'Y', null, 'N', 'D', null, null, 'RUB', 10, null, null,
                      '{calc_confirm_status}', '{nach_amount}', '{perech_amount}', null, {org_id}, null, null)
                      """.format(new_id=new_id, header_id=header_id, contract_name=contract_name, begin_dt=begin,
                                 end_dt=end, calc_confirm_status=calc_confirm_status, nach_amount=nach_amount,
                                 perech_amount=perech_amount, org_id=org_id))
    return new_id


def get_amount(header_id, amount_wo_tax):
    tax_rate_list = db.oebs().execute(
        "select tax_rate from apps.xxoke_history_taxrate_v where contract_id={}".format(header_id)) or [{'tax_rate': 18}]
    return round(amount_wo_tax * (100 + max([rate['tax_rate'] for rate in tax_rate_list])) / 100, 2)


def insert_into_rep_comiss_lines(header_id, contract_name, nach_amount, perech_amount, begin, end, org_id=121, delta=1):
    new_id = db.oebs().execute('select max(rep_line_id) as new_id from apps.xxar_rep_comiss_lines')[0]['new_id'] + delta
    db.oebs().execute("""
                      insert
                      into apps.xxar_rep_comiss_lines
                      (rep_line_id, deleted_flag, period_start_date, period_end_date, k_header_id, k_alias,
                      k_number, reklam_type, reward_type, vat_type, currency_code, keep_reward_amt,
                      keep_pay_amt, dc_reward_amt, dc_pay_amt, dkv_reward_amt, dkv_pay_amt,
                      turnover_reward_amt, turnover_pay_100_amt, turnover_pay_118_amt, line_source,
                      creation_date, last_update_date, last_updated_by, org_id, report_type)
                      values
                      ({new_id}, 'N', to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'),
                      to_date('{end_dt}','DD.MM.YY HH24:MI:SS'), {header_id}, '{contract_name}',
                      59812, 1, 0, 10, 'RUB', '{nach_amount}', '{perech_amount}', '11', 0, '0', 0, '111', 0,0, 'BASE',
                      to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'), to_date('{begin_dt}','DD.MM.YY HH24:MI:SS'),
                      2470, {org_id}, 'KOMISS')
                      """.format(new_id=new_id, header_id=header_id, contract_name=contract_name, begin_dt=begin,
                                 end_dt=end, nach_amount=nach_amount, perech_amount=perech_amount, org_id=org_id))
    return new_id


def delete_from_rep_comiss_lines(object_id):
    db.oebs().execute('delete from apps.xxar_rep_comiss_lines where rep_line_id={id}'.format(id=object_id))


def delete_from_header_comissioners(object_id):
    db.oebs().execute('delete from apps.xxar_header_comissioners where id={id}'.format(id=object_id))


def get_new_header_id():
    new_head = db.oebs().execute("""
                                  select max(val) as new_id from
                                  (select max(to_number(k_header_id)) as val from apps.xxar_rep_comiss_lines
                                  union ALL
                                  select max(to_number(contract_id)) as val from apps.xxar_header_comissioners)
                                  """)[0]['new_id'] + datetime.now().day
    return new_head


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        new_head = get_new_header_id()
        contract_eid = '17-339/11{}{}'.format(datetime.now().day, datetime.now().hour)
        lines_id = insert_into_rep_comiss_lines(new_head, contract_eid, LINES_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END)
        head_id = insert_into_header_comissioners(new_head, contract_eid, HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END)

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert new_head not in [row['header_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_not_found_in_source(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'head_id']) as before:
        before.validate()

        delta = 2
        new_head = get_new_header_id() + delta
        head_id = insert_into_header_comissioners(new_head, CONTRACT_NAME, HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END,
                                                  delta=delta)

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 1) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_not_found_in_headers(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id']) as before:
        before.validate()

        delta = 3
        new_head = get_new_header_id() + delta
        lines_id = insert_into_rep_comiss_lines(new_head, CONTRACT_NAME, LINES_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END,
                                                delta=delta)

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 2) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_reward_amount_not_converge(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        delta = 4
        new_head = get_new_header_id() + delta
        contract_eid = '17-32011/11{}{}'.format(datetime.now().day, datetime.now().hour)
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract_eid, LINES_REWARD_AMOUNT + Decimal('80'), PAY_AMOUNT, BEGIN, END, org_id=221, delta=delta
        )
        head_id = insert_into_header_comissioners(
            new_head, contract_eid, HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END, org_id=221, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 3) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_pay_amount_not_converge(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        delta = 5
        new_head = get_new_header_id() + delta
        contract_eid = '17-310111/11{}{}'.format(datetime.now().day, datetime.now().hour)
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract_eid, LINES_REWARD_AMOUNT, PAY_AMOUNT + Decimal('1.5'), BEGIN, END, org_id=221,
            delta=delta
        )
        head_id = insert_into_header_comissioners(
            new_head, contract_eid, HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END, org_id=221, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 4) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_exclude_contract(shared_data):
    # подробнее в CHECK-2230
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        delta = 6
        new_head = get_new_header_id() + delta
        lines_id = insert_into_rep_comiss_lines(new_head, '573011/11', LINES_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END,
                                                delta=delta)
        head_id = insert_into_header_comissioners(new_head, '573011/11', HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END,
                                                  delta=delta)

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert new_head not in [row['header_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_exclude_contract_with_diff(shared_data):
    # подробнее в CHECK-2230
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'head_id']) as before:
        before.validate()

        delta = 7
        new_head = get_new_header_id() + delta
        head_id = insert_into_header_comissioners(
            new_head, '17-309/11', HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END, org_id=301, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert new_head not in [row['header_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_exclude_v_opt_exclusions(shared_data):
    # подробнее в CHECK-2207
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'head_id', 'lines_id']) as before:
        before.validate()

        delta = 8
        new_head = get_new_header_id() + delta
        contract_eid = api.test_balance().ExecuteSQL(
            'meta', 'select * from bo.v_opt_2015_exclusions')[0]['contract_eid']
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract_eid, LINES_REWARD_AMOUNT + Decimal('10'), PAY_AMOUNT,
            (datetime.now() - relativedelta(months=3)).strftime('%d.%m.%y 00:00:00'),
            (datetime.now() - timedelta(days=1)).strftime('%d.%m.%y 00:00:00'), delta=delta
        )
        head_id = insert_into_header_comissioners(
            new_head, contract_eid, HEADER_REWARD_AMOUNT, PAY_AMOUNT,
            (datetime.now() - relativedelta(months=3)).strftime('%d.%m.%y 00:00:00'),
            (datetime.now() - timedelta(days=1)).strftime('%d.%m.%y 00:00:00'), delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert new_head not in [row['header_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_possible_individual_conditions(shared_data):
    # подробнее в CHECK-2207
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        delta = 9
        new_head = get_new_header_id() + delta
        contract_eid = '17-309222/11{}{}'.format(datetime.now().day, datetime.now().hour)
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract_eid, LINES_REWARD_AMOUNT, PAY_AMOUNT,
            (datetime.now() - relativedelta(months=3)).strftime('%d.%m.%y 00:00:00'),
            (datetime.now() - timedelta(days=1)).strftime('%d.%m.%y 00:00:00'), delta=delta
        )
        head_id = insert_into_header_comissioners(
            new_head, contract_eid, HEADER_REWARD_AMOUNT, 10,
            (datetime.now() - relativedelta(months=3)).strftime('%d.%m.%y 00:00:00'),
            (datetime.now() - timedelta(days=1)).strftime('%d.%m.%y 00:00:00'), calc_confirm_status='Y', delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 4) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_double_diffs(shared_data):
    # подробнее в CHECK-2282
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'head_id']) as before:
        before.validate()

        delta = 10
        new_head = get_new_header_id() + delta
        contract_eid = '1973021/11'
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract_eid, LINES_REWARD_AMOUNT + Decimal('10'), PAY_AMOUNT, BEGIN, END, delta=delta
        )
        head_id = insert_into_header_comissioners(
            new_head, contract_eid, HEADER_REWARD_AMOUNT, PAY_AMOUNT + Decimal('3'), BEGIN, END, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)
    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 2
    assert (new_head, 3) in result
    assert (new_head, 4) in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_auto_analysis_not_found_in_headers(shared_data):
    # подробнее в CHECK-2240
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'lines_id', 'contract']) as before:
        before.validate()

        delta = 11
        contract = '17-30211/11'
        new_head = get_new_header_id() + delta
        lines_id = insert_into_rep_comiss_lines(
            new_head, contract, LINES_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END, org_id=64553, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_rep_comiss_lines(lines_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 2) in result

    ticket = utils.get_check_ticket("arh", cmp_data[0]['cmp_id'])

    comments = list(ticket.comments.get_all())

    for comment in comments:
        if str(contract) in comment.text:
            b_utils.check_that(set([s.login for s in comment.summonees]), equal_to(set([u'chihiro'])))
            b_utils.check_that(comment.text, contains_string(TEXT_2))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_auto_analysis_not_found_in_source_data(shared_data):
    # подробнее в CHECK-2240
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['new_head', 'head_id', 'contract']) as before:
        before.validate()

        delta = 12
        contract = '17-33511/11'
        new_head = get_new_header_id() + delta
        head_id = insert_into_header_comissioners(
            new_head, contract, HEADER_REWARD_AMOUNT, PAY_AMOUNT, BEGIN, END, org_id=64554, delta=delta
        )

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    delete_from_header_comissioners(head_id)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [(row['header_id'], row['state']) for row in cmp_data if row['header_id'] == new_head]
    assert len(result) == 1
    assert (new_head, 1) in result

    ticket = utils.get_check_ticket("arh", cmp_data[0]['cmp_id'])

    comments = list(ticket.comments.get_all())

    for comment in comments:
        if str(contract) in comment.text:
            b_utils.check_that(set([s.login for s in comment.summonees]), equal_to(set([u'aikawa'])))
            b_utils.check_that(comment.text, contains_string(TEXT_3))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ARH)
def test_arh_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_arh(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert len(cmp_data) == DIFFS_COUNT
