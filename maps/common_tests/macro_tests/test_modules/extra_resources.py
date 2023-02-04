from maps.garden.sdk.resources import PythonResource


def fill_graph(gb):
    gb.add_resource(
        PythonResource("path/like/name"))


modules = [{
    "name": "extra_resources",
    "fill_graph": fill_graph
}]
