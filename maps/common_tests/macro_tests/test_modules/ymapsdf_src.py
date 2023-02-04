from maps.garden.sdk.resources.python import PythonResource


def fill_graph(gb):
    for region in ("australia", "europe", "europe_east", "europe_west"):
        gb.add_resource(PythonResource(
            name=f"{region}_src",
            value={"areas": [], "region_name": region},
        ))


modules = [{
    "name": "ymapsdf_src",
    "fill_graph": fill_graph
}]
