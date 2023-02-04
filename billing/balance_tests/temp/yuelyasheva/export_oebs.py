# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import equal_to

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils

# query_select = "select * from apps.test"
# query_insert = "insert into apps.test values (123)"
# query_update = "update apps.test set id=3 where id=123"
# query_delete = "delete from apps.test where id=3"
# # result = db.Oebs().execute(query, {'object_id': mapping_strings[option].format(object_id)})
# #db.oebs().execute_oebs(1, query)
# result_insert = api.test_balance().ExecuteOEBS(1, query_select, {})
# result_select = api.test_balance().ExecuteOEBS(1, query_insert, {})
# result_update = api.test_balance().ExecuteOEBS(1, query_update, {})
# result_delete = api.test_balance().ExecuteOEBS(1, query_delete, {})
#
# db.oebs().execute_oebs(1, query_insert)
# db.oebs().execute_oebs(1, query_select)
# db.oebs().execute_oebs(1, query_update)
# db.oebs().execute_oebs(1, query_delete)
#
# steps.ExportSteps.export_oebs()

steps.ExportSteps.export_oebs(invoice_id=97329747,act_id=101913833)