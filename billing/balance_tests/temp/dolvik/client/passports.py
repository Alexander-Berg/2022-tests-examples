# -*- coding: utf-8 -*-

import datetime

from dateutil.relativedelta import relativedelta
import balance.balance_steps as steps

LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)

client_id = steps.ClientSteps.create()
# Переходим на мультивалютность миграцией
steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                      dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
