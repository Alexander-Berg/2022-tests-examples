# coding: utf-8
from uuid import uuid4

from btestlib.constants import *

import json
from json import JSONEncoder

from balance.tests.integrations.month_closing import InputPeriod, ResultPeriod
from balance.tests.integrations.transaction_pipeline import *
from balance.tests.integrations.utils import *

from btestlib import reporter
from balance import balance_steps as steps
from simpleapi.common.payment_methods import YandexAccountWithdraw, YandexAccountTopup
from cashmachines.data.constants import CMNds, NDS_BY_NAME
from btestlib.data import simpleapi_defaults

log = reporter.logger()


class TestInput(object):
    def __init__(self, test_type, processing, test_input):
        self.processing = processing
        self.payment_currency = getattr(Currencies, test_input['currency'], 'RUB')
        self.tpt_paysys_type_cc = test_input.get('tpt_paysys_type_cc', 'yamoney')
        self.nds = NdsNew.NdsPolicy(test_input.get('nds', 18))
        self.is_postpay = test_input.get('is_postpay', 1)
        self.invoice_type = test_input.get('invoice_type')
        self.pad_type_id = test_input.get('pad_type_id')
        self.type = test_input.get('type')
        self.main_product_id = test_input.get('main_product_id')
        self.rows = test_input.get('rows')
        self.sum_key = test_input.get('sum_key', 'amount')
        self.product_id = test_input.get('product_id')
        self.manual_export = test_input.get('manual_export', True)
        self.paymethod_plus = self.gen_account_paymethod(test_input.get('account_paymethod'))
        self.paymethod_plus_currency = test_input.get('paymethod_plus_currency', 'RUB')
        if self.paymethod_plus:
            self.payment_method = 'yandex_account'
        else:
            self.payment_method = test_input.get('payment_method')
        self.refund_amount = test_input.get('refund_amount')

        self.periods = []
        periods = test_input.get('periods', [])
        for period in periods:
            self.periods.append(InputPeriod(period))

    def get_amounts_and_nds(self):
        amounts = []
        fiscal_nds_list = []
        for row in self.rows:
            amounts.append(row['amount'])
            fiscal_nds = row.get('fiscal_nds')
            fiscal_nds_list.append(NDS_BY_NAME.get(fiscal_nds))

        return amounts, fiscal_nds_list

    def get_plus_client_id(self, service):
        if self.paymethod_plus:
            return steps.CommonPartnerSteps.get_plus_client_id(service)
        else:
            return None

    def gen_account_paymethod(self, account_paymethod):
        if not account_paymethod:
            return
        account_paymethod_id = 'yandex_account-w/{}'.format(uuid4())
        if account_paymethod == 'yandex_account_withdraw':
            return YandexAccountWithdraw(account_paymethod_id)
        elif account_paymethod == 'yandex_account_topup':
            return YandexAccountTopup(account_paymethod_id)


