from infra.rtc.janitor.scenario import Scenario  # noqa
from infra.rtc.janitor import common  # noqa
from infra.rtc.janitor import constants
# import mock
# import yatest.common

scenario_params = {
    113: {
        u'scenario_type': u'noop',
        u'status': u'finished',
        u'ticket_key': u'RUNTIMECLOUD-14566',
        u'name': u'1568658372.96',
        u'labels': {
            u'comment': u'test',
            u'fsm_prev_stage': u'add_comment_for_duty_to_st',
            u'fsm_stages': u'',
            u'source': u'janitor',
            u'fsm_processed': u'yes',
            u'task_name': u'rm_hosts',
            u'source_ver': u'0.1',
            },
        u'creation_time': 1568658373,
        u'scenario_id': 113,
        u'hosts': [
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327335},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327334},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327337},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327339},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101326996},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101817191},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101326995},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101326992},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101326991},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101817119},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327341},
            {u'status': u'done', u'timestamp': 1574776476, u'inv': 101327342}
        ],
        u'script_args': {u'responsible': [], u'schedule_type': u'all'},
        u'issuer': u'antivabo@'
        },
    421: {
        u'scenario_type': u'wait',
        u'status': u'created',
        u'ticket_key': u'RUNTIMECLOUD-15544',
        u'name': u'1579868463.23',
        u'labels': {
            u'comment': u'https://st.yandex-team.ru/RX-1379 ',
            u'fsm_prev_stage': u'add_comment_for_duty_to_st',
            u'fsm_stages': u'',
            u'ticket_created_by': u'sereglond',
            u'source': u'janitor',
            u'fsm_processed': u'yes',
            u'task_name': u'add_hosts',
            u'source_ver': u'0.1',
            },
        u'creation_time': 1579868463,
        u'scenario_id': 421,
        u'hosts': [
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            {u'status': u'queue', u'timestamp': 1579868463},
            ],
        u'script_args': {u'target_project_id': u'rtc-mtn',
                         u'responsible': u'sereglond',
                         u'schedule_type': u'all'},
        u'issuer': u'robot-rtc-autoadmin@',
        },
    423: {
        u'scenario_type': u'switch-to-maintenance',
        u'status': u'finished',
        u'ticket_key': u'RUNTIMECLOUD-15558',
        u'name': u'RUNTIMECLOUD-15558 poweroff',
        u'labels': {
            u'comment': u'',
            u'ref_ticket_key': u'ITDC-225084',
            u'fsm_prev_stage': u'check_to_mnt_complete',
            u'ticket_created_by': u'dorofeevdv',
            u'source': u'janitor',
            u'fsm_stages': u'check_to_mnt_complete',
            u'task_name': u'power_off',
            u'source_ver': u'0.1',
            },
        u'creation_time': 1580112023,
        u'scenario_id': 423,
        u'hosts': [{
            u'status': u'done',
            u'timestamp': 1580122504,
            u'inv': 900702530,
            u'group': 0,
            }],
        u'script_args': {u'responsible': u'dorofeevdv',
                         u'schedule_type': u'all'},
        u'issuer': u'robot-rtc-autoadmin@',
        }
}


class Client():
    def iter_scenarios(self, **kwargs):
        return [s for s in scenario_params.itervalues()]

    def add_scenario(**kwargs):
        kwargs['scenario_id'] = 1000
        return kwargs


class Obj():
    walle_client = Client()


class Ctx():
    obj = Obj()


def test_load_scenario():
    s = Scenario(**scenario_params[113])
    assert s.scenario_id == 113


def test_load_all_scenario():
    client = Client()
    s = Scenario.load_all(client)
    assert len(list(s)) == 3


def addsc(**kwargs):
    kwargs['scenario_id'] = 1000
    return kwargs


# def test_created_save():
#     s = Scenario(**scenario_params[113])
#     assert s.status == 'created'


# def test_started_save():
#     s = Scenario(**scenario_params[113])
#     assert s.status == 'started'


def test_hosts_list():
    s = Scenario(**scenario_params[113])
    assert s.hosts_list == [
        101327335,
        101327334,
        101327337,
        101327339,
        101326996,
        101817191,
        101326995,
        101326992,
        101326991,
        101817119,
        101327341,
        101327342
    ]
    pass


def test_hosts_list_setter_noset():
    s = Scenario(**scenario_params[113])
    s.hosts_list = [101027234, 101027407, 101027415]
    assert s.hosts_list == [
        101327335,
        101327334,
        101327337,
        101327339,
        101326996,
        101817191,
        101326995,
        101326992,
        101326991,
        101817119,
        101327341,
        101327342
    ]


def test_hosts_list_setter_set():
    s = Scenario(**scenario_params[113])
    s.status = 'none'
    s.hosts_list = [101027234, 101027407, 101027415]
    ts = s.creation_time
    assert s.hosts_list == [101027234, 101027407, 101027415]
    assert s.hosts == [
        {u'inv': 101027234,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 101027407,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 101027415,
         u'status': u'none',
         u'timestamp': ts},
    ]


