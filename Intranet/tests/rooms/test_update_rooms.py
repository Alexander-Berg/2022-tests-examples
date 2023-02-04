import pytest
from unittest.mock import patch, Mock

from django.core.management import call_command

from intranet.vconf.src.rooms.models import Room


staff_response = {
    'links': {},
    'page': 1,
    'limit': 10000,
    'result': [
        {'id': 3969,
         'name': {'display': 'Кофеин', 'exchange': 'conf_kzn_caffeine'},
         'floor': {'office': {'city': {'country': {'code': 'ru'}},
                              'id': 178,
                              'timezone': 'Europe/Moscow'}}},
        {'id': 3968,
         'name': {'display': 'Ку5', 'exchange': 'conf_aur_2А13'},
         'floor': {'office': {'city': {'country': {'code': 'ru'}},
                              'id': 181,
                              'timezone': 'Europe/Moscow'}}},
        {'id': 3967,
         'name': {'display': 'Якшы', 'exchange': 'conf_ufa_Yakshy'},
         'floor': {'office': {'city': {'country': {'code': 'ru'}},
                              'id': 195,
                              'timezone': 'Europe/Skopje'}}}
    ],
    'total': 491,
    'pages': 1
}


@pytest.mark.django_db
def test_update_rooms():
    with patch('intranet.vconf.src.ext_api.staff.get_all_rooms', Mock(return_value=staff_response)):
        with patch('intranet.vconf.src.ext_api.calendar.get_events_for_offices', Mock(return_value={})):
            call_command('update_rooms')

    rooms = Room.objects.all().order_by('id')
    assert len(rooms) == 3
    for index, res in enumerate(staff_response['result']):
        assert rooms[index].name == res['name']['display']
        assert rooms[index].room_id == res['id']
        assert rooms[index].email == res['name']['exchange'] + '@yandex-team.ru'
        assert rooms[index].office_id == str(res['floor']['office']['id'])
        assert rooms[index].codec_ip is None
        assert rooms[index].timezone == res['floor']['office']['timezone']
        assert rooms[index].language == res['floor']['office']['city']['country']['code']
