import json

import pytest
from PIL import Image, PngImagePlugin
from django.core.urlresolvers import reverse

from staff.audit.models import Log
from staff.lib.testing import (
    StaffFactory,
    DeviceFactory,
    OfficeFactory,
    FloorFactory,
    RoomFactory,
    TableFactory,
)

from staff.map.controllers.table import MultipleTableError
from staff.map.edit.objects import EquipmentCtl, RoomCtl, ConferenceRoomCtl, CoworkingRoomCtl
from staff.map.edit.objects import TableCtl
from staff.map.models import Device, Table, TableReserve, TableBook, ROOM_TYPES
from staff.map.models.logs import Log as MapLog
from staff.map.models.logs import LogAction
from staff.person.models import Staff

Device._meta.managed = True


class MapTestEditEquipmentData:
    redrose = None
    first = None
    data = None
    user = None


@pytest.fixture
def map_edit_equipment_test_data():
    result = MapTestEditEquipmentData()
    result.redrose = OfficeFactory(name='Red Rose', city=None, intranet_status=1, have_map=True)
    result.first = FloorFactory(name='First', office=result.redrose,
                                intranet_status=1)

    result.data = {
        'coord_x': 10,
        'coord_y': 10,
        'name': 'unknown',
        'name_dns': '',
        'description': '',
        'floor': result.first,
        'type': 4,
        'angle': 0
    }

    result.user = StaffFactory(login='mr_plant')
    return result


@pytest.mark.django_db
def test_that_we_have_stationery_cabinet_in_equipment_form(superuser_client):
    url = reverse('map-add_equipment')
    response = superuser_client.get(url)
    objects = json.loads(response.content)

    assert any(
        equipment_type for equipment_type in objects['choices']['type']
        if equipment_type['value'] == 10
    )


@pytest.mark.django_db
def test_add_equipment_returns_device_type(superuser_client, map_edit_equipment_test_data):
    equipment = map_edit_equipment_test_data.data
    equipment['type'] = 9
    equipment['floor'] = equipment['floor'].id

    data = {
        'equipment': [equipment],
    }

    url = reverse('map-add_equipment')
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200
    assert result['is_security_device'] is True


@pytest.mark.django_db
def test_add_equipment_with_wrong_angle(superuser_client, map_edit_equipment_test_data):
    equipment = map_edit_equipment_test_data.data
    equipment['type'] = 9
    equipment['angle'] = 360
    equipment['floor'] = equipment['floor'].id

    data = {
        'equipment': [equipment],
    }

    url = reverse('map-add_equipment')
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200
    assert 'angle' in result['errors']['equipment']['0']


@pytest.mark.django_db
def test_edit_equipment_with_wrong_angle(superuser_client, map_edit_equipment_test_data):
    equipment = map_edit_equipment_test_data.data
    equipment['type'] = 9
    equipment['floor'] = equipment['floor'].id

    data = {
        'equipment': [equipment],
    }

    url = reverse('map-add_equipment')
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200

    id = result['id']
    url = reverse('map-edit_equipment', kwargs={'equipment_id': id})
    equipment['angle'] = 360
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200
    assert 'angle' in result['errors']['equipment']['0']


@pytest.mark.django_db
def test_edit_equipment_returns_device_type(superuser_client, map_edit_equipment_test_data):
    equipment = map_edit_equipment_test_data.data
    equipment['type'] = 11
    equipment['floor'] = equipment['floor'].id

    data = {
        'equipment': [equipment],
    }

    url = reverse('map-add_equipment')
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200

    id = result['id']
    url = reverse('map-edit_equipment', kwargs={'equipment_id': id})
    equipment['angle'] = 36
    response = superuser_client.post(url, json.dumps(data), content_type='application/json')
    result = json.loads(response.content)
    assert response.status_code == 200
    assert result['is_security_device'] is True


@pytest.mark.django_db
def test_add_equipment_initial_data(superuser_client, map_edit_equipment_test_data):
    url = reverse('map-add_equipment')
    response = superuser_client.get(url)
    result = json.loads(response.content)
    assert response.status_code == 200
    assert result['equipment'][0]['angle']['value'] == 0
    assert result['equipment'][0]['type']['value'] == 0


