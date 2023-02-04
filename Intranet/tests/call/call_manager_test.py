import types
from datetime import timedelta
from unittest.mock import patch, MagicMock, PropertyMock
import pytest

from django.utils import timezone
from django.conf import settings

from intranet.vconf.src.call.constants import CallFilter, STREAM_LANGUAGES
from intranet.vconf.src.call.models import ConferenceCall, Record, Participant
from intranet.vconf.src.call.manager import CallManager
from intranet.vconf.src.call.participant_ctl import PersonParticipant
from intranet.vconf.src.call.ether import HURAL_ETHER_ID

from intranet.vconf.tests.call.factories import (
    CallTemplateFactory,
    ConferenceCallFactory,
    ParticipantFactory,
    create_user,
    create_admin,
)
from intranet.vconf.tests.call.mock import FakeCMSApi, FakeHydrator, get_fake_event


pytestmark = pytest.mark.django_db


@pytest.fixture
def robot():
    robot = create_user(username=settings.VCONF_ROBOT_LOGIN)
    return robot


def test_find_outdated():
    now = timezone.now()
    a = ConferenceCallFactory(
        state=ConferenceCall.STATES.broken,
    )
    ConferenceCallFactory(
        state=ConferenceCall.STATES.active,
        stop_time=now + timedelta(minutes=30)
    )
    c = ConferenceCallFactory(
        state=ConferenceCall.STATES.active,
        stop_time=now - timedelta(minutes=30)
    )
    ConferenceCallFactory(
        state=ConferenceCall.STATES.ended,
        stop_time=now - timedelta(minutes=30)
    )
    e = ConferenceCallFactory(
        state=ConferenceCall.STATES.ended_in_cms,
    )

    res = CallManager.find_outdated()
    res_set = {r.obj.conf_cms_id for r in res}
    exp_set = {r.conf_cms_id for r in [a, c, e]}

    assert res_set == exp_set


def test_as_dict_w_hydrator():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    pt1 = ParticipantFactory(conf_call=call)
    ParticipantFactory(conf_call=call)

    invite_link_mock = PropertyMock(return_value='link')
    with patch('intranet.vconf.src.call.manager.CallManager.invite_link', invite_link_mock):
        manager = CallManager(obj=call, for_user=user)

        hydrator = MagicMock(**{'hydrate.side_effect': lambda pt: PersonParticipant(pt)})
        result = manager.as_dict(hydrator=hydrator, tz='Europe/Moscow')

        call_expected = {
            'id': call.conf_cms_id,
            'name': call.name,
            'state': call.state,
            'start_time': call.start_time,
            'duration': call.duration.seconds // 60,
            'stop_time': call.start_time + call.duration,
            'stream': call.stream,
            'record': call.record,
            'author_login': call.author_login,
            'invite_link': 'link',
        }

        invite_link_mock.assert_called_once()
        hydrator.add_to_fetch.assert_called_once()

        for k, v in call_expected.items():
            assert result[k] == v

        assert isinstance(result['participants'], types.GeneratorType)

        hydrator.hydrate.assert_not_called()

        res_participants = list(result['participants'])

        pt_expected = {
            'id': pt1.obj_id,
            'type': pt1.obj_type,
            'number': pt1.number,
            'camera': pt1.camera,
            'microphone': pt1.microphone,
            'camera_active': pt1.camera_active,
            'microphone_active': pt1.microphone_active,
            'method': pt1.method,
            'state': pt1.state,
        }
        assert res_participants[0] == pt_expected
        assert hydrator.hydrate.call_count == 2


@patch('intranet.vconf.src.call.manager.CMSApi', FakeCMSApi)
@patch('intranet.vconf.src.call.hydrator.ParticipantsHydrator', FakeHydrator)
def test_remove_participant_serialization():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    ParticipantFactory(conf_call=call, obj_type='person', obj_id=user.login)

    calls = CallManager.find_active(
        for_user=user,
        conf_cms_id=call.conf_cms_id,
    )
    call = calls[0]

    data = {
        'id': user.login,
        'type': 'person',
    }
    call.remove_participant(data, only_disconnect=True)
    serialized_data = call.as_dict()
    participant = next(serialized_data['participants'])
    assert participant['state'] == Participant.STATES.disconnected


def test_add_record():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    manager.add_record(data={'name': 'filename', 'node': 'vconf.yandex-team.ru'})

    qs = Record.objects.values()
    assert len(qs) == 1
    del qs[0]['id']
    assert qs[0] == {
        'conf_call_id': call.conf_cms_id,
        'file_name': 'filename',
        'node': 'vconf.yandex-team.ru',
        'is_deleted': False,
    }

    manager.add_record(data={'name': 'filename', 'node': 'vconf.yandex-team.ru'})

    qs = Record.objects.values()
    assert len(qs) == 1


