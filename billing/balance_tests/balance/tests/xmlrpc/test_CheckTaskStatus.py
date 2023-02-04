# -*- coding: utf-8 -*-

from balance import balance_steps as steps
from simpleapi.xmlrpc import balance_xmlrpc as balance
from btestlib.constants import Services, Products
from balance import (
    balance_api as api,
    balance_db as db
)
from balance.tests.conftest import get_free_user


def test_endpoint():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'AGENCY_ID': agency_id})

    query = "update t_config set value_json=:param where item='CREATE_COMPENSATION_MULTIPLE'"
    db.balance().execute(query, {'param': '{"7": [%s]}' % agency_id})

    test_user = next(get_free_user())()
    orders_data = []
    for _ in range(2):
        service_order_id = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
        orders_data.append(
            {
                'ServiceOrderID': service_order_id,
                'ClientID': client_id,
                'ProductID': Products.DIRECT_RUB.id,
                'AgencyID': agency_id,
                'unmoderated': 0
            }
        )

    balance.create_or_update_orders_batch(
        Services.DIRECT,
        orders_data,
        test_user.uid
    )
    task = api.medium().CreateCompensationMultiple(
        test_user.uid,
        Services.DIRECT.id,
        [
            {
                'ServiceOrderID': o['ServiceOrderID'],
                'Sum': 10
            }
            for o in orders_data
        ]
    )

    status = api.medium().CheckTaskStatus(task['compensation_task_id'])
    assert status['status'] in ['IN PROGRESS', 'PARTITIAL SUCCESS', 'SUCCESS', 'FAILED']
