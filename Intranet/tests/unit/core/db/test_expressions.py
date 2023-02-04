import pytest

from intranet.femida.src.core.db import JsonF

from intranet.femida.tests.models import Table


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('data', [{
    'a': {
        'b': {'c': 1},
        'd': 'hi',
    },
}])
@pytest.mark.parametrize('key, result', (
    ('a', {
        'b': {'c': 1},
        'd': 'hi',
    }),
    ('a__b', {'c': 1}),
    ('a__b__c', 1),
    ('a__d', 'hi'),
    ('a__d__x', None),
    ('a__x', None),
))
def test_jsonf(data, key, result):
    Table.objects.create(json_field=data)
    row = Table.objects.values(result=JsonF('json_field', key)).get()
    assert row['result'] == result
