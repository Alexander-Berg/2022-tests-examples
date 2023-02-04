import pytz
from maps.garden.sdk.resources import FileResource
from maps.garden.tools.unaccounted_resources.lib.storage_interfaces.s3 import S3StorageInterface
from datetime import datetime, timedelta

CONTOUR_NAME = "unittest"
BUCKET = "test_bucket"


def create_fake_s3_resource(mocker, key):
    fake_resource = mocker.Mock(spec=FileResource)
    fake_resource.storage_key.value = key
    fake_resource.get_storage_type.return_value = "s3"
    fake_resource.contour_name = "unittest"
    return fake_resource


class FakeEntity:
    def __init__(self, key: str, last_modified: datetime = None):
        if not last_modified:
            last_modified = datetime.now(tz=pytz.utc) - timedelta(days=10)
        self.key = key
        self.last_modified = last_modified


def test_add_simple_add_and_check(environment_settings, mocker, default_missing_remover_config):
    environment_settings["file_storage_s3"] = {
        "mds_host": ...,
        "access_key": ...,
        "secret_key": ...,
        "bucket": BUCKET,
        "prefix": ...,
    }

    interface = S3StorageInterface(default_missing_remover_config, environment_settings)

    resources = {
        "exist_and_registered": ("/1", datetime.now(tz=pytz.utc) - timedelta(days=10)),
        "exist_not_registered": ("/2", datetime.now(tz=pytz.utc)),
        "exist_not_registered_recently": ("/3", datetime.now(tz=pytz.utc) - timedelta(days=10)),
        "not_exist_registered": ("/4", datetime.now(tz=pytz.utc) - timedelta(days=10)),
    }
    interface.add_registered_resource(create_fake_s3_resource(mocker, resources["exist_and_registered"][0]))
    interface.add_registered_resource(create_fake_s3_resource(mocker, resources["not_exist_registered"][0]))
    assert len(interface.garden_resources) == 2

    client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.s3.boto3", autospec=True
    )

    client_patch.resource.return_value.Bucket.return_value.objects.filter.return_value = [
        FakeEntity(*resources["exist_and_registered"]),
        FakeEntity(*resources["exist_not_registered_recently"]),
        FakeEntity(*resources["exist_not_registered"]),
    ]

    interface.fetch_external_resources()
    return interface.get_missing()
