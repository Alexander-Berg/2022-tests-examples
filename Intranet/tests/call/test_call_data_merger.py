import pytest
from unittest.mock import patch, MagicMock
from datetime import timedelta

from intranet.vconf.src.call.merger import CallDataMerger, ObjectDoesNotExist, EventConflict

from intranet.vconf.tests.call.factories import (
    CallTemplateFactory,
    ParticipantTemplateFactory,
    create_user,
)

from intranet.vconf.tests.call.mock import get_event_info_mock


pytestmark = pytest.mark.django_db


person_mock = MagicMock(return_value={
    'result': [
        {
            'login': 'login',
            'name': {
                'first': {
                    'ru': 'First',
                },
                'last': {
                    'ru': 'Last',
                },
            },
            'work_phone': 123,
            'environment': {
                'timezone': 'Europe/Moscow',
            },
            'phones': [{
                'kind': 'common',
                'protocol': 'all',
                'number': '123',
            }]
        }
    ]
})


room_mock = MagicMock(return_value={
    'result': [
        {
            'id': 1,
            'name': {
                'exchange': 'slug1',
            },
        },
        {
            'id': 2,
            'name': {
                'exchange': 'slug2',
            }
        },
        {
            'id': 3,
            'name': {
                'exchange': 'slug3',
            }
        }
    ]
})


def test_call_data_merger_without_event_and_template():
    data_merger = CallDataMerger(user=create_user(username='user'))

    data = data_merger.data['call']
    data['participants'] = list(data['participants'])

    assert data == {
        'event_id': None,
        'template_id': None,
        'name': '',
        'duration': 60,
        'participants': [],
        'record': False,
        'stream': False,
    }


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info_by_slugs', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_event():
    event_id = 1
    data_merger = CallDataMerger(
        user=create_user(username='user'),
        event_id=event_id,
    )
    data = data_merger.data['call']
    participants = sorted((p['id'], p['type']) for p in data.pop('participants'))

    assert data == {
        'event_id': event_id,
        'template_id': None,
        'name': 'Event',
        'duration': 240,
        'record': False,
        'stream': False,
    }
    assert participants == [('1', 'room'), ('2', 'room')]


def _create_template():
    template = CallTemplateFactory(
        duration=timedelta(minutes=120),
        event_id=1,
        event_external_id='abcd1',
        record=True,
        stream=True,
        name='test',
        owners=['login'],
    )
    ParticipantTemplateFactory(
        call_template=template,
        obj_id=2,
        obj_type='room'
    )
    ParticipantTemplateFactory(
        call_template=template,
        obj_id=3,
        obj_type='room',
    )
    ParticipantTemplateFactory(
        call_template=template,
        obj_id='login',
        obj_type='person',
    )
    return template


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_template():
    template = _create_template()
    data_merger = CallDataMerger(
        user=create_user(username='login'),
        template_id=template.id,
    )
    data = data_merger.data['call']
    participants = sorted((p['id'], p['type']) for p in data.pop('participants'))

    assert data == {
        'event_id': template.event_id,
        'template_id': template.id,
        'name': 'test',
        'duration': 120,
        'record': True,
        'stream': True,
    }
    assert participants == [('2', 'room'), ('3', 'room'), ('login', 'person')]


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info', room_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info_by_slugs', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_wrong_template():
    template = _create_template()
    with pytest.raises(ObjectDoesNotExist):
        CallDataMerger(
            user=create_user(username='login2'),
            template_id=template.id,
        )


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info', room_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info_by_slugs', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_event_and_template():
    event_id = 1
    template = _create_template()
    data_merger = CallDataMerger(
        user=create_user(username='login'),
        event_id=event_id,
        template_id=template.id,
    )
    data = data_merger.data['call']
    participants = sorted((p['id'], p['type']) for p in data.pop('participants'))

    assert data == {
        'event_id': event_id,
        'template_id': template.id,
        'name': 'Event',
        'duration': 240,
        'record': True,
        'stream': True,
    }
    assert participants == [('1', 'room'), ('2', 'room'), ('3', 'room'), ('login', 'person')]


@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_conflict():
    event_id = 2
    template = _create_template()
    with pytest.raises(EventConflict):
        CallDataMerger(
            user=create_user(username='login'),
            event_id=event_id,
            template_id=template.id,
        )


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info_by_slugs', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_event_and_one_template():
    event_id = 1
    template = _create_template()
    data_merger = CallDataMerger(
        user=create_user(username='login'),
        event_id=event_id,
    )
    data = data_merger.data
    assert data['call']['template_id'] == template.id
    assert data['template'] is not None


@patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock)
@patch('intranet.vconf.src.call.hydrator.get_room_info_by_slugs', room_mock)
@patch('intranet.vconf.src.call.merger.get_event_info', get_event_info_mock)
def test_call_data_merger_with_event_and_two_templates():
    event_id = 1
    _create_template()
    _create_template()
    data_merger = CallDataMerger(
        user=create_user(username='login'),
        event_id=event_id,
    )
    data = data_merger.data
    assert data['call']['template_id'] is None
    assert data['template'] is None
