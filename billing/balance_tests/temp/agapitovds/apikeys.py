# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime

from balance import balance_steps as steps


# steps.InvoiceSteps.create_cash_payment_fact(invoice_eid=u'Б-3839476866-1',
#                                             amount=D('669600'),
#                                             dt=datetime.datetime.now(),
#                                             type='INSERT',
#                                             invoice_id=150465814)
# 
# steps.ActsSteps.hide(156532518)
# steps.CampaignsSteps.do_campaigns(service_id=129, service_order_id=301055, campaigns_params={'Money': 2008800})
steps.ActsSteps.generate(client_id=1351807455, force=0)
