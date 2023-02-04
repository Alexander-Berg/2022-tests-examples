# -*- coding: utf-8 -*-

from btestlib.secrets import get_secret, Tokens
from btestlib.utils import Date
from utility_scripts.startrek.startrek_client import Startrek
import datetime

ST_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)


def get_tickets():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)

    # получаем первый день предыдущего месяца и текущий день без времени
    # filter_start_dt, _ = Date.previous_month_first_and_last_days()
    filter_start_dt = Date.nullify_time_of_date(datetime.datetime(2021,2,1))
    filter_end_dt = Date.nullify_time_of_date(datetime.datetime.today())
    # filter_end_dt = Date.nullify_time_of_date(datetime.datetime(2020,4,6))

    # запрос для получения всех тикетов, переведенных в протестировано, начиная с прошлого месяца
    query = u'''queue: Balance
        AND Type: !Хотфикс
        AND Components: !"Commision (комиссия)"
        AND Components: !"Tests"
        AND Components: !"Monitoring"
        AND Components: !"Infra"
        and Status: changed(to: Протестировано date: ''' + filter_start_dt.strftime("%d.%m.%Y") + ' .. ' \
            + filter_end_dt.strftime("%d.%m.%Y") + ')'

    tickets = client.issues.find(query)

    wrong_tickets_processing = []

    # собираем тикеты
    for ticket in tickets:
        if ticket.assignee:
            for item in ticket.changelog:
                if item.fields and item.fields[0][u'field'].id == u'status' \
                        and item.fields[0][u'to'].key == u'tested' \
                        and item.updatedBy.login == ticket.assignee.login:
                    wrong_tickets_processing.append({'ticket': ticket.key, 'assignee': ticket.assignee.login})

    wrong_tickets = []
    for ticket in wrong_tickets_processing:
        if not any(item['login'] == ticket['assignee'] for item in wrong_tickets):
            wrong_tickets.append({'login': ticket['assignee'], 'count': 1, 'tickets': [ticket['ticket']]})
        else:
            for item in wrong_tickets:
                if item['login'] == ticket['assignee']:
                    item['count'] += 1
                    item['tickets'].append(ticket['ticket'])

    print(len(wrong_tickets_processing))
    for ticket in wrong_tickets:
        print(ticket)


get_tickets()