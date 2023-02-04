from maps.garden.sdk.module_rpc import module_runner, callstack

from . import graph


def test_graph_collector():
    collector = module_runner._GraphCollector()

    graph.fill_graph(collector, [])

    for proto_task in collector.proto_tasks:
        inserted_from = callstack.decode_traceback_from_proto(proto_task.insertedFrom)
        assert inserted_from[0].name == "fill_graph"
