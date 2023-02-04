from balance import balance_steps as steps
import balance.balance_db as db
import datetime
from decimal import Decimal as D

PERSON_TYPE = 'ur'
service_id = 7
product_id = 1475
dt = datetime.datetime.now()


def test_create_ua_optimized():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    child_service_order_id = steps.OrderSteps.next_id(service_id)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=child_service_order_id,
                                             product_id=product_id,
                                             service_id=service_id)
    parent_service_order_id = steps.OrderSteps.next_id(service_id)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=product_id,
                                              service_id=service_id)
    steps.OrderSteps.merge(parent_order_id,
                           sub_orders_ids=[child_order_id],
                           group_without_transfer=1)
    print steps.OrderSteps.make_optimized(parent_order_id)

    child_service_order_id2 = steps.OrderSteps.next_id(service_id)
    child_order_id2 = steps.OrderSteps.create(client_id=client_id, service_order_id=child_service_order_id2,
                                              product_id=product_id,
                                              service_id=service_id)

    print steps.OrderSteps.merge(parent_order_id,
                                 sub_orders_ids=[child_order_id2],
                                 group_without_transfer=0)

    print steps.ExportSteps.get_export_data('Order', 'UA_TRANSFER', parent_order_id)['input']


def test_from_task():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.create({
        'CLIENT_ID': client_id,
        'REGION_ID': 225,
        'CURRENCY': 'RUB',
        'MIGRATE_TO_CURRENCY': dt,
        # 'MIGRATE_TO_CURRENCY': dt + datetime.timedelta(days=1)
        'SERVICE_ID': service_id,
        'CURRENCY_CONVERT_TYPE': 'MODIFY'
    })
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    child_service_order_id = steps.OrderSteps.next_id(service_id)
    child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=child_service_order_id,
                                             product_id=product_id,
                                             service_id=service_id)
    parent_service_order_id = steps.OrderSteps.next_id(service_id)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=product_id,
                                              service_id=service_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': child_service_order_id, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params={'InvoiceDesireDT': dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                 credit=0, contract_id=None, overdraft=0
                                                 , endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.OrderSteps.merge(parent_order_id,
                           sub_orders_ids=[child_order_id],
                           group_without_transfer=1)
    print steps.OrderSteps.make_optimized_force(parent_order_id)
    # print steps.ExportSteps.get_export_data('Order', 'UA_TRANSFER', parent_order_id)
    # print steps.ExportSteps.get_export_data('Order', 'PROCESS_COMPLETION', child_service_order_id)
    # steps.CampaignsSteps.update_campaigns(service_id, child_service_order_id,
    #                                       {'Money': D('10.66667')}, do_stop=0, campaigns_dt=None)
    # steps.CommonSteps.export('PROCESS_COMPLETION', 'Order', child_order_id)
    steps.CampaignsSteps.do_campaigns(service_id, child_service_order_id, {'Bucks': D('105.66667')}, 0, dt)
    # print steps.ExportSteps.get_export_data('Order', 'PROCESS_COMPLETION', child_service_order_id)
    # print steps.ExportSteps.get_export_data('Order', 'UA_TRANSFER', parent_order_id)['input']