@pytest.mark.django_db
def test_create_equipment(map_edit_equipment_test_data):
    thing = EquipmentCtl.create(map_edit_equipment_test_data.data,
                                map_edit_equipment_test_data.user)
    assert thing.intranet_status == 1

    audit_log_count = Log.objects.filter(
        primary_key=thing.id,
        action='equipment_created'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Device',
        model_pk=thing.id,
        action=LogAction.CREATED.value,
        who=map_edit_equipment_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_edit_equipment(map_edit_equipment_test_data):
    device = EquipmentCtl(DeviceFactory(name='not_printer'), map_edit_equipment_test_data.user)
    # Изменение имени
    data = map_edit_equipment_test_data.data
    device.modify(data)
    assert device.thing.name == 'unknown'

    # Изменение типа
    data['type'] = 1
    device.modify(data)
    assert device.thing.type == 1

    # Изменение координат
    data['coord_x'] = 100
    data['coord_y'] = 100
    data['angle'] = 90
    device.modify(data)
    assert device.thing.coord_x == 100
    assert device.thing.coord_y == 100
    assert device.thing.angle == 90

    audit_log_count = Log.objects.filter(
        primary_key=device.thing.id,
        action='equipment_updated'
    ).count()
    assert audit_log_count == 3

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Device',
        model_pk=device.thing.id,
        action=LogAction.UPDATED.value,
        who=map_edit_equipment_test_data.user.user
    )
    assert map_log_qs.count() == 3


