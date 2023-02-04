# -*- coding: utf-8 -*-


from infra.rtc.janitor.common import render_template


host_list_walle = [
    {
        'abc_prj': 'srertc',
        'dc': 'sas',
        'fqdn': 'sas5-0961.search.yandex.net',
        'hw_owners': ['sivanichkin', 'antivabo'],
        'inv': '100404032',
        'rack': '8',
        'service_owners': [],
        'switch': 'sas1-s788',
        'w_prj': 'rtc-mtn',
        'w_prj_owners': [
            'dldmitry',
            'efmv',
            'max7255',
            'moridin',
            'nekto0n',
            'olegsenin',
            'talion',
            '@svc_rtcsupport',
            '@svc_srertc_administration',
            '@svc_srertc_devops'],
        'w_prj_tags': [
            'rtc',
            'rtc.automation-enabled',
            'rtc.gencfg-reserve',
            'rtc.gpu-none',
            'rtc.reboot_segment-gencfg',
            'rtc.scheduler-gencfg',
            'rtc.stage-production',
            'rtc_network',
            'runtime',
            'search',
            'skynet_installed',
            'yasm_monitored']},
    {
        'abc_prj': 'srertc',
        'dc': 'sas',
        'fqdn': 'sas5-4419.search.yandex.net',
        'hw_owners': ['sivanichkin', 'antivabo'],
        'inv': '100404033',
        'rack': '24',
        'service_owners': [],
        'switch': 'sas1-s795',
        'w_prj': 'rtc-mtn',
        'w_prj_owners': [
            'dldmitry',
            'efmv',
            'max7255',
            'moridin',
            'nekto0n',
            'olegsenin',
            'talion',
            '@svc_rtcsupport',
            '@svc_srertc_administration',
            '@svc_srertc_devops'],
        'w_prj_tags': [
            'rtc',
            'rtc.automation-enabled',
            'rtc.gencfg-reserve',
            'rtc.gpu-none',
            'rtc.reboot_segment-gencfg',
            'rtc.scheduler-gencfg',
            'rtc.stage-production',
            'rtc_network',
            'runtime',
            'search',
            'skynet_installed',
            'yasm_monitored']},
    {
        'abc_prj': 'srertc',
        'dc': 'sas',
        'fqdn': 'sas5-4422.search.yandex.net',
        'hw_owners': ['sivanichkin', 'antivabo'],
        'inv': '100404034',
        'rack': '9',
        'service_owners': [],
        'switch': 'sas1-s789',
        'w_prj': 'rtc-mtn',
        'w_prj_owners': [
            'dldmitry',
            'efmv',
            'max7255',
            'moridin',
            'nekto0n',
            'olegsenin',
            'talion',
            '@svc_rtcsupport',
            '@svc_srertc_administration',
            '@svc_srertc_devops'],
        'w_prj_tags': [
            'rtc',
            'rtc.automation-enabled',
            'rtc.gencfg-reserve',
            'rtc.gpu-none',
            'rtc.reboot_segment-gencfg',
            'rtc.scheduler-gencfg',
            'rtc.stage-production',
            'rtc_network',
            'runtime',
            'search',
            'skynet_installed',
            'yasm_monitored']},
]
host_list_not_walle = [
    {
        "w_prj_owners": [],
        "hw_owners": [],
        "inv": "101283432",
        "abc_prj": "srertc",
        "dc": "vla",
        "w_prj_tags": [],
        "switch": None,
        "fqdn": "vla2-1734.search.yandex.net",
        "w_prj": None,
        "rack": "7A09",
        "service_owners": []
    },
    {
        "w_prj_owners": [],
        "hw_owners": [],
        "inv": "101283448",
        "abc_prj": "srertc",
        "dc": "vla",
        "w_prj_tags": [],
        "switch": None,
        "fqdn": "vla2-1950.search.yandex.net",
        "w_prj": None,
        "rack": "7A09",
        "service_owners": []
    },
    {
        "w_prj_owners": [],
        "hw_owners": [],
        "inv": "101287675",
        "abc_prj": "srertc",
        "dc": "vla",
        "w_prj_tags": [],
        "switch": None,
        "fqdn": "vla2-2011.search.yandex.net",
        "w_prj": None,
        "rack": "7A07",
        "service_owners": []
    }
]


