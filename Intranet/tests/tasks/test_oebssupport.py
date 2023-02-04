# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from datetime import datetime, timedelta
import mock

from django.conf import settings
from django.core.management import call_command


def test_report_failed_payments():
    links = 4
    date_from = (datetime.now() - timedelta(weeks=1)).date()

    with mock.patch('app.tasks.oebssupport.find_tickets') as mocked_find_tickets, \
         mock.patch('app.tasks.oebssupport.create_ticket') as mocked_create_ticket:
        mocked_find_tickets.return_value = return_issue_iterable(links)
        call_command('bugbounty_report_failed_payments')

    find_args = mocked_find_tickets.call_args[1]
    assert find_args == {
        'filters': {
            'queue': settings.STARTREK_OEBSSUPPORT_QUEUE,
            'author': settings.STARTREK_FAILED_PAYMENT_TICKET_AUTHOR,
            'tags': ['MONITORING'],
            'created': {'from': str(date_from)},
        }
    }

    create_args = mocked_create_ticket.call_args[1]
    assert create_args['summary'] == 'Ошибки в выплатах призов ({} - {})'.format(date_from, datetime.now().date())
    assert create_args['type'] == 2

    header = (
        'В тикетах {} обнаружены сообщения об ошибках выплат в bugbounty.\n'
        'Список выплат:\n'
        '#|\n'
        '|| БЮ|Тип отчёта|ID заг-ка|Договор|Плательщик|Период с|Период по|Дата платежа|К выплате всего|Сумма по ПСФ|Налог|Оплачено|Валюта|Результат||\n'
    ).format(', '.join('{}-{}'.format(settings.STARTREK_OEBSSUPPORT_QUEUE, i) for i in range(links)))
    assert create_args['description'].startswith(header)
    rows = {
        'YARU |BUGBOUNTY |1234567 |АБВГДЕ | Редакт Редактедович 0| Sep-11 | Jan-5 |01.01.2020 |1234| |0 | |RUB | Здесь какой-то текст ошибки #1',
        'YARU |BUGBOUNTY |1253253 |asdfg | Редакт Редактедович 1| Sep-21 | Jan-10 |02.01.2020 |2234| |1 | |RUB | Здесь какой-то текст ошибки #2',
        'YARU |BUGBOUNTY |9545832 |ABASWE | Редакт Редактедович 2| Sep-31 | Jan-20 |03.01.2020 |3234| |2 | |RUB | Здесь какой-то текст ошибки #3',
        'YARU |BUGBOUNTY |2987542 |AGUGH | Редакт Редактедович 3| Sep-41 | Jan-40 |04.01.2020 |4234| |3 | |RUB | Здесь какой-то текст ошибки #4',
    }

    assert set(row for row in create_args['description'][len(header):].replace('||', '').split('\n')
               if 'BUGBOUNTY' in row) == rows


def return_issue_iterable(n):
    issues = [mock.Mock() for _ in range(n)]
    for i, issue in enumerate(issues):
        issue.key = '{}-{}'.format(settings.STARTREK_OEBSSUPPORT_QUEUE, i)
        # markdown как в тикетах OEBSSUPPORT
        issue.description = (
            '--------------------\n'
            'БЮ | Тип отчёта |ID заг-ка | Договор | Плательщик | Период с |Период по |Дата платежа|К выплате всего| Сумма по ПСФ | Налог | Оплачено |Валюта| Результат\n'
            '--------------------\n'
            'YARU |BUGBOUNTY |1234567 |АБВГДЕ | Редакт Редактедович 0| Sep-11 | Jan-5 |01.01.2020 |1234| |0 | |RUB | Здесь какой-то текст ошибки #1\n'
            'YARU |BUGBOUNTY |1253253 |asdfg | Редакт Редактедович 1| Sep-21 | Jan-10 |02.01.2020 |2234| |1 | |RUB | Здесь какой-то текст ошибки #2\n'
            'YARU |BUGBOUNTY |9545832 |ABASWE | Редакт Редактедович 2| Sep-31 | Jan-20 |03.01.2020 |3234| |2 | |RUB | Здесь какой-то текст ошибки #3\n'
            'YARU |BUGBOUNTY |2987542 |AGUGH | Редакт Редактедович 3| Sep-41 | Jan-40 |04.01.2020 |4234| |3 | |RUB | Здесь какой-то текст ошибки #4\n'
            'YARU |ASDAGDAG |1234567 |АБВГДЕ | Редакт Редактедович 4| Sep-11 | Jan-5 |01.01.2020 |1234| |0 | |RUB | Здесь какой-то текст ошибки #1\n'
            'YARU |DGSRH |1253253 |asdfg | Редакт Редактедович 5| Sep-21 | Jan-10 |02.01.2020 |2234| |1 | |RUB | Здесь какой-то текст ошибки #2\n'
            'YARU |ABCD |9545832 |ABASWE | Редакт Редактедович 6| Sep-31 | Jan-20 |03.01.2020 |3234| |2 | |RUB | Здесь какой-то текст ошибки #3\n'
            'YARU |AGSGSh |2987542 |AGUGH | Редакт Редактедович 7| Sep-41 | Jan-40 |04.01.2020 |4234| |3 | |RUB | Здесь какой-то текст ошибки #4\n'
            '\n'
            'Служба автоматических рассылок\n'
            '\n'
            '[Created via e-mail received from: xxx@yyy.yandex.ru]\n'
        )

    return issues
