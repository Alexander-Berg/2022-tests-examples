# -*- coding: utf-8 -*-

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from ..common_defaults import AGENCY_NAME


@pytest.mark.parametrize('fixed_discount, scale_discount', [
    pytest.param(1, 1, id='fixed and scale'),
    pytest.param(1, 0, id='fixed'),
    pytest.param(0, 1, id='scale'),
])
def test_discounts(fixed_discount, scale_discount):
    context = Contexts.MEDIA_70_USD_SW
    client_id = balance_steps.ClientSteps.create({'NAME': AGENCY_NAME, 'IS_AGENCY': 1})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code)

    today = utils.Date.nullify_time_of_date(datetime.now())

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': utils.Date.to_iso(today - relativedelta(months=3)),
                       'FINISH_DT': utils.Date.to_iso(today + relativedelta(years=1)),
                       'IS_SIGNED': utils.Date.to_iso(today),
                       'SERVICES': [context.service.id],
                       'CURRENCY': context.currency.num_code,
                       'DISCOUNT_POLICY_TYPE': 24,
                       'FIRM': context.firm.id,
                       'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25,
                       }

    if scale_discount:
        what_day_is_today = datetime.today().day
        # предыдущий месяц начинает учитываться в расчете бюджета с 10 числа,
        # поэтому если 10 число еще на наступило создаем договор и акт на два месяца назад, иначе на прошлый месяц
        contract_start_dt, act_dt = utils.Date.previous_month_first_and_last_days()
        if what_day_is_today < 10:
            contract_start_dt -=  relativedelta(months=1)
            act_dt -= relativedelta(months=1)
        contract_params.update({'EXTERNAL_ID': 'test-hermione-discounts-01/1',
                                'DT': contract_start_dt})
        contract_id, contract_eid = balance_steps.ContractSteps.create_contract_new(context.contract_template,
                                                                                    contract_params)
        campaigns_list = [{'client_id': client_id, 'service_id': context.service.id,
                           'product_id': context.product.id, 'qty': 1000500, 'begin_dt': act_dt}]
        invoice_id, _, _, orders_list = balance_steps.InvoiceSteps.create_force_invoice(client_id,
                                                                                        person_id,
                                                                                        campaigns_list,
                                                                                        context.paysys.id,
                                                                                        act_dt,
                                                                                        agency_id=client_id,
                                                                                        contract_id=contract_id,
                                                                                        )
        balance_steps.InvoiceSteps.pay(invoice_id)
        balance_steps.CampaignsSteps.do_campaigns(context.service.id, orders_list[0]['ServiceOrderID'],
                                                  {context.product.type.code: 1000500}, 0, act_dt)
        balance_steps.ActsSteps.generate(client_id, 1, act_dt)

    if fixed_discount:
        contract_params.update({'DISCOUNT_POLICY_TYPE': 8, 'CONTRACT_DISCOUNT': '19.26',
                                'EXTERNAL_ID': 'test-hermione-discounts-01/2'})
        balance_steps.ContractSteps.create_contract_new(context.contract_template, contract_params)

    return client_id
