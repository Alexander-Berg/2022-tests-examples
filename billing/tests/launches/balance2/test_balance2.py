from pathlib import Path

from mdh.core.models import Schema, Reference, Constrained
from mdh.launches.importers.balance2.instructions import INSTRUCTIONS
from mdh.launches.management.commands.importrecords import Command as ImportCommand


def test_basic(init_user, extract_fixture, init_resource, init_domain, init_reference, init_records):
    fixture_files = INSTRUCTIONS['expected']

    tmpdir = ''

    # Выносим фикстуры во временную директорию.
    for filename in fixture_files:
        tmpdir = Path(extract_fixture(f'{filename}.csv')).parent

    init_user(robot=True)

    # В импорте есть ссылки на записи уже суещствующие в БД.
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

    records_country = init_records(
        'nom_country',
        [
            'af6040b2-4686-454d-bdc7-1031750bd01a',
            'a92a5a3c-3f84-466d-b2f3-3593ec463e6c',
        ]
    )

    records_firm = init_records(
        'nom_firm',
        [
            'cf6040b2-4686-454d-bdc7-1031750bd01a',
            'c92a5a3c-3f84-466d-b2f3-3593ec463e6c',
        ]
    )

    records_currency = init_records(
        'nom_iso_currency',
        [
            'ef6040b2-4686-454d-bdc7-1031750bd01a',
            'e92a5a3c-3f84-466d-b2f3-3593ec463e6c',
        ]
    )

    ImportCommand().handle(
        'balance2',
        # '/home/idlesign/arc/arcadia/billing/mdh/ab/dumped'
        tmpdir
    )

    # Проверим схемы.
    schemas = list(Schema.objects.order_by('alias').all())
    schemas_total = len(schemas)

    assert schemas_total == 10
    schema = schemas[3]
    assert schema.alias == 'nom_pay_policy_payment_method'
    assert len(schema.fields) == 6
    assert schema.fields[4]['choices']['const'][0]['val'] == 0

    schema = schemas[7]
    assert schema.alias == 'nom_pay_policy_service'
    assert schema.fields[0]['default']['const'] is None
    assert schema.fields[4]['choices']['const'][0]['val'] is None
    assert schema.fields[4]['choices']['const'][1]['val'] == 'ph'

    # Справочники.
    references = list(Reference.objects.order_by('alias').all())
    reference = references[3]
    assert reference.alias == 'nom_pay_policy_payment_method'
    assert reference.queue == 'MDHNOMENCLATURE'

    records = list(reference.records.all())
    assert len(records) == 4

    # Проверка наличия ограничений.
    assert Constrained.objects.count() == 3

    # Проверяем успешность повторного импорта после
    # очистки БД от данных предыдущего.
    ImportCommand().handle(
        'balance2',
        tmpdir,
        cleanup=True,
    )
    assert Constrained.objects.count() == 3
