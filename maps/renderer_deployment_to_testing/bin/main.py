from maps.garden.sdk.module_autostart import map_starters
from maps.garden.sdk.module_rpc.module_runner import run_module
from maps.garden.modules.renderer_deployment_to_testing.lib.graph import fill_graph


def main():
    run_module(
        'renderer_deployment_to_testing',
        fill_graph,
        handle_build_status=map_starters.start_with_last_configs,
    )
