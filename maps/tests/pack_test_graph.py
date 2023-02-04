# -*- coding: utf-8 -*-
#

import os

from maps.garden.sdk.core import Version
from maps.garden.sdk.core.test_utils import *
from maps.garden.sdk.core.modules.graph_build.config import FILE_NAMES
from yandex.maps.utils.system import create_dir

from maps.garden.sdk.core.modules.driving_cache_builder.config import *

from maps.garden.libs.road_graph_builder import common

SOURCES = [
    common.rg_build_resource_name(common.COMPACT_FILE),
    common.rg_build_resource_name(common.COMPACT_RTREE_FILE),
    common.rg_build_resource_name(common.COMPACT_PERSISTENT_INDEX_FILE),
]

TEST_GRAPH4_DIR = "/usr/share/yandex/maps/test-graph4"


def main():

    version = Version(properties={"release": "local_execute"})
    with create_test_environment() as environment_settings:
        cook = GraphCook(environment_settings)
        load_input_module(cook, "graph_build")
        load_target_module(cook, "driving_cache_builder")
        for src in SOURCES:
            resource = cook.create_input_resource(src)
            resource.version = version
            with open(os.path.join(TEST_GRAPH4_DIR, FILE_NAMES[src])) as src_data:
                with resource.open("w") as res_data:
                    res_data.write(src_data.read())

        cook.create_build_params_resource(properties={"release_name": "local_execute"})

        execute(cook)

if __name__ == "__main__":
    main()
