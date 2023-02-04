# -*- coding: utf-8 -*-

from infra.rtc.janitor import common  # noqa
import mock


def test_parse_poweroff():
    ticket = mock.Mock()
    ticket.summary = 'Ticket summary'
    ticket.key = 'IDDQD-1'
    ticket.tags = []
    ticket.description = '''Выключение хостов по тикету: ITDC-230461
Список хостов на выключение:
<{Список ID серверов:
%%
100530771
%%
 }>
Ответственный:  Беловодский Павел (dr-god)

<{for_janitor
Тип работ:
выключение хостов для ITDC

Список ID серверов:
100530771,12123123
123123123;test.qweeqwe.ru,;myt1-1077.search.yandex.net\t101170547
101170557 101170537

Тикет из очереди ITDC:
ITDC-230461

Ответственный за работы:
Беловодский Павел (dr-god)

Комментарий:
Тестилище!
    }>'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'comment': 'Тестилище!',
        'hosts': [
            '100530771',
            '12123123',
            '123123123',
            'test.qweeqwe.ru',
            'myt1-1077.search.yandex.net',
            '101170547',
            '101170557',
            '101170537'
        ],
        'ref_ticket_key': 'ITDC-230461',
        'responsible': 'dr-god',
        'ticket_created_by': 'nulltime@',
        'ticket_key': 'IDDQD-1',
        'ticket_summary': 'Ticket summary',
        'type': 'power_off',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_poweroff2():
    ticket = mock.Mock()
    ticket.summary = 'Ticket summary'
    ticket.key = 'IDDQD-1'
    ticket.tags = []
    ticket.description = '''Выключение хостов по тикету: ITDC-230461
Список хостов на выключение:
<{Список ID серверов:
%%
100530771
%%
 }>
Ответственный:  Беловодский Павел (dr-god)

