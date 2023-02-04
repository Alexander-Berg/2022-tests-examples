import datetime

from balance import balance_steps as steps

# steps.InvoiceSteps.pay(52329747)

dt = datetime.datetime.now()

steps.CampaignsSteps.do_campaigns(35, '20000000101557 ', {'Bucks': 30}, 0, dt)

# print steps.ActsSteps.generate(13742445, force=1, date=dt)
