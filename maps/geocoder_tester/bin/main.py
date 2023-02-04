from maps.garden.modules.geocoder_tester.lib.geocoder_index_tester import fill_graph
from maps.garden.sdk.module_autostart import validator_starters
from maps.garden.sdk.module_rpc.module_runner import run_module


def main():
    run_module(
        "geocoder_tester",
        fill_graph,
        handle_build_status=validator_starters.start_validation,
    )
