from pathlib import Path

from mdh.core.models import Schema, Reference, Constrained
from mdh.launches.importers.balance.instructions import INSTRUCTIONS
from mdh.launches.management.commands.importrecords import Command as ImportCommand


def test_basic(
    init_user, extract_fixture, init_resource, init_domain, init_reference, init_node, init_records):

    fixture_files = INSTRUCTIONS['expected']
    tmpdir = ''

    # Выносим фикстуры во временную директорию.
    for filename in fixture_files:
        tmpdir = Path(extract_fixture(f'{filename}.csv')).parent

    init_user(robot=True)

    # В импорте есть ссылки на записи уже существующие в БД.
    # Создаём эти записи со схемой и прочим.
    records_service = init_records(
        'nom_service',
        [
            'bf6040b2-4686-454d-bdc7-1031750bd01a',
            'f92a5a3c-3f84-466d-b2f3-3593ec463e6c',
            '7a451008-ebd3-4fc7-92ec-941dc3953a53',
            '6f20665b-294b-46b0-bfaf-ec2c963daa2b',
            '6a5620d5-6d37-4c3f-9794-9a7eef698979',
            'f84b363a-8cc6-42c0-ab84-0541e112e042',
            '63f965cb-1d0a-48aa-89a4-633acbc68c69',  # на эту запись никто не ссылается
        ]
    )

    ImportCommand().handle(
        'balance',
        # '/home/idlesign/arc/arcadia/billing/mdh/ab/dumped'
        tmpdir
    )

    # Проверим схемы.
    schemas = list(Schema.objects.order_by('alias').all())
    schemas_total = len(schemas)

    assert schemas_total == 4
    schema = schemas[0]
    assert schema.alias == 'nom_balance_service'
    assert len(schema.fields) == 24
    assert schema.fields[0]['type'] == {'alias': 'fk', 'params': {'ref': 'nom_service'}}
    assert schema.fields[1]['alias'] == 'url_orders'
    assert schema.display['widgets']['url_orders']['alias'] == 'url'
    assert schema.display['widgets']['unified_account_type']['alias'] == 'select'
    assert schema.field_objects['unified_account_type'].choices.static == {
        'enqueue': {'ru': 'Автоматически'}, 'manual': {'ru': 'Вручную'}}
    assert schema.constraints == [{'attrs': ['service_uid'], 'ident': '0b89fad177b8e38706110e749714e2e6'}]

    # Справочники.
    references = list(Reference.objects.order_by('alias').all())
    reference = references[0]
    assert reference.alias == 'nom_balance_service'
    assert reference.queue == 'MDHNOMENCLATURE'

    records = list(reference.records.all())
    assert len(records) == 3

    attrs = records[0].attrs
    assert attrs['service_uid'] == str(records_service[0].master_uid)
    assert attrs['url_orders'] is None
    assert attrs['transfer_group_id'] is not None

    attrs = records[1].attrs
    assert attrs['url_orders'] == 'http://partner.taxi.yandex.ru'

    attrs = records[2].attrs
    assert attrs['transfer_group_id'] is None

    reference = references[1]
    assert reference.alias == 'nom_balance_transfer_group'
    records = list(reference.records.all())
    assert records[0].me == {'ru': 'MEDIA_SELLING'}

    # Проверка наличия ограничений.
    assert Constrained.objects.count() == 6

    # Проверяем успешность повторного импорта после
    # очистки БД от данных предыдущего.
    ImportCommand().handle(
        'balance',
        tmpdir,
        cleanup=True,
    )
    assert Constrained.objects.count() == 6
