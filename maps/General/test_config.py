from lib.server import server
from data_types.defaults import VOLUME_UNITS
import copy


def test_config(user):
    config = server.get_config(user) >> 200
    units = config['units']

    volume_units_copy = copy.deepcopy(VOLUME_UNITS)
    for unit_group in volume_units_copy:
        for unit in unit_group["units"]:
            if unit.get("import_name"):
                del unit["import_name"]

    assert units == volume_units_copy