def test_delete_record():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    manager.add_record(data={'name': 'filename', 'node': 'vconf.yandex-team.ru'})

    manager.delete_record()
    record = manager.get_record()

    assert not record


@pytest.mark.parametrize('is_admin, show_all, with_stream, is_external, expected_call_names', [
    (True, True, None, False, {'call', 'author', 'stream', 'author_stream'}),
    (True, True, True, False, {'stream', 'author_stream'}),
    (True, None, None, False, {'author', 'author_stream'}),
    (True, None, True, False, {'author_stream'}),
    (False, True, None, False, {'author', 'author_stream', 'stream'}),
    (False, True, True, False, {'stream', 'author_stream'}),
    (False, None, None, False, {'author', 'author_stream'}),
    (False, None, True, False, {'author_stream'}),
    (False, True, None, True, {'author', 'author_stream'}),
    (False, True, True, True, {'author_stream'}),
    (False, None, None, True, {'author', 'author_stream'}),
    (False, None, True, True, {'author_stream'}),
])
def test_find_calls(is_admin, show_all, with_stream, is_external, expected_call_names):
    create_user_func = create_admin if is_admin else create_user
    user = create_user_func(username='user', is_external=is_external)

    ConferenceCallFactory(name='call', stream=False)
    ConferenceCallFactory(name='author', stream=False, author_login=user.login)
    ConferenceCallFactory(name='stream', stream=True)
    ConferenceCallFactory(name='author_stream', author_login=user.login, stream=True)

    calls = CallManager.find_calls(
        for_user=user,
        call_filter=CallFilter(
            show_all=show_all,
            with_stream=with_stream,
            limit=20,
            page=1,
        ),
    )
    assert {c.obj.name for c in calls} == expected_call_names


@pytest.mark.parametrize('is_admin, expected_call_names', [
    (True, {'call', 'author', 'stream', 'author_stream'}),
    (False, {'author', 'stream', 'author_stream'}),
])
def test_find_active_calls(is_admin, expected_call_names):
    create_user_func = create_admin if is_admin else create_user
    user = create_user_func(username='user')

    ConferenceCallFactory(name='call', stream=False)
    ConferenceCallFactory(name='ended', stream=False, state=ConferenceCall.STATES.ended)
    ConferenceCallFactory(name='author', stream=False, author_login=user.login)
    ConferenceCallFactory(name='stream', stream=True)
    ConferenceCallFactory(name='author_stream', author_login=user.login, stream=True)

    calls = CallManager.find_active(for_user=user)
    assert {c.obj.name for c in calls} == expected_call_names


def test_write_permission_without_for_user():
    call = ConferenceCallFactory()
    manager = CallManager(obj=call)
    assert manager.can_user_write() is False


def test_write_permission_if_noone():
    manager = CallManager(
        obj=ConferenceCallFactory(),
        for_user=create_user(username='user'),
    )
    assert manager.can_user_write() is False


def test_write_permission_if_secret():
    user = create_user(username='user', secret='123')
    manager = CallManager(
        obj=ConferenceCallFactory(secret='123'),
        for_user=user,
    )
    assert manager.can_user_write() is True


def test_write_permission_if_bad_secret():
    user = create_user(username='user', secret='321')
    manager = CallManager(
        obj=ConferenceCallFactory(secret='123'),
        for_user=user,
    )
    assert manager.can_user_write() is False


def test_write_permission_if_author():
    user = create_user(username='user')
    manager = CallManager(
        obj=ConferenceCallFactory(author_login=user.login),
        for_user=user,
    )
    assert manager.can_user_write() is True


def test_write_permission_if_participant():
    user = create_user(username='user')
    call = ConferenceCallFactory()
    ParticipantFactory(conf_call=call, obj_type='person', obj_id=user.login)
    manager = CallManager(obj=call, for_user=user)
    assert manager.can_user_write() is True


def test_write_permission_if_admin():
    manager = CallManager(
        obj=ConferenceCallFactory(),
        for_user=create_admin(username='user'),
    )
    assert manager.can_user_write() is True


@pytest.mark.parametrize('attendee,can_write', [
    ('user', True),
    ('user2', False),
])
def test_write_persmission_if_user_is_event_attendee(attendee, can_write):
    user = create_user(username='user')
    event_id = 1
    manager = CallManager(
        obj=ConferenceCallFactory(event_id=event_id),
        for_user=user,
        event=get_fake_event(event_id=1, login=attendee),
    )
    assert manager.can_user_write() is can_write


