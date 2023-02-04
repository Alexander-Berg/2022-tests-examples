from maps.garden.sdk import core
from maps.garden.sdk.resources.python import PythonResource

from . import common

from maps.garden.common_tests.test_utils.constants import INVALID_SHIPPING_DATE


class Validate(core.Task):
    def __call__(self, continent, stub):
        # The resource must not exist for some tests (e.g. test_builds_list), so
        # we use shipping_date property to mark it as invalid
        shipping_date = continent.version.properties.get("shipping_date")
        if shipping_date == INVALID_SHIPPING_DATE:
            raise RuntimeError("Revolutionary shipping date")


def fill_graph_for_region(gb, region):
    continent = PythonResource(
        name=region,
        value={"area": 0, "region_name": region}
    )
    gb.add_resource(continent)
    continent_expanded = PythonResource(
        name=f"{region}_expanded",
        value={"area": 0, "region_name": region}
    )
    gb.add_resource(continent_expanded)

    gb.add_task(
        core.Demands("{0}_src".format(region)),
        core.Creates(region),
        common.UniteSource(interval=2.0)
    )
    gb.add_task(
        core.Demands(region),
        core.Creates("{0}_expanded".format(region)),
        common.Multiply(ratio=2.0, interval=2.0)
    )


def fill_graph_for_australia(gb):
    continent = PythonResource(
        name="australia",
        value={"area": 0, "region_name": "australia"}
    )
    gb.add_resource(continent)
    continent_expanded = PythonResource(
        name="australia_shrinked",
        value={"area": 0, "region_name": "australia"}
    )
    gb.add_resource(continent_expanded)

    gb.add_task(
        core.Demands("australia_src"),
        core.Creates("australia"),
        common.UniteSource(interval=2.0)
    )
    gb.add_task(
        core.Demands("australia"),
        core.Creates("australia_shrinked"),
        # China buys the dessert in the middle
        common.Multiply(ratio=0.5, interval=2.0),
    )

    region = "australia"
    gb.add_resource(PythonResource(f"{region}_src_validation_stub"))
    gb.add_task(
        core.Demands(continent=f"{region}_src"),
        core.Creates(stub=f"{region}_src_validation_stub"),
        Validate(),
    )


def fill_graph(gb):
    for region in ("europe", "europe_east", "europe_west"):
        fill_graph_for_region(gb, region)
    fill_graph_for_australia(gb)


modules = [{
    "name": "ymapsdf",
    "fill_graph": fill_graph
}]
