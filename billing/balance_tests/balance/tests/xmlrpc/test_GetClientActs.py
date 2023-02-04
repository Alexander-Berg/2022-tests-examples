# coding: utf-8

__author__ = 'a-vasin'

from collections import defaultdict
from datetime import datetime
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta
from hamcrest import empty, anything

import balance.balance_api as api
import balance.balance_db as db
import balance.balance_steps as steps
from btestlib import reporter
from btestlib import utils
from btestlib.constants import Services, Products
from btestlib.matchers import contains_dicts_equal_to
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT
from balance.features import Features


START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
QTY = D('100')
PRICE = D('190')


def get_expected_external_id(client_id, service):
    query = ('SELECT a.EXTERNAL_ID FROM bo.T_ACT a '
             'JOIN bo.T_ACT_TRANS t ON (t.act_id = a.ID) '
             'JOIN bo.T_CONSUME c ON (c.ID = t.CONSUME_ID) '
             'JOIN bo.T_ORDER o ON (c.PARENT_ORDER_ID = o.ID) '
             'WHERE a.CLIENT_ID = :client_id AND o.service_id = :service_id')

    params = {
        'client_id': client_id,
        'service_id': service.id
    }

    external_id = db.balance().execute(query, params)[0]['external_id']
    return external_id


def test_direct_heavy_client():
    client_id = 313908
    client_acts = get_client_acts(Services.DIRECT, client_id)
    expected_acts = create_expected_act_info(Services.DIRECT, client_id)
    utils.check_that(client_acts, contains_dicts_equal_to(expected_acts),
                     u'Сверяем полученные акты с данными из базы')


def test_several_contracts():
    project_first = steps.PartnerSteps.create_cloud_project_uuid()
    completions_amount_first = D('321.5234')
    completions_date_first = datetime.now() + relativedelta(months=-1)
    client_id, person_id, contract_id_first = create_contract_cloud(projects=[project_first],
                                                                    month=completions_date_first)
    create_acts_cloud(client_id=client_id, contract_id=contract_id_first,
                      completion_date=completions_date_first, completion_amount=completions_amount_first,
                      completion_project=project_first)

    project_second = steps.PartnerSteps.create_cloud_project_uuid()
    completions_amount_second = D('1232.8234')
    completions_date_second = datetime.now()
    _, _, contract_id_second = create_contract_cloud(projects=[project_second], month=completions_date_second,
                                                     client_id=client_id, person_id=person_id)
    create_acts_cloud(client_id=client_id, contract_id=contract_id_second,
                      completion_date=completions_date_second, completion_amount=completions_amount_second,
                      completion_project=project_second)

    client_acts = get_client_acts(Services.CLOUD_143, client_id)
    expected_acts_all = create_expected_act_info(Services.CLOUD_143, client_id)
    utils.check_that(client_acts, contains_dicts_equal_to(expected_acts_all),
                     u'Сверяем полученные акты с данными из базы')

    expected_acts_first = [act for act in expected_acts_all if act['CONTRACT_ID'] == contract_id_first]
    client_acts_first = get_client_acts(Services.CLOUD_143, client_id, contract_id_first)
    utils.check_that(client_acts_first, contains_dicts_equal_to(expected_acts_first),
                     u'Сверяем полученные акты по первому договору с данными из базы')

    expected_acts_second = [act for act in expected_acts_all if act['CONTRACT_ID'] == contract_id_second]
    client_acts_second = get_client_acts(Services.CLOUD_143, client_id, contract_id_second)
    utils.check_that(client_acts_second, contains_dicts_equal_to(expected_acts_second),
                     u'Сверяем полученные акты по второму договору с данными из базы')

    # todo-igogor убрать хардкод и вообще это отдельный кейс
    client_acts_invalid = get_client_acts(Services.CLOUD_143, client_id, 114786)
    utils.check_that(client_acts_invalid, empty(),
                     u'Проверяем, что клиент не может получить данные по чужому договору')

    pass


# -----------------------------------
# Utils
def create_contract_cloud(projects, month, client_id=None, person_id=None):
    start_dt = month.replace(day=1)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(CLOUD_RU_CONTEXT, client_id=client_id,
                                                                                       person_id=person_id,
                                                                                       additional_params={
                                                                                           'start_dt': start_dt,
                                                                                           'projects': projects
                                                                                       })
    return client_id, person_id, contract_id


def create_acts_cloud(client_id, contract_id, completion_date, completion_amount, completion_project):
    steps.PartnerSteps.create_cloud_completion(contract_id, completion_date, completion_amount)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, completion_date)


def create_expected_act_info(service, client_id):
    query = """SELECT act.AMOUNT act_amount, act.AMOUNT_NDS act_amount_nds, act.AMOUNT_NSP act_amount_nsp,
            act.ID, act.DT, act.EXTERNAL_ID, act.PAID_AMOUNT, act.PAYMENT_TERM_DT, orders.SERVICE_CODE,
            trans.ACT_QTY, trans.AMOUNT row_amount, trans.AMOUNT_NDS row_amount_nds, trans.AMOUNT_NSP row_amount_nsp,
            trans.PAID_AMOUNT row_paid_amount, invoice.CONTRACT_ID contract_id
            FROM T_CONSUME consume, T_ACT_TRANS trans, T_ACT act, T_ORDER orders, T_INVOICE invoice
            WHERE act.id = trans.ACT_ID AND consume.id = trans.CONSUME_ID AND consume.PARENT_ORDER_ID = orders.ID
            AND act.CLIENT_ID=:client_id AND orders.SERVICE_ID=:service_id AND invoice.id = act.invoice_id
            AND act.hidden < 4 AND act.type = 'generic' AND trans.netting is NULL"""
    params = {
        'client_id': client_id,
        'service_id': service.id
    }
    result = db.balance().execute(query, params)

    act_rows = defaultdict(list)
    for row in result:
        act_rows[row['id']].append({
            'AMOUNT': D(row['row_amount']),
            'PAID_AMOUNT': D(row['row_paid_amount']),
            'AMOUNT_NDS': D(row['row_amount_nds']),
            'AMOUNT_NSP': D(row['row_amount_nsp']),
            'PRODUCT_ID': row['service_code'],
            'QTY': D(row['act_qty'])
        })

    acts = []
    for row in result:
        act_data = {
            'AMOUNT': D(row['act_amount']),
            'AMOUNT_NDS': D(row['act_amount_nds']),
            'AMOUNT_NSP': D(row['act_amount_nsp']),
            'ID': row['id'],
            'DT': row['dt'],
            'PAID_AMOUNT': D(row['paid_amount']),
            'PAYMENT_TERM_DT': row['payment_term_dt'],
            'ROWS': contains_dicts_equal_to(act_rows[row['id']]),
            'EXTERNAL_ID': row['external_id']
        }
        # todo-igogor проще вручную было создать эталоны. Опять переписываю логику баланса
        if row['contract_id']:
            act_data['CONTRACT_ID'] = row['contract_id']
        acts.append(act_data)

    # return contains_dicts_equal_to(acts.values())
    return acts


def get_client_acts(service, client_id, contract_id=None):
    with reporter.step(u'Вызываем GetClientActs для сервиса: {}, клиента: {}'.format(service.name, client_id)):
        return api.medium().GetClientActs(service.token, utils.remove_false({'ClientID': client_id,
                                                                             'ContractID': contract_id}))
