import datetime

import balance.balance_steps as steps

dt = datetime.datetime.now()
# print steps.CLoseMonth.UpdateLimits(dt, force_value=1, client_ids=[])
# print steps.CLoseMonth.CloseFirms(dt)


from balance import balance_steps as steps
from balance import balance_api as api
print api.medium().GetClientByIdBatch([9645035])