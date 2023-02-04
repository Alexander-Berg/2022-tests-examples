from paysys.sre.tools.monitorings.lib.checks.awacs import l7_monitoring
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children


host = "trust.test.l7"

children = empty_children

l7_balancers = [
    {
        "namespace": "trust-emulator-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-emulator-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8303],
        "services": [
            "rtc_balancer_trust-emulator-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-emulator-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [443],
        "services": [
            "rtc_balancer_trust-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-payments-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-payments-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8028],
        "services": [
            "rtc_balancer_trust-payments-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-payments-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-payments-old-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-payments-old-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8027],
        "services": [
            "rtc_balancer_trust-payments-old-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-payments-old-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-payments-xg-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-payments-xg-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8038],
        "services": [
            "rtc_balancer_trust-payments-xg-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-payments-xg-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-paysys-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-paysys-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8025],
        "services": [
            "rtc_balancer_trust-paysys-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-paysys-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-lpm-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-lpm-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [8031],
        "services": [
            "rtc_balancer_trust-lpm-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-lpm-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-directory-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "trust-directory-test.paysys.yandex.net",
        "http_ports": [],
        "https_ports": [443],
        "services": [
            "rtc_balancer_trust-directory-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-directory-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "trust-assistant-test-l7.paysys.yandex.net",
        "datacenters": ["sas", "vla"],
        "host": "assistant.trust.test.yandex.net",
        "http_ports": [],
        "https_ports": [443],
        "services": [
            "rtc_balancer_trust-assistant-test-l7_paysys_yandex_net_sas",
            "rtc_balancer_trust-assistant-test-l7_paysys_yandex_net_vla",
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "scheduler.trust.test.yandex.net",
        "datacenters": ["myt", "sas", "vla"],
        "host": "scheduler.trust.test.yandex.net",
        "http_ports": [],
        "https_ports": [443],
        "services": [
            "rtc_balancer_scheduler_trust_test_yandex_net_myt",
            "rtc_balancer_scheduler_trust_test_yandex_net_sas",
            "rtc_balancer_scheduler_trust_test_yandex_net_vla"
        ],
        "checks": {
            "cpu_usage": {"warn": 60, "crit": 80},
            "cpu_wait": {"warn": 0.5, "crit": 1},
            "codes_5xx": {"warn": 0.5, "crit": 1},
            "attempts_backend_errors": {"warn": 1, "crit": 2},
        },
    },
    {
        "namespace": "callback.trust.test.yandex.net",
        "datacenters": ["myt", "sas", "vla"],
        "host": "callback.trust.test.yandex.net",
        "http_ports": [],
        "https_ports": [443],
        "services": [
            "rtc_balancer_callback_trust_test_yandex_net_myt",
            "rtc_balancer_callback_trust_test_yandex_net_sas",
            "rtc_balancer_callback_trust_test_yandex_net_vla"
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
    return l7_monitoring(l7_balancers)
