# coding: utf-8

from balance.tests.integrations.utils import *
# from balance.tests.integrations.act_data import ActData
from balance.tests.integrations.common import AbstractTestsProvider, BaseTestData, AbstractProcessor


class PaymentData(BaseTestData):
    pass


# "rows" (in transaction_pipeline)
class TestRow(object):
    def __init__(self, row):
        self.amount = row['amount']
        self.product_params = row.get('product_params')


# todo: merge with ActDataProvider (common class)
class PaymentTestsProvider(AbstractTestsProvider):
    def __init__(self, config):
        self.config = config

    def provide_tests(self):
        partner_integration_params = get_partner_integration_params(self.config['service'])
        service_firm_mapper = self.config['tests_extra_context'].service_firm_mapper
        for test in self.config['tests'].tests:
            assert test.type == 'transaction_pipeline', 'not month_closing test'
            for contract in self.config['contracts']:
                if 'services' not in contract:  # TODO: reassert what to do
                    continue
                service = get_service_by_id(self.config['service_id'])
                assert service, 'no such service for %s' % contract
                for test_input, test_output in zip(test.input, test.result):
                    for person_type in get_allowed_person_types(contract['person']):
                        context = create_context(service, contract, test_input, person_type, service_firm_mapper)
                        act_data = PaymentData(partner_integration_params, context, test_input, test_output, contract['ctype'])
                        yield act_data


class PaymentProcessor(AbstractProcessor):
    def __init__(self, case, contract):
        self.case = case
        self.contract = contract
        self.amounts, self.fiscal_nds_list = case.test_input.get_amounts_and_nds()

        # SPENDABLE
        self.side_payment_id, self.transaction_id_payment = None, None
        self.side_refund_id, self.transaction_id_refund = None, None

        # GENERAL
        self.service_order_id_list, self.trust_payment_id, self.purchase_token, self.payment_id = \
            None, None, None, None
        self.trust_refund_id, self.refund_id = None, None

    def generate_rows(self):
        if self.case.ctype == 'GENERAL':
            # создаем платеж
            self.service_order_id_list, self.trust_payment_id, self.purchase_token, self.payment_id = \
                steps.SimpleApi.create_multiple_trust_payments(self.case.context.service,
                                                               [self.contract.service_product_id],
                                                               prices_list=self.amounts,
                                                               paymethod=self.case.test_input.paymethod_plus,
                                                               fiscal_nds_list=self.fiscal_nds_list,
                                                               currency=self.case.context.currency,
                                                               )
            if self.case.test_input.type == 'refund':
                self.trust_refund_id, self.refund_id = \
                    steps.SimpleApi.create_refund(
                        service=self.case.context.service,
                        service_order_id=self.service_order_id_list[0],
                        trust_payment_id=self.trust_payment_id,
                        delta_amount=self.case.test_input.refund_amount
                    )
        else:
            self.side_payment_id, self.transaction_id_payment = \
                steps.PartnerSteps.create_sidepayment_transaction(self.contract.client_id, utils.Date.moscow_offset_dt(),
                                                                  self.amounts[0],
                                                                  self.case.test_input.payment_method,
                                                                  self.case.context.service.id,
                                                                  currency=self.case.context.currency,
                                                                  extra_dt_0=datetime.now())

            if self.case.test_input.type == 'refund':
                self.side_refund_id, self.transaction_id_refund = \
                    steps.PartnerSteps.create_sidepayment_transaction(self.contract.client_id, utils.Date.moscow_offset_dt(),
                                                                      self.case.test_input.refund_amount,
                                                                      self.case.test_input.payment_method,
                                                                      self.case.context.service.id,
                                                                      currency=self.case.context.currency,
                                                                      transaction_type=TransactionType.REFUND,
                                                                      orig_transaction_id=self.transaction_id_payment)

    def export_rows(self):
        if self.case.ctype == 'GENERAL':
            steps.CommonPartnerSteps.export_payment(self.payment_id)
            if self.case.test_input.type == 'refund':
                steps.CommonPartnerSteps.export_payment(self.refund_id)
        elif self.case.test_input.type == 'refund':
            # запускаем обработку сайдпеймента:
            steps.ExportSteps.create_export_record_and_export(self.side_payment_id,
                                                              Export.Type.THIRDPARTY_TRANS,
                                                              Export.Classname.SIDE_PAYMENT,
                                                              service_id=self.case.context.service.id)

            steps.ExportSteps.create_export_record_and_export(self.side_refund_id,
                                                              Export.Type.THIRDPARTY_TRANS,
                                                              Export.Classname.SIDE_PAYMENT,
                                                              service_id=self.case.context.service.id,
                                                              with_export_record=False)

            self.transaction_type = TransactionType.REFUND
            self.expected_payment_id = self.case.test_output.get_side_refund_id(self.side_refund_id)
            self.trust_refund_id = self.case.test_output.get_transaction_id_refund(self.transaction_id_refund)
        else:
            # запускаем обработку сайдпеймента:
            steps.ExportSteps.create_export_record_and_export(self.side_payment_id,
                                                              Export.Type.THIRDPARTY_TRANS,
                                                              Export.Classname.SIDE_PAYMENT,
                                                              service_id=self.case.context.service.id)

            self.transaction_type = TransactionType.PAYMENT
            self.expected_payment_id = self.case.test_output.get_side_payment_id(self.side_payment_id)
            self.trust_refund_id = None

    def create_expected_and_real_data(self):
        if self.case.ctype == 'GENERAL':
            self.payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(self.payment_id)
            self.expected_payment = self.case.test_output.build_expected_data(
                self.case.context,
                self.contract.client_id,
                self.contract.contract_id,
                self.contract.person_id,
                self.trust_payment_id,
                self.payment_id,
            )
        else:
            trust_payment_id = self.case.test_output.get_transaction_id_payment(self.transaction_id_payment)
            self.expected_payment = [
                self.case.test_output.build_expected_data(
                    self.case.context,
                    self.contract.client_id,
                    self.contract.contract_id,
                    self.contract.person_id,
                    trust_payment_id,
                    self.expected_payment_id,
                    self.trust_refund_id,
                    True
                )
            ]
            self.payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
                self.expected_payment_id,
                transaction_type=self.transaction_type,
                source='sidepayment'
            )

    def compare(self):
        utils.check_that(self.payment_data, contains_dicts_with_entries(self.expected_payment),
                         'Сравниваем платеж с шаблоном')