<{for_janitor
Тип работ:
выключение хостов для ITDC

Список ID серверов:
  101170529	myt1-6565.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud
  101170559	bsmc44f.yabs.yandex.ru	Search Portal > Advertising Services > Other products > Metriс
  101170533	myt1-1077.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170530	myt1-0281.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170544	myt1-1126.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170556	myt1-2078.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud
  101170552	myt1-1927.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170555	myt1-0285.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170551	myt1-0290.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC
  101170525	ydb-48-cores-old01.search.yandex.net	Search Portal > Common Tech Services > Системы хранения и обработки данных > YDB (aka KiKiMR)
  101170557	ydb-48-cores-old00.search.yandex.net	Search Portal > Common Tech Services > Системы хранения и обработки данных > YDB (aka KiKiMR)
  101170532	victoria-01-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170554	reserve-8hdd4tb-128r-2650V4-myt.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170539	couchbase-08-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170538	docker-06-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170546	bsmc41f.yabs.yandex.ru	Search Portal > Advertising Services > Other products > Metriс
  101170562	N/A	None
  101170561	myt1-6552.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud
  101170563	myt1-6544.search.yandex.net	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud
  101350385	auto-searcher-07-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170537	auto-searcher-08-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170547	shard-02-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170527	kafka-fat-01-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170550	auto-searcher-06-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170534	auto-searcher-01-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170548	shard-01-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170543	auto-searcher-05-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170549	bsmc42f.yabs.yandex.ru	Search Portal > Advertising Services > Other products > Metriс
  101170564	docker-44-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170565	kafka-01-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170535	auto-searcher-02-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170560	couchbase-07-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170540	auto-searcher-03-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170524	kafka-fat-02-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170531	backup-05-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -
  101170542	auto-searcher-04-myt.prod.vertis.yandex.net	Vertical Services > vertical services infrastructure department > -

Тикет из очереди ITDC:
https://st.yandex-team.ru/ITDC-230461

Ответственный за работы:
Беловодский Павел (dr-god)

Комментарий:
Тестилище!
    }>'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'comment': 'Тестилище!',
        'hosts': [
            "101170529",
            "myt1-6565.search.yandex.net",
            "101170559",
            "bsmc44f.yabs.yandex.ru",
            "101170533",
            "myt1-1077.search.yandex.net",
            "101170530",
            "myt1-0281.search.yandex.net",
            "101170544",
            "myt1-1126.search.yandex.net",
            "101170556",
            "myt1-2078.search.yandex.net",
            "101170552",
            "myt1-1927.search.yandex.net",
            "101170555",
            "myt1-0285.search.yandex.net",
            "101170551",
            "myt1-0290.search.yandex.net",
            "101170525",
            "ydb-48-cores-old01.search.yandex.net",
            "101170557",
            "ydb-48-cores-old00.search.yandex.net",
            "101170532",
            "victoria-01-myt.prod.vertis.yandex.net",
            "101170554",
            "reserve-8hdd4tb-128r-2650V4-myt.vertis.yandex.net",
            "101170539",
            "couchbase-08-myt.prod.vertis.yandex.net",
            "101170538",
            "docker-06-myt.prod.vertis.yandex.net",
            "101170546",
            "bsmc41f.yabs.yandex.ru",
            "101170562",
            "101170561",
            "myt1-6552.search.yandex.net",
            "101170563",
            "myt1-6544.search.yandex.net",
            "101350385",
            "auto-searcher-07-myt.prod.vertis.yandex.net",
            "101170537",
            "auto-searcher-08-myt.prod.vertis.yandex.net",
            "101170547",
            "shard-02-myt.prod.vertis.yandex.net",
            "101170527",
            "kafka-fat-01-myt.prod.vertis.yandex.net",
            "101170550",
            "auto-searcher-06-myt.prod.vertis.yandex.net",
            "101170534",
            "auto-searcher-01-myt.prod.vertis.yandex.net",
            "101170548",
            "shard-01-myt.prod.vertis.yandex.net",
            "101170543",
            "auto-searcher-05-myt.prod.vertis.yandex.net",
            "101170549",
            "bsmc42f.yabs.yandex.ru",
            "101170564",
            "docker-44-myt.prod.vertis.yandex.net",
            "101170565",
            "kafka-01-myt.prod.vertis.yandex.net",
            "101170535",
            "auto-searcher-02-myt.prod.vertis.yandex.net",
            "101170560",
            "couchbase-07-myt.prod.vertis.yandex.net",
            "101170540",
            "auto-searcher-03-myt.prod.vertis.yandex.net",
            "101170524",
            "kafka-fat-02-myt.prod.vertis.yandex.net",
            "101170531",
            "backup-05-myt.prod.vertis.yandex.net",
            "101170542",
            "auto-searcher-04-myt.prod.vertis.yandex.net",
            ],
        'ref_ticket_key': 'ITDC-230461',
        'responsible': 'dr-god',
        'ticket_created_by': 'nulltime@',
        'ticket_key': 'IDDQD-1',
        'ticket_summary': 'Ticket summary',
        'type': 'power_off',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_poweroff_eng():
    ticket = mock.Mock()
    ticket.summary = 'Ticket summary'
    ticket.key = 'IDDQD-1'
    ticket.tags = []
    ticket.description = u"Powering-OFF hosts by ticket: ITDC-240669 \nList of hosts to be powered-off\n<{List of host's IDs:\n%%\n100417025\r\n100416966\r\n100417026\r\n100416967\r\n100417027\r\n100416945\r\n100416968\r\n100417028\n%% \n }>\nResponsible for the maintenance: \u0411\u0440\u0443\u0441\u0435\u043d\u0446\u043e\u0432 \u0410\u043b\u0435\u043a\u0441\u0430\u043d\u0434\u0440 (brusencov92) \n\n<{for_janitor\nOperation type:\nHosts Power-OFF for ITDC maintenance\n\nList of host's IDs:\n100417025\r\n100416966\r\n100417026\r\n100416967\r\n100417027\r\n100416945\r\n100416968\r\n100417028\n\nTicket from ITDC queue:\nITDC-240669\n\nResponsible for the maintenance:\n\u0411\u0440\u0443\u0441\u0435\u043d\u0446\u043e\u0432 \u0410\u043b\u0435\u043a\u0441\u0430\u043d\u0434\u0440 (brusencov92)\n }>"  # noqa
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'hosts': [
            "100417025",
            "100416966",
            "100417026",
            "100416967",
            "100417027",
            "100416945",
            "100416968",
            "100417028"
            ],
        'ticket_created_by': 'nulltime@',
        'ticket_creation_date': '2020-01-01',
        'ticket_key': 'IDDQD-1',
        'ticket_summary': 'Ticket summary',
        'ref_ticket_key': 'ITDC-240669',
        'responsible': 'brusencov92',
        'type': 'power_off'
    }


