import pytest
from json import dumps

from django.core.urlresolvers import reverse

from staff.map.edit.room import bind_room, unbind_room


@pytest.mark.django_db
def test_bind_room_works(rf, map_edit_room_test_data):
    room_id = map_edit_room_test_data.rooms[0].id
    url = reverse('map-bind_room', kwargs={'room_id': room_id})

    # Пересаживаем Ухуру за первый стол
    data = {
        'person': map_edit_room_test_data.staff['uhura'].login,
    }
    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    request.LANGUAGE_CODE = 'ru'

    response = bind_room(request, room_id)
    assert response.status_code == 200, response.status_code

    # Проверяем, что у неё появилась комната
    map_edit_room_test_data.staff['uhura'].refresh_from_db()

    assert map_edit_room_test_data.staff['uhura'].room_id == room_id

    # Привязываем Спока к его комнате. От стола он должен отвязаться
    data = {
        'person': map_edit_room_test_data.staff['spock'].login,
    }
    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    request.LANGUAGE_CODE = 'ru'

    response = bind_room(request, room_id)
    assert response.status_code == 200, response.status_code

    # Проверяем, что комната осталась прежней, а стол отвязался
    map_edit_room_test_data.staff['spock'].refresh_from_db()

    assert map_edit_room_test_data.staff['spock'].room.id == room_id
    assert map_edit_room_test_data.staff['spock'].table is None

    # Привязываем Кирка к другой комнате. От стола в старой комнате он должен отвязаться
    new_room_id = map_edit_room_test_data.rooms[1].id
    url = reverse('map-bind_room', kwargs={'room_id': new_room_id})
    data = {
        'person': map_edit_room_test_data.staff['kirk'].login,
    }
    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    request.LANGUAGE_CODE = 'ru'
    response = bind_room(request, new_room_id)
    assert response.status_code == 200, response.status_code

    # Проверяем, что изменилась комната и отвязался стол
    map_edit_room_test_data.staff['kirk'].refresh_from_db()

    assert map_edit_room_test_data.staff['kirk'].room.id == new_room_id
    assert map_edit_room_test_data.staff['kirk'].table is None


@pytest.mark.django_db
def test_unbind_room_works(rf, map_edit_room_test_data):
    url = reverse('map-unbind_room')

    # Отвяжем Спока от комнаты и убедимся, что от стола он тоже отвязался
    data = {
        'person': map_edit_room_test_data.staff['spock'].login,
    }

    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    request.LANGUAGE_CODE = 'ru'
    response = unbind_room(request)
    assert response.status_code == 200, response.status_code

    map_edit_room_test_data.staff['spock'].refresh_from_db()
    assert map_edit_room_test_data.staff['spock'].table is None
    assert map_edit_room_test_data.staff['spock'].room is None


@pytest.mark.django_db
def test_bind_room_doesnt_work(rf, map_edit_room_test_data):
    room_id = 100500
    url = reverse('map-bind_room', kwargs={'room_id': room_id})
    data = {
        'person': 'imanobody',
    }
    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    response = bind_room(request, room_id)
    assert response.status_code == 400, response.status_code


@pytest.mark.django_db
def test_unbind_room_doesnt_work(rf, map_edit_room_test_data):
    url = reverse('map-unbind_room')
    data = {
        'person': 'imanobody',
    }
    request = rf.post(url, data=dumps(data), content_type='application/json')
    request.user = map_edit_room_test_data.user
    response = unbind_room(request)
    assert response.status_code == 400, response.status_code
