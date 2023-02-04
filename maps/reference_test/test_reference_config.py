import datetime
import os

import yatest.common

from maps.routing.matrix_router.data_preparation.tools.tune_runtime_config.lib \
    import tune_runtime_config


def test_reference_config():
    test_path = (
        "maps/routing/matrix_router/data_preparation/tools/tune_runtime_config/"
        "tests/reference_test"
    )

    input_runtime_config_path = yatest.common.source_path(
        os.path.join(test_path, "input_runtime_config.json"))
    reference_runtime_config_path = yatest.common.source_path(
        os.path.join(test_path, "reference_patched_runtime_config.json"))
    tune_config_path = yatest.common.source_path(
        os.path.join(test_path, "tune.yaml"))
    geodata_file_path = yatest.common.binary_path("geobase/data/v6/geodata6.bin")
    patched_runtime_config_path = yatest.common.work_path(
        "patched_runtime_config.json")
    current_datetime = datetime.datetime(
        year=2000, month=1, day=1, tzinfo=datetime.timezone.utc)

    with open(input_runtime_config_path, "r") as file:
        input_runtime_config_content = file.read()
    with open(tune_config_path, "r") as file:
        tune_config_content = file.read()

    patched_runtime_config_content = tune_runtime_config.patch_runtime_config(
        runtime_config_content=input_runtime_config_content,
        tune_config_content=tune_config_content,
        geodata_path=geodata_file_path,
        current_datetime=current_datetime)
    patched_runtime_config_content += "\n"

    with open(patched_runtime_config_path, "w") as file:
        file.write(patched_runtime_config_content)

    with open(reference_runtime_config_path, "r") as file:
        reference_runtime_config_content = file.read()
    assert patched_runtime_config_content == reference_runtime_config_content
