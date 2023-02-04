from btestlib import environments
from btestlib import utils as butils
from simpleapi.common import logger
from simpleapi.common.utils import remove_empty
from simpleapi.data import defaults

__author__ = 'fellow'

log = logger.get_logger()

server_ora = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_ora().balance_xmlrpc_url,
                                                namespace='Balance')
server_pg = butils.XmlRpc.ReportingServerProxy(environments.simpleapi_env_pg().balance_xmlrpc_url,
                                               namespace='Balance')


def server():
    if environments.SimpleapiEnvironment.XMLRPC_URL == environments.TrustApiUrls.XMLRPC_PG:
        return server_pg
    return server_ora


def create_or_update_orders_batch(service, orders, uid):
    return server().CreateOrUpdateOrdersBatch(uid, orders, service.token)


def create_fast_payment(service_id, service_order_id,
                        client_id, transaction_id,
                        uid, paysys_id='1057'):
    p = remove_empty({
        'passport_id': uid,
        'client_id': client_id,
        'paysys_id': paysys_id,
        'inapp_transaction_id': str(transaction_id),
        'items': [{'service_id': service_id, 'service_order_id': service_order_id, 'qty': 1}, ],
    })
    return server().CreateFastPayment(p)


def create_fast_invoice(service_id, login, service_order_id):
    p = remove_empty({
        'service_id': service_id,
        'service_order_id': service_order_id,
        'login': login,
        'qty': 30,
        'paysys_id': 1002,
        'overdraft': False,
        'mobile': False,
        'back_url': 'http://music.mt.yandex.ru',
    })
    return server().CreateFastInvoice(p)


def validate_app_store_receipt(receipt, operator_uid=defaults.partner_info['operator_uid'],
                               base64=True):
    p = remove_empty({
        'ReceiptData': receipt,
        'Base64': base64,
        'Subscr': True  # fix that later - less hardcode

    })
    return server().ValidateAppStoreReceipt(str(operator_uid), p)


def check_in_app_subscription(service_id,
                              service_order_id,
                              invoice_id):
    p = remove_empty({
        'ServiceID': service_id,
        'ServiceOrderID': service_order_id,
        'InvoiceID': invoice_id
    })
    return server().CheckInAppSubscription(p)


def create_client(operator_uid, client_id, name,
                  email, phone, fax, url, city,
                  service_id=None, currency=None,
                  migrate_to_currency=None,
                  region_id=225):
    p = remove_empty({
        'CLIENT_ID': client_id,
        'NAME': name,
        'EMAIL': email,
        'PHONE': phone,
        'FAX': fax,
        'URL': url,
        'CITY': city,
        'SERVICE_ID': service_id,
        'CURRENCY': currency,
        'MIGRATE_TO_CURRENCY': migrate_to_currency,
        'REGION_ID': region_id,
    })
    return server().CreateClient(operator_uid, p)


def create_person(client_id, email=None, fname=None,
                  kpp=None, lname=None, mname=None,
                  inn=None, person_id=None, phone=None,
                  fax=None, type=None, operator_uid=None):
    p = remove_empty({
        'client_id': client_id,
        'email': email,
        'fname': fname,
        'lname': lname,
        'mname': mname,
        'inn': inn,
        'kpp': kpp,
        'person_id': person_id,
        'phone': phone,
        'fx': fax,
        'type': type,
    })
    return server().CreatePerson(operator_uid, p)


def create_request(client_id, service_order_id, operator_uid):
    p = remove_empty({
        'Qty': 1,
        'ServiceID': 23,
        'ServiceOrderID': service_order_id,
    })
    return server().CreateRequest(operator_uid, client_id, [p])


def create_user_client_association(client_id,
                                   uid_to_link=defaults.partner_info['operator_uid'],
                                   operator_uid=defaults.partner_info['operator_uid']):
    return server().CreateUserClientAssociation(operator_uid, client_id, uid_to_link)


def remove_user_client_association(client_id,
                                   uid_to_unlink=defaults.partner_info['operator_uid'],
                                   operator_uid=defaults.partner_info['operator_uid']):
    return server().RemoveUserClientAssociation(operator_uid, client_id, uid_to_unlink)


def find_client(passport_id, service_id=None):
    p = remove_empty({
        'PassportID': passport_id,
        'ServiceID': service_id,
    })
    return server().FindClient(p)
