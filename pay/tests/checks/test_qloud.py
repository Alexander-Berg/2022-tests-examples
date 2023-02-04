from paysys.sre.tools.monitorings.lib.checks import qloud


def test_base_checks(monkeypatch):
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.checks.active.http.http",
        lambda *args, **kwargs: {"http_test_project.test_app": {}}
    )
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.checks.active.http.https",
        lambda *args, **kwargs: {"https_test_project.test_app": {"children": ["test_qloud"]}}
    )
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.checks.services.pushclient_check",
        dict(),
    )
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.util.helpers.gen_children_qloud",
        lambda *args, **kwargs: {"children": ["test_qloud"]}
    )
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.checks.base.unreachable",
        dict(),
    )
    monkeypatch.setattr(
        "yasm_alert.Tags",
        lambda *args, **kwargs: dict()
    )
    monkeypatch.setattr(
        "paysys.sre.tools.monitorings.lib.checks.doc.doc",
        lambda *args, **kwargs: dict()
    )
    app = "test_project.test_app"
    children = "test_project.test_app.testing.test_app_component@type=ext"
    balancer = "qloud.balancer.paysys-int-test.balancer-l3@type=ext"
    service = "test_project.test_app"
    hostname = "test_app-testing.paysys.yandex.net"
    env = "testing"
    path = "/ping"
    app_port = 12345
    http_error_warn = 6.0 / 60
    http_error_crit = 1.0 / 60
    http_499_warn = 2.0 / 60
    http_499_crit = 3.0 / 60
    http_5xx_warn = 4.0 / 60
    http_5xx_crit = 5.0 / 60

    check = qloud.base_checks(
        app,
        children,
        balancer,
        service,
        hostname,
        env,
        path,
        app_port,
        http_error_warn=http_error_warn,
        http_error_crit=http_error_crit,
        http_499_warn=http_499_warn,
        http_499_crit=http_499_crit,
        http_5xx_warn=http_5xx_warn,
        http_5xx_crit=http_5xx_crit,
    )
    correct_check = {
        'https_test_project.test_app': {
            'children': [
                "test_qloud",
            ],
            'tags': [
                'qloud-ext.test_project.test_app.testing'
            ]
        },
        'http_test_project.test_app': {
        },
        'pushclient_check': {
            'children': [
                "test_qloud",
            ],
            'tags': [
                'qloud-ext.test_project.test_app.testing'
            ]
        },
        'UNREACHABLE': {
            'children': [
                "test_qloud",
            ],
        },
        'test_project.test_app': {
            'tags': [
                'qloud-ext.test_project.test_app.testing'
            ],
            'children': [
                "test_qloud",
            ],
        },
        "http_499": {
            "yasm": {
                "signal": "push-http_499_summ",
                "tags": {},
                "warn": [http_499_warn, http_499_warn],
                "crit": [http_499_crit, None],
                "value_modify": {'type': 'summ', 'window': 60},
                "mgroups": ["QLOUD"],
            },
            "tags": [
                "qloud-balancer",
            ]
        },
        "http_error": {
            "yasm": {
                "signal": "push-proxy_errors_summ",
                "tags": {},
                "warn": [http_error_warn, http_error_warn],
                "crit": [http_error_crit, None],
                "value_modify": {'type': 'summ', 'window': 60},
                "mgroups": ["QLOUD"],
            },
            "tags": [
                "qloud-balancer",
            ]
        },
        "http_5xx": {
            "yasm": {
                "signal": "push-response_5xx_summ",
                "tags": {},
                "warn": [http_5xx_warn, http_5xx_warn],
                "crit": [http_5xx_crit, None],
                "value_modify": {'type': 'summ', 'window': 60},
                "mgroups": ["QLOUD"],
            },
            "tags": [
                "qloud-balancer",
            ]
        },
    }
    assert check == correct_check