def test_description_ITDC_TOO_MANY_HOSTS():
    test_max_hosts = 10
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_TOO_MANY_HOSTS',
        max_hosts_per_ticket=test_max_hosts,
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle
        )
    print message
    assert message == '''


======Too many hosts in ticket/Слишком много хостов в одном тикете
  10 max


++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITDC_PREREQUESTS():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_PREREQUESTS',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,

        walle_hosts=host_list_walle,
        walle_hosts_non_rtc=host_list_walle,
        not_walle_hosts=host_list_not_walle,
        hosts_racks=['7A07', '7A09', '8', '24', '9'],
        valid_hosts=[],
        )
    print message
    assert message == '''


==== Inappropriate hosts in ticket / Нарушение требований к хостам:Нарушение требований к хостам:

======Hosts are in non-RTC projects / Хосты находятся в не-RTC проектах:
  #|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#

======Hosts not in Wall-E / Хосты не в Wall-E:
  #|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
|#

======Hosts are located in different racks / Хосты в разных стойках:
  #|
|| **rack** | **inv** | **fqdn** | **walle_project** | **dc** | **switch** ||
||   //**Rack: 7A07**// ||
|| 101287675 | vla2-2011.search.yandex.net | None | vla | None||
||   //**Rack: 7A09**// ||
|| 101283432 | vla2-1734.search.yandex.net | None | vla | None||
|| 101283448 | vla2-1950.search.yandex.net | None | vla | None||
||   //**Rack: 8**// ||
|| 100404032 | sas5-0961.search.yandex.net | rtc-mtn | sas | sas1-s788||
||   //**Rack: 24**// ||
|| 100404033 | sas5-4419.search.yandex.net | rtc-mtn | sas | sas1-s795||
||   //**Rack: 9**// ||
|| 100404034 | sas5-4422.search.yandex.net | rtc-mtn | sas | sas1-s789||
|#

======No hosts available for processing / Нет хостов, пригодных к обработке


++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITDC_VALID_CONTINUE():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_VALID_CONTINUE',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,
        valid_hosts=host_list_walle,
        )
    print message
    assert message == '''


====== These valid hosts would be processed / Продолжается обработка только валидных хостов:
  #|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#


++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITDC_CLOSE():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_CLOSE',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,
        valid_hosts=host_list_walle,
        )
    print message
    assert message == '''


======== Request processing is terminated / Обработка прекращается


++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_POWEROFF_CONFLICT():
    message = render_template(
        'power_off_message.jinja',
        text_case='POWEROFF_CONFLICT',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,
        intersect_scenarios=[
            {
                'scenario': {'scenario_id': 13, 'name': 'JustForFail', 'ticket_key': 'IDKFA-1'},
                'hosts_info': host_list_not_walle
            },
            {
                'scenario': {'scenario_id': 777, 'name': 'Jeopary', 'ticket_key': 'JEOPARDY-1'},
                'hosts_info': host_list_walle
            },

        ],
        )
    print message
    assert message == '''



====== Hosts present in other shutdown scenarios were detected.
====== Обнаружены хосты, присутствующие в других сценариях на выключение

---
Request processing is terminated
Обработка прекращается

====== ((https://wall-e.yandex-team.ru/scenarios/13 #13: JustForFail)) ((https://st.yandex-team.ru/IDKFA-1 IDKFA-1))
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
|#

---
====== ((https://wall-e.yandex-team.ru/scenarios/777 #777: Jeopary)) ((https://st.yandex-team.ru/JEOPARDY-1 JEOPARDY-1))
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#

---

++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_HOST_SCENARIOS_CONFLICT():
    message = render_template(
        'power_off_message.jinja',
        text_case='HOST_SCENARIOS_CONFLICT',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,
        intersect_scenarios=[
            {
                'scenario': {'scenario_id': 13, 'name': 'JustForFail', 'ticket_key': 'IDKFA-1'},
                'hosts_info': host_list_not_walle
            },
            {
                'scenario': {'scenario_id': 777, 'name': 'Jeopary', 'ticket_key': 'JEOPARDY-1'},
                'hosts_info': host_list_walle
            },

        ],
        )
    print message
    assert message == '''



