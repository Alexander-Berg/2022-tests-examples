# -*- coding: utf-8 -*-

import logging
import pprint
import re

import requests
import yaml

from btestlib.secrets import get_secret, Tokens
from utility_scripts.startrek.startrek_client import Startrek
from utility_scripts.startrek.startrek_client.exceptions import Forbidden

logging.getLogger('utility_scripts.startrek.startrek_client.connection').setLevel(logging.WARNING)

ST_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)
C_TOKEN = get_secret(*Tokens.PIPELINER_OAUTH_TOKEN)

HEADERS = {u'Authorization': u'OAuth {}'.format(C_TOKEN), u'Accept': u'application/json',
           u'Content-Type': u'application/json'}

missed_tags = {u'B0', u'B1', u'B2', u'B3', u'S0', u'S1', u'S2', u'S3'}


def Print(obj):
    print(pprint.pformat(obj).decode('unicode_escape'))


def is_production_bug(ticket):
    return ticket.stage == u'Production' or set(ticket.tags) & missed_tags


def get_release_number_from_summary(summary):
    # "релиз 2.99 (ещё возможно какой-то текст)"
    splitted = summary.split()
    return splitted[1]


def init_release(releases, release):
    releases[release] = {'ticket': None,
                         'key': None,
                         'summary': None,
                         'update_dt': None,
                         'deployed': None,
                         'hot_fixes': []}
    return releases


def get_metrics_new():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    # base_dt = base_dt.strftime('%Y-%m-%d')

    query = u'''queue: Баланс
            AND (Type: Релиз)
            AND Resolution: !Некорректный Resolution: !"Не будет исправлено"
            AND Summary: !"предпродакшен"
            AND Summary: !"ПРЕпродакшен"
            AND Summary: !"ПРЕДпрод"
            AND Summary: !"предпрод"
            AND Summary: !"препрод"
            AND Created: >= "2016-05-01"
            "Sort By": created ASC'''
    tickets = client.issues.find(query)

    for ticket in tickets:
        links = ticket.links.get_all()

        # TO_DEBUG
        # print u'{}: {}'.format(ticket.key, ticket.summary)

        balance_links = [linked for linked in links if u'BALANCE' in linked.object.key]
        trust_links = [linked for linked in links if u'TRUST' in linked.object.key]

        balance_bugs = [linked for linked in balance_links if linked.object.type.key in [u'bug', u'sub-bug']]
        balance_tasks = [linked for linked in balance_links if linked.object.type.key not in [u'bug', u'sub-bug']]

        balance_hotfix = [linked for linked in balance_tasks if u'хотфикса' in linked.object.summary]

        # TO_DEBUG
        # print u'\tHotfixes count: {}'.format(len(balance_hotfix))

        ticket_in_hotfixes = 0
        prod_bugs_in_hotfixes = 0
        real_hotfix_count = 0

        # Дата выкладки ----------------------------------------------------

        # Паттерн для поиска кондукторных тикетов в st-тикете:
        pattern = re.compile(u'https?://c\.yandex-team\.ru/tickets/([0-9]*)')

        # Ищем кондукторные тикеты в description:
        conductor = set(re.findall(pattern, ticket.description or u''))

        # Ищем кондукторные тикеты в комментариях:
        for comment in ticket.comments:
            conductor = conductor.union(set(re.findall(pattern, comment.text or u'')))

        # Ищем среди кондукторных тикетов подтверждение выкладки: "done" статус
        deployed = False
        updated = []
        for id in conductor:
            ticket_response = requests.get(u'https://c.yandex-team.ru/api/v1/tickets/{}'.format(id), headers=HEADERS)
            if ticket_response.json()['value']['status'] == u'done' and ticket_response.json()['value'][
                'branch'] in [u'stable', u'hotfix']:
                deployed = True

                # Плохой вариант: по update_dt кондукторного тикета (часто он меньше, чем реальные даты выкладок)
                # updated.append(ticket_response.json()['value']['updated_at'])

                # Хорошие вариант: перебор тасков, входящих в кондукторный тикет
                iteration = 1
                while True:
                    next_task_name = u'PAYSYS-{}-{}'.format(id, iteration)
                    response = requests.get(u'https://c.yandex-team.ru/api/task_log/{}'.format(next_task_name),
                                            headers=HEADERS)
                    if response.ok:
                        task_log = yaml.load(response.text)
                        dates = [item[':date'] for item in task_log]
                        if dates:
                            updated.append(max(dates))
                        iteration += 1
                    else:
                        break

        max_update_dt = max(updated) if updated else None

        # --------------------------------------------------------------------

        for hotfix in [x.object for x in balance_hotfix]:

            if hotfix.status.key == u'closed' and (hotfix.resolution.id == u'1' if hotfix.resolution else True):
                # Для хот-фиксов выкладываемые задачи лежат в "Связанные" (relates)
                tickets_to_deploy = [x.object for x in hotfix.links.get_all() if x.type.id == u'relates']

                production_bugs = [x for x in tickets_to_deploy if is_production_bug(x)]

                ticket_in_hotfixes += len(tickets_to_deploy)
                prod_bugs_in_hotfixes += len(production_bugs)
                real_hotfix_count += 1

                # TO_DEBUG
                # print u'\t\t{} \ {} : {}, Resolution: {}, Date: {}'.format(len(tickets_to_deploy),
                #                                                           len(production_bugs),
                #                                                           hotfix.key,
                #                                                           hotfix.resolution.key if hotfix.resolution else None,
                #                                                           hotfix.createdAt)

        print u'{}: {:>20} : {:>2} : {:>2} : {:>2} : {}'.format(ticket.key, ticket.summary, real_hotfix_count,
                                                                ticket_in_hotfixes, prod_bugs_in_hotfixes,
                                                                max_update_dt)
    pass


