# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()


# steps.InvoiceSteps.pay_fair(134717284, payment_sum=1000, orig_id=1060490184, operation_type='ACTIVITY', source_id=1060490184, cash_receipt_number='Б-3576540456-1-62434141')

orders = [
    (42, 10590494740, {'Money': D('50') * 10}),
]

for service_id, service_order_id, data in orders:
    steps.CampaignsSteps.do_campaigns(
        service_id,
        service_order_id,
        data,
        0,
        NOW,
    )


# # Выставляем акт
client_ids = [1356270504]
for client_id in client_ids:
    steps.ActsSteps.generate(client_id, force=1, date=NOW)

# steps.ExportSteps.export_oebs(client_id=1354331861)
#
# person_ids = [
#     17709856,
# ]
# for p_id in person_ids:
#     steps.ExportSteps.export_oebs(person_id=p_id)
#
#
# # steps.ExportSteps.export_oebs(contract_id=14207310)
#
# invoice_ids = [
#     146428625,
# ]
# for i_id in invoice_ids:
#     steps.ExportSteps.export_oebs(invoice_id=i_id)

# act_ids = [155255272]
# for a_id in act_ids:
#     steps.ExportSteps.export_oebs(act_id=a_id)