@pytest.mark.django_db
def test_delete_equipment(map_edit_equipment_test_data):
    device = EquipmentCtl(DeviceFactory(), map_edit_equipment_test_data.user)
    device.disable()
    assert device.thing.intranet_status == 0

    audit_log_count = Log.objects.filter(
        primary_key=device.thing.id,
        action='equipment_disabled'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Device',
        model_pk=device.thing.id,
        action=LogAction.DISABLED.value,
        who=map_edit_equipment_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_add_room(map_edit_room_test_data):
    room = RoomCtl.create(map_edit_room_test_data.data, map_edit_room_test_data.user)
    assert room.intranet_status == 1

    audit_log_count = Log.objects.filter(
        primary_key=room.id,
        action='room_created'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=room.id,
        action=LogAction.CREATED.value,
        who=map_edit_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_modify_room(map_edit_room_test_data):
    room_ctl = RoomCtl(map_edit_room_test_data.room, map_edit_room_test_data.user)
    # Меняем у комнаты вообще все
    room_ctl.update(map_edit_room_test_data.data)
    room_ctl.save()
    assert room_ctl.instance.coord_x == map_edit_room_test_data.data['coord_x']
    assert room_ctl.instance.coord_y == map_edit_room_test_data.data['coord_y']
    assert room_ctl.instance.name == map_edit_room_test_data.data['name']
    assert room_ctl.instance.additional == map_edit_room_test_data.data['additional']
    assert room_ctl.instance.floor_id == map_edit_room_test_data.data['floor'].id
    assert room_ctl.instance.room_type == map_edit_room_test_data.data['room_type']
    assert room_ctl.instance.num == map_edit_room_test_data.data['num']

    audit_log_count = Log.objects.filter(
        primary_key=map_edit_room_test_data.room.id,
        action='room_updated'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=map_edit_room_test_data.room.id,
        action=LogAction.UPDATED.value,
        who=map_edit_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_disable_room(map_edit_room_test_data):
    RoomCtl(map_edit_room_test_data.room, map_edit_room_test_data.user).disable()
    assert map_edit_room_test_data.room.intranet_status == 0

    audit_log_count = Log.objects.filter(
        primary_key=map_edit_room_test_data.room.id,
        action='room_deleted'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=map_edit_room_test_data.room.id,
        action=LogAction.DISABLED.value,
        who=map_edit_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


def build_room_geometry(data) -> str:
    return ",".join([f"{x},{2048 - y}" for x, y in data])


@pytest.mark.django_db
@pytest.mark.parametrize(
    'room_type, room_ctl_class',
    [
        (ROOM_TYPES.OFFICE, RoomCtl),
        (ROOM_TYPES.COWORKING, CoworkingRoomCtl),
    ]
)
def test_create_room_with_tables(room_type, room_ctl_class):
    office = OfficeFactory(name='Benua', city=None, intranet_status=1)
    floor = FloorFactory(name='Fifth', office=office, intranet_status=1)

    # геометрия комнаты
    room_geometry = (
        (0, 0),
        (200, 0),
        (200, 200),
        (0, 200),
        (0, 0),
    )

    # входит в геометрию комнаты
    table1 = TableFactory(
        num=123,
        floor=floor,
        coord_x=150,
        coord_y=150,
    )

    # не входит в геометрию комнаты
    table2 = TableFactory(
        num=124,
        floor=floor,
        coord_x=250,
        coord_y=250,
    )

    room = RoomFactory(
        floor=floor,
        room_type=room_type,
        num=12,
        coord_x=100,
        coord_y=100,
        name='Room1',
        additional='description',
        geometry=build_room_geometry(room_geometry),
    )

    user = StaffFactory(login='mr_sokoban')
    room_ctl_class(room, user).save()

    table1.refresh_from_db()
    table2.refresh_from_db()

    assert table1.room_id == room.id
    assert table2.room_id is None


@pytest.mark.django_db
@pytest.mark.parametrize(
    'room_type, room_ctl_class',
    [
        (ROOM_TYPES.OFFICE, RoomCtl),
        (ROOM_TYPES.COWORKING, CoworkingRoomCtl),
    ]
)
def test_edit_room_with_tables(room_type, room_ctl_class):
    office = OfficeFactory(name='Benua', city=None, intranet_status=1)
    floor = FloorFactory(name='Fifth', office=office, intranet_status=1)

    # геометрия комнаты
    room_geometry = (
        (0, 0),
        (200, 0),
        (200, 200),
        (0, 200),
        (0, 0),
    )

    # входит в геометрию комнаты
    table1 = TableFactory(
        num=123,
        floor=floor,
        coord_x=150,
        coord_y=150,
    )

    # не входит в геометрию комнаты
    table2 = TableFactory(
        num=124,
        floor=floor,
        coord_x=250,
        coord_y=250,
    )

    room = RoomFactory(
        floor=floor,
        room_type=room_type,
        num=12,
        coord_x=100,
        coord_y=100,
        name='Room1',
        additional='description',
        geometry=build_room_geometry(room_geometry),
    )

    user = StaffFactory(login='mr_sokoban')
    room_ctl = room_ctl_class(room, user)
    room_ctl.save()

    new_geometry = (
        (155, 155),
        (255, 155),
        (255, 255),
        (155, 255),
        (155, 155),
    )

    room_ctl.update({
        'geometry': build_room_geometry(new_geometry),
    })
    room_ctl.save()

    table1.refresh_from_db()
    table2.refresh_from_db()

    assert table1.room_id is None
    assert table2.room_id == room.pk

    # при занулении геометрии комнаты должны пропадать привязки комнаты к столам
    room_ctl.update({
        'geometry': '',
    })
    room_ctl.save()

    table1.refresh_from_db()
    table2.refresh_from_db()

    assert table1.room_id is None
    assert table2.room_id is None


@pytest.mark.django_db
@pytest.mark.parametrize(
    'room_type, room_ctl_class',
    [
        (ROOM_TYPES.OFFICE, RoomCtl),
        (ROOM_TYPES.COWORKING, CoworkingRoomCtl),
    ]
)
def test_edit_tables_in_room(room_type, room_ctl_class):
    office = OfficeFactory(name='Benua', city=None, intranet_status=1)
    floor = FloorFactory(name='Fifth', office=office, intranet_status=1)

    # геометрия комнаты
    room_geometry = (
        (0, 0),
        (200, 0),
        (200, 200),
        (0, 200),
        (0, 0),
    )
    room = RoomFactory(
        floor=floor,
        room_type=room_type,
        num=12,
        coord_x=100,
        coord_y=100,
        name='Room1',
        additional='description',
        geometry=build_room_geometry(room_geometry),
    )
    user = StaffFactory(login='mr_sokoban')
    room_ctl_class(room, user).save()
    table1_data = {
        'floor': floor,
        'coord_x': 150,
        'coord_y': 150,
    }
    table1 = TableCtl.create(table1_data, user)

    # не входит в геометрию комнаты
    table2_data = {
        'floor': floor,
        'coord_x': 250,
        'coord_y': 250,
    }
    table2 = TableCtl.create(table2_data, user)

    assert table2.room_id is None
    assert table1.room_id == room.pk


class MapEditConferenceRoomTestData:
    redrose = None
    first = None
    data = None
    user = None


@pytest.fixture
def map_edit_conference_room_test_data():
    result = MapEditConferenceRoomTestData()
    result.redrose = OfficeFactory(name='Red Rose', city=None, intranet_status=1)
    result.first = FloorFactory(name='First', office=result.redrose,
                                intranet_status=1)

    result.data = {
        'room_type': ROOM_TYPES.CONFERENCE,
        'coord_x': 10,
        'coord_y': 10,
        'name': 'name',
        'name_en': 'name_en',
        'floor': result.first,

        'name_alternative': 'alternative',
        'name_exchange': 'exchange_1234',
        'order': 1,
        'is_cabin': True,
        'is_avallable_for_reserve': True,
        'geometry': 'round',
        'iso_label_pos': '',
        'hide_floor_num': True,
        'iso_x': 10,
        'iso_y': 10,
        'label_pos': 'T',
        'marker_board': True,
        'cork_board': True,
        'phone': 12345,
        'video_conferencing': 12345,
        'voice_conferencing': True,
        'projector': 0,
        'panel': 0,
        'desk': True,
        'seats': 6,
        'capacity': 6,
        'additional': 'additional'
    }

    result.user = StaffFactory(login='mr_plant')
    return result


@pytest.mark.django_db
def test_add_conference_room(map_edit_conference_room_test_data):
    room = ConferenceRoomCtl.create(map_edit_conference_room_test_data.data,
                                    map_edit_conference_room_test_data.user)
    assert room.intranet_status == 1

    audit_log_count = Log.objects.filter(
        primary_key=room.id,
        action='conference_room_created'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=room.id,
        action=LogAction.CREATED.value,
        who=map_edit_conference_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_modify_conference_room(map_edit_conference_room_test_data):
    room_ctl = ConferenceRoomCtl(RoomFactory(room_type=ROOM_TYPES.CONFERENCE,
                                             name='cat_is_angry'),
                                 map_edit_conference_room_test_data.user)

    # меняем все у созданной переговорки
    room_ctl.update(map_edit_conference_room_test_data.data).save()

    # проверяем, что оно поменялось
    assert room_ctl.instance.name == map_edit_conference_room_test_data.data['name']
    assert room_ctl.instance.coord_x == map_edit_conference_room_test_data.data['coord_x']
    assert room_ctl.instance.coord_y == map_edit_conference_room_test_data.data['coord_y']
    assert room_ctl.instance.floor_id == map_edit_conference_room_test_data.data['floor'].id
    assert room_ctl.instance.name_alternative == map_edit_conference_room_test_data.data[
        'name_alternative']
    assert room_ctl.instance.name_exchange == map_edit_conference_room_test_data.data[
        'name_exchange']
    assert room_ctl.instance.order == map_edit_conference_room_test_data.data['order']
    assert room_ctl.instance.is_cabin == map_edit_conference_room_test_data.data['is_cabin']
    assert room_ctl.instance.is_avallable_for_reserve == map_edit_conference_room_test_data.data[
        'is_avallable_for_reserve']
    assert room_ctl.instance.geometry == map_edit_conference_room_test_data.data['geometry']
    assert room_ctl.instance.iso_label_pos == map_edit_conference_room_test_data.data[
        'iso_label_pos']
    assert room_ctl.instance.hide_floor_num == map_edit_conference_room_test_data.data[
        'hide_floor_num']
    assert room_ctl.instance.iso_x == map_edit_conference_room_test_data.data['iso_x']
    assert room_ctl.instance.iso_y == map_edit_conference_room_test_data.data['iso_y']
    assert room_ctl.instance.label_pos == map_edit_conference_room_test_data.data['label_pos']
    assert room_ctl.instance.marker_board == map_edit_conference_room_test_data.data['marker_board']
    assert room_ctl.instance.cork_board == map_edit_conference_room_test_data.data['cork_board']
    assert room_ctl.instance.phone == map_edit_conference_room_test_data.data['phone']
    assert room_ctl.instance.video_conferencing == map_edit_conference_room_test_data.data[
        'video_conferencing']
    assert room_ctl.instance.voice_conferencing == map_edit_conference_room_test_data.data[
        'voice_conferencing']
    assert room_ctl.instance.projector == map_edit_conference_room_test_data.data['projector']
    assert room_ctl.instance.panel == map_edit_conference_room_test_data.data['panel']
    assert room_ctl.instance.desk == map_edit_conference_room_test_data.data['desk']
    assert room_ctl.instance.seats == map_edit_conference_room_test_data.data['seats']
    assert room_ctl.instance.capacity == map_edit_conference_room_test_data.data['capacity']
    assert room_ctl.instance.additional == map_edit_conference_room_test_data.data['additional']

    audit_log_count = Log.objects.filter(
        primary_key=room_ctl.instance.id,
        action='conference_room_updated'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=room_ctl.instance.id,
        action=LogAction.UPDATED.value,
        who=map_edit_conference_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_disable_conference_room(map_edit_conference_room_test_data):
    room_ctl = ConferenceRoomCtl(RoomFactory(room_type=ROOM_TYPES.CONFERENCE),
                                 map_edit_conference_room_test_data.user)
    room_ctl.disable()
    assert room_ctl.instance.intranet_status == 0

    audit_log_count = Log.objects.filter(
        primary_key=room_ctl.instance.id,
        action='conference_room_deleted'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Room',
        model_pk=room_ctl.instance.id,
        action=LogAction.DISABLED.value,
        who=map_edit_conference_room_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_add_table(map_edit_table_test_data):
    table = TableCtl.create(map_edit_table_test_data.table_data, map_edit_table_test_data.user)
    assert table.intranet_status, 1
    assert table.coord_x == map_edit_table_test_data.table_data['coord_x']
    assert table.coord_y == map_edit_table_test_data.table_data['coord_y']
    assert table.floor == map_edit_table_test_data.table_data['floor']

    audit_log_count = Log.objects.filter(
        primary_key=table.id,
        action='table_created'
    ).count()
    assert audit_log_count == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Table',
        model_pk=table.id,
        action=LogAction.CREATED.value,
        who=map_edit_table_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_modify_table(map_edit_table_test_data):
    table = TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user)
    table.modify(map_edit_table_test_data.table_data)
    assert table.table.coord_x == map_edit_table_test_data.table_data['coord_x']
    assert table.table.coord_y == map_edit_table_test_data.table_data['coord_y']
    assert table.table.floor == map_edit_table_test_data.table_data['floor']

    audit_log_count = Log.objects.filter(
        primary_key=table.table.id,
        action='table_location_updated'
    ).count()
    assert audit_log_count == 1

    map_log_count = MapLog.objects.filter(
        model_name='django_intranet_stuff.Table',
        model_pk=map_edit_table_test_data.table.id,
        action=LogAction.UPDATED.value,
        who=map_edit_table_test_data.user.user
    ).count()
    assert map_log_count == 1


@pytest.mark.django_db
def test_occupy_table(map_edit_table_test_data):
    uhura = StaffFactory(
        login='uhura',
        department=map_edit_table_test_data.department,
        table=map_edit_table_test_data.another_table,
    )

    map_edit_table_test_data.mccoy.table = map_edit_table_test_data.table
    map_edit_table_test_data.mccoy.save()

    # Сажаем Ухуру и Кирка, Маккоя оставляем, Спока удаляем
    data = {
        'new0': {
            'staff': uhura, 'staff__login': 'uhura',
            'department': map_edit_table_test_data.department,
            'department_id': map_edit_table_test_data.department.id,
        },
        'new1': {
            'staff': map_edit_table_test_data.kirk,
            'staff__login': map_edit_table_test_data.kirk.id,
            'department': map_edit_table_test_data.department,
            'department_id': map_edit_table_test_data.department.id,
        },
        map_edit_table_test_data.mccoy.id: {
            'staff': map_edit_table_test_data.mccoy,
            'staff__login': map_edit_table_test_data.mccoy.id,
            'department': map_edit_table_test_data.department,
            'department_id': map_edit_table_test_data.department.id,
        }
    }

    with pytest.raises(MultipleTableError):
        result = TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user).occupy(data.copy())

    uhura.refresh_from_db()
    map_edit_table_test_data.kirk.refresh_from_db()
    map_edit_table_test_data.mccoy.refresh_from_db()
    map_edit_table_test_data.spock.refresh_from_db()
    uhura.office = map_edit_table_test_data.redrose
    uhura.save()

    result = TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user).occupy(data.copy())
    occupied = Staff.objects.filter(table=map_edit_table_test_data.table)
    assert occupied.count() == 3

    # Проверяем данные в базе
    assert uhura in occupied
    assert map_edit_table_test_data.kirk in occupied
    assert map_edit_table_test_data.mccoy in occupied

    # Спока не было в data, так что он обесстолился
    assert map_edit_table_test_data.spock not in occupied

    # Проверяем, что изменения в ответе правильные
    assert (map_edit_table_test_data.another_table, uhura) in result['occupy_changed']
    assert (None, map_edit_table_test_data.kirk) in result['occupy_changed']
    assert map_edit_table_test_data.spock in result['occupy_deleted']

    # У капитана должна удалиться бронь
    reserve = TableReserve.objects.active().filter(staff=map_edit_table_test_data.kirk).count()
    assert reserve == 0

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Table',
        model_pk=map_edit_table_test_data.table.id,
        action=LogAction.OCCUPIED.value,
        who=map_edit_table_test_data.user.user
    )
    assert map_log_qs.count() == 2  # once for Kirk, once for Uhura


@pytest.mark.django_db
def test_disable_table(map_edit_table_test_data):
    table_id = map_edit_table_test_data.table.id
    TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user).delete()

    assert Table.objects.active().filter(id=table_id).count() == 0

    reserves = TableReserve.objects.active().filter(table__id=table_id).count()
    assert reserves == 0

    occupied = Staff.objects.filter(table__id=table_id).count()
    assert occupied == 0

    booked = TableBook.objects.filter(table__id=table_id).count()
    assert booked == 0

    log_qs = Log.objects.filter(
        primary_key=table_id,
        action='table_deleted'
    )
    assert log_qs.count() == 1

    log_qs = Log.objects.filter(
        primary_key=map_edit_table_test_data.spock.id,
        action='staff_updated'
    )
    assert log_qs.count() == 1

    map_log_qs = MapLog.objects.filter(
        model_name='django_intranet_stuff.Table',
        model_pk=table_id,
        action=LogAction.DELETED.value,
        who=map_edit_table_test_data.user.user
    )
    assert map_log_qs.count() == 1


@pytest.mark.django_db
def test_reserve_table_delete(map_edit_table_test_data):
    table_id = map_edit_table_test_data.table.id
    assert TableReserve.objects.active().filter(table__id=table_id).count() != 0

    TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user).reserve({})

    assert TableReserve.objects.active().filter(table__id=table_id).count() == 0


def test_png_metadata_size():
    # Эта картинка имеет метаданные c размером больше 1МБ, но не больше чем 2
    PngImagePlugin.MAX_TEXT_CHUNK = 2 * 1024 * 1024

    Image.open("/src/staff/map/tests/img/large-metadata-img.png")

    assert True
