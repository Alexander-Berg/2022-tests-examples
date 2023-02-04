from typing import List

from mdh.core.models import Schema, Reference, Node, Resource
from mdh.launches.importers import AbcImporter
from mdh.launches.management.commands.importrecords import Command as ImportCommand


def test_basic(init_schema, init_user, extract_fixture, init_node, init_domain):

    user = init_user(robot=True)
    domain = init_domain(AbcImporter.domain_alias, user=user)

    cmd = ImportCommand()
    cmd.handle('abc', '.')

    domain.refresh_from_db()

    # Если область существовала ранее, то её параметры не должны изменяться.
    assert domain.titles['en'] == 'Domain title'

    # Проверим схемы.
    schemas = list(Schema.objects.order_by('alias').all())
    schemas_total = len(schemas)

    assert schemas_total == 1
    schema = schemas[0]
    assert schema.alias == 'abc_services'
    assert len(schema.fields) == 6
    assert schema.fields[0]['alias'] == 'name'

    # Справочники.
    references = list(Reference.objects.order_by('alias').all())
    reference = references[0]
    assert not reference.queue  # очередь утверждения не используется (АБЦ - источник)

    # Создание узла и ресурсов.
    assert Node.objects.filter(alias='abc').count() == 1

    resources: List[Resource] = list(Resource.objects.filter(node__alias='abc'))
    assert len(resources) == 1

    record = resources[0].record_add(
        creator=user,
        attrs={
            'slug': 'myservice',
            'name': {
                'ru': 'серв',
                'en': 'serv',
            },
            'state': 'develop',
            'has_children': 'n',
            'id': 1,
            'parent_id': None,
        },
    )
    assert record.id
