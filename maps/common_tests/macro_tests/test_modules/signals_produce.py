from maps.garden.sdk.resources import PythonResource


def fill_graph(gb):
    gb.add_resource(PythonResource("source"))

modules = [{
    "name": "signals_produce",
    "fill_graph": fill_graph
    }]
