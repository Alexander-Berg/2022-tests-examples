# coding: utf-8

import copy

from balance import balance_steps as steps
from btestlib.constants import SpendablePaymentType
from btestlib.utils import Date
from balance.tests.integrations.act_data import ActData
from balance.tests.integrations.transaction_pipeline import PaymentData
from balance.tests.integrations import utils


class ContractDataProvider(object):
    def __init__(self, case):
        self.contract_start_dt = self._get_contract_start_dt(case)
        self.client_id, self.person_id, self.contract_id = None, None, None
        self.service_product_id = None

        # borograam: я всё порывался вынести создание контракта в отдельный метод [Act|Payment]Data (тут case),
        # но тогда код разъедется по файлам и *Data сами станут contract_provider'ами - стоит ли смешивать понятия?
        if isinstance(case, ActData):
            self._create_contract_for_acts(case)
        elif isinstance(case, PaymentData):
            self._create_contract_for_payments(case)

    @staticmethod
    def _get_contract_start_dt(case):
        offset = getattr(case.test_input, 'start_month_offset', -3)
        return Date.shift_date(Date.first_day_of_month(), months=offset)

    def _create_contract_for_acts(self, case):
        if case.ctype == 'GENERAL':
            self.client_id, self.person_id, self.contract_id = utils.create_contract(
                case.context,
                case.partner_integration_params,
                self.contract_start_dt,
                is_postpay=case.test_input.is_postpay
            )
        else:  # надо чтоль всё создание договоров вынести в utils
            params = dict(
                start_dt=self.contract_start_dt,
                nds=case.context.nds.nds_id,
                payment_type=SpendablePaymentType.MONTHLY,
                selfemployed=1
            )

            self.client_id, self.person_id, self.contract_id, _ = steps.ContractSteps.create_partner_contract(
                case.context,
                additional_params=params,
                partner_integration_params=copy.deepcopy(case.partner_integration_params),
                full_person_params=True,
                is_offer=True
            )

    def _create_contract_for_payments(self, case):
        if case.ctype == 'SPENDABLE':
            self.client_id, self.person_id, self.contract_id = utils.create_contract(
                case.context,
                case.partner_integration_params,
                self.contract_start_dt,
                is_postpay=1,
                selfemployed=1
            )
        else:
            try:
                self.service_product_id = steps.SimpleApi.create_service_product(case.context.service)
                self.client_id, self.person_id, self.contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
                    case.context.service)
            except AssertionError as e:
                self.partner_id = steps.SimpleApi.create_partner(case.context.service)
                self.service_product_id = steps.SimpleApi.create_service_product(case.context.service,
                                                                                 partner_id=self.partner_id)
                self.client_id, self.person_id, self.contract_id, _ = steps.ContractSteps.create_partner_contract(
                    case.context,
                    client_id=self.partner_id,
                    partner_integration_params=copy.deepcopy(case.partner_integration_params),
                    additional_params={'start_dt': self.contract_start_dt}
                )
