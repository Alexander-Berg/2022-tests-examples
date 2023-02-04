import pytz
from maps.garden.sdk.sandbox import SandboxResource
from maps.garden.tools.unaccounted_resources.lib.storage_interfaces.sandbox import SandboxStorageInterface
from datetime import datetime, timedelta


def create_fake_sandbox_resource(mocker, resource_id):
    fake_resource = mocker.Mock(spec=SandboxResource)
    fake_resource.resource_id = resource_id
    return fake_resource


def create_fake_entity(entity_id: int, time_created: datetime):
    return {"id": entity_id, "time": {"created": time_created.isoformat()}}


def test_add_simple_add_and_check(environment_settings, mocker, default_missing_remover_config):
    interface = SandboxStorageInterface(default_missing_remover_config, environment_settings)

    resources = {
        "exist_and_registered": (1, datetime.now(tz=pytz.utc) - timedelta(days=10)),
        "exist_not_registered_recently": (2, datetime.now(tz=pytz.utc)),
        "exist_not_registered": (3, datetime.now(tz=pytz.utc) - timedelta(days=10)),
        "not_exist_registered": (4, datetime.now(tz=pytz.utc) - timedelta(days=10)),
    }
    interface.add_registered_resource(create_fake_sandbox_resource(mocker, resources["exist_and_registered"][0]))
    interface.add_registered_resource(create_fake_sandbox_resource(mocker, resources["not_exist_registered"][0]))
    assert len(interface.garden_resources) == 2

    sandbox_client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.sandbox.SandboxStorage", autospec=True
    )
    sandbox_batch_client_patch = mocker.patch(
        "maps.garden.sdk.sandbox.batch_client.SandboxBatchTask", autospec=True
    )
    sandbox_client_patch.return_value.batch = sandbox_batch_client_patch
    sandbox_client_patch.return_value.batch.get_all_resources_infos_iter.return_value = [
        create_fake_entity(*resources["exist_and_registered"]),
        create_fake_entity(*resources["exist_not_registered_recently"]),
        create_fake_entity(*resources["exist_not_registered"]),
    ]

    interface.fetch_external_resources()
    return interface.get_missing()
