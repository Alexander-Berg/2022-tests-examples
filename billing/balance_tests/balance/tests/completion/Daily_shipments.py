import datetime

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import reporter

not_existing_service_order_id = steps.OrderSteps.next_id(70)

client_id = steps.ClientSteps.create()
service_order_id = steps.OrderSteps.next_id(service_id=70)
steps.OrderSteps.create(client_id=client_id, product_id=501394, service_id=70, service_order_id=service_order_id)
test_data = [
    # {'Money': '131.12',
    #  'ServiceID': 70,
    #  'ServiceOrderID': not_existing_service_order_id,
    #  'dt': datetime.datetime(2015, 1, 2, 0, 0)},
    # {'Money': '141.1192',
    #  'ServiceID': 70,
    #  'ServiceOrderID': service_order_id,
    #  'dt': datetime.datetime(2015, 1, 3, 0, 0)},
    # {'Days': '15',
    #  'ServiceID': 70,
    #  'ServiceOrderID': service_order_id,
    #  'dt': datetime.datetime(2015, 1, 1, 23, 0)},
    {'Months': '15',
     'ServiceID': 70,
     'ServiceOrderID': service_order_id,
     'dt': datetime.datetime(2015, 1, 1, 0, 0)}
]

answer = api.medium().DailyShipments(test_data)
reporter.log(answer)
