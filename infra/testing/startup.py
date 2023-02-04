import infra.callisto.deploy.deployer.config.config_pb2 as config_pb2  # noqa

import helper


def test_inaccessible_targets():
    with helper.Helper() as h:
        h.configure(config_pb2.TConfig(Startup=config_pb2.TStartup(TargetReadDeadline=1500)))
        h.use_target_table()
        h.run_deployer(wait_until_ready=True)


# First download smth, then restart with broken targets and check that nothing was removed.
def wannabe_test_preserving_downloaded():
    pass  # Helper doesn't support restarts for now.
