import os
import shutil
import tarfile

from yatest.common import source_path

from maps.garden.sdk import test_utils
from maps.garden.libs.basemap_icons.lib import graph
from maps.garden.libs.stylerepo.test_utils import test_utils as stylerepo_test_utils
from maps.garden.libs.stylerepo.bundle_info import BundleInfo
from maps.garden.modules.renderer_map_stable_bundle.lib import (
    bundle_info as stable_map_bundle_info,
    graph as stable_map_bundle_graph,
)


DATA_PATH = "maps/garden/libs/basemap_icons/tests/data"
OUTPUT_RESOURCE = "rendered_icons"


def _compare_tars(output_path, expected_path):
    output_tar = tarfile.open(output_path)
    expected_tar = tarfile.open(expected_path)
    assert set(output_tar.getnames()) == set(expected_tar.getnames())
    for name in output_tar.getnames():
        output_icon = output_tar.extractfile(name).read()
        expected_icon = expected_tar.extractfile(name).read()
        assert output_icon == expected_icon


def _check_rendered_icons(resources):
    icons_tar_resource = resources[OUTPUT_RESOURCE]

    icons_tar_resource.ensure_available()
    assert icons_tar_resource.version.properties["bundle_revision"] == "42"

    expected_path = source_path(os.path.join(DATA_PATH, "expected/icons.tar"))
    if os.getenv("YA_MAPS_CANONIZE_TESTS"):
        shutil.copyfile(icons_tar_resource.path(), expected_path)
    else:
        _compare_tars(icons_tar_resource.path(), expected_path)


def test_workflow(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    bi = stable_map_bundle_info.get_map_bundle_info()
    # Do not create resource for 'maps' stylesheet in create_design_input_resources because it is unused
    bundle_info = BundleInfo(
        service=bi.service,
        stylesets=[],
        precompiled_extruding=bi.precompiled_extruding,
        stage=bi.stage,
    )

    stable_map_bundle_graph.fill_graph(cook.input_builder(), regions=None)
    graph.fill_render_icons_graph(
        cook.target_builder(),
        bundle_info,
        OUTPUT_RESOURCE,
    )

    stylerepo_test_utils.create_design_input_resources(
        cook,
        bundle_info,
        revision="42",
        design_filename="",
        icons_dir_or_tar=source_path(os.path.join(DATA_PATH, "input/icons")),
        cartograph_url="",
    )

    result_resources = test_utils.execute(cook)
    _check_rendered_icons(result_resources)
