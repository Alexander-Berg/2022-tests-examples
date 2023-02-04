from paysys.sre.tools.monitorings.configs.trust.base import timeline_sox

host = "trust.test.timeline-sox"

children = ["paysys-timeline-sox@stage=timeline-sox-test-stage"]

l7_balancers = [
    {
        "namespace": "sox-agent-api.timeline-test.paysys.yandex-team.ru",
        "datacenters": ["man", "sas"],
        "host": "sox-agent-api.timeline-test.paysys.yandex-team.ru",
        "http_ports": [80],
        "https_ports": [443],
        "services": [
            "rtc_balancer_sox-agent-api_timeline-test_paysys_yandex-team_ru_man",
            "rtc_balancer_sox-agent-api_timeline-test_paysys_yandex-team_ru_sas",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
]


def checks():
    return timeline_sox.get_checks(children, l7_balancers)
