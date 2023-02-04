from maps.garden.sdk.resources import PythonResource


def fill_graph(graph_builder):
    graph_builder.add_resource(PythonResource('rasp_export_src'))


modules = [
    {
        'name': 'rasp_export_src',
        'fill_graph': fill_graph
    }
]