@pytest.mark.parametrize('is_external, is_ip_external, can_download', [
    (False, False, True),
    (False, True, False),
    (True, False, True),
    (True, True, False),
])
def test_download_record_permission(is_external, is_ip_external, can_download):
    manager = CallManager(
        obj=ConferenceCallFactory(author_login='user'),
        for_user=create_user(username='user', is_external=is_external, is_ip_external=is_ip_external)
    )
    assert manager.can_user_download_record() == can_download


def test_permission_decorator():
    manager = CallManager(
        obj=ConferenceCallFactory(),
        for_user=create_user(username='user'),
    )
    with pytest.raises(manager.PermissionDenied):
        manager.update_duration(120)


def test_visibility_if_doesnt_have_permission():
    user = create_user(username='user')
    call = ConferenceCallFactory()
    manager = CallManager(obj=call, for_user=user)
    data = manager.as_dict()
    assert 'secret' not in data
    assert 'invite_link' not in data


def test_visibility_if_has_permission():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    data = manager.as_dict()
    assert 'secret' in data
    assert 'invite_link' in data


@patch('intranet.vconf.src.call.manager.CMSApi.get_free_owner', lambda: 'robot-cms-meeting00@cms.yandex-team.ru')
def test_create_space():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    space_create_mock = MagicMock(**{'space_create.return_value': MagicMock(
        headers={'Location': 'qwertyuiopasdfghjkl'},
        status_code=200,
    )})
    manager.cms = space_create_mock
    manager._create_space()
    space_create_mock.space_create.assert_called_once_with(**dict(
        call_id=call.meeting_id,
        uri=call.uri,
        name=call.name,
        secret=call.secret,
        stream=call.stream,
        record=call.record,
        stream_id=call.uri,
        owner='robot-cms-meeting00@cms.yandex-team.ru',
    ))
    assert call.conf_cms_id == 'kl'


def test_create_chat():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    create_chat_mock = MagicMock(return_value={'invite_hash': 'invite_hash'})
    with patch('intranet.vconf.src.call.manager.MessengerAPI.create_chat', create_chat_mock):
        manager._create_chat()
        create_chat_mock.assert_called_once_with(**dict(
            name=call.name,
            description=call.name,
            admin_logins=[call.author_login],
            member_logins=[call.author_login],
            chat_id=call.uri,
            is_hural=False,
        ))
        assert call.chat
        assert call.chat_invite_hash == 'invite_hash'


def test_start_stream():
    user = create_user(username='user')
    call = ConferenceCallFactory(author_login=user.login)
    manager = CallManager(obj=call, for_user=user)
    get_ether_id_mock = MagicMock(return_value=HURAL_ETHER_ID)
    with patch('intranet.vconf.src.call.manager.get_ether_id', get_ether_id_mock):
        manager._init_stream()
        get_ether_id_mock.assert_called_once_with(**dict(call=call))
        assert call.ether_back_id == HURAL_ETHER_ID.back_id
        assert call.ether_front_id == HURAL_ETHER_ID.front_id


def test_create_related_calls(robot):
    master_template = CallTemplateFactory(name='master')

    CallTemplateFactory(name='first_child', master_template=master_template)
    CallTemplateFactory(name='second_child', master_template=master_template)
    CallTemplateFactory(name='not_child')

    call = ConferenceCallFactory(template=master_template)
    manager = CallManager(call)

    mocked_create = MagicMock()

    with patch('intranet.vconf.src.call.manager.CallManager.create', mocked_create):
        manager._create_related_calls()
        assert mocked_create.call_count == 2


def test_as_detail_dict_for_multilanguage_templates():
    user = create_user(username='user')

    call_ru = ConferenceCallFactory(
        lang=STREAM_LANGUAGES.ru,
        stream=True,
    )
    call_en = ConferenceCallFactory(
        master_call=call_ru,
        lang=STREAM_LANGUAGES.en,
        stream=True,
    )

    manager_ru = CallManager(call_ru, for_user=user)
    call_ru_dict = manager_ru.as_detail_dict()
    manager_en = CallManager(call_en, for_user=user)
    call_en_dict = manager_en.as_detail_dict()

    assert call_ru_dict['lang'] == 'ru'
    assert call_ru_dict['translated_streams'] == {
        'ru': call_ru.conf_cms_id,
        'en': call_en.conf_cms_id,
    }

    assert call_en_dict['lang'] == 'en'
    assert call_en_dict['translated_streams'] == {
        'ru': call_ru.conf_cms_id,
        'en': call_en.conf_cms_id,
    }
