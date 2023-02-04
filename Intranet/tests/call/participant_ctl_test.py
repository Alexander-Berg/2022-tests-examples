
from unittest.mock import patch, MagicMock, PropertyMock

from intranet.vconf.src.call.participant_ctl import (
    ParticipantCtl,
    PersonParticipant,
    RoomParticipant,
    ArbitraryParticipant,
    ParticipantValidationError,
)
from intranet.vconf.src.call.models import Participant, CALL_METHODS
from intranet.vconf.src.call.manager import CallManager

from django.conf import settings

from intranet.vconf.tests.call.factories import ConferenceCallFactory, ParticipantFactory, create_user
from intranet.vconf.tests.call.mock import get_passcode_by_id_mock

import pytest

pytestmark = pytest.mark.django_db


def test_get_or_create_obj():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)

    data = {
        'id': 'person',
        'type': 'person',
        'number': '4444',
        'camera': True,
        'microphone': True,
        'method': 'cisco',
    }

    res = data.copy()
    res['obj_id'] = res.pop('id')
    res['obj_type'] = res.pop('type')

    ctl = ParticipantCtl(data)
    ctl.get_or_create_obj(manager.obj)
    assert isinstance(ctl.obj, Participant)
    obj = Participant.objects.get(id=ctl.obj.id)

    for k, v in res.items():
        assert getattr(obj, k) == v

    assert obj.state == Participant.STATES.disconnected

    ctl2 = ParticipantCtl(data)

    ctl2.get_or_create_obj(manager.obj)
    assert isinstance(ctl2.obj, Participant)
    obj2 = Participant.objects.get(id=ctl2.obj.id)

    for k, v in res.items():
        assert getattr(obj2, k) == v

    assert obj2.state == Participant.STATES.disconnected

    assert obj == obj2


def test_add_to_call():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)

    with patch('intranet.vconf.src.call.participant_ctl.ParticipantCtl.get_or_create_obj') as goco_mock:
        with patch('intranet.vconf.src.call.participant_ctl.ParticipantCtl._send_mail') as sm_mock:
            with patch('intranet.vconf.src.call.participant_ctl.ParticipantCtl._add_to_cms') as atc_mock:
                with patch('intranet.vconf.src.call.participant_ctl.ParticipantCtl._get_uri') as gu_mock:
                    ctl = ParticipantCtl({})
                    ctl.obj = ParticipantFactory(method=Participant.METHODS.email, conf_call=call)
                    ctl.add_to_call(manager)
                    goco_mock.assert_called_once()
                    sm_mock.assert_called_once()
                    atc_mock.assert_not_called()
                    gu_mock.assert_not_called()

                    goco_mock.reset_mock()
                    sm_mock.reset_mock()
                    atc_mock.reset_mock()
                    gu_mock.reset_mock()

                    ctl = ParticipantCtl({})
                    ctl.obj = ParticipantFactory(method=Participant.METHODS.cisco, conf_call=call)
                    ctl.add_to_call(manager)
                    goco_mock.assert_called_once()
                    sm_mock.assert_not_called()
                    atc_mock.assert_called_once()
                    gu_mock.assert_called_once()


def test_send_mail():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)

    with patch('intranet.vconf.src.call.participant_ctl.send_mail') as sm_mock:
        with patch('intranet.vconf.src.call.participant_ctl.ParticipantCtl._get_email', MagicMock(return_value='')) as gm_mock:
            with patch('intranet.vconf.src.call.participant_ctl.CallManager.invite_link', PropertyMock(return_value='')) as il_mock:
                ctl = ParticipantCtl({})
                ctl.obj = ParticipantFactory(method=Participant.METHODS.email, conf_call=call)
                ctl._send_mail(manager)

                sm_mock.assert_called_once_with(
                    *(
                        'Meeting invitation',
                        settings.EMAIL_TEXT.format(link=''),
                        'vconf@yandex-team.ru',
                        [''],
                    ),
                    **{'fail_silently': False},
                )
                gm_mock.assert_called_once()
                il_mock.assert_called_once()


def test_person_get_actions():
    ext_data = {
        'work_phone': '7777',
        'phones': [{'kind': 'common', 'protocol': 'voice'}]
    }
    res = PersonParticipant.get_actions(ext_data)
    assert res == [
        CALL_METHODS.cisco,
        CALL_METHODS.mobile,
        CALL_METHODS.email,
        CALL_METHODS.messenger_q,
    ]


def test_person_get_email():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ctl = PersonParticipant({})
    ctl.obj = ParticipantFactory(method=Participant.METHODS.email, conf_call=call)
    assert ctl._get_email() == '%s@yandex-team.ru' % ctl.obj.obj_id