def test_add_hosts_subticket():
    ticket = mock.Mock()
    ticket.summary = 'Ticket summary'
    ticket.key = 'IDDQD-1'
    ticket.tags = []
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    ticket.description = '''

Связанный тикет на  ввод хостов в датацентр vla

Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
rtc-vla-test

<{Список ID серверов:
  101283388
  101287675
  101283408
  101283432
  101283448

}>

<{for_janitor

Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
rtc-vla-test

Демонтаж:
Нет

Список ID серверов:
101283388
101287675
101283408
101283432
101283448

Кто сдает хосты:
--- (tester@)
}>

'''.decode('utf-8')
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'ticket_key': 'IDDQD-1',
        'responsible': 'tester@',
        'ticket_summary': 'Ticket summary',
        'dismantle': False,
        'target_project_id': 'rtc-vla-test',
        'ticket_created_by': 'nulltime@',
        'hosts': [
            u'101283388',
            u'101287675',
            u'101283408',
            u'101283432',
            u'101283448'
        ],
        'type': 'add_hosts',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_preorder_1():
    ticket = mock.Mock()
    ticket.summary = 'Ввод хостов из предзаказа в rtc-mtn от 13.05.2020'
    ticket.key = 'RUNTIMECLOUD-16334'
    ticket.tags = []
    ticket.description = '''
Тип работ:
забрать хосты из предзаказа

ID предзаказа:
8803

Wall-e проект:
rtc-mtn

Забрать все хосты из предзаказа:
Да

 <{Список ID серверов:

 }>

<{for_janitor
 Тип работ:
забрать хосты из предзаказа

ID предзаказа:
8803

Wall-e проект:
rtc-mtn

Забрать все хосты из предзаказа:
Да
 }>
'''.decode('utf-8')
    ticket.createdBy.login = u'sereglond'
    ticket.createdAt = u'2020-05-13T18:04:57.469+0000'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'preorder_id': '8803',
        'ticket_created_by': 'sereglond',
        'ticket_creation_date': '2020-05-13',
        'ticket_key': 'RUNTIMECLOUD-16334',
        'ticket_summary': 'Ввод хостов из предзаказа в rtc-mtn от 13.05.2020',
        'type': 'preorder_add_hosts',
        'target_project_id': 'rtc-mtn',
        'whole_preorder': True
    }


def test_parse_upgrade():
    ticket = mock.Mock()
    ticket.summary = 'Ticket summary'
    ticket.key = 'IDDQD-1'
    ticket.tags = []
    ticket.description = '''Апгрейд хостов от 09.07.2020

Тип работ:
апгрейд хостов

Список ID серверов:
101169012
101168733

Комментарий:
Тестилище!
<{for_janitor
 Тип работ:
апгрейд хостов

Список ID серверов:
101169012
101168733

Комментарий:
Тестилище!
}>
'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'comment': 'Тестилище!',
        'hosts': [
            '101169012',
            '101168733'
        ],
        'ticket_key': 'IDDQD-1',
        'ticket_created_by': 'nulltime@',
        'ticket_summary': 'Ticket summary',
        'type': 'upgrade_hosts',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_add_hosts():
    ticket = mock.Mock()
    ticket.summary = 'Ввод хостов  в rtc-yt-mtn от 13.11.2019'
    ticket.key = 'RUNTIMECLOUD-15143'
    ticket.tags = []
    ticket.description = '''Тип работ:
ввод хостов в wall-e проект

Wall-e проект:
rtc-yt-mtn-amd

Дата:
Нет ответа

Кто сдает хосты:
Аброскин Андрей (sereglond)

Комментарий:
Вводим обратно прошитые AMD

<{Список ID серверов:
102340888
102340893
}>

<{for_janitor
 Тип работ:
ввод хостов в wall-e проект

Wall-e проект:
rtc-yt-mtn-amd

Список ID серверов:
102340888
102340893
``

Кто сдает хосты:
Аброскин Андрей (sereglond)

Комментарий:
Вводим обратно прошитые AMD
 }>
