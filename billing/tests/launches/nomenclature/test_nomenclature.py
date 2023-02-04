from pathlib import Path

import pytest
from django.conf import settings

from mdh.core.changes.clone_utils import CsvCloner
from mdh.core.models import Schema, Reference, Record, Foreign, Nested
from mdh.core.models.schema import DEFAULT_DISPLAY_ME
from mdh.launches.importers.nomenclature.instructions import INSTRUCTIONS
from mdh.launches.management.commands.importrecords import Command as ImportCommand


@pytest.fixture
def run_command(extract_fixture, init_user, init_node):
    fixture_files = INSTRUCTIONS['expected']

    tmpdir = ''
    # Выносим фикстуры во временную директорию.
    for filename in fixture_files:
        tmpdir = Path(extract_fixture(f'BO_{filename}.csv')).parent

    init_user(robot=True)
    init_node('front', id=settings.MDH_DEFAULT_NODE_ID)

    cmd = ImportCommand()
    cmd.handle(
        'nomenclature',
        # '/home/idlesign/arc/arcadia/billing/mdh/ab/dumped'
        tmpdir
    )

    assert Record.objects.all().count() == 34
    assert Foreign.objects.count() == 51

    # Продукты имеют зависымые справочники.
    assert Nested.objects.count() == 5


def test_basic(run_command):

    schemas = list(Schema.objects.all())
    assert schemas

    # проверяем правильность наименований параметров
    validators = str(schemas[0].fields[0]['validators'])
    assert 'min_length' not in validators
    assert 'min' in validators

    references = list(Reference.objects.all())
    assert references

    assert len(references) == len(schemas)

    for schema in schemas:
        me = schema.display['me']
        assert me and me != DEFAULT_DISPLAY_ME
        assert schema.display['widgets']
        assert schema.is_master
        assert schema.is_published
        assert schema.version_master == 1

        if schema.alias == 'nom_discount_type':
            assert schema.display['linked'] == [
                {'dom': 'nomenclature', 'ref': 'nom_product_group', 'lnk': {'_master_uid': 'discount_id'}},
                {'dom': 'nomenclature', 'ref': 'nom_product', 'lnk': {'_master_uid': 'commission_type'}}
            ]

    for reference in references:
        assert reference.is_published
        assert reference.schema_id
        assert reference.queue == 'MDHNOMENCLATURE'

        # Проверка подмены us на en.
        if reference.alias == 'nom_language':
            assert list(reference.records.all())[0].attrs['code'] == 'en'

    # Проверяем проброс атрибутов импортёром в метаданные записи
    # и удаление избыточных атрибутов записей.
    records = list(Record.objects.filter(editor__username='testme'))
    assert len(records) == 1
    record: Record = records[0]
    assert record.reference_id
    assert record.resource.reference_link.domain.title
    assert 'hidden' not in record.attrs
    assert record.attrs['only_test_env'] == False
    assert record.is_archived
    assert record.version == 1
    assert record.version_master == 1
    assert str(record.dt_upd) == '2008-08-21 17:45:24+00:00'
    assert record.remote_id == '11100'


@pytest.fixture
def check_cloned():

    def check_cloned_(new_record, sample_record):
        sample_record_uid_old = sample_record.attrs['commission_type']

        sample_record.refresh_from_db()
        assert sample_record.attrs['commission_type'] == sample_record_uid_old  # не изменилось

        assert new_record.attrs['name'] == sample_record.attrs['name']
        assert str(new_record.attrs['commission_type']) != sample_record_uid_old
        assert str(new_record.attrs['engine_id']) == sample_record.attrs['engine_id']
        assert new_record.version == 1
        assert new_record.version_master == 1
        assert new_record.remote_id is None
        assert new_record.attrs['id'] is None

        # Детализация проверки клонирования вложенных записей.
        nested_sample = sample_record.get_nested()
        nested_new = new_record.get_nested()
        assert set(nested_sample) == set(nested_new)

        assert nested_sample['nom_tax'][0].attrs['id'] is not None
        assert nested_new['nom_tax'][0].attrs['id'] is None

    return check_cloned_


