import pytest
from unittest.mock import patch, MagicMock

from intranet.vconf.src.call.models import ConferenceCall, Participant
from intranet.vconf.src.call.views.api import _get_participants
from intranet.vconf.src.call.ether import HURAL_ETHER_ID
from intranet.vconf.tests.call.factories import ConferenceCallFactory, ParticipantFactory


pytestmark = pytest.mark.django_db


def test__get_participants():
    staff_resp = {
        'result': [
            {
                'phone': '1805',
                'id': 35,
                'equipment': {'video_conferencing': '1805'}
            },
            {
                'phone': '4892',
                'id': 33,
                'equipment': {'video_conferencing': ''}
            }
        ],
    }

    staff_mock = MagicMock(return_value=staff_resp)

    with patch('intranet.vconf.src.call.views.api.get_from_staff_api', staff_mock):
        res = _get_participants(['1805', '4892'])
        assert res == [
            {'type': 'room', 'id': 35, 'method': 'cisco'},
            {'type': 'room', 'id': 33, 'method': 'cisco'},
        ]
        staff_mock.assert_called_once_with(
            path='/rooms/',
            params={
                '_fields': ','.join(['id', 'phone', 'equipment.video_conferencing']),
                '_query': 'phone in ["1805","4892"] or equipment.video_conferencing in ["1805","4892"]'
            }
        )


def test_old_create(ya_client):
    response = ya_client.get('/api/create/')
    assert response.status_code == 400, response.content


def test_old_create_with_event_id(ya_client):
    call = ConferenceCallFactory(event_id=123)
    response = ya_client.get('/api/create/', data={'event_id': call.event_id})
    assert response.status_code == 200
    assert response.json()['data']['id'] == call.name


def test_stream_list(ya_client):
    # not a stream
    call1 = ConferenceCallFactory(stream=False, state=ConferenceCall.STATES.active)
    ParticipantFactory(conf_call=call1, state=Participant.STATES.active)
    # ended stream
    call2 = ConferenceCallFactory(stream=True, state=ConferenceCall.STATES.ended)
    ParticipantFactory(conf_call=call2, state=Participant.STATES.ended)
    # active stream without participants
    ConferenceCallFactory(stream=True, state=ConferenceCall.STATES.active)
    # active stream without active participants
    call4 = ConferenceCallFactory(stream=True, state=ConferenceCall.STATES.active)
    ParticipantFactory(conf_call=call4, state=Participant.STATES.ended)
    # active stream with active participant
    call5 = ConferenceCallFactory(
        stream=True,
        state=ConferenceCall.STATES.active,
        ether_back_id=HURAL_ETHER_ID.back_id,
        ether_front_id=HURAL_ETHER_ID.front_id,
    )
    ParticipantFactory(conf_call=call5, state=Participant.STATES.active)

    response = ya_client.get('/api/streams/', data={'state': 'active'})
    assert response.status_code == 200
    data = response.json()
    assert len(data['response_text']) == 1
    call_data = data['response_text'][0]
    assert call_data['ether_back_id'] == HURAL_ETHER_ID.back_id
    assert call_data['ether_front_id'] == HURAL_ETHER_ID.front_id
    assert call_data['id'] == call5.conf_cms_id


def test_active_list(ya_client):
    calls = [
        # звонок с двумя активными участниками
        ConferenceCallFactory(
            state=Participant.STATES.active,
            event_id=101,
            event_external_id='xxxyandex.ru',
        ),
        # закончившийся звонок с неативным участником
        ConferenceCallFactory(state=Participant.STATES.ended, event_id=102),
        # активный звонок без участников
        ConferenceCallFactory(state=Participant.STATES.active, event_id=103),
    ]

    ParticipantFactory(conf_call=calls[0], state=ConferenceCall.STATES.active)
    ParticipantFactory(conf_call=calls[0], state=ConferenceCall.STATES.active)
    ParticipantFactory(conf_call=calls[0], state=ConferenceCall.STATES.ended)
    ParticipantFactory(conf_call=calls[1], state=ConferenceCall.STATES.ended)

    response = ya_client.get('/api/active_calls/')
    assert response.status_code == 200
    data = response.json()

    expected_result = [
        {
            'event_id': 101,
            'event_external_id': 'xxxyandex.ru',
            'active_participants_count': 2,
        },
        {
            'event_id': 103,
            'event_external_id': None,
            'active_participants_count': 0,
        },
    ]

    assert data['response_text'] == expected_result
