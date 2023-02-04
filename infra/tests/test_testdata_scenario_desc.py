# -*- coding: utf-8 -*-


from infra.rtc.janitor.constants import PROG_NAME, PROG_VER


def create_scenario_dsc(
    id,
    type='power_off',
    status='created',
    janitor_owned=True,
    issuer='testuser@',
    hosts=[100404032],
    name=None,
    ts=1585580302,
    labels={},
    target_project_id='rtc-mtn',
    ticket_key=None
):

    data = {
        'creation_time': ts,
        'hosts': [{'inv': i, 'status': 'queue', 'timestamp': ts} for i in hosts],
        'issuer': issuer,
        'labels': {},
        'name': '{}-{}'.format(id, type),
        'scenario_id': int(id),
        'scenario_type': type,
        'script_args': {},
        'status': status,
        'ticket_key': 'TICKET-{}'.format(id)
    }

    if type == 'switch-to-maintenance':
        data['script_args'] = {
            'schedule_type': 'all',
            'responsible': issuer,
        }

    elif type == 'janitor_power_off':
        data.update({
            'name': 'POWEROFF-{} poweroff'.format(id),
            'ticket_key': 'POWEROFF-{}'.format(id),
            'scenario_type': 'switch-to-maintenance',
            'script_args': {
                'schedule_type': 'all',
                'responsible': issuer,
            },
            'labels': {
                'source': PROG_NAME,
                'source_ver': PROG_VER,
                'task_name': 'power_off',
                'ref_ticket_key': 'ITDC-2004',
                'ticket_created_by': issuer,
                'comment': 'Test',
            }
        })

    elif type == 'hosts-add':
        data['script_args'] = {
            'schedule_type': 'all',
            'target_project_id': target_project_id
        }

    elif type == 'janitor_add_hosts':
        t = 'HOSTADD-{}'.format(id)
        data.update({
            'name': 'Add {} hosts to RTC {} ({})'.format(len(hosts), target_project_id, t),
            'ticket_key': t,
            'scenario_type': 'hosts-add',
            'script_args': {
                'schedule_type': 'all',
                'target_project_id': target_project_id
            },
            'labels': {
                'source': PROG_NAME,
                'source_ver': PROG_VER,
                'task_name': 'add_hosts',
                'ticket_created_by': issuer,
                'responsible': issuer,
                'comment': 'Test',
            }
        })

    if labels:
        data['labels'].update(labels)
    if ticket_key:
        data['ticket_key'] = ticket_key
    if name:
        data['name'] = name

    return data


def test_scenario_desc_generator_janitor_add_hosts():
    assert create_scenario_dsc(id=10002, type='janitor_add_hosts') == {
        'creation_time': 1585580302,
        'hosts': [
            {'inv': 100404032,
             'status': 'queue',
             'timestamp': 1585580302}
        ],
        'issuer': 'testuser@',
        'labels': {
            'source': PROG_NAME,
            'source_ver': PROG_VER,
            'task_name': 'add_hosts',
            'ticket_created_by': 'testuser@',
            'responsible': 'testuser@',
            'comment': 'Test'
            },
        'name': 'Add 1 hosts to RTC rtc-mtn (HOSTADD-10002)',
        'scenario_id': 10002,
        'scenario_type': 'hosts-add',
        'script_args': {
            'schedule_type': 'all',
            'target_project_id': 'rtc-mtn'
        },
        'status': 'created',
        'ticket_key': 'HOSTADD-10002'}


def test_scenario_desc_generator_hosts_add():
    assert create_scenario_dsc(id=10002, type='hosts-add') == {
        'creation_time': 1585580302,
        'hosts': [
            {'inv': 100404032,
             'status': 'queue',
             'timestamp': 1585580302}
        ],
        'issuer': 'testuser@',
        'labels': {},
        'name': '10002-hosts-add',
        'scenario_id': 10002,
        'scenario_type': 'hosts-add',
        'script_args': {
            'schedule_type': 'all',
            'target_project_id': 'rtc-mtn'
        },
        'status': 'created',
        'ticket_key': 'TICKET-10002'}
