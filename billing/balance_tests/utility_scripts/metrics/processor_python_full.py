# -*- coding: utf-8 -*-
import datetime
import decimal
import pprint
from decimal import Decimal as D

import btestlib.reporter as reporter
from btestlib.secrets import get_secret, Tokens
from utility_scripts.startrek.startrek_client import Startrek

TESTERS = [u'torvald', u'atkaya', u'sandyk', u'aikawa', u'igogor', u'blubimov', u'fellow', u'ulrich']
TRUST_DEV = [u'khoruzhik', u'vaclav', u'buyvich', u'zhmyh', u'skydreamer']

BUGS_TYPES = [u'bug', u'sub-bug']

ctx = decimal.getcontext()
ctx.prec = 3

NEW_LIFE_DATE = datetime.datetime(2015, 9, 10)


def Print(obj):
    reporter.log(pprint.pformat(obj).decode('unicode_escape'))


def get_all_tickets():
    client = Startrek(useragent='tst', base_url="https://st-api.yandex-team.ru",
                      token=get_secret(*Tokens.PIPELINER_OAUTH_TOKEN))
    query = u'''Queue: Баланс AND (type: !DDL)
            AND Created: >= "01-01-2015" AND Updated: >= "01-01-2015"
            AND (Resolution: !Duplicate Resolution: !"Invalid")
            AND status: !New status: !Open
            AND Tags: !without_test'''
    tickets = client.issues.find(query)

    tickets_with_data = {}
    release_dates = {}

    total = len(tickets)
    current = 0
    for ticket in tickets:
        current += 1
        message = '{} of {} tickets'.format(current, total)
        # reporter.log('{} of {} tickets'.format(current, total))

        # Получаем последнюю fixVersion тикета
        fixVersion = ticket.fixVersions[-1].name if ticket.fixVersions else u'NoVersion'

        # Отбрасываем тикеты, не входящие в релизы, релиз 2.х и задачи без fixVersion (разработки там нет)
        if fixVersion in [u'Внерелизные работы', u'NoVersion', u'2.x']:
            continue

        # Для релизных тикетов сохраняем Deadline, как дату выкладки (ожидаемую)
        if ticket.type.key == u'release':
            release_dates[fixVersion] = ticket.deadline

        # Если версия ещё не встречалась - инициализируем вложенную структуру
        if fixVersion not in tickets_with_data:
            tickets_with_data[fixVersion] = {'all': {'all': D('0'), 'automated': D('0'), 'tasks': D('0')},
                                             'py': {'all': D('0'), 'automated': D('0'), 'tasks': D('0')},
                                             'mt': {'all': D('0'), 'automated': D('0'), 'tasks': D('0')}}

        # Если тикет - задача, сохраняем для статистики
        if ticket.type.key not in BUGS_TYPES:
            tickets_with_data[fixVersion]['all']['tasks'] += D('1')

        # Считаем количество задач с тегами 'py' и 'mt'.
        # Статистика будет врать, если будут тикеты с обоими тикетами одновременно.
        if 'py' in ticket.tags:

            message = message + ' py '

            tickets_with_data[fixVersion]['py']['all'] += D('1')
            if 'automated' in ticket.tags:

                message = message + ' automated '

                tickets_with_data[fixVersion]['py']['automated'] += D('1')
                tickets_with_data[fixVersion]['all']['automated'] += D('1')
        if 'mt' in ticket.tags:

            message = message + ' mt '

            tickets_with_data[fixVersion]['mt']['all'] += D('1')
            if 'automated' in ticket.tags:

                message = message + ' automated '

                tickets_with_data[fixVersion]['mt']['automated'] += D('1')
                tickets_with_data[fixVersion]['all']['automated'] += D('1')
        tickets_with_data[fixVersion]['all']['all'] += D('1')

        reporter.log(message)

    reporter.log('tickets_with_data: {}'.format(tickets_with_data))
    reporter.log('release_dates: {}'.format(release_dates))

    # Вспомогательная функция для фильтрации релизов без даты ИЛИ релизов, случившихся ДО начала разметки тикетов тегами
    def is_new_release(version):
        if version in release_dates:
            date = datetime.datetime.strptime(release_dates[version], '%Y-%m-%d')
            return date >= NEW_LIFE_DATE
        else:
            return False

    # Фильтруем релизы
    release_stats = {release: value for release, value in tickets_with_data.iteritems() if is_new_release(release)}
    reporter.log('release_stats: {}'.format(release_stats))

    # Структура для предачи метрики
    total_stats = {'py': {'open': D('0'), 'automated': D('0'), 'all': D('0')},
                   'mt': {'open': D('0'), 'automated': D('0'), 'all': D('0')},
                   'all': {'open': D('0'), 'automated': D('0'), 'all': D('0')}}

    # Считаем статистику
    for version in sorted(release_stats.keys()):

        reporter.log('{}'.format(version), )

        # Сортируем ключи так, чтобы ключ 'all' оказался последним (хрупко!)
        clusters = release_stats[version].keys()
        clusters.sort(reverse=True)

        # Обходи ключи и считаем статистику
        for cluster in clusters:
            all = release_stats[version][cluster]['all']
            processed = release_stats[version][cluster]['automated']
            open = all - processed
            release_stats[version][cluster]['open'] = open
            processed_percentage = (processed / all) * D('100') if all else None
            release_stats[version][cluster]['percentage'] = processed_percentage

            total_stats[cluster]['automated'] += processed
            total_stats[cluster]['open'] += open
            total_stats[cluster]['all'] += processed + open

            reporter.log('\t{}: {:<6} ({}\{})'.format(cluster,
                                                      '{}%'.format(
                                                          processed_percentage) if processed_percentage is not None
                                                      else '',
                                                      processed,
                                                      all), )
        print('\t{}'.format(release_dates[version] if version in release_dates else 'None'))

    reporter.log(('-' * 46))

    print total_stats

    # Cчитаем итоговые отношения
    for cluster, value in total_stats.iteritems():
        if total_stats[cluster]['all']:
            percentage = (D(total_stats[cluster]['automated']) / total_stats[cluster]['all']) * D('100')
        else:
            percentage = 0
        total_stats[cluster]['percentage'] = percentage

        # py_percentage = (D(total_stats['py']['automated']) / py_total) * D('100')
        # mt_percentage = (D(total_stats['mt']['automated']) / mt_total) * D('100')
        # all_percentage = (D(total_stats['all']['automated']) / all_total) * D('100')

    reporter.log(('TOTAL\tPy: {:<6} ({}\{})\tMT: {:<6} ({}\{})\tALL: {:<6} ({}\{})'.format(
        D(total_stats['py']['percentage']),
        D(total_stats['py']['automated']),
        D(total_stats['py']['all']),
        D(total_stats['mt']['percentage']),
        D(total_stats['mt']['automated']),
        D(total_stats['mt']['all']),
        D(total_stats['all']['percentage']),
        D(total_stats['all']['automated']),
        D(total_stats['all']['all']))))
    reporter.log(('\nActual on: {}'.format(datetime.datetime.now().date())))

    for version in release_stats:
        release_stats[version]['date'] = datetime.datetime.strptime(release_dates[version], '%Y-%m-%d')

    total_stats['date'] = datetime.datetime.now().date()

    # Отдаём данные для метрики
    # return datetime.datetime(now.year, now.month, now.day), py_percentage, D(ttl['py']['processed']), py_total,  \
    #                               mt_percentage, D(ttl['mt']['processed']), mt_total
    return total_stats, release_stats


if __name__ == '__main__':
    get_all_tickets()
    pass
