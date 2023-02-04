from mdh.core.models import Schema, Reference, Node, Resource
from mdh.launches.management.commands.importrecords import Command as ImportCommand


def test_basic(init_schema, init_user, extract_fixture, init_node):

    init_user(robot=True)

    cmd = ImportCommand()
    cmd.handle('oebs', '.')
    # И второй запуск команды, чтобы проверить подхват ранее созданных объектов.
    cmd.handle('oebs', '.')

    # Проверим схемы.
    schemas = list(Schema.objects.order_by('alias').all())
    schemas_total = len(schemas)

    assert schemas_total == 13
    schema = schemas[1]
    assert schema.alias == 'oe_xxgl_analytics'
    assert len(schema.fields) == 21
    assert schema.fields[0]['alias'] == 'flex_value_id'

    # Справочники.
    references = list(Reference.objects.order_by('alias').all())
    reference = references[0]
    assert not reference.queue
    assert len(references) == schemas_total

    # Создание узла и ресурсов.
    assert Node.objects.filter(alias='oebs').count() == 1
    assert Resource.objects.filter(node__alias='oebs').count() == schemas_total
