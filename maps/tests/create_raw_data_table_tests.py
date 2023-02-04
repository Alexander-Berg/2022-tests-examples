import mock
import unittest

from maps.pylibs.yt.lib import YtContext

import maps.carparks.tools.carparks_miner.lib.create_raw_data_table \
    as tested_module


# YqlClient is imported as "from .. import YqlClient",
# therefore we need to patch it in module, not in original library
@mock.patch(tested_module.__name__ + ".YqlClient")
class RemoveRedundantPointsTests(unittest.TestCase):
    # we do not check result, just check that code runs
    def test_ok(self, yql_client_mock):
        ytc = mock.MagicMock(spec_set=YtContext)
        ytc.create_temp_table.return_value = "temp_table"
        yql_client_mock.return_value.query.return_value\
            .get_results.return_value.is_success = True

        tested_module.create_raw_data_table(
            ytc, "metrica_table", "analyzer_table")

        yql_client_mock.return_value.query.return_value.run.assert_called_once()

    def test_failure(self, yql_client_mock):
        ytc = mock.MagicMock(spec_set=YtContext)
        ytc.create_temp_table.return_value = "temp_table"
        yql_client_mock.return_value.query.return_value\
            .get_results.return_value.is_success = False

        self.assertRaises(Exception,
                          tested_module.create_raw_data_table,
                          ytc,
                          "metrica_table",
                          "analyzer_table")
