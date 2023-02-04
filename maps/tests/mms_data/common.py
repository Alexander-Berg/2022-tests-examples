from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import FileResource


def _create_file(path, size=1024):
    with open(path, "wb") as resource_file:
        resource_file.truncate(size)


def _create_empty_resource(name, environment_settings):
    """ Creates a resource which is a valid garden resource but is not
        guaranteed to be a valid mms file """

    resource = FileResource(
        name,
        filename_template="{_name}"
    )
    resource.load_environment_settings(environment_settings)
    resource.version = Version(
        properties={
            "release_name": "0.0.0-0",
            "masstransit_data_release": "0.0.0-0"
        }
    )
    _create_file(resource.path())

    # Performing logged_commit is required to calculate resource size.
    # It also cleans up resource files. However, the resource has to be
    # present in local storage. Hence the call of ensure_available is necessary.
    resource.logged_commit()
    resource.calculate_size()
    resource.ensure_available()
    return resource


def create_data_mms(environment_settings):
    return _create_empty_resource("data_mms", environment_settings)
