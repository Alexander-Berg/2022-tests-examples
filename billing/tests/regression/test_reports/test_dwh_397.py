import luigi.date_interval as di  # noqa: F401
from dwh.grocery.dwh_397 import DWH397, ActByPage  # noqa: F401
from dwh.grocery.targets import YTTableTarget  # noqa: F401


class TestDWH397:

    # TODO стоит лишь убрать NO_CHECK_IMPORTS, так все должно будет заработать само, тест не понадобится
    def test_nothing_but_import(self):
        assert True
