import pytest

from maps.infra.ecstatic.tool.ecstatic_api import ecstatic_api

from maps.garden.sdk.core import Version

from maps.garden.sdk.ecstatic import resources
from maps.garden.sdk.ecstatic import tasks

from maps.garden.sdk.test_utils.ecstatic import DatasetFile


CONTENT = b"content"
FILENAME = "hello.txt"
DATASET = "dataset"
VERSION = "20.11.07-1"


def prepare_data(coordinator, tmp_path, branch):
    datafile = tmp_path / FILENAME
    datafile.write_bytes(CONTENT)

    coordinator.upload_dataset(
        DATASET,
        VERSION,
        tmp_path,
        branches=["+testing/hold"]
    )


def test_simple(ecstatic_mock, tmp_path):
    coordinator = ecstatic_api.EcstaticAPI(
        tvm_client_id="client_id",
        tvm_secret="secret",
    )

    ecstatic_mock.register_dataset(DATASET, ["testing", "prestable"])

    prepare_data(coordinator, tmp_path, "+testing/hold")

    assert ecstatic_mock._dataset_versions[DATASET][VERSION][0].filename == FILENAME
    assert ecstatic_mock._dataset_versions[DATASET][VERSION][0].content == CONTENT

    coordinator.download_datasets([(DATASET, VERSION, tmp_path / "output")])

    assert (tmp_path / "output" / FILENAME).read_bytes() == CONTENT

    assert coordinator.dataset_versions(DATASET) == f"{DATASET}={VERSION}"

    assert coordinator.dataset_deploy_status(DATASET) == f"""Dataset:Tag=Version\tBranch\t#Clients\t#Downloaded\t#Active\t#Error\tGroup states
{DATASET}={VERSION}\ttesting\t1\t1\t1\t0\t[R]
{DATASET}={VERSION}\tprestable\t1\t1\t1\t0\t[-]

"""

    coordinator.move_dataset_version(DATASET, VERSION, "+prestable")

    assert coordinator.dataset_deploy_status(DATASET) == f"""Dataset:Tag=Version\tBranch\t#Clients\t#Downloaded\t#Active\t#Error\tGroup states
{DATASET}={VERSION}\ttesting\t1\t1\t1\t0\t[R]
{DATASET}={VERSION}\tprestable\t1\t1\t1\t0\t[A]

"""

    coordinator.move_dataset_version(DATASET, VERSION, "-testing")

    assert coordinator.dataset_deploy_status(DATASET) == f"""Dataset:Tag=Version\tBranch\t#Clients\t#Downloaded\t#Active\t#Error\tGroup states
{DATASET}={VERSION}\ttesting\t1\t1\t1\t0\t[-]
{DATASET}={VERSION}\tprestable\t1\t1\t1\t0\t[A]

"""

    coordinator.remove_dataset_version(DATASET, VERSION)
    assert coordinator.dataset_versions(DATASET) == ""


def test_remove_cases(ecstatic_mock, tmp_path):
    coordinator = ecstatic_api.EcstaticAPI(
        tvm_client_id="client_id",
        tvm_secret="secret",
    )

    ecstatic_mock.register_dataset(DATASET, ["testing", "prestable"])

    prepare_data(coordinator, tmp_path, "+testing/hold")

    with pytest.raises(Exception):
        coordinator.remove_dataset_version("unknown-dataset", VERSION)

    # remove unknown version is ok
    coordinator.remove_dataset_version(DATASET, "unknown-version")


def test_move_cases(ecstatic_mock, tmp_path):
    coordinator = ecstatic_api.EcstaticAPI(
        tvm_client_id="client_id",
        tvm_secret="secret",
    )

    ecstatic_mock.register_dataset(DATASET, ["testing", "prestable"])

    prepare_data(coordinator, tmp_path, "+testing/hold")

    with pytest.raises(Exception):
        coordinator.move_dataset_version("unknown-dataset", VERSION, "+stable")

    with pytest.raises(Exception):
        coordinator.move_dataset_version(DATASET, "unknown-version", "+stable")

    with pytest.raises(Exception):
        coordinator.move_dataset_version(DATASET, VERSION, "badbranch")

    # remove from unknown branch is ok
    coordinator.move_dataset_version(DATASET, VERSION, "-unknownbranch")

    # move to unknown branch is ok
    coordinator.move_dataset_version(DATASET, VERSION, "+unknownbranch")


def test_upload_task(ecstatic_mock, tmp_path):
    ecstatic_mock.register_dataset(DATASET, branches=["testing"])

    datafile = tmp_path / FILENAME
    datafile.write_bytes(CONTENT)

    class DirResourceMock:
        def path(self):
            return tmp_path

    resource = resources.DatasetResource(
        name="resource_name",
        dataset_name_template=DATASET,
        dataset_version_template="{dataset_version}"
    )
    resource.version = Version(properties={
        "dataset_version": VERSION
    })

    task = tasks.UploadDatasetTask(branch="testing", hold=True)
    task(DirResourceMock(), resource)

    assert ecstatic_mock.get_dataset_version(DATASET, VERSION) == [
        DatasetFile(filename=FILENAME, content=CONTENT)
    ]


def test_activate_task(ecstatic_mock, tmp_path):
    ecstatic_mock.register_dataset(DATASET, branches=["testing", "stable"])
    ecstatic_mock.create_dataset_version(
        DATASET, VERSION, files=[DatasetFile(filename=FILENAME, content=CONTENT)])

    resource = resources.DatasetResource(
        name="resource_name",
        dataset_name_template=DATASET,
        dataset_version_template="{dataset_version}"
    )
    resource.version = Version(properties={
        "dataset_version": VERSION
    })
    resource.load_environment_settings({"tvm": {"secret": "secret", "client_id": "123"}})
    resource.save_yandex_environment()
    resource.logged_commit()
    resource.calculate_size()

    task = tasks.ActivateTask(branch="stable")
    task(ecstatic_dataset=resource)

    assert ecstatic_mock.get_dataset_branch(DATASET, "stable").active_version == VERSION
