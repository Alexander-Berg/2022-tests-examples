import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()
compl_dt_prev_day = dt - datetime.timedelta(days=1)
compl_dt_prev_prev_day = dt - datetime.timedelta(days=2)
next_day = dt + datetime.timedelta(days=1)
ORDER_DT = dt

PERSON_TYPE = 'ur'
SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003


def create_order(client_id, product_id, service_id, agency_id):
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    return service_order_id, order_id


agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
client_id_1 = steps.ClientSteps.create()
client_id_2 = steps.ClientSteps.create()
_, child_order_1 = create_order(client_id_1, PRODUCT_ID, SERVICE_ID, agency_id)
_, child_order_2 = create_order(client_id_1, PRODUCT_ID, SERVICE_ID, agency_id)
_, parent_order = create_order(agency_id, PRODUCT_ID, SERVICE_ID, None)
print steps.OrderSteps.merge(parent_order, [child_order_1, child_order_2])