def get_metrics_trust(period_start, period_end, work_days):
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    # base_dt = base_dt.strftime('%Y-%m-%d')
    # period_start = "2018-01-01"
    # period_end = "2018-04-01"
    query = u'''Queue: TRUST
                AND (Type: Release or Type: Hotfix)
                AND Created: >= "{}" AND Created: <= "{}"
                "Sort By": Created DESC'''.format(period_start, period_end)
    tickets = client.issues.find(query)

    hotfixes = [ticket for ticket in tickets if ticket.type.key == u'hotfix']
    releases = [ticket for ticket in tickets if ticket.type.key == u'release']

    period_tasks = []
    period_bugs = []
    period_missed = []

    hotfix_tasks = []
    hotfix_bugs = []
    hotfix_missed = []

    release_tasks = []
    release_bugs = []
    release_missed = []

    for ticket in tickets:
        try:
            links = [link.object for link in ticket.links.get_all()]

            balance_links = [link for link in links if u'BALANCE' in link.key]
            trust_links = [link for link in links if u'TRUST' in link.key]
            paysys_links = [link for link in links if u'PAYSYS' in link.key]

            total_bugs = [link for link in links if link.type.key in [u'bug', u'sub-bug']]
            total_tasks = [link for link in links if link.type.key not in [u'bug', u'sub-bug']]
            total_missed = [link for link in links if 'qa_checked' in link.tags]
            total_missed_tasks = [task for task in total_missed if task.type.key not in [u'bug', u'sub-bug']]

            trust_bugs = [bug for bug in total_bugs if u'TRUST' in link.key]
            trust_tasks = [task for task in total_tasks if u'TRUST' in link.key]
            trust_missed = [link for link in trust_links if 'qa_checked' in link.tags]

            print u'\n{}: {:>20} : Total tickets : {:>2}'.format(ticket.key, ticket.summary, len(links))
            print u'TRUST: {}, BALANCE: {}, PAYSYS: {}'.format(len(trust_links), len(balance_links), len(paysys_links))
            print u'Tasks: {}, Bugs: {}, Missed: {}, Missed tasks: {}'.format(len(total_tasks), len(total_bugs),
                                                                              len(total_missed),
                                                                              len(total_missed_tasks))
            print u'Trust tasks: {}, Trust bugs: {}, Trust Missed: {}'.format(len(trust_tasks), len(trust_bugs),
                                                                              len(trust_missed))

            period_tasks += total_tasks
            period_bugs += total_bugs
            period_missed += total_missed

            if ticket.type.key == u'hotfix':
                hotfix_tasks += total_tasks
                hotfix_bugs += total_bugs
                hotfix_missed += total_missed
            else:
                release_tasks += total_tasks
                release_bugs += total_bugs
                release_missed += total_missed
        except Forbidden:
            print 'FORBIDDEN'
            continue

    # start = datetime.datetime.strptime(period_start, "%Y-%m-%d")
    # end = datetime.datetime.strptime(period_end, "%Y-%m-%d")
    # days = (end - start).days
    days = work_days

    def slice_(descr, tickets_, tasks, bugs, missed, days):
        tasks_bugs_cnt = len(tasks) + len(bugs)
        print "{:<10}: {:<4} total: {:<4} tasks: {:<4} bugs: {:<4} missed: {:<4} ticket_per_deploy: {:<6.2f} " \
              "deploy_per_day: {:<6.2f} ticket_per_day: {:.2f}".format(
            descr, len(tickets_), tasks_bugs_cnt, len(tasks), len(bugs), len(missed),
            tasks_bugs_cnt / float(len(tickets_)), len(tickets_) / float(days), tasks_bugs_cnt / float(days)
        )

    print "\nPeriod: {} : {}   days: {}".format(period_start, period_end, days)
    slice_("Deploys", tickets, period_tasks, period_bugs, period_missed, days)
    slice_("Releases", releases, release_tasks, release_bugs, release_missed, days)
    slice_("Hotfixes", hotfixes, hotfix_tasks, hotfix_bugs, hotfix_missed, days)

    pass


