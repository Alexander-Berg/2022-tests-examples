# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()


class Handles(object):
    CLIENT = 'client?client_id={object_id}'
    CLIENT_PERSON = 'client/person?client_id={object_id}'
    CLIENT_REPRESENTATIVES = 'client/representatives'

    CONTRACT_CLIENT_CREDIT_LIMIT = 'contract/client-credit/limit?client_id={object_id}'

    FIRM = 'firm/list'

    EDO_PERSON_ACTUAL_OFFERS = 'edo/person/actual-offers?client_id={object_id}'
    EDO_PERSON_CONTRACTS = 'edo/person/contracts?client_id={object_id}'
    EDO_PERSON_OFFERS_HISTORY = 'edo/person/offers-history?person_id={object_id}'
    EDO_TYPES = 'edo/types'

    INVOICE = 'invoice?invoice_id={object_id}'
    INVOICE_ACTS = 'invoice/acts?invoice_id={object_id}'
    INVOICE_CONSUMES = 'invoice/consumes?invoice_id={object_id}'
    INVOICE_OEBS_DATA = 'invoice/oebs-data?invoice_id={object_id}'
    INVOICE_OPERATIONS = 'invoice/operations?invoice_id={object_id}'
    INVOICE_TRANSFER_CHECK = 'invoice/transfer/check?invoice_id={object_id}'

    MANAGER = 'manager/list'

    ORDER = 'order?order_id={object_id}'
    ORDER_LIST = 'order/list?client_id={object_id}'
    ORDER_OPERATIONS = 'order/operations?order_id={object_id}'
    ORDER_UNTOUCHED_REQUESTS = 'order/untouched-requests?order_id={object_id}'
    ORDER_WITHDRAW_FROM_ORDERS = 'order/withdraw/from-orders?client_id={object_id}'
    ORDER_WITHDRAW_VALIDATE_AMOUNT = 'order/withdraw/validate-amount?order_id={object_id}'

    PAYSYS = 'paysys/list'

    PERSON = 'person?person_id={object_id}'
    PERSON_EDIT = 'person/set-person'

    SERVICE = 'service?service_id={object_id}'
    SERVICE_LIST = 'service/list'

    USER = 'user/permissions'


class ErrorData(object):
    CLIENT_NOT_FOUND = {
        u'description': u'Client with ID {object_id} not found in DB',
        u'error': u'CLIENT_NOT_FOUND',
    }
    ERROR_ID = -1
