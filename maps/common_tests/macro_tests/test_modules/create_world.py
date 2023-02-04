from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common


def fill_graph(gb):
    world = PythonResource(
        name="world",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("australia_shrinked", "europe_expanded"),
        core.Creates(world="world"),
        common.CreateWorld())


def unique_fill_graph(gb):
    world = PythonResource(
        name="unique_world",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("australia_shrinked", "europe_expanded"),
        core.Creates(world="unique_world"),
        common.CreateWorld())


def world_with_params_fill_graph(gb):
    world = PythonResource(
        name="world_with_params",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("build_params", "australia_shrinked", "europe_expanded"),
        core.Creates(world="world_with_params"),
        common.CreateWorldWithParams())


def fill_deployment_graph(gb):
    world = PythonResource(
        name="world_deployment",
        value={"area": 0, "region_names": []}
    )
    gb.add_resource(world)

    gb.add_task(
        core.Demands("world"),
        core.Creates("world_deployment"),
        common.CreateDeploymentWorld())


modules = [
    {
        "name": "world_creator",
        "fill_graph": fill_graph
    },
    {
        "name": "build_with_propagated_properties",
        "fill_graph": fill_graph
    },
    {
        "name": "build_with_different_common_property",
        "fill_graph": fill_graph
    },
    {
        "name": "unique_world_creator",
        "fill_graph": unique_fill_graph
    },
    {
        "name": "world_with_params_creator",
        "fill_graph": world_with_params_fill_graph
    },
    {
        "name": "world_creator_deployment",
        "fill_graph": fill_deployment_graph
    }
]
