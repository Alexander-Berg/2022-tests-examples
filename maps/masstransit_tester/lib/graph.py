from . import masstransit_data_validation
from . import masstransit_validation


def fill_graph(graph_builder, regions=None):
    masstransit_data_validation.fill_graph(graph_builder, regions)
    masstransit_validation.fill_graph(graph_builder, regions)
