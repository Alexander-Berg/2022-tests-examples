from maps.garden.sdk.module_autostart import reduce_starters
from maps.garden.sdk.module_rpc.module_runner import run_module
from maps.garden.modules.renderer_design_testing.lib.graph import fill_graph


def main():
    run_module(
        'renderer_design_testing',
        fill_graph,
        handle_build_status=reduce_starters.start_with_last_sources,
    )
