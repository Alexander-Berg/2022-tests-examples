import pytest
import pytz
from datetime import datetime

from maps.garden.libs_server.build import build_defs
from maps.garden.libs_server.build.datasets_registrar import DatasetsRegistrar

NOW = datetime(2020, 12, 25, 17, 45, 00)


@pytest.mark.freeze_time(NOW)
@pytest.mark.use_local_mongo
def test_registered(db):
    module_1 = "test_module"
    module_2 = "another_module"
    contour = "test"
    dataset_module_1 = build_defs.Dataset(
        foreign_key={"task_id": "123321", "region": "aao"},
        contour_name=contour,
        module_name=module_1,
        properties={"release_name": "111111"},
        resources=[
            build_defs.BuildExternalResource(
                resource_name="test_resource",
                properties={}
            )
        ],
        updated_at=datetime.now(pytz.utc)
    )
    another_dataset_module_1 = build_defs.Dataset.parse_obj(dataset_module_1)
    another_dataset_module_1.foreign_key = {"task_id": "33333", "region": "aao"}

    dataset_module_2 = build_defs.Dataset.parse_obj(dataset_module_1)
    dataset_module_2.module_name = module_2

    registrator = DatasetsRegistrar(db)
    registrator.actualize_datasets(
        module_name=module_1,
        contour_name=contour,
        datasets=[dataset_module_1]
    )
    found_datasets = registrator.get_datasets(
        module_name=module_1,
        contour_name=contour
    )
    assert dataset_module_1 in found_datasets

    registrator.actualize_datasets(
        module_name=module_1,
        contour_name=contour,
        datasets=[another_dataset_module_1]
    )
    found_datasets = registrator.get_datasets(
        module_name=module_1,
        contour_name=contour
    )
    assert dataset_module_1 not in found_datasets
    assert another_dataset_module_1 in found_datasets

    registrator.actualize_datasets(
        module_name=module_1,
        contour_name=contour,
        datasets=[another_dataset_module_1, dataset_module_1]
    )
    found_datasets = registrator.get_datasets(
        module_name=module_1,
        contour_name=contour
    )
    assert dataset_module_1 in found_datasets
    assert another_dataset_module_1 in found_datasets

    registrator.actualize_datasets(
        module_name=module_2,
        contour_name=contour,
        datasets=[dataset_module_2]
    )
    found_datasets = registrator.get_datasets(
        module_name=module_2,
        contour_name=contour
    )
    assert dataset_module_2 in found_datasets

    found_datasets = registrator.get_datasets(
        module_name=module_1,
        contour_name=contour
    )
    assert dataset_module_1 in found_datasets
    assert another_dataset_module_1 in found_datasets
