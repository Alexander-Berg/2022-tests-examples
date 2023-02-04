# coding: utf-8

__author__ = 'a-vasin'

import binascii
from random import randint

import cashmachines.data.passwords as pwd
from btestlib import environments as env
from btestlib.constants import Firms
from cashmachines.data.constants import *

PRICE = 10.5
QTY = 15

STABLE_SN = u'00000003820034331904'
CASHMACHINES_BY_HOST = {
    env.WhitespiritEnvironmentHost.DEV.value: {
        u'00000000381005563105',
        u'00000000381001543159',
        u'00000000381002827554'
    },

    env.WhitespiritEnvironmentHost.TEST.value: {
        u'00000000381001942057',
        u'00000000381004956051',
        u'00000000381007528533'
    }
}


def rnm10():
    return ''.join([str(randint(0, 9)) for _ in range(10)])


def rnm(serial_number, inn):
    rnm10_value = rnm10()
    t_g = rnm10_value + inn.rjust(12, '0') + serial_number
    return unicode(rnm10_value + '%06i' % (binascii.crc_hqx(t_g.encode('ascii'), 0xFFFF),))


def config(serial_number, inn=Firms.YANDEX_1.inn, groups=None):
    if groups is None:
        groups = Group.values()

    return {
        u"mysecret": pwd.mysecret(serial_number),
        u"device_payload": pwd.payload(serial_number),

        u"reg_info": {
            u"account_address": u"119021, Россия, г. Москва, ул. Льва Толстого, д. 16",
            u"account_address_name": u"Стойка номер 666",

            u"agent_mode": [
                AgentType.NONE_AGENT,
                AgentType.AGENT
            ],

            u"ofd_inn": u"7704358518",
            u"ofd_name": u"ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ОФД\"",

            u"representative_for_registration": u"Волож А. Ю.",
            u"representative_inn": u"",

            u"rnm": rnm(serial_number, inn),
            u"tax_mode": [TaxationType.OSN],
            u"terminal_number": u"6660666",

            u"user_inn": inn,
            u"user_name": u"ООО \"ЯНДЕКС\"",
            u"user_reply_email": u"isupov@yandex-team.ru",

            u"work_mode": [
                WorkMode.INTERNET_USAGE,
                WorkMode.IN_SERVICE_USAGE,
                WorkMode.AUTOMATIC
            ]
        },

        u"ofd_info": {
            u"ofd_addr": u"test.kkt.ofd.yandex.net:12345",
            u"check_url": u"nalog.ru",
            u"transport": u"0"
        },

        u"group_info": {
            u"groups": groups
        }
    }


def groups_config(serial_number, inn=Firms.YANDEX_1.inn, groups=None):
    if groups is None:
        groups = Group.values()

    params = config(serial_number, inn, groups)
    del params[u'ofd_info']
    del params[u'reg_info']
    return params


def rows(prices, qtys, ndses=None, payment_type_types=None):
    if ndses is None:
        ndses = [CMNds.NDS_20] * len(prices)

    if payment_type_types is None:
        payment_type_types = [PaymentTypeType.PREPAYMENT] * len(prices)

    return [{
                u"price": unicode(price),
                u"qty": unicode(qty),
                u"tax_type": nds.name,
                u"payment_type_type": payment_type_type,
                u"text": u"Test row"
            }
            for price, qty, nds, payment_type_type in zip(prices, qtys, ndses, payment_type_types)]


def payments(amounts, payment_types=None):
    if payment_types is None:
        payment_types = [PaymentType.CARD] * len(amounts)

    return [{
                u"amount": unicode(amount),
                u"payment_type": payment_type
            }
            for amount, payment_type in zip(amounts, payment_types)]


def receipts(rows, payments, inn=Firms.YANDEX_1.inn, receipt_type=ReceiptType.INCOME, taxation_type=TaxationType.OSN,
             agent_type=AgentType.NONE_AGENT, conditional=None, trace=None):
    return utils.remove_empty({
        u"receipt_content": {
            u"firm_inn": inn,
            u"firm_reply_email": u"test@yandex.ru",
            u"firm_url": u"http://test.com",
            u"receipt_type": receipt_type.name,
            u"taxation_type": taxation_type,
            u"agent_type": agent_type,
            u"client_email_or_phone": u"isupov@yandex-team.ru",
            u"supplier_phone": u"+79161734352",
            u"rows": rows,
            u"payments": payments,
            u"additional_document_requisite": u"req_doc_id",
            u"additional_user_requisite": {
                u"name": u"user_requisite",
                u"value": u"requisite_value"
            }
        },
        u"conditional": conditional,
        u"trace": trace
    })


def ip_serial_number(ip):
    return 'ip:{}:3333'.format(ip)


def get_sn_and_mysecret(serial_number, ip):
    return serial_number, pwd.mysecret(serial_number)


def ds_receipts(fn_sn, fp, receipt_id, document_index, dt, shift_number, inn=Firms.YANDEX_1.inn,
                receipt_type=ReceiptType.INCOME, taxation_type=TaxationType.OSN,
                payment_type_type=PaymentTypeType.PREPAYMENT):
    return {
        u"client_email_or_phone": u"+79161734352",
        u"document_index": document_index,
        u"dt": dt,  # u"2017-06-29 17:02:00"
        u"firm_inn": inn,
        u"fn": {
            u"sn": fn_sn
        },
        u"fp": fp,
        u"id": receipt_id,
        u"payment_type_type": payment_type_type,
        u"receipt_type": receipt_type.name,
        u"shift_number": shift_number,
        u"taxation_type": taxation_type
    }


def sn_condition(serial_number):
    return "(= (int (or (. device sn) 0)) {})".format(serial_number)


# a-vasin: https://github.yandex-team.ru/Billing/whitespirit/blob/master/docs/conditional.md
# accept_device, before, after
def conditional(**kwargs):
    return {
        "dialect": "hy",
        "conditions": [dict(condition=condition, expression=str(expression))
                       for condition, expression in kwargs.iteritems()]
    }
