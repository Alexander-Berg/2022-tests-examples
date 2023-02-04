import random

from src.change_registry.models import change_record
from src.common import FieldMapper
from src.common.test_utils import get_random_date


def test_map():
    source_name = f'test-source-{random.random()}'
    custom_source_date = f'test-{random.random()}'
    source = {
        'id': random.randint(100, 500),
        custom_source_date: get_random_date(),
        'budget_position_code': random.randint(1000, 5000),
    }
    custom_mapping = {'source_date': custom_source_date}
    target = FieldMapper(system_fields={'id', 'source', 'source_id'})

    result = target.map(source_name, source, change_record, custom=custom_mapping)

    assert result['source'] == source_name
    assert result['source_id'] == source['id']
    assert result['source_date'] == source[custom_source_date]
    assert result['budget_position_code'] == source['budget_position_code']


def test_map_alias():
    source_name = f'test-source-{random.random()}'
    source_date_alias = f'test-{random.random()}'
    source = {
        'id': random.randint(100, 500),
        source_date_alias: random.randint(1000, 5000),
    }
    aliases = {'source_date': source_date_alias}
    target = FieldMapper(system_fields={'id', 'source', 'source_id'})

    result = target.map(source_name, source, change_record, aliases=aliases)

    assert result['source'] == source_name
    assert result['source_id'] == source['id']
    assert result['source_date'] == source[source_date_alias]