def test_create_scenario_add_hosts():
    s = Scenario.action(
        Ctx(),
        type='add_hosts',
        ticket_created_by='tester@',
        responsible='tester@',
        comment='Check_test',
        ticket_key='TEST-600',
        target_project_id='rtc',
        hosts=[100404032, 100404033, 100404034, 100404035]
    )
    ts = s.creation_time
    assert s.scenario_id is None
    assert s.labels == {
        'source': constants.PROG_NAME,
        'source_ver': constants.PROG_VER,
        'task_name': 'add_hosts',
        'ticket_created_by': 'tester@',
        'responsible': 'tester@',
        'comment': 'Check_test'
    }
    assert s.hosts == [
        {u'inv': 100404032,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404033,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404034,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404035,
         u'status': u'none',
         u'timestamp': ts},
    ]
    assert s.hosts_list == [100404032, 100404033, 100404034, 100404035]
    assert s.issuer == 'none'
    assert s.name == 'Add 4 hosts to RTC rtc (TEST-600)'
    assert s.scenario_type == 'hosts-transfer'
    assert s.script_args == {
        'target_project_id': 'rtc'
        }
    assert s.status == 'none'
    assert s.ticket_key == 'TEST-600'


def test_create_scenario_rm_hosts():
    s = Scenario.action(
        Ctx(),
        type='rm_hosts',
        ticket_created_by='tester@',
        responsible='tester@',
        comment='Check_test',
        ticket_key='TEST-600',
        dismantle='false',
        hosts=[100404032, 100404033, 100404034, 100404035]
    )
    ts = s.creation_time
    assert s.scenario_id is None
    assert s.labels == {
        'source': constants.PROG_NAME,
        'source_ver': constants.PROG_VER,
        'task_name': 'rm_hosts',
        'ticket_created_by': 'tester@',
        'responsible': 'tester@',
        'dismantle': 'false',
        'comment': 'Check_test',
    }
    assert s.hosts == [
        {u'inv': 100404032,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404033,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404034,
         u'status': u'none',
         u'timestamp': ts},
        {u'inv': 100404035,
         u'status': u'none',
         u'timestamp': ts},
    ]
    assert s.hosts_list == [100404032, 100404033, 100404034, 100404035]
    assert s.issuer == 'none'
    # assert s.name == 'Add 4 hosts to RTC rtc (TEST-600 from 2020-04-12 18:25:33)'
    assert s.scenario_type == 'hosts-transfer'
    assert s.script_args == {
        'delete': True
        }
    assert s.status == 'none'
    assert s.name == 'Remove 4 hosts from wall-e (TEST-600)'
    assert s.ticket_key == 'TEST-600'


def test_create_scenario_preorder_add_hosts():
    parsed_ticket = {
        'preorder_id': '8803',
        'ticket_created_by': 'sereglond',
        'ticket_creation_date': '2020-05-13',
        'ticket_key': 'RUNTIMECLOUD-16334',
        'ticket_summary': 'Ввод хостов из предзаказа в rtc-mtn от 13.05.2020',
        'type': 'preorder_add_hosts',
        'target_project_id': 'rtc-mtn',
        'whole_preorder': True
    }
    s = Scenario.action(
        Ctx(),
        **parsed_ticket
    )
    assert s.scenario_id is None
    assert s.labels == {
        'source': 'janitor',
        'source_ver': '0.1',
        'task_name': 'preorder_add_hosts',
        'ticket_created_by': 'sereglond',
    }
    assert s.scenario_type == 'preorder-add-hosts'
    assert s.ticket_key == 'RUNTIMECLOUD-16334'
    assert s.script_args == {
        'target_project_id': 'rtc-mtn',
        'preorder_id': '8803',
        'whole_preorder': True
    }


def test_scenario_list():
    s = Scenario.action(
        Ctx(),
        type='add_hosts',
        ticket_created_by='tester@',
        responsible='tester@',
        comment='Check_test',
        ticket_key='TEST-600',
        target_project_id='rtc',
        hosts=[100404032, 100404033, 100404034, 100404035]
    )
    assert s.fsm_stages == []
    assert s.fsm_curr_stage == ''

    s.labels['fsm_stages'] = 'three,two,one'
    assert s.fsm_stages == ['three', 'two', 'one']
    assert s.fsm_curr_stage == 'one'


def test_load_scenario_live():
    s = Scenario(**{
        u'action_timestamp': 1587389530,
        u'creation_time': 1587389530,
        u'hosts': [
            {u'group': 0,
             u'inv': 102912715,
             u'status': u'processing',
             u'timestamp': 1587479058},
            {u'group': 0,
             u'inv': 102912711,
             u'status': u'processing',
             u'timestamp': 1587479057},
            {u'group': 0,
             u'inv': 102912712,
             u'status': u'processing',
             u'timestamp': 1587479057},
            {u'group': 0,
             u'inv': 102912708,
             u'status': u'processing',
             u'timestamp': 1587479059},
            {u'group': 0,
             u'inv': 102912706,
             u'status': u'processing',
             u'timestamp': 1587479058},
            {u'group': 0,
             u'inv': 102912713,
             u'status': u'processing',
             u'timestamp': 1587479058},
            {u'group': 0,
             u'inv': 102912710,
             u'status': u'processing',
             u'timestamp': 1587479058},
            {u'group': 0,
             u'inv': 102912709,
             u'status': u'processing',
             u'timestamp': 1587479059}
            ],
        u'issuer': u'staggot@',
        u'labels': {},
        u'name': u'test RUNTIMECLOUD-16186',
        u'scenario_id': 4233,
        u'scenario_type': u'hosts-add',
        u'script_args': {u'target_hardware_segment': u'ext.gateway'},
        u'status': u'started',
        u'ticket_key': u'RUNTIMECLOUD-16186'}
    )
    assert s.scenario_id == 4233
