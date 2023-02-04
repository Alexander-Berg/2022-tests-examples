import json
from datetime import timedelta, datetime, date

from assertpy import assert_that
from factory.django import mute_signals
import pytest

from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.urlresolvers import reverse
from django.conf import settings
from django.db.models import signals

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.person.models import Staff
from staff.lib.testing import (
    DeviceFactory,
    FloorFactory,
    GroupFactory,
    MapLogFactory,
    OfficeFactory,
    RegionFactory,
    RoomFactory,
    RoomUsageFactory,
    StaffFactory,
    TableFactory,
    TableReserveFactory,
)

from staff.map.api.views import staff_office_logs_import
from staff.map.forms.region import RegionForm
from staff.map.models import (
    DEVICE_TYPES,
    Device,
    FloorMap,
    Region,
    ROOM_TYPES,
    SECURITY_DEVICES,
    SourceTypes,
)

Device._meta.managed = True


@pytest.mark.django_db
def test_offices(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        deleted = OfficeFactory(name='Del', city=None, intranet_status=0)

        url = reverse('map-offices')
        response = client.get(url)

        objects = json.loads(response.content)

        exclude_from_check = {
            map_test_data.jay.office.id,
            map_test_data.bob.office.id,
            Staff.objects.get(login='tester').office.id,
        }

        assert deleted.id not in [o['id'] for o in objects], 'Deleted office in results'
        assert set(
            o['id']
            for o in objects
            if o['id'] not in exclude_from_check
        ) == {map_test_data.redrose.id, map_test_data.comode.id}


@pytest.mark.django_db
def test_tables(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        deleted = TableFactory(floor=map_test_data.first, intranet_status=0)

        url = reverse('map-tables')
        response = client.get(url)

        objects = json.loads(response.content)

        assert deleted.id not in [o['id'] for o in objects], 'Deleted table in results'
        assert set(o['id'] for o in objects) == {
            map_test_data.tbl_1.id, map_test_data.tbl_2.id, map_test_data.tbl_with_coord_is_0.id
        }

        url = reverse('map-tables') + '?floor_id=%s' % map_test_data.first.pk
        response = client.get(url)

        objects = json.loads(response.content)
        assert set(o['id'] for o in objects) == {
            map_test_data.tbl_1.id}, 'Only first floor room suppose to be here'


@pytest.mark.django_db
def test_table_employees(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        url = reverse('map-tables') + '?floor_id=%s' % map_test_data.first.pk

        response = client.get(url)
        objects = json.loads(response.content)
        assert map_test_data.bob.login not in objects[0]['employees'][0][
            'login'], 'Dismissed employee in results'
        assert map_test_data.jay.login == objects[0]['employees'][0]['login']


@pytest.mark.django_db
def test_table_reservations(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        TableReserveFactory(table=map_test_data.tbl_2, staff=map_test_data.jay, intranet_status=1,
                            department=None)
        TableReserveFactory(table=map_test_data.tbl_2, staff=map_test_data.jay, intranet_status=0,
                            department=None)

        url = reverse('map-tables') + '?floor_id=%s' % map_test_data.second.pk

        response = client.get(url)
        objects = json.loads(response.content)
        assert map_test_data.jay.login == objects[0]['reservations'][0]['login']


@pytest.mark.django_db
def test_table_area(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        x, y = map_test_data.tbl_1.coord_x, map_test_data.tbl_1.coord_y
        url = reverse('map-table') + '?x=%d&y=%d&floor_id=%d' % (x, y, map_test_data.first.pk)
        response = client.get(url)
        objects = json.loads(response.content)
        assert len(objects) == 1

        map_test_data.tbl_2.floor = map_test_data.first
        map_test_data.tbl_2.save()
        url = reverse('map-table') + '?x=%d&y=%d&floor_id=%d' % (x, y, map_test_data.first.id)
        response = client.get(url)
        objects = json.loads(response.content)
        assert len(objects) == 2

        url = reverse('map-table') + '?x=%d&y=%d&floor_id=%d&distance=%d' % (
            x,
            y,
            map_test_data.first.id,
            50
        )
        response = client.get(url)
        objects = json.loads(response.content)
        assert len(objects) == 1
        assert objects[0]['id'] == map_test_data.tbl_1.pk

        url = reverse('map-table') + '?login=%s&distance=%d' % (
            map_test_data.jay.login, 50)
        response = client.get(url)
        objects = json.loads(response.content)
        assert len(objects) == 1
        assert objects[0]['id'] == map_test_data.tbl_1.pk


@pytest.mark.django_db
def test_table_with_coord_is_0(map_test_data, client):
    url = reverse('map-table') + f'?table_id={map_test_data.tbl_with_coord_is_0.id}'
    response = client.get(url)
    objects = json.loads(response.content)
    assert len(objects) == 1
    assert objects[0]['id'] == map_test_data.tbl_with_coord_is_0.pk


@pytest.mark.django_db
def test_table_errors(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        url = reverse('map-table') + '?login=mr_x'
        objects = json.loads(client.get(url).content)
        assert 'errors' in objects

        url = reverse('map-table') + '?table_num=666'
        objects = json.loads(client.get(url).content)
        assert 'errors' in objects

        url = reverse('map-table') + '?floor_id=42'
        objects = json.loads(client.get(url).content)
        assert 'errors' in objects

        url = reverse('map-table') + '?distance=!@#$'
        objects = json.loads(client.get(url).content)
        assert 'errors' in objects


@pytest.mark.django_db
def test_rooms(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(name='Copy', floor=map_test_data.first, intranet_status=1,
                               room_type=ROOM_TYPES.OFFICE)
        lib = RoomFactory(name='Library', floor=map_test_data.second, intranet_status=1,
                          room_type=ROOM_TYPES.OFFICE)
        coworking = RoomFactory(name='Coworking', floor=map_test_data.second, intranet_status=1,
                                room_type=ROOM_TYPES.COWORKING)
        deleted = RoomFactory(name='Del', floor=map_test_data.first, intranet_status=0)

        url = reverse('map-rooms')
        response = client.get(url)

        objects = json.loads(response.content)

        assert coworking.id not in [o['id'] for o in objects], 'Coworking room in results'
        assert deleted.id not in [o['id'] for o in objects], 'Deleted room in results'
        assert set(o['id'] for o in objects) == {copyroom.id, lib.id}

        url += '?floor_id=%s' % map_test_data.first.pk
        response = client.get(url)

        objects = json.loads(response.content)
        assert set(o['id'] for o in objects) == {
            copyroom.id}, 'Only first floor room suppose to be here'


@pytest.mark.django_db
def test_room(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(name='Copy', floor=map_test_data.first, intranet_status=1,
                               room_type=ROOM_TYPES.OFFICE, num=666)

        url = reverse('map-room')
        response = client.get(url + '?room_num=%s' % copyroom.num)
        object = json.loads(response.content)

        assert object['name'] == copyroom.name


@pytest.mark.django_db
def test_room_with_employee(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(name='Copy', floor=map_test_data.first, intranet_status=1,
                               room_type=ROOM_TYPES.OFFICE, num=666)

        person = StaffFactory(login='spock', table=None, room=copyroom)

        url = reverse('map-room')
        response = client.get(url + '?room_num=%s' % copyroom.num)
        object = json.loads(response.content)

        assert object['name'] == copyroom.name
        assert len(object['employees']) == 1
        employee = object['employees'][0]
        assert employee['login'] == person.login


@pytest.mark.django_db
def test_room_without_employee(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(name='Copy', floor=map_test_data.first, intranet_status=1,
                               room_type=ROOM_TYPES.OFFICE, num=666)
        table = TableFactory(floor=map_test_data.first, room=copyroom)
        StaffFactory(login='spock', table=table, room=copyroom)

        url = reverse('map-room')
        response = client.get(url + '?room_num=%s' % copyroom.num)
        object = json.loads(response.content)

        assert object['name'] == copyroom.name
        assert len(object['employees']) == 0


@pytest.mark.django_db
def test_rooms_usage(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(
            name='Copy',
            floor=map_test_data.first,
            intranet_status=1,
            room_type=ROOM_TYPES.OFFICE,
        )
        lib = RoomFactory(
            name='Library',
            floor=map_test_data.second,
            intranet_status=1,
            room_type=ROOM_TYPES.OFFICE,
        )
        coworking = RoomFactory(
            name='Coworking',
            floor=map_test_data.first,
            intranet_status=1,
            room_type=ROOM_TYPES.COWORKING,
        )
        deleted = RoomFactory(name='Del', floor=map_test_data.first, intranet_status=0)

        date_from = date.today() - timedelta(days=1)
        date_to = date.today()

        for room in (copyroom, lib, coworking, deleted):
            RoomUsageFactory(
                room=room,
                date=date_from,
                usage=0.5,
                source=SourceTypes.PACS.value,
            )

        url = reverse('map-rooms-usage')
        response = client.get(url)

        assert response.status_code == 400

        data = {
            'floor_id': map_test_data.first.pk,
            'date_from': date_from,
            'date_to': date_to,
        }
        response = client.get(url, data=data)

        assert response.status_code == 200
        objects = json.loads(response.content)

        assert objects['limit_usage'] == settings.MAP_LIMIT_ROOM_USAGE
        assert len(objects['rooms_usage']) == 1


@pytest.mark.django_db
def test_export_rooms_usage(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        copyroom = RoomFactory(
            name='Copy',
            floor=map_test_data.first,
            intranet_status=1,
            room_type=ROOM_TYPES.OFFICE,
        )
        lib = RoomFactory(
            name='Library',
            floor=map_test_data.second,
            intranet_status=1,
            room_type=ROOM_TYPES.OFFICE,
        )
        coworking = RoomFactory(
            name='Coworking',
            floor=map_test_data.first,
            intranet_status=1,
            room_type=ROOM_TYPES.COWORKING,
        )
        deleted = RoomFactory(name='Del', floor=map_test_data.first, intranet_status=0)

        date_from = date.today() - timedelta(days=1)
        date_to = date.today()

        for room in (copyroom, lib, coworking, deleted):
            RoomUsageFactory(
                room=room,
                date=date_from,
                usage=0.5,
                source=SourceTypes.PACS.value,
            )

        url = reverse('map-export-rooms-usage')
        response = client.get(url)
        assert response.status_code == 404
        data = {
            'date_from': date_from,
            'date_to': date_to,
        }
        response = client.get(url, data=data)
        assert response.status_code == 200


@pytest.mark.django_db
def test_conference_rooms(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        saturn = RoomFactory(
            name='Saturn',
            floor=map_test_data.first,
            intranet_status=1,
            room_type=ROOM_TYPES.CONFERENCE,
            name_exchange='saturn',
        )
        serafim = RoomFactory(
            name='Serafim',
            floor=map_test_data.second,
            intranet_status=1,
            room_type=ROOM_TYPES.CONFERENCE,
        )
        coffee = RoomFactory(
            name='Coffee',
            floor=map_test_data.second,
            intranet_status=1,
            room_type=ROOM_TYPES.ROOM_COFFEE,
        )
        deleted = RoomFactory(
            name='Del',
            floor=map_test_data.first,
            intranet_status=0,
            room_type=ROOM_TYPES.CONFERENCE,
            name_exchange='saturn',
        )

        url = reverse('map-conference_rooms')
        response = client.get(url)

        objects = json.loads(response.content)

        assert deleted.id not in [o['id'] for o in objects], 'Deleted conf. room in results'
        assert coffee.id not in [o['id'] for o in objects], 'Coffee point in results'
        assert set(o['id'] for o in objects) == {saturn.id, serafim.id}

        response = client.get(reverse('map-conference_room'), data={'name_exchange': 'saturn'})
        objects = json.loads(response.content)
        assert objects['id'] == saturn.id

        url += '?floor_id=%s' % map_test_data.first.pk
        response = client.get(url)

        objects = json.loads(response.content)
        assert set(o['id'] for o in objects) == {
            saturn.id}, 'Only first floor conf. room suppose to be here'


@pytest.mark.django_db
def test_coworking_rooms(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        alpha = RoomFactory(name='Alpha', floor=map_test_data.first, intranet_status=1,
                            room_type=ROOM_TYPES.COWORKING)
        theta = RoomFactory(name='Theta', floor=map_test_data.second, intranet_status=1,
                            room_type=ROOM_TYPES.COWORKING)
        coffee = RoomFactory(name='Coffee', floor=map_test_data.second, intranet_status=1,
                             room_type=ROOM_TYPES.ROOM_COFFEE)
        deleted = RoomFactory(name='Del', floor=map_test_data.first, intranet_status=0,
                              room_type=ROOM_TYPES.COWORKING)

        url = reverse('map-coworking_rooms')
        response = client.get(url)

        objects = json.loads(response.content)
        assert deleted.id not in [o['id'] for o in objects], 'Deleted coworking in results'
        assert coffee.id not in [o['id'] for o in objects], 'Coffee point in results'
        assert set(o['id'] for o in objects) == {alpha.id, theta.id}

        url += '?floor_id=%s' % map_test_data.first.pk
        response = client.get(url)

        objects = json.loads(response.content)
        assert set(o['id'] for o in objects) == {
            alpha.id}, 'Only first floor coworking suppose to be here'


@pytest.mark.django_db
def test_equipment(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        device = DeviceFactory(name='prn-666', floor=map_test_data.first, intranet_status=1)
        router = DeviceFactory(name='rtr-10', floor=map_test_data.second, intranet_status=1)
        deleted = DeviceFactory(name='Del', floor=map_test_data.first, intranet_status=0)

        url = reverse('map-equipment')
        response = client.get(url)

        objects = json.loads(response.content)

        assert deleted.id not in [o['id'] for o in objects], 'Deleted printer in results'
        assert set(o['id'] for o in objects) == {device.id, router.id}

        url += '?floor_id=%s' % map_test_data.first.pk
        response = client.get(url)

        objects = json.loads(response.content)
        assert {o['id'] for o in objects} == {device.id}, 'Only first floor printer suppose to be here'


@pytest.mark.django_db
def test_public_equipment_types(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        public_device_types = (d_type[0] for d_type in DEVICE_TYPES if d_type[0] not in SECURITY_DEVICES)
        devices = [
            DeviceFactory(type=available_type, intranet_status=1)
            for available_type in public_device_types
        ]

        url = reverse('map-equipment')
        response = client.get(url)
        objects = json.loads(response.content)

        assert set(device.id for device in devices) == set(obj['id'] for obj in objects)


@pytest.mark.django_db
def test_security_equipment_types(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        public_type = next(iter(DEVICE_TYPES))[0]
        assert public_type not in SECURITY_DEVICES, 'Make sure to pick public device type here'

        public_device = DeviceFactory(type=public_type, intranet_status=1)
        security_devices = [
            DeviceFactory(type=security_type, intranet_status=1)
            for security_type in SECURITY_DEVICES
        ]

        url = reverse('map-equipment')
        response = client.get(url)
        objects = json.loads(response.content)

        returned = set(obj['id'] for obj in objects)
        assert public_device.id in returned, (
            f'Device with public type {DEVICE_TYPES[public_device.type]} should be in results'
        )

        for device in security_devices:
            assert device.id not in returned, (
                f'Security type {DEVICE_TYPES[device.type]} device should NOT be in results'
            )


@pytest.mark.django_db
def test_floors(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        FloorMap.objects.create(floor=map_test_data.first, is_ready=True, file_name='1')
        FloorMap.objects.create(floor=map_test_data.second, is_ready=True, file_name='2')

        response = client.get(reverse('map-floors'))
        objects = json.loads(response.content)
        assert {o['id'] for o in objects} == {map_test_data.first.id, map_test_data.second.id}


@pytest.mark.django_db
def test_floors_with_office_filter(map_test_data, client):
    with mute_signals(signals.pre_save, signals.post_save):
        FloorMap.objects.create(floor=map_test_data.first, is_ready=True, file_name='1')
        FloorMap.objects.create(floor=map_test_data.second, is_ready=True, file_name='2')

        response = client.get(reverse('map-floors'), {'office_id': map_test_data.redrose.id})
        objects = json.loads(response.content)
        assert len(objects) == 1
        assert objects[0]['id'] == map_test_data.first.id


def test_creating_region(company, superuser_client):
    floor = FloorFactory(office=company.offices['KR'])
    group = GroupFactory(type=GROUP_TYPE_CHOICES.SERVICE, name='test service', url='svc_test')
    office = floor.office
    office.have_map = True
    office.save()

    region_coordinates = '1445,1344,1450,1309,1456,1301,1464,1299,1502,1298,1512,1304,1516,1314,1445,1344'
    region_data = {
        'region': [{
            'name': 'test region',
            'description': 'test region description',
            'floor': floor.id,
            'office': floor.office.id,
            'group': group.id,
            'coord_x': 0,
            'coord_y': 0,
            'geometry': region_coordinates,
        }],
    }
    add_region_url = reverse('map-add_region')

    response = superuser_client.post(add_region_url, json.dumps(region_data), content_type='application/json')
    assert response.status_code == 200
    result = json.loads(response.content)

    assert 'id' in result
    region_id = result['id']

    assert Region.objects.values().get() == {
        'id': region_id,
        'name': region_data['region'][0]['name'],
        'description': region_data['region'][0]['description'],
        'floor_id': region_data['region'][0]['floor'],
        'group_id': group.id,
        'geometry': RegionForm.from_text_to_polygon(region_coordinates),
        'intranet_status': 1,
    }


def test_editing_region(company, superuser_client):
    old_region_coordinates = '1445,1344,1450,1309,1456,1301,1464,1299,1502,1298,1512,1304,1516,1314,1516,1354,1445,1344'
    new_region_coordinates = '1445,1344,1450,1309,1456,1301,1464,1299,1502,1298,1512,1304,1445,1344'

    company.offices['KR'].have_map = True
    company.offices['KR'].save()

    region = RegionFactory(
        floor=FloorFactory(office=company.offices['KR']),
        group=GroupFactory(type=GROUP_TYPE_CHOICES.SERVICE, name='test service', url='svc_test'),
        geometry=RegionForm.from_text_to_polygon(old_region_coordinates),
    )

    new_group = GroupFactory(type=GROUP_TYPE_CHOICES.SERVICE, name='another test service', url='svc_test2')

    new_region_data = {
        'region': [{
            'name': 'test region name',
            'description': 'test region description',
            'floor': region.floor.id,
            'coord_x': 0,
            'coord_y': 0,
            'group': new_group.id,
            'geometry': new_region_coordinates,
        }]
    }

    edit_region_url = reverse('map-edit_region', kwargs={'region_id': region.id})
    response = superuser_client.post(edit_region_url, json.dumps(new_region_data), content_type='application/json')
    assert response.status_code == 200
    result = json.loads(response.content)
    assert 'id' in result
    result['id'] == region.id

    assert Region.objects.values().get() == {
        'id': region.id,
        'name': new_region_data['region'][0]['name'],
        'description': new_region_data['region'][0]['description'],
        'floor_id': new_region_data['region'][0]['floor'],
        'group_id': new_group.id,
        'geometry': RegionForm.from_text_to_polygon(new_region_coordinates),
        'intranet_status': 1,
    }


def test_deleting_region(company, superuser_client):
    region_coordinates = '1445,1344,1450,1309,1456,1301,1464,1299,1502,1298,1512,1304,1516,1314,1516,1354'
    region = RegionFactory(geometry=RegionForm.from_text_to_polygon(region_coordinates))

    delete_region_url = reverse('map-delete_region', kwargs={'region_id': region.id})
    response = superuser_client.post(delete_region_url)
    assert response.status_code == 200
    assert json.loads(response.content) == {}

    assert Region.objects.filter(id=region.id).values_list('intranet_status', flat=True).get() == 0


class TestEditHistory:
    @staticmethod
    def _assert_that_json_has_log(json_entry, log_model):
        assert_that({
            'who': log_model.who.id,
            'model_name': log_model.model_name,
            'model_pk': log_model.model_pk,
            'action': log_model.action,
        }).is_subset_of(json_entry)

    @pytest.mark.django_db
    def test_response_codes(self, client):
        edit_history_url = reverse('map-export_edit_history')

        response = client.get(edit_history_url)
        assert response.status_code == 400

        response = client.get(edit_history_url, data={
            'date_from': '2012-01-01',
            'date_to': '2012-01-01'
        })
        assert response.status_code == 200
        assert json.loads(response.content) == []

    @pytest.mark.django_db
    def test_full_export(self, client):
        log1 = MapLogFactory()
        log2 = MapLogFactory()

        date_from = log1.created_at - timedelta(days=1)
        date_to = log2.created_at + timedelta(days=1)

        edit_history_url = reverse('map-export_edit_history')
        response = client.get(edit_history_url, data={
            'date_from': date_from.strftime('%Y-%m-%d'),
            'date_to': date_to.strftime('%Y-%m-%d')
        })

        assert response.status_code == 200
        content = json.loads(response.content)

        assert_that(content).is_type_of(list)
        assert_that(content).is_length(2)

        self._assert_that_json_has_log(content[0], log1)
        self._assert_that_json_has_log(content[1], log2)

    @pytest.mark.django_db
    def test_date_filtering(self, client):
        # @todo MapLogFactory(created_at=...) ignores provided value
        log1 = MapLogFactory()
        log1.created_at = datetime(2012, 5, 1)
        log1.save()

        log2 = MapLogFactory()
        log2.created_at = datetime(2012, 7, 1)
        log2.save()

        log3 = MapLogFactory()
        log3.created_at = datetime(2012, 10, 1)
        log3.save()

        edit_history_url = reverse('map-export_edit_history')
        response = client.get(edit_history_url, data={
            'date_from': '2012-07-01',
            'date_to': '2013-01-01',
        })

        assert response.status_code == 200
        content = json.loads(response.content)

        assert_that(content).is_type_of(list)
        assert_that(content).is_length(2)

        self._assert_that_json_has_log(content[0], log2)
        self._assert_that_json_has_log(content[1], log3)

        edit_history_url = reverse('map-export_edit_history')
        response = client.get(edit_history_url, data={
            'date_from': '2012-05-01',
            'date_to': '2012-05-01',
        })

        assert response.status_code == 200
        content = json.loads(response.content)

        assert_that(content).is_type_of(list)
        assert_that(content).is_length(1)

        self._assert_that_json_has_log(content[0], log1)


@pytest.mark.django_db()
def test_staff_office_logs_import(company, rf):
    request = rf.post(reverse('map:map-staff-office-logs-import'))

    request.FILES['import_file'] = SimpleUploadedFile(
        name='file.csv',
        content="Дата,login,Офис,id\n2021-08-10,jay,ОКО,123".encode('utf-8'),
        content_type='csv',
    )

    result = staff_office_logs_import(request)
    assert result.status_code == 200