def test_cloner(run_command, django_assert_num_queries, check_cloned):

    lines = (
        'nom_product___id,hint,engine_id___old,engine_id___new,commission_type___old,commission_type___new\n'
        '42,some,-,-,0,37\n'
        '674,some,103,621,37,0\n'
        '1546,some,103,621,0,37\n'
        '1546,other,0,-,0,-\n'
    )
    records = {
        record.remote_id: record
        for record in
        Record.objects.filter(remote_id__in=['1546', '674', '42', '1213']).order_by('remote_id')
    }
    sample_record: Record = records['42']
    assert sample_record.attrs['id'] is not None

    with django_assert_num_queries(83) as _:
        cloner = CsvCloner(lines.splitlines())
        results = cloner.run()

    assert Record.objects.all().count() == 43
    # добавились новые записи (4 продукта (один клонируется дважды) + 5 вложенных)

    assert Foreign.objects.count() == 93
    # добавился кеш внешних ключей для новых записей

    check_cloned(results[42][0], sample_record)


def test_cloner_by_uuid(run_command, django_assert_num_queries, check_cloned):

    lines = [
        'nom_product___master_uid,hint,engine_id___old,engine_id___new,commission_type___old,commission_type___new'
    ]
    records = {}

    remotes = dict(Record.objects.filter(remote_id__in=['0', '37']).values_list('remote_id', 'master_uid'))

    for record in Record.objects.filter(remote_id__in=['42']).order_by('remote_id'):
        records[record.remote_id] = record
        lines.append(
            f"{record.master_uid},some,-,-,{remotes['0']},{remotes['37']}",
        )

    sample_record: Record = records['42']
    assert sample_record.attrs['id'] is not None

    assert Record.objects.all().count() == 34

    with django_assert_num_queries(53) as _:
        cloner = CsvCloner(lines)
        results = cloner.run()

    assert Record.objects.all().count() == 40
    # добавились новые записи (1 продукт + 5 вложенных)

    assert Foreign.objects.count() == 69
    # добавился кеш внешних ключей для новых записей (7 для продуктов + 10 для вложенных)

    check_cloned(results[f'{sample_record.master_uid}'][0], sample_record)


def test_cloner_upgrade(run_command, django_assert_num_queries):

    lines = [
        'nom_product___master_uid,hint,name___old,name___new,commission_type___old,commission_type___new'
    ]
    records = {}

    remotes = dict(Record.objects.filter(remote_id__in=['37']).values_list('remote_id', 'master_uid'))
    commission_type_new = remotes['37']
    commission_type_old = None

    for record in Record.objects.filter(remote_id__in=['42']).order_by('remote_id'):
        records[record.remote_id] = record
        commission_type_old = record.attrs['commission_type']
        lines.append(
            f"{record.master_uid},hinted,oldname,newname,oldcomtype,{commission_type_new}",
        )

    assert commission_type_old
    sample_record: Record = records['42']
    assert sample_record.attrs['id'] is not None

    with django_assert_num_queries(11) as _:

        cloner = CsvCloner(lines, upgrade_mode=True)
        results = cloner.run()

        upgraded = results[f'{sample_record.record_uid}'][0]

    upgraded.refresh_from_db()

    assert upgraded.is_approved
    assert upgraded.issue == ''
    assert upgraded.master_uid == sample_record.master_uid
    assert not upgraded.is_master

    # дожидаемся публикации в фоне.
    Record.publish(upgraded)

    upgraded.refresh_from_db()
    assert upgraded.is_master
    assert upgraded.version == 1
    assert upgraded.version_master == 2

    assert upgraded.attrs['name'] == 'newname'
    assert upgraded.attrs['commission_type'] == f'{commission_type_new}'
    assert upgraded.foreign['commission_type'].master_uid == commission_type_new
