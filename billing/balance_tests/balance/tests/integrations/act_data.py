# coding: utf-8

from balance.tests.integrations.utils import *
from balance.tests.integrations.common import AbstractTestsProvider, AbstractProcessor, BaseTestData


class ActData(BaseTestData):
    def __init__(self, *args, **kwargs):
        super(ActData, self).__init__(*args, **kwargs)

        if self.ctype == 'GENERAL':
            self.page = None
        else:
            self.page = get_page_info_by_service_id(self.context.service.id)


class ActTestsProvider(AbstractTestsProvider):
    def __init__(self, config):
        self.config = config

    def provide_tests(self):
        partner_integration_params = get_partner_integration_params(self.config['service'])
        service_firm_mapper = self.config['tests_extra_context'].service_firm_mapper
        for test in self.config['tests'].tests:
            assert test.type == 'month_closing', 'not month_closing test'
            for contract in self.config['contracts']:
                if 'services' not in contract:  # TODO: reassert what to do
                    continue
                # todo: может нужно проходиться по всем айдишникам сервисов?
                service = get_service_by_id(self.config['service_id'])
                assert service, 'no such service for %s' % contract
                for test_input, test_output in zip(test.input, test.result):
                    for person_type in get_allowed_person_types(contract['person']):
                        context = create_context(service, contract, test_input, person_type, service_firm_mapper)
                        act_data = ActData(partner_integration_params, context, test_input, test_output,
                                           contract['ctype'])
                        yield act_data


class ActRow(object):
    def __init__(self, period, result):
        self.period = period
        self.result = result


class ActProcessor(AbstractProcessor):
    def __init__(self, case, contract):
        self.case = case
        self.contract = contract
        self.data = []
        self.expected_data = []
        self.invoice_data = None
        self.expected_invoice_data = None
        self.rows, self.compls_dicts = [], []

    def generate_act_data(self):
        for period, result in zip(self.case.test_input.periods, self.case.test_output.periods):
            act_data = ActRow(period, result)
            yield act_data

    def generate_rows(self, act_data):
        # GENERATE ROWS
        self.rows, self.compls_dicts = [], []
        if self.case.ctype == 'GENERAL':
            sum_key = self.case.test_input.sum_key
            self.rows = act_data.period.build_tpt_rows_for_general(self.case.context,
                                                                   self.contract.contract_id,
                                                                   self.contract.client_id,
                                                                   sum_key)
            self.compls_dicts = act_data.period.build_oebs_completions(self.case.context,
                                                                       self.case.test_input.product_id,
                                                                       act_data.period.payment_dt)
            act_data.period.build_partner_completions(self.case.context,
                                                      self.contract.client_id,
                                                      act_data.period.payment_dt)
        else:
            self.rows = act_data.period.build_tpt_rows(self.case.context.service.id, self.case.page.payment_type)

    def export_rows(self, act_data):
        # EXPORT
        if self.case.ctype == 'GENERAL':
            sum_key = self.case.test_input.sum_key
            steps.SimpleApi.create_fake_tpt_data(self.case.context,
                                                 self.contract.client_id,
                                                 self.contract.person_id,
                                                 self.contract.contract_id,
                                                 act_data.period.payment_dt,
                                                 self.rows,
                                                 sum_key=sum_key)

            steps.CommonPartnerSteps.create_partner_oebs_completions(self.contract.contract_id,
                                                                     self.contract.client_id,
                                                                     self.compls_dicts)

            steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
                self.contract.client_id,
                self.contract.contract_id,
                act_data.period.end_dt,
                manual_export=self.case.test_input.manual_export,
            )
        else:
            steps.SimpleApi.create_fake_tpt_data(self.case.context,
                                                 self.contract.client_id,
                                                 self.contract.person_id,
                                                 self.contract.contract_id,
                                                 act_data.period.payment_dt,
                                                 self.rows)

            steps.CommonPartnerSteps.generate_partner_acts_fair(self.contract.contract_id,
                                                                act_data.period.end_dt)

    def create_expected_and_real_data(self, act_data):
        # EXPECTED AND REAL DATA
        if self.case.ctype == 'GENERAL':
            self.expected_data.extend(
                act_data.result.build_expected_act_data(
                    self.case.context,
                    act_data.period.end_dt
                )
            )

            self.expected_invoice_data = \
                act_data.result.build_expected_invoice_data(self.case.context,
                                                            self.contract.contract_id,
                                                            self.contract.person_id,
                                                            self.contract.contract_start_dt)

            invoice_data = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(self.contract.client_id)

            # связано с тем что иногда для клиента создаются два счета один с ндс другой без, берем первый (с ндс)
            # возможно в будущем нужно будет указывать какой именно выбирать
            # todo: nurlykhan подбирать invoice по service_code (act_data.result.product_service_code)

            self.invoice_data = []
            for invoice in act_data.result.invoice:
                service_code = None
                is_service_code_exist = True
                if 'product_service_code' in invoice:
                    service_code = invoice['product_service_code']
                    is_service_code_exist = service_code is not None
                invoice_id = steps.InvoiceSteps.get_invoice_by_service_or_service_code(self.contract.contract_id,
                                                                                       service=self.case.context.service,
                                                                                       service_code=service_code,
                                                                                       # service_code may be None
                                                                                       is_service_code_exist=is_service_code_exist)[0]
                invoice_data = [invoice for invoice in invoice_data if invoice['id'] == invoice_id]
                assert len(invoice_data) == 1, 'по сервис коду {} найдены несколько invoice для клиента {}'.format(service_code, self.contract.client_id)
                self.invoice_data.extend(invoice_data)

            self.data = steps.ActsSteps.get_act_data_by_client(self.contract.client_id)
        else:
            self.expected_data.extend(
                act_data.result.build_expected_data_for_spendable(
                    self.case.context,
                    self.contract.client_id,
                    self.contract.contract_id,
                    self.case.page,
                    act_data.period.payment_dt,
                    act_data.period.end_dt
                )
            )
            self.data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(self.contract.contract_id)

    def compare(self):
        if self.case.ctype == 'GENERAL':
            utils.check_that(self.invoice_data, contains_dicts_with_entries(self.expected_invoice_data),
                             u'Сравниваем данные из счета с шаблоном')
        utils.check_that(self.data, contains_dicts_with_entries(self.expected_data),
                         u'Сравниваем данные из акта с шаблоном')
