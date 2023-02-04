import json

import pytest
from assertpy import assert_that

from django.utils.timezone import now

from staff.lib.testing import TableFactory, UserFactory, RoomFactory, StaffFactory
from staff.map.edit.objects import TableCtl
from staff.map.models import Table
from staff.map.models.logs import Log, LogFactory, LogValueError, LogAction


def test_build_log(monkeypatch):
    user = UserFactory.build()
    table = TableFactory.build(pk=42)

    log = LogFactory._build_log(who=user, obj=table, action=LogAction.CREATED)
    assert log.who == user
    assert log.action == LogAction.CREATED.value
    assert log.model_pk == table.pk
    assert log.model_name == 'django_intranet_stuff.Table'
    assert log.data

    data = json.loads(log.data)
    assert_that(data).is_type_of(list)
    assert_that(data).is_length(1)

    assert_that({
        'model': 'django_intranet_stuff.table',
        'pk': table.pk,
    }).is_subset_of(data[0])

    assert_that(data[0]['fields']).contains_key('floor', 'coord_x', 'coord_y')

    room = RoomFactory.build(pk=420)
    log = LogFactory._build_log(who=user, obj=room, action=LogAction.UPDATED, data={'place': 'holder'})
    assert log.model_name == 'django_intranet_stuff.Room'
    assert log.action == LogAction.UPDATED.value
    assert log.model_pk == room.pk
    assert log.data == '{"place": "holder"}'


@pytest.mark.django_db
def test_create_log(monkeypatch):
    table = TableFactory.create()
    user = UserFactory.create()

    time_before = now()
    LogFactory.create_log(who=user, obj=table, action=LogAction.UPDATED)
    time_after = now()

    log_qs = Log.objects.filter(action=LogAction.UPDATED.value, model_name='django_intranet_stuff.Table')
    assert log_qs.count() == 1

    log_from_db = log_qs.get()
    assert time_before <= log_from_db.created_at <= time_after


@pytest.mark.django_db
def test_checks_log_actions_for_validity(monkeypatch):
    with pytest.raises(LogValueError):
        LogFactory.create_log(
            action='invalid action',
            who=UserFactory.create(),
            obj=TableFactory.create()
        )

    assert Log.objects.all().count() == 0


@pytest.mark.django_db
def test_occupy_table_logging(map_edit_table_test_data):
    uhura = StaffFactory(
        login='uhura',
        department=map_edit_table_test_data.department,
        office=map_edit_table_test_data.redrose,
    )

    data = {
        'new0': {
            'staff': uhura,
            'staff__login': 'uhura',
            'department': map_edit_table_test_data.department,
            'department_id': map_edit_table_test_data.department.id,
        },
    }

    TableCtl(map_edit_table_test_data.table, map_edit_table_test_data.user).occupy(data)

    log_qs = Log.objects.filter(
        model_name='django_intranet_stuff.Table',
        action=LogAction.OCCUPIED.value
    )
    assert log_qs.count() == 1

    log = log_qs.get()
    assert log.who == map_edit_table_test_data.user.user
    assert log.model_pk == map_edit_table_test_data.table.pk
    assert log.data == '{"occupied_by": "uhura"}'


def test_log_get_model_name():
    table = TableFactory.build()

    assert Log.get_model_name(Table) == 'django_intranet_stuff.Table'
    assert Log.get_model_name(table) == 'django_intranet_stuff.Table'

    with pytest.raises(ValueError):
        Log.get_model_name('Table')
