# coding: utf-8

import os
import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as ut
from btestlib.data.person_defaults import InnType
import btestlib.config as balance_config

try:
    import balance_contracts
    from balance_contracts.oebs.act import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/act/'

pytestmark = [reporter.feature(Features.OEBS, Features.ACT),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

dt = datetime.datetime.now()
to_iso = ut.Date.date_to_iso_format
dt_delta = ut.Date.dt_delta

HALF_YEAR_AFTER_NOW_ISO = to_iso(dt + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(dt - datetime.timedelta(days=180))

PERSON_TYPE = 'sw_ur'
PAYSYS_ID = 1044
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

contract_type = 'shv_agent'


def check_retrodiscount_export(contract_params):
    client_id = steps.ClientSteps.create({'IS_AGENCY': '1'})
    steps.CommonSteps.export('OEBS', 'Client', client_id)
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, inn_type=InnType.RANDOM)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    if contract_params['contract_needed']:
        contract_params_dict = {
            'CLIENT_ID': client_id,
            'PERSON_ID': person_id,
            'DT': HALF_YEAR_BEFORE_NOW_ISO,
            'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
            'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
            'SERVICES': [7],
            'FIRM': 7,
            'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
        }
        contract_params_dict.update(contract_params['retro_discount_params'])
        contract_id, _ = steps.ContractSteps.create_contract(contract_type, contract_params_dict)
        # steps.ExportSteps.export_oebs(person_id=person_id, contract_id=contract_id, client_id=client_id)
        # steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    else:
        contract_id = None
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    steps.ActsSteps.generate(client_id, force=1, date=dt)
    act_id = db.get_acts_by_client(client_id)[0]['id']


    return client_id, person_id, invoice_id, act_id

def check_json_contract(act_id, json_file):
    # try:
    #     db.balance().execute(
    #         """update t_person_firm set oebs_export_dt = sysdate where person_id = :person_id""",
    #         {'person_id': person_id})
    # except Exception:
    #     pass

    steps.ExportSteps.init_oebs_api_export('Act', act_id)
    actual_json_data = steps.ExportSteps.get_json_data('Act', act_id)

    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_OEBS_PATH,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_OEBS_PATH,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


@pytest.mark.parametrize('contract_params, json_file',
                         [
                             ({'contract_needed': False}, 'retrodiscount_contract_needed_false.json'),
                             ({'contract_needed': True, 'retro_discount_params': {'RETRO_DISCOUNT': 0}}, 'retrodiscount_contract_needed_0.json'),
                             ({'contract_needed': True, 'retro_discount_params': {}}, 'retrodiscount_contract_needed_none.json'),
                             ({'contract_needed': True, 'retro_discount_params': {'RETRO_DISCOUNT': 15}}, 'retrodiscount_contract_needed_15.json'),
                         ],
                         ids=['wo_contract'
                             , 'with_contract_with_zero_retrodiscount'
                             , 'with_contract_w/o_retrodiscount'
                             , 'with_contract_with_retrodiscount'
                              ]
                         )
def test_check_retrodiscount_export(contract_params, json_file):
    client_id, person_id, invoice_id, act_id = check_retrodiscount_export(contract_params)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(act_id, json_file)

    else:
        steps.ExportSteps.export_oebs(person_id=person_id, invoice_id=invoice_id, act_id=act_id, client_id=client_id)
        external_id = db.get_act_by_id(act_id)[0]['external_id']
        refundment_amount = db.balance().execute('select AMOUNT from T_ACT_REFUNDMENT where ACT_ID = :act_id',
                                                 {'act_id': act_id}, fail_empty=False)
        refundment_amount_oebs = \
            db.oebs().execute('select global_attribute8 from apps.ra_customer_trx_all trx where trx_number = :external_id',
                              {'external_id': external_id})[0]['global_attribute8']
        if refundment_amount == []:
            ut.check_that(refundment_amount_oebs, hamcrest.equal_to('0'))
        else:
            ut.check_that([{'amount': refundment_amount_oebs}], hamcrest.equal_to(refundment_amount))


if __name__ == "__main__":
    pytest.main("test_check_retrodiscount_export.py -vk 'test_check_retrodiscount_export'")
