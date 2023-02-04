import pytest

from unittest.mock import patch, MagicMock

from intranet.vconf.src.call.hydrator import ParticipantsHydrator


pytestmark = pytest.mark.django_db


def test_add_to_fetch():
    ph = ParticipantsHydrator(lang='ru')
    ph.add_to_fetch([
        {'type': 'person', 'id': 'user1'},
        {'type': 'room', 'id': '1234'},
        {'type': 'person', 'id': 'user2'},
    ])

    assert ph.persons_ids == ['user1', 'user2']
    assert ph.rooms_ids == ['1234']

    ph.add_to_fetch([
        {'type': 'person', 'id': 'user3'},
        {'type': 'room', 'id': '1235'},
    ])

    assert ph.persons_ids == ['user1', 'user2', 'user3']
    assert ph.rooms_ids == ['1234', '1235']


def test_fetch():
    ph = ParticipantsHydrator(lang='ru')

    person_mock = MagicMock(side_effect=lambda ids: {'result': [{'login': k} for k in ids]})
    room_mock = MagicMock(side_effect=lambda ids: {'result': [{'id': k} for k in ids]})
    with patch('intranet.vconf.src.call.hydrator.get_person_info', person_mock):
        with patch('intranet.vconf.src.call.hydrator.get_room_info', room_mock):
            ph.persons_ids = ['user1', 'user2']
            ph.rooms_ids = ['1234']
            ph.fetch()
            person_mock.assert_called_once_with(['user1', 'user2'])
            room_mock.assert_called_once_with(['1234'])
            assert ph.persons_data == {'user1': {'login': 'user1'}, 'user2': {'login': 'user2'}}
            assert ph.rooms_data == {'1234': {'id': '1234'}}
            assert ph.persons_ids == []
            assert ph.rooms_ids == []

            person_mock.reset_mock()
            room_mock.reset_mock()

            ph.persons_ids = ['user1', 'user3']
            ph.rooms_ids = ['1235']
            ph.fetch()
            person_mock.assert_called_once_with(['user1', 'user3'])
            room_mock.assert_called_once_with(['1235'])
            assert ph.persons_data == {'user1': {'login': 'user1'}, 'user2': {'login': 'user2'}, 'user3': {'login': 'user3'}}
            assert ph.rooms_data == {'1234': {'id': '1234'}, '1235': {'id': '1235'}}
            assert ph.persons_ids == []
            assert ph.rooms_ids == []

            person_mock.reset_mock()
            room_mock.reset_mock()

            ph.fetch()
            person_mock.assert_not_called()
            room_mock.assert_not_called()
            assert ph.persons_data == {'user1': {'login': 'user1'}, 'user2': {'login': 'user2'}, 'user3': {'login': 'user3'}}
            assert ph.rooms_data == {'1234': {'id': '1234'}, '1235': {'id': '1235'}}
            assert ph.persons_ids == []
            assert ph.rooms_ids == []
