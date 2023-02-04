import mock
import unittest

from maps.pylibs.yt.lib import YtContext

import maps.carparks.tools.carparks_miner.lib.common as tested_module


class CreateTests(unittest.TestCase):
    def test_for_nonexisting_table(self):
        ytc = mock.MagicMock(spec_set=YtContext)
        ytc.exists.return_value = False

        creator = mock.MagicMock(return_value="temp")

        tested_module.create(ytc, "table", creator)

        ytc.exists.assert_called_once_with("table")
        creator.assert_called_once_with()
        ytc.copy.assert_called_once_with("temp", "table", force=True)

    def test_for_existing_table(self):
        ytc = mock.MagicMock(spec_set=YtContext)
        ytc.exists.return_value = True

        creator = mock.MagicMock()

        tested_module.create(ytc, "table", creator)

        ytc.exists.assert_called_once_with("table")
        creator.assert_not_called()
        ytc.copy.assert_not_called()

    def test_overwrite_existing_table(self):
        ytc = mock.MagicMock(spec_set=YtContext)
        ytc.exists.return_value = True

        creator = mock.MagicMock(return_value="temp")

        tested_module.create(ytc, "table", creator, overwrite_if_exists=True)

        ytc.exists.assert_not_called()
        creator.assert_called_once_with()
        ytc.copy.assert_called_once_with("temp", "table", force=True)
