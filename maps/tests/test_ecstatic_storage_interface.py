from maps.garden.sdk.ecstatic import DatasetResource
from maps.garden.tools.unaccounted_resources.lib.storage_interfaces.ecstatic import EcstaticStorageInterface

DATASET_NAME = "dataset"
CONTOUR_NAME = "datatesting"


def create_fake_ecstatic_resource(mocker, version):
    fake_resource = mocker.Mock(spec=DatasetResource)
    fake_resource.dataset_name = DATASET_NAME
    fake_resource.dataset_version = version
    fake_resource.contour_name = CONTOUR_NAME
    return fake_resource


def create_fake_entity(versions: list):
    return "\n".join((f"{DATASET_NAME}={version}" for version in versions))


def test_add_simple_add_and_check(environment_settings, mocker, default_missing_remover_config):
    environment_settings["garden"]["contour"] = CONTOUR_NAME
    interface = EcstaticStorageInterface(default_missing_remover_config, environment_settings)

    resources = {
        "exist_and_registered": '1',
        "exist_not_registered": '2',
        "exist_not_registered_recently": '3',  # it is last registered id, so we do not list it in get_missing
        "not_exist_registered": '4',
    }
    interface.add_registered_resource(create_fake_ecstatic_resource(mocker, resources["exist_and_registered"]))
    interface.add_registered_resource(create_fake_ecstatic_resource(mocker, resources["not_exist_registered"]))
    assert len(interface.garden_resources) == 2

    client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.ecstatic.EcstaticAPI", autospec=True
    )

    client_patch.return_value.make_dataset_url.return_value = "https//ecstatic"
    client_patch.return_value.dataset_versions.return_value = create_fake_entity([
        resources["exist_and_registered"],
        resources["exist_not_registered_recently"],
        resources["exist_not_registered"]
    ])

    interface.fetch_external_resources()
    return interface.get_missing()
