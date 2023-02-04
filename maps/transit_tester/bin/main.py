from maps.garden.modules.transit_tester.lib.transit_tester import fill_graph
from maps.garden.sdk.module_autostart import validator_starters
from maps.garden.sdk.module_rpc.module_runner import run_module


def main():
    run_module(
        "transit_tester",
        fill_graph,
        handle_build_status=validator_starters.start_validation,
    )
