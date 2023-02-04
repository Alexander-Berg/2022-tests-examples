from paysys.sre.tools.monitorings.lib.checks.graphite import graphite_check


class TestGraphiteCheck:
    def test_graphite_check1(self):
        assert graphite_check("test", "test", 1, 2) == \
            {
                "test": {
                    "active": "graphite",
                    "active_kwargs": {
                        "CRIT": "metric > 1",
                        "WARN": "metric > 2",
                        "base_url": "https://ps-mg.yandex-team.ru",
                        "metric": "test",
                        "time_window": "-20min",
                        "timeout": 45,
                        "null_status": "WARN",
                        "ignore_errors": True,
                    },
                    "aggregator_kwargs": {
                        "nodata_mode": "skip"
                    },
                    "children": [],
                    "refresh_time": 60,
                    "meta": {
                        "urls": [
                            {
                                "title": "Graph",
                                "url": "https://ps-mg.yandex-team.ru/render/?target=test&target=constantLine(2)&target=constantLine(1)&width=1024&height=512&from=-3h",
                                "type": "screenshot_url"
                            }
                        ],
                    },
                }
            }

    def test_graphite_check2(self):
        assert graphite_check("test", "test", 1, 2, less=True) == \
            {
                "test": {
                    "active": "graphite",
                    "active_kwargs": {
                        "CRIT": "metric < 1",
                        "WARN": "metric < 2",
                        "base_url": "https://ps-mg.yandex-team.ru",
                        "metric": "test",
                        "time_window": "-20min",
                        "timeout": 45,
                        "null_status": "WARN",
                        "ignore_errors": True,
                    },
                    "aggregator_kwargs": {
                        "nodata_mode": "skip"
                    },
                    "children": [],
                    "refresh_time": 60,
                    "meta": {
                        "urls": [
                            {
                                "title": "Graph",
                                "url": "https://ps-mg.yandex-team.ru/render/?target=test&target=constantLine(2)&target=constantLine(1)&width=1024&height=512&from=-3h",
                                "type": "screenshot_url"
                            }
                        ],
                    },
                }
            }
