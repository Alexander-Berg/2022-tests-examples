__author__ = 'aikawa'

        # if (self.paysys and self.paysys.id == 1002 and self.dt < datetime.datetime(2008, 1, 1) and
        #         self.receipt_sum_1c == 0):

import datetime

from balance import balance_steps as steps

invoice_id = 1021
steps.InvoiceSteps.pay(invoice_id)