====== The following hosts are currently being handled by a different scenario.
====== Следующие хосты в данный момент обрабатываются другим сценарием.

---
We are waiting for the hosts to be released.
Ожидаем свобождения хостов.

====== ((https://wall-e.yandex-team.ru/scenarios/13 #13: JustForFail)) ((https://st.yandex-team.ru/IDKFA-1 IDKFA-1))
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
|#
---
====== ((https://wall-e.yandex-team.ru/scenarios/777 #777: Jeopary)) ((https://st.yandex-team.ru/JEOPARDY-1 JEOPARDY-1))
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
---

++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITCD_DENIED():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITCD_DENIED',
        ticket_key='IDDQD-1',
        hosts_list=host_list_not_walle + host_list_walle,
        responsible='nulltime',
        )
    print message
    assert message == '''



====== ITDC responsible has insufficient rights for operation.
====== У ответственных от ITDC нет прав на операцию

---
  Responsible/ответственный: nulltime@


++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||101283432 | vla2-1734.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101283448 | vla2-1950.search.yandex.net | None | srertc | vla | None| 7A09 ||
||101287675 | vla2-2011.search.yandex.net | None | srertc | vla | None| 7A07 ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITDC_CMS():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_CMS',
        ticket_key='IDDQD-1',
        hosts_list=host_list_walle,
        hosts_ids=[h['inv'] for h in host_list_walle],
    )
    print message
    assert message == '''



====== Switch to Maintenance requested.
====== Запрошен перевод в Maintenance.

---
Waiting for CMS approval.
Ждем разрешения CMS

Show host's status in
Просмотр статуса хостов в
((https://wall-e.yandex-team.ru/projects/hosts?fqdn=100404032%20100404033%20100404034&perPage=200 Wall-E WebUI))
<{Wall-E CLI
%%echo "100404032 100404033 100404034" | wall-e hosts list-only%%
}>

---

++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ITDC_OVER():
    message = render_template(
        'power_off_message.jinja',
        text_case='ITDC_OVER',
        ticket_key='IDDQD-1',
        hosts_list=host_list_walle,
        active_scenarios=10,
        queued_scenarios=100
    )
    print message
    assert message == '''


====== Exceeded the limit on the number of simultaneous scenarios. The script is queued for execution.
====== Превышен лимит на количество одновременных сценариев.

---
The script is queued for execution.
Сценарий поставлен в очередь на выполнение.

---
  In Progress/Выполняется: 10
  In Queue/В очереди: 100

---

++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_ALL_COMPLETE():
    message = render_template(
        'power_off_message.jinja',
        text_case='ALL_COMPLETE',
        ticket_key='IDDQD-1',
        hosts_list=host_list_walle,
    )
    print message
    assert message == '''



Hosts have been switched to Maintenance:
Xосты выведены в Maintenance

---

++Ticket/Тикет: ((https://st.yandex-team.ru/IDDQD-1 IDDQD-1)), <{passed hosts list/переданный список хостов:
#|
|| **inv** | **fqdn** | **walle_project** | **abc_service** | **dc** | **switch** | **rack** ||
||100404032 | sas5-0961.search.yandex.net | rtc-mtn | srertc | sas | sas1-s788| 8 ||
||100404033 | sas5-4419.search.yandex.net | rtc-mtn | srertc | sas | sas1-s795| 24 ||
||100404034 | sas5-4422.search.yandex.net | rtc-mtn | srertc | sas | sas1-s789| 9 ||
|#
}>++


//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''


def test_description_RTC_COMPLETE():
    message = render_template(
        'power_off_message.jinja',
        text_case='RTC_COMPLETE',
        ticket_key='IDDQD-1',
        hosts_list=host_list_walle,
        hosts_ids=[h['inv'] for h in host_list_walle],
    )
    print message
    assert message == '''



<{Включение хостов
%%echo "100404032 100404033 100404034" | wall-e hosts set-assigned --power-on --ignore-maintenance --reason IDDQD-1%%
}>



//This message is generated by robot//
//Автоматически сгенерированное сообщение//
'''
