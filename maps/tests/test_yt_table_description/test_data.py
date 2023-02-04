from collections.abc import Callable
from typeguard import check_type

from maps.garden.tools.stat_updater.lib.yt_table_descriptions.data import YT_TABLE_DESCRIPTIONS
from maps.garden.tools.stat_updater.lib.yt_table_descriptions.parsing_tools import (
    MappingDescriptionType, get_mongo_key_field_name
)


def test_data_typing():
    for table_name, description in YT_TABLE_DESCRIPTIONS.items():
        assert isinstance(table_name, str)
        check_type(argname=table_name, value=description, expected_type=MappingDescriptionType)
        assert(not isinstance(get_mongo_key_field_name(description), Callable))