class TestResult(object):
    def __init__(self, test_type, test_result):
        self.test_type = test_type

        if test_type == 'transaction_pipeline':
            for key in test_result:
                setattr(self, key, test_result[key])
        else:
            self.periods = []
            for period in test_result['periods']:
                self.periods.append(ResultPeriod(period))

    def get_side_refund_id(self, side_refund_id):
        return side_refund_id if getattr(self, 'set_payment_id', 1) else None

    def get_transaction_id_refund(self, transaction_id_refund):
        return transaction_id_refund if getattr(self, 'set_trust_refund_id', 1) else None

    def get_side_payment_id(self, side_payment_id):
        return side_payment_id if getattr(self, 'set_payment_id', 1) else None

    def get_transaction_id_payment(self, transaction_id_payment):
        return transaction_id_payment if getattr(self, 'set_trust_payment_id', 1) else None

    def get_force_partner(self, client_id, person_id, contract_id, context):
        if all(getattr(self, key) == 'force_partner' for key in ['contract_id', 'partner_id', 'person_id']):
            client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)
        elif all(getattr(self, key) == 'from_plus_contract' for key in ['contract_id', 'partner_id', 'person_id']):
            client_id, person_id, contract_id = steps.CommonPartnerSteps.get_active_plus_ids_by_service(context.service,
                                                                                                        dict(
                                                                                                            currency=context.currency.iso_code))
        return client_id, person_id, contract_id

    def get_invoice_eid(self, contract_id, service):
        if getattr(self, 'set_invoice_eid', 1):
            invoice_id, invoice_eid = get_invoice_eid_from_service(contract_id, service)
        else:
            invoice_eid = None
        return invoice_eid

    def get_oebs_org_id(self, contract_id):
        return get_firm_by_id(steps.CommonPartnerSteps.get_contract_firm(contract_id)).oebs_org_id

    def get_product_id(self, context):
        if self.set_product_id:
            return get_main_product_id_by_query(self.product_id, context)
        return None

    def build_expected_data(self, context, client_id, contract_id, person_id,
                            trust_payment_id, payment_id, trust_refund_id=None, side_payment=False):
        if side_payment:
            expected_payment = steps.SimpleApi.create_expected_tpt_row(
                context, client_id, contract_id, person_id,
                trust_payment_id=trust_payment_id,
                trust_refund_id=trust_refund_id,
                payment_id=payment_id,
                amount=self.amount,
                paysys_type_cc=getattr(self, 'paysys_type_cc', None),
                payment_type=self.payment_type,
                internal=getattr(self, 'internal', None)
            )
            return expected_payment

        client_id, person_id, contract_id = self.get_force_partner(client_id, person_id, contract_id, context)
        invoice_eid = self.get_invoice_eid(contract_id, context.service)
        main_product_id = self.get_product_id(context)

        expected_payment_tpt = [
            steps.SimpleApi.create_expected_tpt_row(
                context, client_id, contract_id,
                person_id, trust_payment_id, payment_id,
                amount=self.amount,
                payment_type=self.payment_type,
                paysys_type_cc=self.paysys_type_cc,
                internal=getattr(self, 'internal', None),
                product_id=main_product_id,
                invoice_eid=(invoice_eid if getattr(self, 'set_invoice_eid', True) else None),
                yandex_reward=getattr(self, 'yandex_reward', None),
                yandex_reward_wo_nds=getattr(self, 'yandex_reward_wo_nds', None),
                oebs_org_id=(None if not getattr(self, 'set_oebs_org_id', 1) else self.get_oebs_org_id(contract_id)),
                )]
        return expected_payment_tpt


class TestData(object):
    def __init__(self, test_data):
        self.type = test_data.get('type')
        self.processing = test_data.get('processing', 'emulator')
        self.input = []
        self.result = []
        self.setup_tests(test_data['input'], test_data['result'])

    # self.tests = [(input[0], result[0]), ..., (input[k], result[k])]
    # create TestInput, TestResult classes
    def setup_tests(self, input_list, result_list):
        for test_input, test_output in zip(input_list, result_list):
            self.input.append(TestInput(self.type, self.processing, test_input))
            self.result.append(TestResult(self.type, test_output))


class ServiceFirmMapper(object):
    def __init__(self, firm_services_map):
        self.service_firm_map = {}
        for firm_id, services in (firm_services_map or {}).items():
            self.service_firm_map.update({service_id: int(firm_id) for service_id in services})

    def get_firm_to_service(self, service_id):
        return self.service_firm_map.get(service_id)


class TestsExtraContext(object):
    def __init__(self, tests_extra_context):
        tests_extra_context = tests_extra_context or {}
        self.service_firm_mapper = ServiceFirmMapper(tests_extra_context.get('firm_services_map'))


class Tests(object):
    def __init__(self, tests=None):
        self.tests = []
        for test in tests or []:
            self.tests.append(TestData(test))

    def split_payments_and_acts(self):
        payment_tests, act_tests = Tests(), Tests()
        for test in self.tests:
            if test.type == 'month_closing':
                act_tests.tests.append(test)
            else:
                payment_tests.tests.append(test)
        return payment_tests, act_tests


class TestDataEncoder(JSONEncoder):
    def default(self, o):
        return o.__dict__
