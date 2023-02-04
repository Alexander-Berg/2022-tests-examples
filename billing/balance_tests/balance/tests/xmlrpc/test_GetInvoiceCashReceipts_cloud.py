# -*- coding: utf-8 -*-

import xmlrpclib
from datetime import datetime, timedelta
from itertools import chain

import pytest
from hamcrest import anything

import balance.balance_steps as steps
import balance.balance_db as db
import btestlib.utils as utils
from balance.tests.partner_schema_acts.test_cloud_acts import CLOUD_KZ_PH_CONTEXT
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT as CLOUD_RU_UR_CONTEXT
from btestlib.matchers import contains_dicts_equal_to, contains_dicts_with_entries
from btestlib import reporter


TODAY = utils.Date.nullify_time_of_date(datetime.now())


def merge_dicts(*dicts, **kwargs):
    return dict(chain(kwargs.items(), *[dict_.items() for dict_ in dicts]))


class TestGetInvoiceOebsPayments:
    @staticmethod
    def check_result(expected, invoice_id, invoice_eid, message1=None, message2=None, check_method=None):
        result_1 = steps.CommonPartnerSteps.get_invoice_cash_receipts({'InvoiceID': invoice_id})
        result_2 = steps.CommonPartnerSteps.get_invoice_cash_receipts({'InvoiceEID': invoice_eid})

        message1 = message1 if message1 else u'Сравниваем ответ ручки и ожидаемый результат'
        message2 = message2 if message2 else u'Один результат для запросов по id и eid'

        check_method = check_method if check_method else contains_dicts_equal_to

        utils.check_that(result_1, check_method(expected), message1)
        utils.check_that(result_1, contains_dicts_equal_to(result_2), message2)

    @pytest.mark.parametrize('context', [CLOUD_KZ_PH_CONTEXT, CLOUD_RU_UR_CONTEXT])
    def test_invoice_with_no_payments(self, context):
        contract_params = prepare_contract(context)

        self.check_result([], contract_params['invoice_id'], contract_params['invoice_eid'],
                          message1='Платежей нет, ждем пустой массив')

    @pytest.mark.parametrize('context', [CLOUD_KZ_PH_CONTEXT, CLOUD_RU_UR_CONTEXT])
    def test_invoice_with_correction_payments(self, context):
        contract_params = prepare_contract(context)

        payment_1_params = {'dt': TODAY - timedelta(days=1), 'doc_sum': 101.1}
        expected_payment_1 = expected_payment(**payment_1_params)
        make_correction_payment(**merge_dicts(payment_1_params, invoice_id=contract_params['invoice_id']))

        payment_2_params = {'dt': TODAY, 'doc_sum': 55.5}
        expected_payment_2 = expected_payment(**payment_2_params)
        make_correction_payment(**merge_dicts(payment_2_params, invoice_id=contract_params['invoice_id']))

        expected_result = [expected_payment_1, expected_payment_2]

        self.check_result(expected_result, contract_params['invoice_id'], contract_params['invoice_eid'],
                          check_method=contains_dicts_with_entries)

    @pytest.mark.smoke
    @pytest.mark.parametrize('context', [CLOUD_KZ_PH_CONTEXT, CLOUD_RU_UR_CONTEXT])
    def test_invoice_with_oebs_payments(self, context):
        contract_params = prepare_contract(context)

        payment_1_params = {'dt': TODAY - timedelta(days=1), 'doc_sum': 191.1}
        expected_payment_1 = expected_payment(payment_number=None, **payment_1_params)
        make_oebs_payment(**merge_dicts(payment_1_params, invoice_eid=contract_params['invoice_eid'],
                                        invoice_id=contract_params['invoice_id']))

        payment_2_params = {'dt': TODAY, 'doc_sum': 145.5}
        expected_payment_2 = expected_payment(payment_number=None, **payment_2_params)
        make_oebs_payment(**merge_dicts(payment_2_params, invoice_eid=contract_params['invoice_eid'],
                                        invoice_id=contract_params['invoice_id']))

        expected_result = [expected_payment_1, expected_payment_2]

        self.check_result(expected_result, contract_params['invoice_id'], contract_params['invoice_eid'],
                          check_method=contains_dicts_with_entries)

    @pytest.mark.smoke
    @pytest.mark.parametrize('context', [CLOUD_RU_UR_CONTEXT])
    def test_call_with_both_ids(self, context):
        contract_params = prepare_contract(context)
        params = {'InvoiceID': contract_params['invoice_id'], 'InvoiceEID': contract_params['invoice_eid']}

        with pytest.raises(xmlrpclib.Fault):
            steps.CommonPartnerSteps.get_invoice_cash_receipts(params)


def expected_payment(
        doc_sum,
        dt=anything(),
        payment_date=anything(),
        payment_number='Testing',
        source_type=None,
        inn=anything(),
        customer_name=None
):

    return utils.remove_empty({
        'dt': dt,
        'doc_date': dt,
        'payment_date': payment_date,
        'payment_number': payment_number,
        'doc_sum': doc_sum,
        'source_type': source_type,
        'inn': inn,
        'customer_name': customer_name,
    })


def prepare_contract(context):
    project_id = steps.PartnerSteps.create_cloud_project_uuid()
    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        context, additional_params={'projects': [project_id]})

    row = db.balance().execute(
        "select id, external_id from t_invoice where CONTRACT_ID = :contract_id and type = 'personal_account'",
        dict(contract_id=contract_id), single_row=True, descr=u'Получаем id лицевого счета')

    return {
        'client_id': client_id,
        'person_id': person_id,
        'contract_id': contract_id,
        'contract_eid': contract_eid,
        'invoice_id': row['id'],
        'invoice_eid': row['external_id'],
    }


def make_correction_payment(invoice_id, doc_sum, dt=None):
    with reporter.step(u'Зачисляем {} на лицевой счет {}, correction_payment'.format(doc_sum, invoice_id)):
        steps.InvoiceSteps.pay(invoice_id, payment_sum=doc_sum, payment_dt=dt)


def make_oebs_payment(invoice_eid, doc_sum, dt, invoice_id=None):
    with reporter.step(u'Зачисляем {} на лицевой счет {}, oebs_payment'.format(doc_sum, invoice_eid)):
        cash_fact_id, _ = steps.InvoiceSteps.create_cash_payment_fact(invoice_eid, doc_sum, dt, 'INSERT',
                                                                      invoice_id=invoice_id)

    return {'cpf_id': cash_fact_id}
