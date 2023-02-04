from maps.garden.sdk.module_autostart import deploy_starters
from maps.garden.sdk.module_rpc.module_runner import run_module
from maps.garden.modules.renderer_design_testing_deploy.lib.graph import fill_graph


def main():
    run_module(
        'renderer_design_testing_deploy',
        fill_graph,
        handle_build_status=deploy_starters.deploy_instantly,
    )
