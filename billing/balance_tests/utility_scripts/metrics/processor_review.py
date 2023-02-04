# -*- coding: utf-8 -*-

from btestlib.secrets import get_secret, Tokens
from btestlib.utils import Date
from utility_scripts.startrek.startrek_client import Startrek

QA_TEAM = [u'atkaya', u'torvald', u'sandyk', u'apopkov', u'yuelyasheva']
PARTNER_DEV = [u'halty', u'a-vasin', u'quark', u'vorobyov-as', u'isupov', u'mindlin', u'sfreest', u'roman-nagaev']
CORE_DEV = [u'lightrevan', u'natabers', u'azurkin', u'aikawa', u'sasorokin', u'venikman1']
FRONT_DEV = [u'dolvik', u'elena-kot', u'leonid-k', u'leon-minasyan', u'enovikov11', u'danixoon']

ST_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)


def get_tickets_count(query):
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)

    tickets = client.issues.find(query)

    qa_tickets = []
    partner_tickets = []
    core_tickets = []
    front_tickets = []

    # собираем тикеты по каждой группе
    for ticket in tickets:
        for reviewer in ticket.qaReviewer:
            if reviewer.login in PARTNER_DEV:
                partner_tickets.append(ticket.key)
            if reviewer.login in CORE_DEV:
                core_tickets.append(ticket.key)
            if reviewer.login in QA_TEAM:
                qa_tickets.append(ticket.key)
            if reviewer.login in FRONT_DEV:
                front_tickets.append(ticket.key)

    # удаляем дубликаты тикетов (если несколько ревьюеров из одной группы у одной и той же задачи)
    partner_tickets = list(set(partner_tickets))
    core_tickets = list(set(core_tickets))
    qa_tickets = list(set(qa_tickets))
    front_tickets = list(set(front_tickets))

    # считаем количество тикетов каждой группы
    count_partner_tickets = len(partner_tickets)
    count_core_tickets = len(core_tickets)
    count_qa_tickets = len(qa_tickets)
    count_front_tickets = len(front_tickets)

    return {'PARTNERS': count_partner_tickets, 'CORE': count_core_tickets, 'QA': count_qa_tickets,
            'FRONT': count_front_tickets}


def get_metrics():
    query = u'''queue: Баланс AND status: Ревью'''
    return get_tickets_count(query)


def get_month_metrics():
    # получаем первый и последний дни предыдущего месяца
    filter_start_dt, filter_end_dt = Date.previous_month_first_and_last_days()

    # запрос для получения всех тикетов, переведенных в ревью в прошлом месяце
    query = u'''queue: Balance
        AND tags: !"without_test"
        AND Type: !Хотфикс
        AND Components: !"Commision (комиссия)"
        AND Status: !Новый AND Status: !Открыт AND Status: !"В работе" AND Status: !CVS
        AND QA-Reviewer: notEmpty()
        and Status: changed(to: Ревью date: ''' + filter_start_dt.strftime("%d.%m.%Y") + ' .. ' \
            + filter_end_dt.strftime("%d.%m.%Y") + ')'

    return get_tickets_count(query)
