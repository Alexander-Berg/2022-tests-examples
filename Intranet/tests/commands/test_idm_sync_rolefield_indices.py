import pytest
from django.core import management
from django.db import connection

from idm.core.constants.rolefield import FIELD_STATE, FIELD_TYPE

field_data = {
    'name': 'тестовое поле',
    'name_en': 'test field',
    'slug': 'test_field-1',
    'options': {},
    'type': FIELD_TYPE.CHARFIELD
}

CHECK_INDEX_SQL = "SELECT COUNT(1) FROM pg_indexes WHERE tablename='upravlyator_role' AND indexname=%s"

pytestmark = pytest.mark.django_db


def test_create_fields_index(simple_system):
    field = field_data.copy()
    field['state'] = FIELD_STATE.CREATED
    rolefield = simple_system.systemrolefields.create(**field)

    management.call_command('idm_sync_rolefield_indices')
    rolefield.refresh_from_db()

    assert rolefield.state == FIELD_STATE.ACTIVE

    with connection.cursor() as cursor:
        idx_name = 'role_fields_data_system_{}_{}_idx'.format(simple_system.pk, field['slug'].replace('-', '_'))
        cursor.execute(CHECK_INDEX_SQL, [idx_name])
        idx_count = cursor.fetchone()
        assert idx_count[0] == 1

        cursor.execute('DROP INDEX {}'.format(idx_name))


def test_drop_fields_index(simple_system):
    field = field_data.copy()
    field['state'] = FIELD_STATE.DEPRIVING
    rolefield = simple_system.systemrolefields.create(**field)

    idx_name = 'role_fields_data_system_{}_{}_idx'.format(simple_system.pk, field['slug'].replace('-', '_'))

    with connection.cursor() as cursor:
        sql_query = ("CREATE INDEX {} ON upravlyator_role ((fields_data->>'{}')) "
                     "WHERE system_id={}".format(idx_name, field['slug'], simple_system.pk))
        cursor.execute(sql_query)

    management.call_command('idm_sync_rolefield_indices')
    rolefield.refresh_from_db()

    assert rolefield.state == FIELD_STATE.DEPRIVED

    with connection.cursor() as cursor:
        cursor.execute(CHECK_INDEX_SQL, [idx_name])
        idx_count = cursor.fetchone()
        assert idx_count[0] == 0
