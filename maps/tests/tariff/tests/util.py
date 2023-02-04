from maps.wikimap.stat.tasks_payment.dictionaries.tariff.schema import TariffsSchema

from library.python import resource
from marshmallow import ValidationError
import yaml


def get_yamls(prefix='maps/tasks_tariffs/'):
    return [tariffs_yaml.decode() for tariffs_yaml in resource.itervalues(prefix)]


def get_all_valid_tariffs_descriptions(prefix='maps/tasks_tariffs/'):
    result = []
    for tariffs_yaml in get_yamls(prefix):
        try:
            result.append(
                TariffsSchema().load(yaml.full_load(tariffs_yaml))
            )
        except ValidationError as e:
            # Schema validation errors are checked in `test_should_validate_by_schema()`
            result.append(e.valid_data)
    return result