'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'comment': 'Вводим обратно прошитые AMD',
        'hosts': [
            '102340888',
            '102340893',
        ],
        'responsible': 'sereglond',
        'target_project_id': 'rtc-yt-mtn-amd',
        'ticket_key': 'RUNTIMECLOUD-15143',
        'ticket_created_by': 'nulltime@',
        'ticket_summary': 'Ввод хостов  в rtc-yt-mtn от 13.11.2019',
        'type': 'add_hosts',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_add_hosts_multidc_processing():
    ticket = mock.Mock()
    ticket.summary = 'Ввод/перемещение хостов в yp-iss-[dc] от 28.09.2020'
    ticket.key = 'RUNTIMECLOUD-17273'
    ticket.tags = ['janitor_processed:parent', 'janitor_for_processing']
    ticket.description = '''Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
yp-iss-[dc]

Кто сдает хосты:
Богданов Евгений (evbogdanov)

<{Список ID серверов:
 900900254
900900195
900326094
900326220
522506
522521
522476
900336010
900316731
900316843
900316927
900316941
522243
522240
522239
299348
299405
900155728
397378
 }>

<{for_janitor
 Тип работ:
ввод/перемещение хостов в wall-e проект

Wall-e проект:
yp-iss-[dc]

Список ID серверов:
900900254
900900195
900326094
900326220
522506
522521
522476
900336010
900316731
900316843
900316927
900316941
522243
522240
522239
299348
299405
900155728
397378

Кто сдает хосты:
Богданов Евгений (evbogdanov)
 }>
'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'hosts': [
            '900900254',
            '900900195',
            '900326094',
            '900326220',
            '522506',
            '522521',
            '522476',
            '900336010',
            '900316731',
            '900316843',
            '900316927',
            '900316941',
            '522243',
            '522240',
            '522239',
            '299348',
            '299405',
            '900155728',
            '397378'
        ],
        'responsible': 'evbogdanov',
        'target_project_id': 'yp-iss-[dc]',
        'ticket_key': 'RUNTIMECLOUD-17273',
        'ticket_created_by': 'nulltime@',
        'ticket_summary': 'Ввод/перемещение хостов в yp-iss-[dc] от 28.09.2020',
        'type': 'multi_dc_parent_processing',
        'ticket_creation_date': '2020-01-01'
    }


def test_parse_rm_hosts():
    ticket = mock.Mock()
    ticket.summary = 'ВЫвод хостов из   от 28.09.2020'
    ticket.key = 'RUNTIMECLOUD-17273'
    ticket.tags = ['hosts-comp:fini', 'janitor_for_processing']
    ticket.description = '''
Тип работ:
вЫвод хостов из wall-e

Переместить серверы в проект ABC после вывода:
Регламентные операции

Демонтаж:
Нет

Кто сдает хосты:
Беркович Данил (nulltime)

Комментарий:
Testy test!

<{Список ID серверов:
 900900254
900900195
900326094
900326220
522506
522521
522476
900336010
900316731
900316843
900316927
900316941
522243
522240
522239
299348
299405
900155728
397378
 }>

<{for_janitor
 Тип работ:
вЫвод хостов из wall-e

Переместить серверы в проект ABC после вывода:
Регламентные операции

Демонтаж:
Нет

Список ID серверов:
900900254
900900195
900326094
900326220
522506
522521
522476
900336010
900316731
900316843
900316927
900316941
522243
522240
522239
299348
299405
900155728
397378

Кто сдает хосты:
Беркович Данил (nulltime)

Комментарий:
Testy test!
 }>
'''.decode('utf-8')
    ticket.createdBy.login = 'nulltime@'
    ticket.createdAt = '2020-01-01'
    parsed = common.parse_ticket(ticket)
    assert parsed == {
        'hosts': [
            '900900254',
            '900900195',
            '900326094',
            '900326220',
            '522506',
            '522521',
            '522476',
            '900336010',
            '900316731',
            '900316843',
            '900316927',
            '900316941',
            '522243',
            '522240',
            '522239',
            '299348',
            '299405',
            '900155728',
            '397378'
        ],
        'abc_service_name': 'Регламентные операции',
        'responsible': 'nulltime',
        'ticket_key': 'RUNTIMECLOUD-17273',
        'ticket_created_by': 'nulltime@',
        'comment': 'Testy test!',
        'dismantle': False,
        'ticket_summary': 'ВЫвод хостов из   от 28.09.2020',
        'type': 'rm_hosts',
        'ticket_creation_date': '2020-01-01'
    }
