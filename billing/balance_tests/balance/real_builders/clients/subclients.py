# -*- coding: utf-8 -*-

from . import steps

from balance import balance_steps
from jsonrpc import dispatcher
from ..common_defaults import AGENCY_NAME

import balance.balance_db as db


CONTEXT = steps.CONTEXT


# несколько субклиентов у одного агентства, без пагинации
# (есть/нет названия, есть/нет email, есть/нет телефон, есть/нет url, агентство/не агентство)
# есть субклиент без заказов
def test_no_pagination_various_subclients():
    agency_id = balance_steps.ClientSteps.create({'IS_AGENCY': 1, 'NAME': AGENCY_NAME})

    clients_with_orders = list()
    # субклиент-физлицо, без контактов
    clients_with_orders.append(balance_steps.ClientSteps.create({'NAME': 'Физик без контактов', 'CLIENT_TYPE_ID': 0,
                                                                 'EMAIL': '', 'PHONE': '', 'URL': ''}))
    # субклиент-ПБОЮЛ, с контактами
    clients_with_orders.append(balance_steps.ClientSteps.create({'NAME': 'ПБОЮЛ с контактами', 'CLIENT_TYPE_ID': 1,
                                                                 'EMAIL': 'subclient_pboul@ya.ru', 'PHONE': '123123',
                                                                 'URL': 'http://client.info/'}))
    # субклиент-ООО, агентство, без названия
    clients_with_orders.append(balance_steps.ClientSteps.create({'IS_AGENCY': 1,  'CLIENT_TYPE_ID': 2, 'NAME': '',
                                                                 'EMAIL': '', 'PHONE': '',
                                                                 'URL': 'http://client.info/'}))
    # субклиент-ЗАО, без контактов
    clients_with_orders.append(balance_steps.ClientSteps.create({'NAME': 'ЗАО без контактов', 'CLIENT_TYPE_ID': 3,
                                                                 'EMAIL': '', 'PHONE': '123123', 'URL': ''}))
    # субклиент-ОАО, без контактов
    clients_with_orders.append(balance_steps.ClientSteps.create({'NAME': 'ОАО без контактов', 'CLIENT_TYPE_ID': 4,
                                                                 'EMAIL': 'subclient_oao@ya.ru', 'PHONE': '',
                                                                 'URL': ''}))
    # субклиент неведомого типа, без контактов
    clients_with_orders.append(balance_steps.ClientSteps.create({'NAME': 'Субклиент неведомого типа',
                                                                 'CLIENT_TYPE_ID': 10,
                                                                 'EMAIL': '', 'PHONE': '', 'URL': ''}))

    for client_id in clients_with_orders:
        steps.create_base_request(client_id=client_id, agency_id=agency_id)

    client_without_order = balance_steps.ClientSteps.create({'NAME': 'Меня не видно', 'AGENCY_ID': agency_id})
    return agency_id, clients_with_orders, client_without_order


# много субклиентов с разными именами для проверки пагинации
def test_pagination_same_subclients():
    agency_id = balance_steps.ClientSteps.create({'IS_AGENCY': 1, 'NAME': AGENCY_NAME})
    client_names = ['Субклиент Витя', 'Субклиент Женя', 'Субклиент Ваня', 'Субклиент Лёня', 'Субклиент Глеб',
                    'Субклиент Аля', 'Субклиент Юля', 'Субклиент Сережа', 'Субклиент Рома', 'Субклиент Кирилл',
                    'Субклиент Артем', 'Субклиент Наташа', 'Субклиент Веня', 'Субклиент Ксюша', 'Субклиент Леша']
    clients = list()
    for name in client_names:
        clients.append(balance_steps.ClientSteps.create({'NAME': name}))
    for client_id in clients:
        steps.create_base_request(client_id=client_id, agency_id=agency_id)
    return agency_id

