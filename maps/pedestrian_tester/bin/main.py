from maps.garden.modules.pedestrian_tester.lib import pedestrian_validation
from maps.garden.sdk.module_autostart import validator_starters
from maps.garden.sdk.module_rpc.module_runner import run_module


def main():
    run_module(
        "pedestrian_tester",
        pedestrian_validation.fill_graph,
        handle_build_status=validator_starters.start_validation,
    )