def test_person_get_uri():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ctl = PersonParticipant({})

    ctl.obj = ParticipantFactory(method=Participant.METHODS.cisco, conf_call=call)
    assert ctl._get_uri() == '%s@yandex-team.ru' % ctl.obj.number

    ctl.obj = ParticipantFactory(method=Participant.METHODS.messenger_q, conf_call=call)
    assert ctl._get_uri() == '%s@q.yandex-team.ru' % ctl.obj.obj_id

    ctl.obj = ParticipantFactory(method=Participant.METHODS.mobile, conf_call=call)
    assert ctl._get_uri() == '55%s@yandex-team.ru' % ctl.obj.number


def test_room_get_uri():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ctl = RoomParticipant({})

    ctl.obj = ParticipantFactory(method=Participant.METHODS.cisco, conf_call=call)
    assert ctl._get_uri() == '%s@yandex-team.ru' % ctl.obj.number


def test_arbitrary_get_email():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ctl = ArbitraryParticipant({})
    ctl.obj = ParticipantFactory(method=Participant.METHODS.mobile, conf_call=call)
    assert ctl._get_email() == ctl.obj.obj_id


def test_arbitrary_get_uri():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ctl = ArbitraryParticipant({})

    ctl.obj = ParticipantFactory(method=Participant.METHODS.mobile, conf_call=call)
    assert ctl._get_uri() == ctl.obj.obj_id


@patch('intranet.vconf.src.call.participant_ctl.get_passcode_by_id', get_passcode_by_id_mock)
@pytest.mark.parametrize('_id,result', [
    ('https://yandex.zoom.us/j/1234567890', '1234567890@zoom'),
    ('https://yandex.zoom.us/j/7706330695?pwd=AsdJjjYyy', '7706330695.154827@zoom'),
    ('https://yandex.zoom.us/j/123', None),
    ('https://yandex.zoom.us/j/123abc', None),
    ('https://google.zoom.us/j/123', None),
    ('989001231212', None),
    ('email@email.com', None),
    ('sip:lala@y-t.ru', None),
])
def test_normalize_zoom_url(_id, result):
    assert ArbitraryParticipant.normalize_zoom_url(_id) == result


def test_get_normalized_id_and_actions_error():
    with pytest.raises(ParticipantValidationError):
        ArbitraryParticipant._get_normalized_id_and_actions('lalala')


@patch('intranet.vconf.src.call.participant_ctl.get_passcode_by_id', get_passcode_by_id_mock)
@pytest.mark.parametrize('_id,expected_normalized,expected_actions', [
    ('  89001231212', '+79001231212', [CALL_METHODS.mobile]),
    ('989001231212', '+79001231212', [CALL_METHODS.mobile]),
    ('9810419001002030', '+419001002030', [CALL_METHODS.mobile]),
    ('1234', '1234', [CALL_METHODS.mobile]),
    ('  1234@zOOmcrc.Com', '1234@zoomcrc.com', [CALL_METHODS.zoom]),
    (' https://yandex.zoom.us/j/634666720', '634666720@zoom', [CALL_METHODS.zoom]),
    (' https://yandex.zoom.us/j/7706330695?pwd=AsdJjjYyy', '7706330695.154827@zoom', [CALL_METHODS.zoom]),
    ('sip:lala@y-t.ru', 'sip:lala@y-t.ru', [CALL_METHODS.mobile]),
    ('email@email.com', 'email@email.com', [CALL_METHODS.email, CALL_METHODS.mobile]),
    ('12345@zmeu.us', '12345@zmeu.us', [CALL_METHODS.zoom]),
    ('  12345@zoom  ', '12345@zoom', [CALL_METHODS.zoom]),
])
def test_get_normalized_id_and_actions(_id, expected_normalized, expected_actions):
    normalized, actions = ArbitraryParticipant._get_normalized_id_and_actions(_id)
    assert normalized == expected_normalized
    assert actions == expected_actions


@pytest.mark.parametrize('first_name,last_name,result', [
    ('name', 'surname', 'name surname'),
    ('☆' * 50, '',  '☆' * 16),  # один такой символ занимает 3 байта в utf-8
    ('name' * 200, 'surname', ('name' * 200)[:50]),
    ('', '', 'some_login'),
    ('', 'surname', 'surname')
])
def test_build_label(first_name, last_name, result):
    data = {
        'name': {
            'first': {
                'en': first_name,
            },
            'last': {
                'en': last_name,
            }
        },
        'login': 'some_login',
    }
    label = PersonParticipant.build_label(data)
    assert label == result
