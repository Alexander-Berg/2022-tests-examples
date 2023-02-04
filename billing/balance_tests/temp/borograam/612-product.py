# -*- coding: utf-8 -*-
from balance import balance_steps as steps
from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS
from datetime import datetime as dt
from btestlib import utils
from btestlib.constants import Services
from decimal import Decimal as D
from dateutil.relativedelta import relativedelta

# ROOT_PATH = '//home/market/testing/mbi/billing/tlog/revenues/'
# SOURCE_NAME = 'revenues'
amount = D('1.25')

context = BLUE_MARKET_PAYMENTS
date = utils.Date.first_day_of_month(dt.now())

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, context.person_type.code)

# создаем договор для клиента-партнера
_, _, contract_id, _ = steps.ContractSteps.create_partner_contract(
    context,
    client_id=client_id,
    person_id=person_id,
    is_offer=1,
    additional_params={'start_dt': date})

steps.PartnerSteps.create_fake_partner_stat_aggr_tlog_completion(
    date,
    type_='express_delivery_to_customer',
    service_id=Services.BLUE_MARKET.id,
    client_id=client_id, amount=amount,
    last_transaction_id=1)

steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, date)
act_data = steps.ActsSteps.get_all_act_data(client_id)
act_id = act_data[0]['id']
invoice_id = act_data[0]['invoice_id']
steps.ExportSteps.export_oebs(
    client_id=client_id,
    person_id=person_id,
    contract_id=contract_id,
    invoice_id=invoice_id,
    act_id=act_id)

print 'act: `{}`, invoice: `{}`, contract: `{}`, client: `{}`, person: `{}`'.format(
    act_id, invoice_id, contract_id, client_id, person_id)
