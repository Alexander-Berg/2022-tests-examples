#!/usr/bin/python

from argparse import ArgumentParser
from itertools import izip
import sys

from graph import graph, check_graph_version, edges_geometry
from maps import router_edges_json, router_edges
from snap_route import snap_route


def main():
    parser = ArgumentParser(
        add_help=True,
        description="""Tests snap_route.py. Reads queries from stdin""")
    parser.add_argument(
        "--router_host", type=str, default="router.maps.yandex.net")
    args = parser.parse_args()

    for query in sys.stdin:
        print "Snapping", query
        router_json = router_edges_json(query, args.router_host)
        check_graph_version(router_json["graph_version"])

        edges = router_edges(router_json)
        snapped_edges = snap_route(edges_geometry(edges))
        index = graph.edges_index()
        assert len(edges) == len(snapped_edges)
        assert all(edge in index.same_base(snapped_edge)
            for edge, snapped_edge in izip(edges, snapped_edges))


if __name__ == "__main__":
    main()
