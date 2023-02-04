import datetime
import hamcrest

from btestlib import utils as utils
from balance import balance_steps as steps
import balance.balance_db as db

dt = datetime.datetime.now()

promocode_list_given_to_smb = [
    {'promocode': 'ALD9S4F4HFSERVV8', 'inn': '6194001393', 'promocode_id': 2271481},
    {'promocode': 'ALD9AA8S6DNLA3GL', 'inn': '7727286588', 'promocode_id': 2271480},
    {'promocode': 'ALD9ANBBMMFTCPH2', 'inn': '7813246740', 'promocode_id': 2271479},
    {'promocode': 'ALD9UQJXMSW85V3L', 'inn': '231218184708', 'promocode_id': 2271478},
    {'promocode': 'ALD978A658KSGFUU', 'inn': '5258128389', 'promocode_id': 2271477},
    {'promocode': 'ALD99AMK8A7G86E9', 'inn': '7814647618', 'promocode_id': 2271476},
    {'promocode': 'ALD93JR3XKH533N9', 'inn': '7813247688', 'promocode_id': 2271475},
    {'promocode': 'ALD99T9CCH8BJS3K', 'inn': '0273908120', 'promocode_id': 2271474},
    {'promocode': 'ALD9T5T6834DXE2X', 'inn': '4217177226', 'promocode_id': 2271473},
    {'promocode': 'ALD9EDELK93GUFBU', 'inn': '7810434834', 'promocode_id': 2271472},
]


def basic_test():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475

    for promocode in promocode_list_given_to_smb:
        promocode_id = promocode['promocode_id']
        client_id = steps.ClientSteps.create()
        steps.PromocodeSteps.set_dates(promocode_id, start_dt=dt - datetime.timedelta(days=1))
        steps.PromocodeSteps.delete_reservation(promocode_id)
        steps.PromocodeSteps.reserve_promocode(client_id, promocode_id, begin_dt=dt - datetime.timedelta(days=1))
        steps.PromocodeSteps.set_services(promocode_id, service_list=[7])
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params=dict(inn=promocode['inn']))

        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)

        service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id2)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 3001, 'BeginDT': dt},
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 3001, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode['promocode']))
        #
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
        is_promo_applied = steps.PromocodeSteps.is_applied(promocode_id, invoice_id)
        utils.check_that(is_promo_applied, hamcrest.equal_to(True))


basic_test()

promocode_list_not_given_yet = [
    {'promocode': 'ALD97EUTFULUY5ME', 'inn': '6194001393', 'promocode_id': 2272762},
    # {'promocode': 'ALD9XBR6A46KKSBZ', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD9AWXR9V8Q2YES', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD9Z75DCCZ9JDAR', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD9VPZR5CLPTPU3', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD99R4EEYDEXJW6', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD93EC6AKNJ76MD', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD9HPX32WACU5HJ', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD9NPGPHHJ9APQQ', 'inn': 6194001393, 'promocode_id': },
    # {'promocode': 'ALD99YVJJY9KVUUP', 'inn': 6194001393, 'promocode_id': },
]
def basic_test2():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    MSR = 'Bucks'

    for promocode in promocode_list_not_given_yet:
        promocode_id = promocode['promocode_id']
        client_id = steps.ClientSteps.create()
        steps.PromocodeSteps.set_dates(promocode_id, start_dt=dt - datetime.timedelta(days=1))
        steps.PromocodeSteps.delete_reservation(promocode_id)
        steps.PromocodeSteps.reserve_promocode(client_id, promocode_id, begin_dt=dt - datetime.timedelta(days=1))
        steps.PromocodeSteps.set_services(promocode_id, service_list=[7])
        person_id = steps.PersonSteps.create(client_id, PERSON_TYPE, params=dict(inn=promocode['inn']))

        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)

        service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id2)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 3001, 'BeginDT': dt},
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 3001, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(PromoCode=promocode['promocode']))

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
        is_promo_applied = steps.PromocodeSteps.is_applied(promocode_id, invoice_id)
        utils.check_that(is_promo_applied, hamcrest.equal_to(False))

basic_test2()


# steps.InvoiceSteps.pay(51920735)
