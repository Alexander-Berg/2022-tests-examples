from yatest.common import source_path

import os.path

HOSTS_CONFIG = source_path("maps/analyzer/services/jams_analyzer/modules/usershandler/config/usershandler-hosts.conf")


def test_configs():
    # Just check if configs for different stagings exists
    def check_staging(staging):
        config_path = HOSTS_CONFIG + "." + staging
        assert os.path.exists(config_path), "'{}' staging config should exist: {}".format(staging, config_path)

    check_staging("load")
    check_staging("testing")
    check_staging("stable")
