from subprocess import check_call

import sys

if __name__ == "__main__":
    (
        create_topology,
        create_data,
        create_data_patch,
        verify_shortcuts,
        road_graph,
        topology,
        data,
        persistent,
        turn_penalties,
        jams_pb,
        closures_pb,
        l_topo,
        l_data,
        l_patch
    ) = sys.argv[1:]

    check_call([
        create_topology,
        "--partition", "5 2 5",
        "--road-graph", road_graph,
        "--result", l_topo])
    check_call([
        create_data,
        "--topology", l_topo,
        "--road-graph", road_graph,
        "--result", l_data,
        "--do-not-lock-memory"])
    check_call([
        verify_shortcuts, 
        "--topology", l_topo, 
        "--data", l_data, 
        "--road-graph", road_graph])
    check_call([
        create_data_patch,
        "--topology", l_topo,
        "--road-graph", road_graph,
        "--edges-persistent-index", persistent,
        "--profiles", jams_pb,
        "--closures", closures_pb,
        "--base", l_data,
        "--fb", l_patch,
        "--do-not-lock-memory"])
    check_call([
        verify_shortcuts,
        "--topology", l_topo,
        "--data", l_data,
        "--road-graph", road_graph,
        "--patch", l_patch])
