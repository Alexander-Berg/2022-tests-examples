from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.configs.whitespirit.base import whitespirit
from paysys.sre.tools.monitorings.lib.checks.active.http import https_bundle

host = "whitespirit-test.whitespirit-test"

children = ['whitespirit-test']


def checks():
    return merge(
        whitespirit.get_checks(children),
        {'mem-free': {
            "aggregator": "timed_more_than_limit_is_problem",
            "aggregator_kwargs": {
                "limits": [
                    {
                        "crit": 0,
                        "day_end": 7,
                        "day_start": 1,
                        "time_end": 23,
                        "time_start": 0,
                        "warn": 0
                    }
                ],
                "nodata_mode": "skip"
            }
        }},
        https_bundle(
            'balance-hudsucker',
            port=8081,
            headers={'Host': 'balance-hudsucker.paysys.yandex.net'},
        ),
    )