def get_metrics():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru", token=ST_TOKEN)
    # base_dt = base_dt.strftime('%Y-%m-%d')

    query = u'''queue: Баланс
            AND (Type: Релиз)
            AND Resolution: !Некорректный Resolution: !"Не будет исправлено"
            AND Summary: !"предпродакшен"
            AND Summary: !"ПРЕпродакшен"
            AND Summary: !"ПРЕДпрод"
            AND Summary: !"предпрод"
            AND Summary: !"препрод"
            AND Created: >= "2017-03-01"
            "Sort By": created ASC'''
    tickets = client.issues.find(query)

    # Инициализация итоговой структуры. Ранние релизы нужны так как в выборку попадают фиксы к ним, но не сами релизы
    releases = {u'2.12': {'ticket': None,
                          'key': None,
                          'summary': None,
                          'update_dt': None,
                          'deployed': None,
                          'hot_fixes': []},
                u'2.13': {'ticket': None,
                          'key': None,
                          'summary': None,
                          'update_dt': None,
                          'deployed': None,
                          'hot_fixes': []},
                u'2.20': {'ticket': None,
                          'key': None,
                          'summary': None,
                          'update_dt': None,
                          'deployed': None,
                          'hot_fixes': []}
                }
    # releases = dict()

    for ticket in tickets:
        links = ticket.links.get_all()

        balance_links = [linked for linked in links if u'BALANCE' in linked.object.key]
        trust_links = [linked for linked in links if u'TRUST' in linked.object.key]

        balance_bugs = [linked for linked in balance_links if linked.object.type.key in [u'bug', u'sub-bug']]
        balance_tasks = [linked for linked in balance_links if linked.object.type.key not in [u'bug', u'sub-bug']]

        # Проверяем наличие пропущенных багов среди входящих задач
        hot_fix = False
        for bug in balance_bugs:
            if is_production_bug(bug.object):
                hot_fix = True

        # Паттерн для поиска кондукторных тикетов в st-тикете:
        pattern = re.compile(u'https?://c\.yandex-team\.ru/tickets/([0-9]*)')

        # Ищем кондукторные тикеты в description:
        conductor = set(re.findall(pattern, ticket.description or u''))

        # Ищем кондукторные тикеты в комментариях:
        for comment in ticket.comments:
            conductor = conductor.union(set(re.findall(pattern, comment.text or u'')))

        # Ищем среди кондукторных тикетов подтверждение выкладки: "done" статус
        deployed = False
        updated = []
        for id in conductor:
            ticket_response = requests.get(u'https://c.yandex-team.ru/api/v1/tickets/{}'.format(id), headers=HEADERS)
            if ticket_response.json()['value']['status'] == u'done' and ticket_response.json()['value'][
                'branch'] in [u'stable', u'hotfix']:
                deployed = True

                # Плохой вариант: по update_dt кондукторного тикета (часто он меньше, чем реальные даты выкладок)
                # updated.append(ticket_response.json()['value']['updated_at'])

                # Хорошие вариант: перебор тасков, входящих в кондукторный тикет
                iteration = 1
                while True:
                    next_task_name = u'PAYSYS-{}-{}'.format(id, iteration)
                    response = requests.get(u'https://c.yandex-team.ru/api/task_log/{}'.format(next_task_name),
                                            headers=HEADERS)
                    if response.ok:
                        task_log = yaml.load(response.text)
                        dates = [item[':date'] for item in task_log]
                        if dates:
                            updated.append(max(dates))
                        iteration += 1
                    else:
                        break

        max_update_dt = max(updated) if updated else None

        # Разбиваем список тикетов на релизы и хот-фиксы к ним.
        if u'релиз 2.' in ticket.summary:
            release = get_release_number_from_summary(ticket.summary)
            releases[release] = {'ticket': ticket,
                                 'key': ticket.key,
                                 'summary': ticket.summary,
                                 'update_dt': max_update_dt,
                                 'deployed': deployed,
                                 'hot_fixes': []}

        # Хот-фикс всегда является дочерним тикетом к релизу
        elif ticket.parent and u'релиз 2.' in ticket.parent.summary:
            release = get_release_number_from_summary(ticket.parent.summary)
            if release not in releases:
                init_release(releases, release)
            # Добавляем хот-фиксный тикет в структуру релиза,
            # кроме тикета "Выкладка релиза на *продакшен* баланса" - далее он будет приклеен к тикету на релиз
            if ticket.summary != u'Выкладка релиза на *продакшен* баланса':
                releases[release]['hot_fixes'].append({'ticket': ticket,
                                                       'key': ticket.key,
                                                       'summary': ticket.summary,
                                                       'update_dt': max_update_dt,
                                                       'deployed': deployed,
                                                       'is_hot_fix': hot_fix,
                                                       'balance_links': balance_links,
                                                       'balance_bugs': balance_bugs,
                                                       'trust_links': trust_links})
        else:
            print(u'Uncategorized: {} {}'.format(ticket.key, ticket.summary))

        # Склеиваем данные тикета "Выкладка релиза на *продакшен* баланса" (использовались до 2.67) c релизным тикетом:
        if ticket.summary == u'Выкладка релиза на *продакшен* баланса':
            release = get_release_number_from_summary(ticket.parent.summary)
            releases[release]['update_dt'] = max_update_dt
            releases[release]['deployed'] = deployed

        # Выводим информацию для отладки:
        print(
            u'{0:<2} {1:<2} {2} B:{3} T:{4} {5}: {6}'.format(hot_fix, deployed, max_update_dt, len(balance_links),
                                                             len(trust_links), ticket.key, ticket.summary))

        for item in balance_bugs:
            print(
                u'{:>32} {} {:>10} {}'.format('Bug', item.object.key, item.object.stage, item.object.tags))
        for item in balance_tasks:
            print(
                u'{:>32} {} {:>10} {}'.format('Task', item.object.key, item.object.stage, item.object.tags))
        for item in trust_links:
            print(
                u'{:>32} {} {:>10} {}'.format('TRUST', item.object.key, item.object.stage, item.object.tags))
        print(u'{:>32}: {}'.format('CONDUCTOR', conductor))

    pass


if __name__ == "__main__":
    # get_metrics_new()
    # get_metrics_trust("2016-07-01", "2016-10-01", 66)
    # get_metrics_trust("2016-10-01", "2017-01-01", 64)
    # get_metrics_trust("2017-01-01", "2017-04-01", 57)
    # get_metrics_trust("2017-04-01", "2017-07-01", 61)
    # get_metrics_trust("2017-07-01", "2017-10-01", 65)
    get_metrics_trust("2017-10-01", "2018-01-01", 64)
    # get_metrics_trust("2018-01-01", "2018-04-01", 52)
    get_metrics_trust("2018-04-01", "2018-07-01", 63)
    get_metrics_trust("2018-07-01", "2018-09-21", 60)
