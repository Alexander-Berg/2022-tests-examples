import pytest
from mock import patch, Mock

from intranet.vconf.src.rooms.management.commands.sync_codec_ips import (
    Command,
    find_new_and_updated_codec_ips,
    get_extended_room_data_from_cucm,
)
from intranet.vconf.src.rooms.models import Room

from intranet.vconf.tests.rooms.factories import RoomFactory

pytestmark = pytest.mark.django_db

patch_path = 'intranet.vconf.src.rooms.management.commands.sync_codec_ips.{to_patch}'


class MockedCUCMClient:
    def __init__(self, *args, **kwargs):
        pass

    def get_codec_number_to_ipv6_and_model_map(self, *args, **kwargs):
        return {
            '18001': {
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a2bd]',
                'model': 123,
            },
            '18002': {
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a3ce]',
                'model': 123,
            },
            '18003': {
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a4df]',
                'model': 123,
            },
        }


@pytest.fixture
def staff_response():
    return {
        'links': {},
        'page': 1,
        'limit': 10000,
        'result': [
            {
                'id': 3969,
                'equipment': {'video_conferencing': '18001'},
                'name': {'display': 'Кофеин', 'exchange': 'conf_kzn_caffeine'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 178,
                        'timezone': 'Europe/Moscow',
                    },
                },
            },
            {
                'id': 3968,
                'equipment': {'video_conferencing': '18002'},
                'name': {'display': 'Ку5', 'exchange': 'conf_aur_2А13'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 195,
                        'timezone': 'Europe/Moscow',
                    },
                },
            },
            {
                'id': 3967,
                'equipment': {'video_conferencing': '18003'},
                'name': {'display': 'Якшы', 'exchange': 'conf_ufa_Yakshy'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 195,
                        'timezone': 'Europe/Skopje',
                    },
                },
            },
            {
                'id': 3966,
                'equipment': {'video_conferencing': ''},
                'name': {'display': 'Тест', 'exchange': 'test_rr'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 178,
                        'timezone': 'Europe/Moscow',
                    },
                },
            },
        ],
        'total': 4,
        'pages': 1,
    }


@patch(patch_path.format(to_patch='yenv.type'), 'testing')
@patch(patch_path.format(to_patch='settings.TESTING_CODECS_ROOM_IDS'), [3969, 3968])
def test_command_in_testing(staff_response):
    patched_cucm_numbers_to_ips_map = patch(
        patch_path.format(to_patch='CUCMClient'),
        MockedCUCMClient,
    )
    patched_staff_response = patch(
        patch_path.format(to_patch='get_all_rooms'),
        Mock(return_value=staff_response)
    )

    RoomFactory(room_id='3969', codec_ip='[2a02:6b8:0:3203:0:0:0:0]')

    with patched_staff_response, patched_cucm_numbers_to_ips_map:

        Command().handle()

        assert Room.objects.count() == 2
        assert set(Room.objects.values_list('codec_ip', flat=True)) == {
            '[2a02:6b8:0:3203:277:8dff:fe90:a2bd]',
            '[2a02:6b8:0:3203:277:8dff:fe90:a3ce]',
        }


@patch(patch_path.format(to_patch='yenv.type'), 'production')
@patch(patch_path.format(to_patch='settings.PRODUCTION_BLACKLIST_CODEC_ROOM_IDS'), [3967])
@patch(patch_path.format(to_patch='settings.PRODUCTION_ALLOWED_OFFICE_IDS'), [178, 195])
def test_command_in_production(staff_response):
    patched_cucm_numbers_to_ips_map = patch(
        patch_path.format(to_patch='CUCMClient'),
        MockedCUCMClient,
    )
    patched_staff_response = patch(
        patch_path.format(to_patch='get_all_rooms'),
        Mock(return_value=staff_response),
    )

    RoomFactory(room_id='3969', codec_ip='[2a02:6b8:0:3203:0:0:0:0]')

    with patched_staff_response, patched_cucm_numbers_to_ips_map:
        Command().handle()

        assert Room.objects.count() == 2
        assert set(Room.objects.values_list('codec_ip', flat=True)) == {
            '[2a02:6b8:0:3203:277:8dff:fe90:a2bd]',
            '[2a02:6b8:0:3203:277:8dff:fe90:a3ce]',
        }


def test_find_new_codec_ips(staff_response):
    patched_cucm_numbers_to_ips_map = patch(
        patch_path.format(to_patch='CUCMClient'),
        MockedCUCMClient,
    )

    RoomFactory(room_id='3969', codec_ip='[2a02:6b8:0:3203:277:8dff:fe90:a2bd]'),
    room_to_update = RoomFactory(room_id='3967', codec_ip='[1.2.3.4.5.6.7.8]')

    with patched_cucm_numbers_to_ips_map:
        rooms = get_extended_room_data_from_cucm(staff_response['result'])
        new_rooms, rooms_to_update = find_new_and_updated_codec_ips(
            staff_room_id_to_room_map=Room.objects.in_bulk(field_name='room_id'),
            rooms=rooms,
        )

        assert len(new_rooms) == 1
        assert new_rooms[0].codec_ip == '[2a02:6b8:0:3203:277:8dff:fe90:a3ce]'
        assert new_rooms[0].email == 'conf_aur_2А13@yandex-team.ru'
        assert new_rooms[0].room_id == '3968'

        room_to_update.codec_ip = '[2a02:6b8:0:3203:277:8dff:fe90:a4df]'
        assert rooms_to_update == [room_to_update]


def test_add_codec_ips_and_models_to_rooms(staff_response):
    patched_cucm_client = patch(
        patch_path.format(to_patch='CUCMClient'),
        MockedCUCMClient,
    )

    with patched_cucm_client:
        expected_result = [
            {
                'id': 3969,
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a2bd]',
                'equipment': {'video_conferencing': '18001'},
                'name': {'display': 'Кофеин', 'exchange': 'conf_kzn_caffeine'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 178,
                        'timezone': 'Europe/Moscow',
                    },
                },
            },
            {
                'id': 3968,
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a3ce]',
                'equipment': {'video_conferencing': '18002'},
                'name': {'display': 'Ку5', 'exchange': 'conf_aur_2А13'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 195,
                        'timezone': 'Europe/Moscow',
                    },
                },
            },
            {
                'id': 3967,
                'ip': '[2a02:6b8:0:3203:277:8dff:fe90:a4df]',
                'equipment': {'video_conferencing': '18003'},
                'name': {'display': 'Якшы', 'exchange': 'conf_ufa_Yakshy'},
                'floor': {
                    'office': {
                        'city': {'country': {'code': 'ru'}},
                        'id': 195,
                        'timezone': 'Europe/Skopje',
                    },
                },
            },
        ]

        assert get_extended_room_data_from_cucm(staff_response['result']) == expected_result
