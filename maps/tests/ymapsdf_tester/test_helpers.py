from maps.garden.sdk import test_utils

from maps.garden.sdk.core import Version
from maps.garden.sdk.yt import YtTableResource


YT_SERVER = "plato"


def generate_table_resource(name, environment_settings, schema=None):
    resource = YtTableResource(name, path_template="/" + name, server=YT_SERVER)
    if schema:
        resource.set_schema(schema)
    resource.version = Version()
    resource.load_environment_settings(environment_settings)
    return resource


def generate_input_table_resource(data_dir, name, environment_settings, schema=None):
    schema_manager = test_utils.ymapsdf_schema.YmapsdfSchemaManager()
    if schema is None:
        schema = schema_manager.schema_for_table(name).make_yt_schema()
    test_data = test_utils.data.read_table_from_file(data_dir, name, schema=schema)

    input_resource = generate_table_resource(name, environment_settings, schema)
    input_resource.write_table(test_data)

    input_resource.logged_commit()
    input_resource.calculate_size()

    return input_resource
