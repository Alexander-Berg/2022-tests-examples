from maps.garden.sdk.module_autostart import validator_starters
from maps.garden.modules.masstransit_tester.lib import graph
from maps.garden.sdk.module_rpc.module_runner import run_module


def main():
    run_module(
        "masstransit_tester",
        graph.fill_graph,
        handle_build_status=validator_starters.start_if_validated,
    )
